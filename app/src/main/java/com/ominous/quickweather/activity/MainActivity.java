package com.ominous.quickweather.activity;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.webkit.WebView;

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
import com.ominous.tylerutils.util.StringUtils;
import com.ominous.tylerutils.util.WindowUtils;

import java.io.IOException;
import java.lang.ref.WeakReference;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import fi.iki.elonen.NanoHTTPD;

//TODO contentDescription EVERYWHERE
//TODO More logging
//TODO Remove unnecessary string resources
//TODO Find any strings that need to be translated
public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener, Weather.WeatherListener, WeatherDrawerLayout.OnDefaultLocationSelectedListener {
    private static final String TAG = "MainActivity";
    public  static final String EXTRA_ALERT = "EXTRA_ALERT";

    private WeatherCardRecyclerView weatherCardRecyclerView;
    private WeatherDrawerLayout drawerLayout;
    private SwipeRefreshLayout swipeRefreshLayout;
    private CoordinatorLayout coordinatorLayout;
    private Toolbar toolbar;
    private FileWebServer fileWebServer;
    private Snackbar obtainingLocSnackbar, backLocPermDeniedSnackbar, switchToOwmSnackbar, locPermDeniedSnackbar, locDisabledSnackbar;

    private final ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), r -> getWeather());

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

        fileWebServer.start();

        this.getWeather();

        drawerLayout.updateLocations(this);

        if (WeatherPreferences.getProvider().equals(WeatherPreferences.PROVIDER_DS)) {
            if (switchToOwmSnackbar == null) {
                switchToOwmSnackbar = SnackbarUtils.notifySwitchToOWM(coordinatorLayout, this);
            } else {
                switchToOwmSnackbar.show();
            }

            if (WeatherPreferences.getShowAnnouncement().equals(WeatherPreferences.ENABLED)) {
                new TextDialog(this)
                        .setTitle(getString(R.string.dialog_transition_title))
                        .setContent(StringUtils.fromHtml(getString(R.string.dialog_transition_announcement)))
                        .addCloseButton()
                        .show();

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
                backLocPermDeniedSnackbar = SnackbarUtils.notifyBackLocPermDenied(coordinatorLayout, requestPermissionLauncher);
            } else {
                backLocPermDeniedSnackbar.show();
            }
        } else if (backLocPermDeniedSnackbar != null) {
            backLocPermDeniedSnackbar.dismiss();
        }

        try {
            Location location = WeatherLocationManager.getLocation(this, false);

            toolbar.setTitle(weatherLocation.location);

            Weather.getWeatherAsync(WeatherPreferences.getProvider(), WeatherPreferences.getApiKey(), location.getLatitude(), location.getLongitude(), this);
        } catch (WeatherLocationManager.LocationPermissionNotAvailableException e) {
            if (locPermDeniedSnackbar == null) {
                locPermDeniedSnackbar = SnackbarUtils.notifyLocPermDenied(coordinatorLayout, requestPermissionLauncher);
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
                    locPermDeniedSnackbar = SnackbarUtils.notifyLocPermDenied(coordinatorLayout, requestPermissionLauncher);
                } else {
                    locPermDeniedSnackbar.show();
                }
                swipeRefreshLayout.setRefreshing(false);
            }
        }

        WeatherWorkManager.enqueueNotificationWorker(true);

        if (!WeatherPreferences.getShowPersistentNotification().equals(WeatherPreferences.ENABLED)) {
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

        WindowUtils.setLightNavBar(getWindow(),textColor == ColorUtils.COLOR_TEXT_BLACK);
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

    private static class FileWebServer extends NanoHTTPD {
        private final WeakReference<Context> context;

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
}
