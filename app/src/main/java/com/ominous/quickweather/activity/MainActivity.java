package com.ominous.quickweather.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.ominous.quickweather.R;
import com.ominous.quickweather.util.ColorUtils;
import com.ominous.quickweather.util.CustomTabs;
import com.ominous.quickweather.util.Weather;
import com.ominous.quickweather.util.WeatherPreferences;
import com.ominous.quickweather.view.WeatherCardRecyclerView;
import com.ominous.quickweather.view.WeatherDrawerLayout;

import java.util.List;

//TODO contentDescription EVERYWHERE
//TODO More logging
public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener, Weather.WeatherListener, WeatherDrawerLayout.OnDefaultLocationSelectedListener {
    private SwipeRefreshLayout swipeRefreshLayout;

    private WeatherCardRecyclerView weatherCardRecyclerView;
    private WeatherDrawerLayout drawerLayout;
    private Toolbar toolbar;

    private LocationManager locationManager;
    private CoordinatorLayout coordinatorLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (WeatherPreferences.isInitialized()) {
            locationManager = ContextCompat.getSystemService(this,LocationManager.class);

            setContentView(R.layout.activity_main);
            initViews();
            initDrawer();
        } else {
            startActivity(new Intent(this, SettingsActivity.class));
            finish();
        }
    }

    private void initDrawer() {
        drawerLayout.initialize(this, toolbar, this);
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
    }

    private WeatherPreferences.WeatherLocation getLocation() {
        String defaultLocation = WeatherPreferences.getDefaultLocation();
        List<WeatherPreferences.WeatherLocation> locations = WeatherPreferences.getLocations();
        WeatherPreferences.WeatherLocation weatherLocation = locations.get(0);

        for (WeatherPreferences.WeatherLocation location : locations) {
            if (location.location.equals(defaultLocation)) {
                weatherLocation = location;
            }
        }

        return weatherLocation;
    }

    private void getWeather() {
        WeatherPreferences.WeatherLocation weatherLocation = getLocation();

        Location location = null;

        if (weatherLocation.location.equals(getString(R.string.text_current_location))) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            } else {
                Snackbar
                        .make(coordinatorLayout, R.string.text_no_location_permission,Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.text_settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                startActivity(
                                        new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                                .setData(Uri.fromParts("package", getPackageName(), null)));
                            }
                        })
                        .show();
            }
        } else {
            location = new Location(LocationManager.NETWORK_PROVIDER);
            location.setLatitude(weatherLocation.latitude);
            location.setLongitude(weatherLocation.longitude);
        }

        if (location != null) {
            toolbar.setTitle(weatherLocation.location);

            swipeRefreshLayout.setRefreshing(true);

            Weather.getWeather(WeatherPreferences.getApiKey(), location.getLatitude(), location.getLongitude(), this);
        }
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
                getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility() & ~ View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR & ~ View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
    }

    @Override
    public void onWeatherError(String error) {//TODO: Better Snacks
        Log.e("onWeatherError",error);
        Snackbar.make(coordinatorLayout,"WeatherError: " + error,Snackbar.LENGTH_LONG).show();
        swipeRefreshLayout.setRefreshing(false);
    }

    private void initViews() {
        coordinatorLayout       = findViewById(R.id.coordinator_layout);
        weatherCardRecyclerView = findViewById(R.id.weather_card_recycler_view);
        swipeRefreshLayout      = findViewById(R.id.swipeRefreshLayout);
        toolbar                 = findViewById(R.id.toolbar);
        drawerLayout            = findViewById(R.id.drawerLayout);

        swipeRefreshLayout.setOnRefreshListener(this);
    }
}
