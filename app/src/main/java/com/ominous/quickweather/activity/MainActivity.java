package com.ominous.quickweather.activity;

import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;
import com.ominous.quickweather.R;
import com.ominous.quickweather.util.ColorUtils;
import com.ominous.quickweather.util.CustomTabs;
import com.ominous.quickweather.util.Logger;
import com.ominous.quickweather.util.NotificationUtil;
import com.ominous.quickweather.weather.Weather;
import com.ominous.quickweather.util.WeatherPreferences;
import com.ominous.quickweather.view.WeatherCardRecyclerView;
import com.ominous.quickweather.view.WeatherDrawerLayout;
import com.ominous.quickweather.weather.WeatherLocationManager;
import com.ominous.quickweather.weather.WeatherWorkManager;

//TODO contentDescription EVERYWHERE
//TODO More logging
public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener, Weather.WeatherListener, WeatherDrawerLayout.OnDefaultLocationSelectedListener {
    private static final String TAG = "MainActivity";
    public  static final String EXTRA_ALERTURI = "EXTRA_ALERTURI";

    private WeatherCardRecyclerView weatherCardRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private WeatherDrawerLayout drawerLayout;
    private Toolbar toolbar;

    private CoordinatorLayout coordinatorLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (WeatherPreferences.isInitialized()) {
            this.setContentView(R.layout.activity_main);
            this.initViews();

            Bundle bundle;
            String uri;

            if ((bundle = this.getIntent().getExtras()) != null &&
                    (uri = bundle.getString(EXTRA_ALERTURI)) != null) {
                CustomTabs.getInstance(this).launch(this, Uri.parse(uri));
            }
        } else {
            ContextCompat.startActivity(this, new Intent(this, SettingsActivity.class), null);
            this.finish();
        }
    }

    @Override
    public void onDefaultLocationSelected(String location) {
        WeatherPreferences.setDefaultLocation(location);

        getWeather();
    }

    @Override
    protected void onResume() {
        super.onResume();

        getWeather();

        drawerLayout.updateLocations(this);
    }

    private void getWeather() {
        WeatherPreferences.WeatherLocation weatherLocation = WeatherLocationManager.getLocationFromPreferences();

        try {
            Location location = WeatherLocationManager.getLocation(this);

            if (location != null) {
                toolbar.setTitle(weatherLocation.location);

                swipeRefreshLayout.setRefreshing(true);

                Weather.getWeather(WeatherPreferences.getApiKey(), location.getLatitude(), location.getLongitude(), this);
            }
        } catch (WeatherLocationManager.LocationPermissionNotAvailableException e) {
            Snackbar
                    .make(coordinatorLayout, R.string.text_no_location_permission, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.text_settings, v ->
                            ContextCompat.startActivity(MainActivity.this,
                                    new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                            .setData(Uri.fromParts("package", getPackageName(), null)), null))
                    .show();
        }

        if (!WeatherPreferences.getShowPersistentNotification().equals(WeatherPreferences.ENABLED)) {
            NotificationUtil.cancelPersistentNotification(this);
        }

        WeatherWorkManager.enqueueNotificationWorker();
    }

    @Override
    public void onRefresh() {
        getWeather();
    }

    @Override
    public void onWeatherRetrieved(Weather.WeatherResponse weatherResponse) {
        weatherCardRecyclerView.update(weatherResponse);

        updateColors(weatherResponse.currently.temperature);

        swipeRefreshLayout.setRefreshing(false);
    }

    private void updateColors(double temperature) {
        int color = ColorUtils.getColorFromTemperature(temperature);
        int darkColor = ColorUtils.getDarkenedColor(color);
        int textColor = ColorUtils.getTextColor(color);

        toolbar.setBackgroundColor(color);
        toolbar.setTitleTextColor(textColor);

        drawerLayout.setSpinnerColor(textColor);
        getWindow().setStatusBarColor(darkColor);
        getWindow().setNavigationBarColor(darkColor);

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
}
