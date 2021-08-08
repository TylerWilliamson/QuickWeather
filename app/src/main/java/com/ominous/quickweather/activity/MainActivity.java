/*
 *     Copyright 2019 - 2021 Tyler Williamson
 *
 *     This file is part of QuickWeather.
 *
 *     QuickWeather is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     QuickWeather is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with QuickWeather.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.ominous.quickweather.activity;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Rect;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;
import android.widget.FrameLayout;

import com.google.android.material.snackbar.Snackbar;
import com.ominous.quickweather.R;
import com.ominous.quickweather.card.RadarCardView;
import com.ominous.quickweather.data.WeatherDatabase;
import com.ominous.quickweather.dialog.TextDialog;
import com.ominous.quickweather.util.ColorUtils;
import com.ominous.quickweather.util.DialogUtils;
import com.ominous.quickweather.util.Logger;
import com.ominous.quickweather.util.NotificationUtils;
import com.ominous.quickweather.util.SnackbarUtils;
import com.ominous.quickweather.util.WeatherPreferences;
import com.ominous.quickweather.view.WeatherCardRecyclerView;
import com.ominous.quickweather.view.WeatherNavigationView;
import com.ominous.quickweather.weather.Weather;
import com.ominous.quickweather.weather.WeatherLocationManager;
import com.ominous.quickweather.weather.WeatherModel;
import com.ominous.quickweather.weather.WeatherResponse;
import com.ominous.quickweather.work.WeatherWorkManager;
import com.ominous.tylerutils.browser.CustomTabs;
import com.ominous.tylerutils.util.StringUtils;
import com.ominous.tylerutils.util.WindowUtils;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import fi.iki.elonen.NanoHTTPD;

//TODO contentDescription EVERYWHERE
//TODO More logging
//TODO Remove unnecessary string resources
//TODO Find any strings that need to be translated
public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener, WeatherNavigationView.OnDefaultLocationSelectedListener {
    public static final String EXTRA_ALERT = "EXTRA_ALERT";
    public static final String ACTION_OPENALERT = "com.ominous.quickweather.ACTION_OPENALERT";
    private static final String TAG = "MainActivity";
    private WeatherCardRecyclerView weatherCardRecyclerView;
    private DrawerLayout drawerLayout;
    private WeatherNavigationView navigationView;
    private RadarCardView radarCardView;
    private ActionBarDrawerToggle drawerToggle;
    private SwipeRefreshLayout swipeRefreshLayout;
    private CoordinatorLayout coordinatorLayout;
    private FrameLayout fullscreenContainer;
    private Toolbar toolbar;
    private Snackbar obtainingLocSnackbar, backLocPermDeniedSnackbar, switchToOwmSnackbar, locPermDeniedSnackbar, locDisabledSnackbar;

    private FullscreenHelper fullscreenHelper;
    private FileWebServer fileWebServer;
    private MainViewModel mainViewModel;

    private final ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), r -> this.getWeather());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        QuickWeather.setMainActivity(this);

        ColorUtils.initialize(this);//Initializing after Activity created to get day/night properly

        if (WeatherPreferences.isInitialized()) {
            setContentView(R.layout.activity_main);
            initViews();
            initViewModel();

            onReceiveIntent(getIntent());
        } else {
            ContextCompat.startActivity(this, new Intent(this, SettingsActivity.class), null);
            finish();
        }

        fileWebServer = new FileWebServer(this, 4234);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        onReceiveIntent(intent);
    }

    private void onReceiveIntent(Intent intent) {
        String action;
        if ((action = intent.getAction()) != null) {
            switch (action) {
                case Intent.ACTION_VIEW:
                    WeatherPreferences.WeatherLocation weatherLocation;

                    if (intent.getScheme().equals("geo") &&
                            (weatherLocation = getWeatherLocationFromGeoUri(intent.getDataString())) != null) {
                        ContextCompat.startActivity(this, new Intent(this, SettingsActivity.class)
                                .putExtra(SettingsActivity.EXTRA_SKIP_WELCOME, true)
                                .putExtra(SettingsActivity.EXTRA_WEATHERLOCATION, weatherLocation), null);
                    }
                    break;
                case ACTION_OPENALERT:
                    Bundle bundle;
                    WeatherResponse.Alert alert;

                    if ((bundle = intent.getExtras()) != null &&
                            (alert = (WeatherResponse.Alert) bundle.getSerializable(EXTRA_ALERT)) != null) {
                        DialogUtils.showDialogForAlert(this, alert);
                        WeatherDatabase.getInstance(this).insertAlert(alert);
                    }
                    break;
            }
        }
    }

    private WeatherPreferences.WeatherLocation getWeatherLocationFromGeoUri(String geoUri) {
        Matcher matcher;

        //geo:0,0?q=37.78918,-122.40335
        matcher = Pattern.compile("geo:0,0\\?.*?q=([0-9.\\-]+),([0-9.\\-]+).*").matcher(geoUri);
        if (matcher.matches()) {
            try {
                String latStr = matcher.group(1);
                String lonStr = matcher.group(2);
                double lat = Double.parseDouble(latStr == null ? "0" : latStr);
                double lon = Double.parseDouble(lonStr == null ? "0" : lonStr);

                if (lat != 0 && lon != 0) {
                    return new WeatherPreferences.WeatherLocation(null, lat, lon);
                }
            } catch (Throwable t) {
                //Pattern did not match, should never happen
            }
        }

        //geo:0,0?q=my+street+address
        matcher = Pattern.compile("geo:0,0\\?.*?q=([^&]*).*").matcher(geoUri);
        if (matcher.matches()) {
            try {
                return new WeatherPreferences.WeatherLocation(URLDecoder.decode(matcher.group(1), "UTF-8"), 0, 0);
            } catch (Throwable t) {
                //
            }
        }

        //geo:12.34,56.78
        matcher = Pattern.compile("geo:([0-9.\\-]+),([0-9.\\-]+)(\\?.*)?").matcher(geoUri);
        if (matcher.matches()) {
            try {
                String latStr = matcher.group(1);
                String lonStr = matcher.group(2);
                double lat = Double.parseDouble(latStr == null ? "0" : latStr);
                double lon = Double.parseDouble(lonStr == null ? "0" : lonStr);

                if (lat != 0 && lon != 0) {
                    return new WeatherPreferences.WeatherLocation(null, lat, lon);
                }
            } catch (Throwable t) {
                //Pattern did not match, should never happen
            }
        }

        return null;
    }

    @Override
    public void onBackPressed() {
        if (fullscreenHelper.isFullscreen) {
            mainViewModel.getFullscreenModel().postValue(FullscreenModel.CLOSING);
        } else if (drawerLayout.isDrawerVisible(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    public void registerRadarCardView(RadarCardView radarCardView) {
        this.radarCardView = radarCardView;
        fullscreenHelper = new FullscreenHelper(getWindow(), radarCardView.getWebView(), fullscreenContainer);
    }

    public void fullscreenify(boolean expand, boolean doImmediately) {
        if (fullscreenHelper != null) {
            fullscreenHelper.fullscreenify(expand, doImmediately ? 0 : 250);
        }
    }

    @Override
    public void onDefaultLocationSelected(String location) {
        drawerLayout.closeDrawer(GravityCompat.START);

        WeatherPreferences.setDefaultLocation(location);

        this.getWeather();
    }

    @Override
    protected void onResume() {
        super.onResume();

        QuickWeather.setMainActivity(this);

        ColorUtils.setNightMode(this);

        fileWebServer.start();

        this.getWeather();

        mainViewModel.getFullscreenModel().postValue(FullscreenModel.CLOSED);

        navigationView.updateLocations();
        drawerLayout.closeDrawer(GravityCompat.START);

        //TODO remove after disabling Dark Sky
        if (WeatherPreferences.getProvider().equals(WeatherPreferences.PROVIDER_DS)) {
            if (switchToOwmSnackbar == null) {
                switchToOwmSnackbar = SnackbarUtils.notifySwitchToOWM(coordinatorLayout, this);
            } else {
                switchToOwmSnackbar.show();
            }

            if (WeatherPreferences.getShowAnnouncement().equals(WeatherPreferences.ENABLED)) {
                new TextDialog(this)
                        .setTitle(getString(R.string.dialog_transition_announcement_title))
                        .setContent(StringUtils.fromHtml(getString(R.string.dialog_transition_announcement)))
                        .addCloseButton()
                        .show();

                WeatherPreferences.setShowAnnouncement(WeatherPreferences.DISABLED);
            }

            new TextDialog(this)
                    .setTitle(getString(R.string.dialog_transition_finalwarning_title))
                    .setContent(getString(R.string.dialog_transition_finalwarning))
                    .addCloseButton()
                    .show();
        } else if (switchToOwmSnackbar != null) {
            switchToOwmSnackbar.dismiss();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        new WebView(this).clearCache(true);

        if (fileWebServer != null) {
            fileWebServer.stop();
        }
    }

    private void initViewModel() {
        mainViewModel = QuickWeather.getViewModelProvider().get(MainViewModel.class);

        mainViewModel.getWeatherModel().observe(this, weatherModel -> {
            swipeRefreshLayout.setRefreshing(
                    weatherModel.status == WeatherModel.WeatherStatus.UPDATING ||
                            weatherModel.status == WeatherModel.WeatherStatus.OBTAINING_LOCATION);

            for (Snackbar s : new Snackbar[]{obtainingLocSnackbar, backLocPermDeniedSnackbar,
                    switchToOwmSnackbar, locPermDeniedSnackbar, locDisabledSnackbar}) {
                if (s != null) {
                    s.dismiss();
                }
            }

            switch (weatherModel.status) {
                case SUCCESS:
                    WeatherPreferences.WeatherLocation weatherLocation = WeatherLocationManager.getLocationFromPreferences();

                    if (weatherLocation.location.equals(getResources().getString(R.string.text_current_location)) &&
                            !WeatherLocationManager.isBackgroundLocationEnabled(this) &&
                            WeatherLocationManager.isLocationEnabled(this) &&
                            (WeatherPreferences.getShowAlertNotification().equals(WeatherPreferences.ENABLED) || WeatherPreferences.getShowPersistentNotification().equals(WeatherPreferences.ENABLED))) {
                        if (backLocPermDeniedSnackbar == null) {
                            backLocPermDeniedSnackbar = SnackbarUtils.notifyBackLocPermDenied(coordinatorLayout, requestPermissionLauncher);
                        } else {
                            backLocPermDeniedSnackbar.show();
                        }
                    } else if (backLocPermDeniedSnackbar != null) {
                        backLocPermDeniedSnackbar.dismiss();
                    }

                    toolbar.setTitle(weatherLocation.location);

                    weatherCardRecyclerView.update(weatherModel.response);

                    updateColors(weatherModel.response.currently.temperature);

                    break;
                case OBTAINING_LOCATION:
                    if (obtainingLocSnackbar == null) {
                        obtainingLocSnackbar = SnackbarUtils.notifyObtainingLocation(coordinatorLayout);
                    } else {
                        obtainingLocSnackbar.show();
                    }
                    break;
                case ERROR_OTHER:
                    Logger.e(this, TAG, weatherModel.errorMessage, null);

                    swipeRefreshLayout.setRefreshing(false);
                    break;
                case ERROR_LOCATION_ACCESS_DISALLOWED:
                    if (locPermDeniedSnackbar == null) {
                        locPermDeniedSnackbar = SnackbarUtils.notifyLocPermDenied(coordinatorLayout, requestPermissionLauncher);
                    } else {
                        locPermDeniedSnackbar.show();
                    }
                    break;
                case ERROR_LOCATION_DISABLED:
                    if (locDisabledSnackbar == null) {
                        locDisabledSnackbar = SnackbarUtils.notifyLocationDisabled(coordinatorLayout);
                    } else {
                        locDisabledSnackbar.show();
                    }
                    break;
            }
        });

        mainViewModel.getFullscreenModel().observe(this, fullscreenModel -> {
            fullscreenify(
                    fullscreenModel == FullscreenModel.OPEN || fullscreenModel == FullscreenModel.OPENING,
                    fullscreenModel == FullscreenModel.OPEN || fullscreenModel == FullscreenModel.CLOSED
            );

            if (radarCardView != null) {
                radarCardView.setRadarState(
                        fullscreenModel == FullscreenModel.OPEN || fullscreenModel == FullscreenModel.OPENING);
            }
        });
    }

    private void getWeather() {
        mainViewModel.obtainWeatherAsync();

        WeatherWorkManager.enqueueNotificationWorker(true);

        if (!WeatherPreferences.getShowPersistentNotification().equals(WeatherPreferences.ENABLED)) {
            NotificationUtils.cancelPersistentNotification(this);
        }
    }

    @Override
    public void onRefresh() {
        this.getWeather();
    }

    private void updateColors(double temperature) {
        int color = ColorUtils.getColorFromTemperature(temperature, false);
        int darkColor = ColorUtils.getDarkenedColor(color);
        int textColor = ColorUtils.getTextColor(color);

        toolbar.setBackgroundColor(color);
        toolbar.setTitleTextColor(textColor);

        drawerToggle.getDrawerArrowDrawable().setColor(textColor);
        getWindow().setStatusBarColor(darkColor);
        getWindow().setNavigationBarColor(color);

        CustomTabs.getInstance(this).setColor(color);

        WindowUtils.setLightNavBar(getWindow(), textColor == ColorUtils.COLOR_TEXT_BLACK);
    }

    private void initViews() {
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        coordinatorLayout = findViewById(R.id.coordinator_layout);
        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.drawerLayout);
        weatherCardRecyclerView = findViewById(R.id.weather_card_recycler_view);
        fullscreenContainer = findViewById(R.id.fullscreen_container);
        navigationView = findViewById(R.id.navigationView);

        swipeRefreshLayout.setOnRefreshListener(this);

        navigationView.initialize(this);

        drawerToggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.drawer_opened,
                R.string.drawer_closed);

        drawerToggle.syncState();
        drawerLayout.addDrawerListener(drawerToggle);
    }

    public enum FullscreenModel {
        OPEN,
        OPENING,
        CLOSED,
        CLOSING
    }

    //TODO clean up
    private static class FullscreenHelper {
        private final Rect initialRect = new Rect();
        private final Rect initialMargins = new Rect();
        private final Rect fullscreenRect = new Rect();
        private final View currentView;
        private final ViewGroup currentFullscreenContainer;
        private final ValueAnimator animatorExpand;
        private final ValueAnimator animatorContract;
        private ViewGroup currentViewParent;
        private ViewGroup.LayoutParams currentInitialLayoutParams;
        private FrameLayout.LayoutParams fullscreenViewLayoutParams;
        private boolean isFullscreen;

        public FullscreenHelper(Window window, View view, ViewGroup fullscreenContainer) {
            currentFullscreenContainer = fullscreenContainer;
            currentView = view;

            animatorExpand = ValueAnimator.ofFloat(1f, 0f);
            animatorExpand.addUpdateListener(valueAnimator -> doAnimation((Float) valueAnimator.getAnimatedValue()));
            animatorExpand.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    doAnimation(0f);

                    WindowUtils.setImmersive(window, true);
                }

                @Override
                public void onAnimationStart(Animator animation) {
                    doAnimation(1f);
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });

            animatorContract = ValueAnimator.ofFloat(0f, 1f);
            animatorContract.addUpdateListener(valueAnimator -> doAnimation((Float) valueAnimator.getAnimatedValue()));
            animatorContract.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    currentFullscreenContainer.removeView(currentView);

                    if (currentView.getParent() != null) {
                        ((ViewGroup) currentView.getParent()).removeView(currentView);
                    }

                    if (currentViewParent != null) {
                        currentViewParent.addView(currentView, currentInitialLayoutParams);
                    }
                }

                @Override
                public void onAnimationStart(Animator animation) {
                    WindowUtils.setImmersive(window, false);
                    doAnimation(0f);
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
        }

        public void fullscreenify(boolean expand, int duration) {
            if (!animatorExpand.isRunning() && !animatorContract.isRunning()) {
                isFullscreen = expand;

                fullscreenViewLayoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

                currentFullscreenContainer.getGlobalVisibleRect(fullscreenRect);

                if (expand) {
                    if (currentView != null) {
                        currentView.getGlobalVisibleRect(initialRect);

                        initialMargins.set(initialRect.left, initialRect.top - fullscreenRect.top, fullscreenRect.right - initialRect.right, fullscreenRect.bottom - initialRect.bottom);

                        currentViewParent = (ViewGroup) currentView.getParent();
                        currentInitialLayoutParams = currentView.getLayoutParams();

                        if (currentViewParent != null) {
                            currentViewParent.removeView(currentView);
                        }

                        currentFullscreenContainer.addView(currentView);
                    }

                    animatorExpand.setDuration(duration);
                    animatorExpand.start();
                } else {
                    animatorContract.setDuration(duration);
                    animatorContract.start();
                }
            }
        }

        private void doAnimation(float f) {
            if (currentView != null) {
                fullscreenViewLayoutParams.setMargins((int) (initialMargins.left * f), (int) (initialMargins.top * f), (int) (initialMargins.right * f), (int) (initialMargins.bottom * f));
                currentView.setLayoutParams(fullscreenViewLayoutParams);
            }
        }
    }

    private static class FileWebServer extends NanoHTTPD {
        private final Resources resources;

        public FileWebServer(Context context, int port) {
            super(port);

            this.resources = context.getResources();
        }

        @Override
        public Response serve(IHTTPSession session) {
            String uri = session.getUri().substring(1);

            if (uri.equals("favicon.ico")) {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Not Found");
            } else {
                try {
                    return newChunkedResponse(Response.Status.OK,
                            getMimeTypeForFile(uri),
                            resources.getAssets().open(uri));
                } catch (IOException e) {
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "SERVER INTERNAL ERROR: IOException: " + e.getMessage());
                }
            }
        }

        @Override
        public void start() {
            try {
                super.start();
            } catch (IOException ioe) {
                //Only throws exception if port in use
                //Unless we are REALLY unlucky, we should be fine
            }
        }
    }

    public static class MainViewModel extends AndroidViewModel {
        private MutableLiveData<WeatherModel> weatherModel;
        private final Weather.WeatherListener weatherListener = new Weather.WeatherListener() {
            @Override
            public void onWeatherRetrieved(WeatherResponse weatherResponse) {
                weatherModel.postValue(new WeatherModel(weatherResponse));
            }

            @Override
            public void onWeatherError(String error, Throwable throwable) {
                throwable.printStackTrace();

                weatherModel.postValue(
                        new WeatherModel(null, WeatherModel.WeatherStatus.ERROR_OTHER, error));
            }
        };
        private MutableLiveData<FullscreenModel> fullscreenModel;

        public MainViewModel(@NonNull Application application) {
            super(application);
        }

        public MutableLiveData<WeatherModel> getWeatherModel() {
            if (weatherModel == null) {
                weatherModel = new MutableLiveData<>();
            }

            return weatherModel;
        }

        public MutableLiveData<FullscreenModel> getFullscreenModel() {
            if (fullscreenModel == null) {
                fullscreenModel = new MutableLiveData<>();
            }

            return fullscreenModel;
        }

        private void obtainWeatherAsync() {
            new Thread(this::obtainWeather).start();
        }

        private void obtainWeather() {
            weatherModel.postValue(new WeatherModel(null, WeatherModel.WeatherStatus.UPDATING, ""));

            try {
                Location location = WeatherLocationManager.getLocation(getApplication(), false);

                if (location == null) {
                    weatherModel.postValue(new WeatherModel(null, WeatherModel.WeatherStatus.OBTAINING_LOCATION, "ERROR_LOCATION_DISABLED"));
                    //TODO: ensure we cannot get infinite loops
                    WeatherLocationManager.getCurrentLocation(getApplication(), l -> obtainWeather());
                } else {
                    Weather.getWeatherAsync(getApplication(), WeatherPreferences.getProvider(), WeatherPreferences.getApiKey(), location.getLatitude(), location.getLongitude(), weatherListener);
                }
            } catch (WeatherLocationManager.LocationPermissionNotAvailableException e) {
                weatherModel.postValue(new WeatherModel(null, WeatherModel.WeatherStatus.ERROR_LOCATION_ACCESS_DISALLOWED, getApplication().getString(R.string.snackbar_background_location)));
            } catch (WeatherLocationManager.LocationDisabledException e) {
                weatherModel.postValue(new WeatherModel(null, WeatherModel.WeatherStatus.ERROR_LOCATION_DISABLED, getApplication().getString(R.string.error_gps_disabled)));
            }
        }
    }
}