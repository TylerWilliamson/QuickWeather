/*
 *     Copyright 2019 - 2022 Tyler Williamson
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

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.Application;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.android.material.navigation.NavigationView;
import com.ominous.quickweather.R;
import com.ominous.quickweather.api.Gadgetbridge;
import com.ominous.quickweather.card.RadarCardView;
import com.ominous.quickweather.data.WeatherDatabase;
import com.ominous.quickweather.data.WeatherLogic;
import com.ominous.quickweather.data.WeatherModel;
import com.ominous.quickweather.data.WeatherResponseOneCall;
import com.ominous.quickweather.location.WeatherLocationManager;
import com.ominous.quickweather.util.ColorUtils;
import com.ominous.quickweather.util.DialogHelper;
import com.ominous.quickweather.util.FullscreenHelper;
import com.ominous.quickweather.util.NotificationUtils;
import com.ominous.quickweather.util.SnackbarHelper;
import com.ominous.quickweather.util.WeatherPreferences;
import com.ominous.quickweather.view.WeatherCardRecyclerView;
import com.ominous.quickweather.view.WeatherNavigationView;
import com.ominous.quickweather.web.FileWebServer;
import com.ominous.quickweather.work.WeatherWorkManager;
import com.ominous.tylerutils.async.Promise;
import com.ominous.tylerutils.browser.CustomTabs;
import com.ominous.tylerutils.http.HttpException;
import com.ominous.tylerutils.plugins.ApkUtils;
import com.ominous.tylerutils.plugins.GithubUtils;
import com.ominous.tylerutils.util.LocaleUtils;
import com.ominous.tylerutils.util.WindowUtils;

import org.json.JSONException;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_ALERT = "EXTRA_ALERT";
    public static final String ACTION_OPENALERT = "com.ominous.quickweather.ACTION_OPENALERT";
    private WeatherCardRecyclerView weatherCardRecyclerView;
    private DrawerLayout drawerLayout;
    private WeatherNavigationView navigationView;
    private RadarCardView radarCardView;
    private ActionBarDrawerToggle drawerToggle;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FrameLayout fullscreenContainer;
    private Toolbar toolbar;
    private ImageView toolbarMyLocation;

    private FileWebServer fileWebServer;
    private MainViewModel mainViewModel;

    private FullscreenHelper fullscreenHelper;
    private SnackbarHelper snackbarHelper;
    private DialogHelper dialogHelper;

    private final ActivityResultLauncher<String[]> requestLocationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), r -> this.getWeather());
    private final ActivityResultLauncher<String> requestNotificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), r -> this.checkPermissions());
    private final OnBackPressedCallback drawerBackPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    };
    private final OnBackPressedCallback fullscreenBackPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            mainViewModel.getFullscreenModel().postValue(FullscreenHelper.FullscreenState.CLOSING);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initActivity();

        if (isInitialized()) {
            initViews();
            initViewLogic();
            initViewModel();

            onReceiveIntent(getIntent());
        } else {
            ContextCompat.startActivity(this, new Intent(this, SettingsActivity.class), null);
            finish();
        }
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        drawerToggle.syncState();
    }

    private void initActivity() {
        ColorUtils.initialize(this);//Initializing after Activity created to get day/night properly

        setTaskDescription(
                Build.VERSION.SDK_INT >= 28 ?
                        new ActivityManager.TaskDescription(
                                getString(R.string.app_name),
                                R.mipmap.ic_launcher_round,
                                ContextCompat.getColor(this, R.color.color_app_accent)) :
                        new ActivityManager.TaskDescription(
                                getString(R.string.app_name),
                                BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_round),
                                ContextCompat.getColor(this, R.color.color_app_accent))
        );

        OnBackPressedDispatcher onBackPressedDispatcher = getOnBackPressedDispatcher();
        onBackPressedDispatcher.addCallback(drawerBackPressedCallback);
        onBackPressedDispatcher.addCallback(fullscreenBackPressedCallback);
    }

    private void checkPermissions() {
        if (!NotificationUtils.canShowNotifications(this)) {
            snackbarHelper.notifyNotificationPermissionDenied(requestNotificationPermissionLauncher);
        }
    }

    private boolean isInitialized() {
        try {
            return Promise.create((a) -> WeatherPreferences.isInitialized() &&
                    WeatherDatabase.getInstance(this).locationDao().getCount() > 0).await();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
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
                    WeatherDatabase.WeatherLocation weatherLocation;

                    if ("geo".equals(intent.getScheme()) &&
                            (weatherLocation = getWeatherLocationFromGeoUri(intent.getDataString())) != null) {
                        ContextCompat.startActivity(this, new Intent(this, SettingsActivity.class)
                                .putExtra(SettingsActivity.EXTRA_WEATHERLOCATION, weatherLocation), null);
                    }
                    break;
                case ACTION_OPENALERT:
                    Bundle bundle;
                    WeatherResponseOneCall.Alert alert;

                    if ((bundle = intent.getExtras()) != null &&
                            (alert = (WeatherResponseOneCall.Alert) bundle.getSerializable(EXTRA_ALERT)) != null) {
                        dialogHelper.showAlert(alert);
                    }
                    break;
            }
        }
    }

    private WeatherDatabase.WeatherLocation getWeatherLocationFromGeoUri(String geoUri) {
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
                    return new WeatherDatabase.WeatherLocation(lat, lon, null);
                }
            } catch (Throwable t) {
                //Pattern did not match, should never happen
            }
        }

        //geo:0,0?q=my+street+address
        matcher = Pattern.compile("geo:0,0\\?.*?q=([^&]*).*").matcher(geoUri);
        if (matcher.matches()) {
            try {
                return new WeatherDatabase.WeatherLocation(0, 0, URLDecoder.decode(matcher.group(1), "UTF-8"));
            } catch (Throwable t) {
                //
            }
        }

        //geo:12.34,56.78
        matcher = Pattern.compile("geo:([0-9.\\-]+),([0-9.\\-]+)(\\?.*)?").matcher(geoUri);

        if (matcher.matches()) {
            try {
                double lat = LocaleUtils.parseDouble(Locale.getDefault(), matcher.group(1));
                double lon = LocaleUtils.parseDouble(Locale.getDefault(), matcher.group(2));

                if (lat != 0 && lon != 0) {
                    return new WeatherDatabase.WeatherLocation(lat, lon, null);
                }
            } catch (Throwable t) {
                //Pattern did not match, should never happen
            }
        }

        return null;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!isInitialized()) {
            ContextCompat.startActivity(this, new Intent(this, SettingsActivity.class), null);
            finish();
        }

        ColorUtils.setNightMode(this);

        fileWebServer.start();

        this.getWeather();

        mainViewModel.getFullscreenModel().postValue(FullscreenHelper.FullscreenState.CLOSED);

        drawerLayout.closeDrawer(GravityCompat.START);

        snackbarHelper.dismiss();

        checkPermissions();
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
        mainViewModel = new ViewModelProvider(this)
                .get(MainActivity.MainViewModel.class);

        mainViewModel.getWeatherModel().observe(this, weatherModel -> {
            swipeRefreshLayout.setRefreshing(
                    weatherModel.status == WeatherModel.WeatherStatus.UPDATING ||
                            weatherModel.status == WeatherModel.WeatherStatus.OBTAINING_LOCATION);

            snackbarHelper.dismiss();

            checkPermissions();

            switch (weatherModel.status) {
                case SUCCESS:
                    updateWeather(weatherModel);

                    if (WeatherPreferences.shouldRunBackgroundJob()) {
                        WeatherWorkManager.enqueueNotificationWorker(true);

                        if (WeatherPreferences.getShowPersistentNotification().equals(WeatherPreferences.ENABLED)) {
                            NotificationUtils.updatePersistentNotification(this, weatherModel.weatherLocation, weatherModel.responseOneCall);
                        }
                    }

                    if (weatherModel.responseOneCall.alerts != null) {
                        Promise.create((a) -> {
                            for (WeatherResponseOneCall.Alert alert : weatherModel.responseOneCall.alerts) {
                                WeatherDatabase.getInstance(this).insertAlert(alert);
                            }
                        });
                    }

                    break;
                case OBTAINING_LOCATION:
                    snackbarHelper.notifyObtainingLocation();
                    break;
                case ERROR_OTHER:
                    snackbarHelper.notifyError(weatherModel.errorMessage);

                    swipeRefreshLayout.setRefreshing(false);
                    break;
                case ERROR_LOCATION_ACCESS_DISALLOWED:
                    snackbarHelper.notifyLocPermDenied(requestLocationPermissionLauncher);
                    break;
                case ERROR_LOCATION_DISABLED:
                    snackbarHelper.notifyLocDisabled();
                    break;
            }
        });

        mainViewModel.getFullscreenModel().observe(this, fullscreenState -> {
            if (fullscreenHelper != null) {
                fullscreenHelper.fullscreenify(fullscreenState);
            }

            fullscreenBackPressedCallback.setEnabled(fullscreenState == FullscreenHelper.FullscreenState.OPEN || fullscreenState == FullscreenHelper.FullscreenState.OPENING);

            if (radarCardView != null) {
                radarCardView.setRadarState(
                        fullscreenState == FullscreenHelper.FullscreenState.OPEN || fullscreenState == FullscreenHelper.FullscreenState.OPENING);
            }
        });

        mainViewModel.getLocationModel().observe(this, weatherLocations -> {
            if (weatherLocations.size() > 0) {
                SubMenu locationSubMenu = navigationView.getMenu().getItem(0).getSubMenu();
                locationSubMenu.clear();

                int selectedId = 0;

                for (WeatherDatabase.WeatherLocation weatherLocation : weatherLocations) {
                    locationSubMenu.add(0, weatherLocation.id, 0, weatherLocation.isCurrentLocation ? MainActivity.this.getString(R.string.text_current_location) : weatherLocation.name)
                            .setCheckable(true)
                            .setIcon(R.drawable.navigation_item_icon);

                    if (weatherLocation.isSelected) {
                        selectedId = weatherLocation.id;
                    }
                }

                if (selectedId == 0) {
                    selectedId = weatherLocations.get(0).id;

                    Promise
                            .create(selectedId)
                            .then(defaultId -> {
                                WeatherDatabase.getInstance(MainActivity.this).locationDao().setDefaultLocation(defaultId);
                            });
                }

                locationSubMenu.setGroupCheckable(0, true, true);
                locationSubMenu.findItem(selectedId).setChecked(true);
            }
        });
    }

    private void getWeather() {
        if (WeatherPreferences.isValidProvider()) {
            mainViewModel.obtainWeatherAsync();

            WeatherWorkManager.enqueueNotificationWorker(true);

            if (!WeatherPreferences.getShowPersistentNotification().equals(WeatherPreferences.ENABLED)) {
                NotificationUtils.cancelPersistentNotification(this);
            }
        } else {
            snackbarHelper.notifyInvalidProvider();
        }
    }

    private void updateWeather(WeatherModel weatherModel) {
        if (WeatherPreferences.getGadgetbridgeEnabled().equals(WeatherPreferences.ENABLED)) {
            Gadgetbridge.broadcastWeather(this, weatherModel.weatherLocation, weatherModel.responseOneCall);
        }

        if (weatherModel.weatherLocation.isCurrentLocation &&
                !WeatherLocationManager.isBackgroundLocationPermissionGranted(this) &&
                WeatherLocationManager.isLocationPermissionGranted(this) &&
                WeatherPreferences.shouldRunBackgroundJob()) {
            snackbarHelper.notifyBackLocPermDenied(requestLocationPermissionLauncher, WeatherPreferences.shouldShowNotifications());
        }

        toolbar.setTitle(weatherModel.weatherLocation.isCurrentLocation ? getString(R.string.text_current_location) : weatherModel.weatherLocation.name);

        weatherCardRecyclerView.update(weatherModel);

        int color = ColorUtils.getColorFromTemperature(weatherModel.responseOneCall.current.temp, false);
        int darkColor = ColorUtils.getDarkenedColor(color);
        int textColor = ColorUtils.getTextColor(color);

        toolbar.setBackgroundColor(color);
        toolbar.setTitleTextColor(textColor);

        if (weatherModel.weatherLocation.isCurrentLocation) {
            toolbarMyLocation.setImageTintList(ColorStateList.valueOf(textColor));
            toolbarMyLocation.setVisibility(View.VISIBLE);
        } else {
            toolbarMyLocation.setVisibility(View.GONE);
        }

        drawerToggle.getDrawerArrowDrawable().setColor(textColor);
        getWindow().setStatusBarColor(darkColor);
        getWindow().setNavigationBarColor(color);

        CustomTabs.getInstance(this).setColor(color);

        WindowUtils.setLightNavBar(getWindow(), textColor == ColorUtils.COLOR_TEXT_BLACK);
    }

    private void initViews() {
        setContentView(R.layout.activity_main);

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        toolbar = findViewById(R.id.toolbar);
        toolbarMyLocation = findViewById(R.id.toolbar_mylocation_indicator);
        drawerLayout = findViewById(R.id.drawerLayout);
        weatherCardRecyclerView = findViewById(R.id.weather_card_recycler_view);
        fullscreenContainer = findViewById(R.id.fullscreen_container);
        navigationView = findViewById(R.id.navigationView);

        drawerToggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.drawer_open_desc,
                R.string.drawer_close_desc);

        fileWebServer = new FileWebServer(this, 4234);
        snackbarHelper = new SnackbarHelper(findViewById(R.id.coordinator_layout));
        dialogHelper = new DialogHelper(this);
    }

    public void initViewLogic() {
        weatherCardRecyclerView.setOnRadarWebViewCreatedListener((radarCardView) -> {
            MainActivity.this.radarCardView = radarCardView;
            fullscreenHelper = new FullscreenHelper(getWindow(), radarCardView.getWebView(), fullscreenContainer);

            radarCardView.setOnFullscreenClicked((expand) -> mainViewModel
                    .getFullscreenModel()
                    .postValue(expand ?
                            FullscreenHelper.FullscreenState.OPENING :
                            FullscreenHelper.FullscreenState.CLOSING));
        });

        swipeRefreshLayout.setOnRefreshListener(this::getWeather);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            private final GithubUtils.GitHubRepo quickWeatherRepo = GithubUtils.getRepo("TylerWilliamson", "QuickWeather");

            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (item.getGroupId() == R.id.settings_group) {
                    if (itemId == R.id.menu_settings) {
                        ContextCompat.startActivity(MainActivity.this,
                                new Intent(MainActivity.this, SettingsActivity.class),
                                ActivityOptions.makeCustomAnimation(MainActivity.this, R.anim.slide_left_in, R.anim.slide_right_out).toBundle());
                    } else if (itemId == R.id.menu_check_for_updates) {
                        Promise.create(quickWeatherRepo)
                                .then((repo) -> {
                                    final String currentVersion = ApkUtils.getReleaseVersion(MainActivity.this).split("-")[0];
                                    final GithubUtils.GitHubRelease latestRelease = quickWeatherRepo.getLatestRelease();

                                    if (currentVersion.equals(latestRelease.tag_name)) {
                                        snackbarHelper.notifyNoNewVersion();
                                    } else {
                                        snackbarHelper.notifyNewVersion(latestRelease);
                                    }
                                }, (e) -> snackbarHelper.logError(getString(R.string.text_error_new_version), e));
                    } else if (itemId == R.id.menu_whats_new) {
                        Promise.create(quickWeatherRepo)
                                .then((repo) -> {
                                    final String version = ApkUtils.getReleaseVersion(MainActivity.this).split("-")[0];
                                    final String releaseNotes = repo.getRelease(version).body;

                                    runOnUiThread(() -> dialogHelper.showReleaseNotes(version, releaseNotes));
                                }, (e) -> snackbarHelper.notifyError(getString(R.string.text_error_getting_release), e));
                    } else if (itemId == R.id.menu_report_a_bug) {
                        CustomTabs.getInstance(MainActivity.this)
                                .launch(MainActivity.this, Uri.parse(quickWeatherRepo.getNewIssueUrl(null, null)));
                    }
                } else {
                    navigationView.getMenu().getItem(0).getSubMenu().findItem(itemId).setChecked(true);
                    Promise.create((a) -> {
                        WeatherDatabase.getInstance(MainActivity.this).locationDao().setDefaultLocation(itemId);
                        getWeather();
                    });
                }

                drawerLayout.closeDrawer(GravityCompat.START);

                return true;
            }
        });

        drawerLayout.addDrawerListener(drawerToggle);
        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {

            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                drawerBackPressedCallback.setEnabled(true);
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                drawerBackPressedCallback.setEnabled(false);
            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });
    }

    public static class MainViewModel extends AndroidViewModel {
        private MutableLiveData<WeatherModel> weatherModelLiveData;
        private MutableLiveData<FullscreenHelper.FullscreenState> fullscreenModel;
        private LiveData<List<WeatherDatabase.WeatherLocation>> locationModel;

        public MainViewModel(@NonNull Application application) {
            super(application);
        }

        public MutableLiveData<WeatherModel> getWeatherModel() {
            if (weatherModelLiveData == null) {
                weatherModelLiveData = new MutableLiveData<>();
            }

            return weatherModelLiveData;
        }

        public MutableLiveData<FullscreenHelper.FullscreenState> getFullscreenModel() {
            if (fullscreenModel == null) {
                fullscreenModel = new MutableLiveData<>();
            }

            return fullscreenModel;
        }

        public LiveData<List<WeatherDatabase.WeatherLocation>> getLocationModel() {
            if (locationModel == null) {
                locationModel = WeatherDatabase.getInstance(this.getApplication().getApplicationContext()).locationDao().getLiveWeatherLocations();
            }

            return locationModel;
        }

        private void obtainWeatherAsync() {
            Promise.create((a) -> {
                weatherModelLiveData.postValue(new WeatherModel(null, null, WeatherModel.WeatherStatus.UPDATING, null, null));

                WeatherModel weatherModel = new WeatherModel();
                weatherModel.status = WeatherModel.WeatherStatus.ERROR_OTHER;
                weatherModel.errorMessage = null;

                try {
                    weatherModel = WeatherLogic.getCurrentWeather(getApplication(), false, false);

                    if (weatherModel.location == null) {
                        this.weatherModelLiveData.postValue(new WeatherModel(null, null, WeatherModel.WeatherStatus.OBTAINING_LOCATION, null, null));

                        weatherModel = WeatherLogic.getCurrentWeather(getApplication(), false, true);
                    }

                    if (weatherModel.location == null) {
                        weatherModel.errorMessage = getApplication().getString(R.string.error_null_location);
                    } else if (weatherModel.responseOneCall == null || weatherModel.responseOneCall.current == null) {
                        weatherModel.errorMessage = getApplication().getString(R.string.error_null_response);
                    } else {
                        weatherModel.status = WeatherModel.WeatherStatus.SUCCESS;
                    }
                } catch (WeatherLocationManager.LocationPermissionNotAvailableException e) {
                    weatherModel.status = WeatherModel.WeatherStatus.ERROR_LOCATION_ACCESS_DISALLOWED;
                    weatherModel.errorMessage = getApplication().getString(R.string.snackbar_background_location_notifications);
                } catch (WeatherLocationManager.LocationDisabledException e) {
                    weatherModel.status = WeatherModel.WeatherStatus.ERROR_LOCATION_DISABLED;
                    weatherModel.errorMessage = getApplication().getString(R.string.error_gps_disabled);
                } catch (IOException e) {
                    weatherModel.errorMessage = getApplication().getString(R.string.error_connecting_api);
                } catch (JSONException e) {
                    weatherModel.errorMessage = getApplication().getString(R.string.error_unexpected_api_result);
                } catch (InstantiationException | IllegalAccessException e) {
                    weatherModel.errorMessage = getApplication().getString(R.string.error_creating_result);
                } catch (HttpException e) {
                    weatherModel.errorMessage = e.getMessage();
                }

                this.weatherModelLiveData.postValue(weatherModel);
            });
        }
    }
}