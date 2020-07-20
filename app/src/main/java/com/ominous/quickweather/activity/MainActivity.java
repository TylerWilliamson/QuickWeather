package com.ominous.quickweather.activity;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.view.View;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;
import com.ominous.quickweather.R;
import com.ominous.quickweather.dialog.TextDialog;
import com.ominous.quickweather.util.ColorUtils;
import com.ominous.quickweather.util.DialogUtils;
import com.ominous.quickweather.util.Logger;
import com.ominous.quickweather.util.NotificationUtils;
import com.ominous.quickweather.util.SnackbarUtils;
import com.ominous.quickweather.util.WeatherPreferences;
import com.ominous.quickweather.view.WeatherCardRecyclerView;
import com.ominous.quickweather.view.WeatherDrawerLayout;
import com.ominous.quickweather.weather.Weather;
import com.ominous.quickweather.weather.WeatherLocationManager;
import com.ominous.quickweather.weather.WeatherResponse;
import com.ominous.quickweather.work.WeatherWorkManager;
import com.ominous.tylerutils.browser.CustomTabs;

import java.io.IOException;
import java.lang.ref.WeakReference;

import fi.iki.elonen.NanoHTTPD;

//TODO contentDescription EVERYWHERE
//TODO More logging
//TODO Remove unnecessary String.format and string resources
public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener, Weather.WeatherListener, WeatherDrawerLayout.OnDefaultLocationSelectedListener {
    private static final String TAG = "MainActivity";
    public  static final String EXTRA_ALERT = "EXTRA_ALERT";

    private final static int REQUEST_PERMISSION_LOCATION = 1000;
    private final static int REQUEST_PERMISSION_BACKGROUND = 1001;

    private WeatherCardRecyclerView weatherCardRecyclerView;
    private WeatherDrawerLayout drawerLayout;
    private SwipeRefreshLayout swipeRefreshLayout;
    private CoordinatorLayout coordinatorLayout;
    private Toolbar toolbar;
    private FileWebServer fileWebServer;

    private Snackbar obtainingLocSnackbar, backLocPermDeniedSnackbar, switchToOwmSnackbar, locPermDeniedSnackbar, locDisabledSnackbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ColorUtils.initialize(this);//Initializing after Activity created to get day/night properly

        if (WeatherPreferences.isInitialized()) {
            this.setContentView(R.layout.activity_main);
            this.initViews();

            Bundle bundle;
            WeatherResponse.Alert alert;

            if ((bundle = this.getIntent().getExtras()) != null &&
                    (alert = (WeatherResponse.Alert) bundle.getSerializable(EXTRA_ALERT)) != null) {
                DialogUtils.showDialogForAlert(this,alert);
            }
        } else {
            ContextCompat.startActivity(this, new Intent(this, SettingsActivity.class), null);
            this.finish();
        }

        fileWebServer = new FileWebServer(this,4234);
    }

    @Override
    public void onDefaultLocationSelected(String location) {
        WeatherPreferences.setDefaultLocation(location);

        this.getWeather();
    }

    @Override
    protected void onResume() {
        super.onResume();

        ColorUtils.setNightMode(this);

        try {
            fileWebServer.start();
        } catch (IOException ioe) {
            //Only throws exception if port in use
            //Unless we are REALLY unlucky, we should be fine
        }

        this.getWeather();

        drawerLayout.updateLocations(this);

        if (WeatherPreferences.getProvider().equals(WeatherPreferences.PROVIDER_DS)) {
            if (switchToOwmSnackbar == null) {
                switchToOwmSnackbar = SnackbarUtils.notifySwitchToOWM(coordinatorLayout, this);
            } else {
                switchToOwmSnackbar.show();
            }

            if (WeatherPreferences.getShowAnnouncement().equals(WeatherPreferences.ENABLED)) {
                new TextDialog(this).show(getString(R.string.dialog_transition_title), new SpannableString(Html.fromHtml(getString(R.string.dialog_transition_announcement))));

                WeatherPreferences.setShowAnnouncement(WeatherPreferences.DISABLED);
            }
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

    private void getWeather() {
        swipeRefreshLayout.setRefreshing(true);

        for (Snackbar s : new Snackbar[] {obtainingLocSnackbar, backLocPermDeniedSnackbar, switchToOwmSnackbar, locPermDeniedSnackbar, locDisabledSnackbar}) {
            if (s != null) {
                s.dismiss();
            }
        }

        WeatherPreferences.WeatherLocation weatherLocation = WeatherLocationManager.getLocationFromPreferences();

        if (weatherLocation.location.equals(getResources().getString(R.string.text_current_location)) &&
                !WeatherLocationManager.isBackgroundLocationEnabled(this) &&
                WeatherLocationManager.isLocationEnabled(this) &&
                (WeatherPreferences.getShowAlertNotification().equals(WeatherPreferences.ENABLED) || WeatherPreferences.getShowPersistentNotification().equals(WeatherPreferences.ENABLED))) {
            if (backLocPermDeniedSnackbar == null) {
                backLocPermDeniedSnackbar = SnackbarUtils.notifyBackLocPermDenied(coordinatorLayout, this, REQUEST_PERMISSION_BACKGROUND);
            } else {
                backLocPermDeniedSnackbar.show();
            }
        } else if (backLocPermDeniedSnackbar != null) {
            backLocPermDeniedSnackbar.dismiss();
        }

        try {
            Location location = WeatherLocationManager.getLocation(this);

            toolbar.setTitle(weatherLocation.location);

            Weather.getWeatherAsync(WeatherPreferences.getProvider(), WeatherPreferences.getApiKey(), location.getLatitude(), location.getLongitude(), this);
        } catch (WeatherLocationManager.LocationPermissionNotAvailableException e) {
            if (locPermDeniedSnackbar == null) {
                locPermDeniedSnackbar = SnackbarUtils.notifyLocPermDenied(coordinatorLayout, this, REQUEST_PERMISSION_LOCATION);
            } else {
                locPermDeniedSnackbar.show();
            }
            swipeRefreshLayout.setRefreshing(false);
        } catch (WeatherLocationManager.LocationDisabledException e) {
            if (locDisabledSnackbar == null) {
                locDisabledSnackbar = SnackbarUtils.notifyLocationDisabled(coordinatorLayout);
            } else {
                locDisabledSnackbar.show();
            }
            swipeRefreshLayout.setRefreshing(false);
        } catch (WeatherLocationManager.LocationNotAvailableException e) {
            if (obtainingLocSnackbar == null) {
                obtainingLocSnackbar = SnackbarUtils.notifyObtainingLocation(coordinatorLayout);
            } else {
                obtainingLocSnackbar.show();
            }

            try {
                WeatherLocationManager.getCurrentLocation(this, location -> {
                    getWeather();
                    obtainingLocSnackbar.dismiss();
                });
            } catch (WeatherLocationManager.LocationDisabledException ex) {
                obtainingLocSnackbar.dismiss();

                if (locDisabledSnackbar == null) {
                    locDisabledSnackbar = SnackbarUtils.notifyLocationDisabled(coordinatorLayout);
                } else {
                    locDisabledSnackbar.show();
                }
                swipeRefreshLayout.setRefreshing(false);
            } catch (WeatherLocationManager.LocationPermissionNotAvailableException ex) {
                obtainingLocSnackbar.dismiss();

                if (locPermDeniedSnackbar == null) {
                    locPermDeniedSnackbar = SnackbarUtils.notifyLocPermDenied(coordinatorLayout, this, REQUEST_PERMISSION_LOCATION);
                } else {
                    locPermDeniedSnackbar.show();
                }
                swipeRefreshLayout.setRefreshing(false);
            }
        }

        if (WeatherPreferences.getShowPersistentNotification().equals(WeatherPreferences.ENABLED) ||
                WeatherPreferences.getShowAlertNotification().equals(WeatherPreferences.ENABLED)) {
            WeatherWorkManager.enqueueNotificationWorker(true);
        } else {
            NotificationUtils.cancelPersistentNotification(this);
        }
    }

    @Override
    public void onRefresh() {
        getWeather();
    }

    @Override
    public void onWeatherRetrieved(WeatherResponse weatherResponse) {
        weatherCardRecyclerView.update(weatherResponse);

        updateColors(weatherResponse.currently.temperature);

        swipeRefreshLayout.setRefreshing(false);
    }

    private void updateColors(double temperature) {
        int color = ColorUtils.getColorFromTemperature(temperature,false);
        int darkColor = ColorUtils.getDarkenedColor(color);
        int textColor = ColorUtils.getTextColor(color);

        toolbar.setBackgroundColor(color);
        toolbar.setTitleTextColor(textColor);

        drawerLayout.setSpinnerColor(textColor);
        getWindow().setStatusBarColor(darkColor);
        getWindow().setNavigationBarColor(color);

        CustomTabs.getInstance(this).setColor(color);

        if (Build.VERSION.SDK_INT >= 26) {
            if (textColor == ColorUtils.COLOR_TEXT_BLACK) {
                getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            } else {
                getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
    }

    @Override
    public void onWeatherError(String error, Throwable throwable) {
        Logger.e(this, TAG, error, throwable);

        swipeRefreshLayout.setRefreshing(false);
    }

    private void initViews() {
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        coordinatorLayout = findViewById(R.id.coordinator_layout);
        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.drawerLayout);
        weatherCardRecyclerView = findViewById(R.id.weather_card_recycler_view);

        swipeRefreshLayout.setOnRefreshListener(this);

        drawerLayout.initialize(this, toolbar, this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        getWeather();
    }

    private static class FileWebServer extends NanoHTTPD {
        private WeakReference<Context> context;

        public FileWebServer(Context context, int port) {
            super(port);

            this.context = new WeakReference<>(context);
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
                            context.get().getAssets().open(uri));
                } catch (IOException e) {
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "SERVER INTERNAL ERROR: IOException: " + e.getMessage());
                }
            }
        }
    }
}
