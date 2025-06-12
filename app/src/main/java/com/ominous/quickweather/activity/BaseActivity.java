/*
 *   Copyright 2019 - 2025 Tyler Williamson
 *
 *   This file is part of QuickWeather.
 *
 *   QuickWeather is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   QuickWeather is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with QuickWeather.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.ominous.quickweather.activity;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.ominous.quickweather.R;
import com.ominous.quickweather.api.Gadgetbridge;
import com.ominous.quickweather.data.CurrentWeather;
import com.ominous.quickweather.data.WeatherCardType;
import com.ominous.quickweather.data.WeatherDataManager;
import com.ominous.quickweather.data.WeatherDatabase;
import com.ominous.quickweather.data.WeatherModel;
import com.ominous.quickweather.location.WeatherLocationManager;
import com.ominous.quickweather.pref.WeatherPreferences;
import com.ominous.quickweather.util.ColorHelper;
import com.ominous.quickweather.util.DialogHelper;
import com.ominous.quickweather.util.NotificationUtils;
import com.ominous.quickweather.util.SnackbarHelper;
import com.ominous.quickweather.view.WeatherCardRecyclerView;
import com.ominous.quickweather.work.WeatherWorkManager;
import com.ominous.tylerutils.anim.OpenCloseState;
import com.ominous.tylerutils.async.Promise;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

public abstract class BaseActivity extends AppCompatActivity implements ILifecycleAwareActivity {
    private final List<LifecycleListener> lifecycleListeners = new ArrayList<>();

    protected WeatherViewModel weatherViewModel;
    protected Date date = null;

    protected SnackbarHelper snackbarHelper;
    protected WeatherCardRecyclerView weatherCardRecyclerView;
    protected DrawerLayout drawerLayout;
    protected SwipeRefreshLayout swipeRefreshLayout;
    protected Toolbar toolbar;
    protected ImageView toolbarMyLocation;
    protected DialogHelper dialogHelper;
    protected CoordinatorLayout coordinatorLayout;
    protected ConstraintLayout baseLayout;

    protected final WeatherLocationManager weatherLocationManager = WeatherLocationManager.getInstance();

    private final ActivityResultLauncher<String[]> requestLocationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), r -> this.getWeather());
    private final ActivityResultLauncher<String> requestNotificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), r -> this.checkNotificationPermission());

    @Override
    protected void onStart() {
        super.onStart();

        for (LifecycleListener lifecycleListener : lifecycleListeners) {
            lifecycleListener.onStart();
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        openSettingsIfNotInitialized();

        ColorHelper
                .getInstance(this)
                .setNightMode(this);

        setTaskDescription(
                Build.VERSION.SDK_INT >= 28 ?
                        new ActivityManager.TaskDescription(
                                getString(R.string.app_name),
                                R.mipmap.ic_launcher_round,
                                ContextCompat.getColor(this, R.color.color_app_accent)) :
                        new ActivityManager.TaskDescription(
                                getString(R.string.app_name),
                                BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_round),
                                ContextCompat.getColor(this, R.color.color_app_accent)));

        for (LifecycleListener lifecycleListener : lifecycleListeners) {
            lifecycleListener.onCreate(savedInstanceState);
        }

        weatherViewModel = new ViewModelProvider(this)
                .get(WeatherViewModel.class);

        initViews();
        initViewModel();
    }

    protected void getWeather() {
        WeatherDataManager.getInstance().getWeatherAsync(
                getApplication().getApplicationContext(),
                weatherViewModel.getWeatherModel(),
                false,
                date);

        WeatherWorkManager.enqueueNotificationWorker(this, true);

        if (!WeatherPreferences.getInstance(this).shouldShowPersistentNotification()) {
            NotificationUtils.cancelPersistentNotification(this);
        }
    }

    protected void initViews() {
        setContentView(R.layout.activity_base);

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        drawerLayout = findViewById(R.id.drawerLayout);
        toolbar = findViewById(R.id.toolbar);
        toolbarMyLocation = findViewById(R.id.toolbar_mylocation_indicator);
        weatherCardRecyclerView = findViewById(R.id.weather_card_recycler_view);
        coordinatorLayout = findViewById(R.id.coordinator_layout);
        baseLayout = findViewById(R.id.base_layout);

        setSupportActionBar(toolbar);

        swipeRefreshLayout.setOnRefreshListener(this::getWeather);
        snackbarHelper = new SnackbarHelper(coordinatorLayout);
        dialogHelper = new DialogHelper(this);

        ViewCompat.setOnApplyWindowInsetsListener(
                coordinatorLayout,
                (v, windowInsetsCompat) -> {
                    Insets insets = windowInsetsCompat.getInsets(
                            WindowInsetsCompat.Type.statusBars() |
                                    WindowInsetsCompat.Type.navigationBars());

                    ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();

                    mlp.setMargins(
                            0,
                            insets.top,
                            0,
                            insets.bottom);
                    v.setLayoutParams(mlp);

                    return windowInsetsCompat;
                }
        );


    }

    protected void initViewModel() {
        weatherViewModel.getWeatherModel().observe(this, weatherModel -> {
            swipeRefreshLayout.setRefreshing(
                    weatherModel.status == WeatherModel.WeatherStatus.UPDATING ||
                            weatherModel.status == WeatherModel.WeatherStatus.OBTAINING_LOCATION);

            snackbarHelper.dismiss();

            switch (weatherModel.status) {
                case SUCCESS:
                    updateWeather(weatherModel);

                    WeatherWorkManager.enqueueNotificationWorker(this, true);

                    if (WeatherPreferences.getInstance(this).shouldShowPersistentNotification()) {
                        NotificationUtils.updatePersistentNotification(this, weatherModel.weatherLocation, weatherModel.currentWeather);
                    }

                    if (weatherModel.currentWeather.alerts != null) {
                        Promise.create((a) -> {
                            for (CurrentWeather.Alert alert : weatherModel.currentWeather.alerts) {
                                WeatherDatabase.getInstance(this).insertAlert(alert);
                            }
                        });

                        Bundle bundle = getIntent().getExtras();
                        CurrentWeather.Alert bundleAlert;

                        if (bundle != null &&
                                (bundleAlert = (CurrentWeather.Alert) bundle.getSerializable(MainActivity.EXTRA_ALERT)) != null) {
                            Intent intent = getIntent();

                            intent.removeExtra(MainActivity.EXTRA_ALERT);
                            intent.setAction(Intent.ACTION_MAIN);

                            setIntent(intent);

                            for (CurrentWeather.Alert alert : weatherModel.currentWeather.alerts) {
                                if (bundleAlert.getId() == alert.getId()) {
                                    dialogHelper.showAlert(bundleAlert);
                                }
                            }

                        }
                        break;
                    }

                    //TODO snackbar if currentweather is out-of-date

                    break;
                case OBTAINING_LOCATION:
                    snackbarHelper.notifyObtainingLocation();
                    break;
                case ERROR_OTHER:
                    snackbarHelper.notifyError(weatherModel.errorMessage, weatherModel.error);
                    break;
                case ERROR_LOCATION_ACCESS_DISALLOWED:
                    snackbarHelper.notifyLocPermDenied(requestLocationPermissionLauncher);
                    break;
                case ERROR_LOCATION_DISABLED:
                    snackbarHelper.notifyLocDisabled();
                    break;
                case ERROR_LOCATION_UNAVAILABLE:
                    snackbarHelper.notifyNullLoc();
                    break;
            }
        });
    }

    protected void updateWeather(WeatherModel weatherModel) {
        WeatherPreferences weatherPreferences = WeatherPreferences.getInstance(this);

        if (WeatherPreferences.getInstance(this).shouldDoGadgetbridgeBroadcast()) {
            Gadgetbridge.getInstance().broadcastWeather(this, weatherModel.weatherLocation, weatherModel.currentWeather);
        }

        if (weatherModel.weatherLocation.isCurrentLocation &&
                !weatherLocationManager.isBackgroundLocationPermissionGranted(this) &&
                weatherLocationManager.isLocationPermissionGranted(this) &&
                weatherPreferences.shouldRunBackgroundJob()) {
            snackbarHelper.notifyBackLocPermDenied(requestLocationPermissionLauncher,
                    WeatherPreferences.getInstance(this).shouldShowNotifications());
        }

        checkNotificationPermission();

        weatherCardRecyclerView.update(weatherModel);
    }

    private void checkNotificationPermission() {
        if (!NotificationUtils.canShowNotifications(this) &&
                WeatherPreferences.getInstance(this).shouldShowNotifications()) {
            snackbarHelper.notifyNotificationPermissionDenied(requestNotificationPermissionLauncher);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        for (LifecycleListener lifecycleListener : lifecycleListeners) {
            lifecycleListener.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        openSettingsIfNotInitialized();

        ColorHelper
                .getInstance(this)
                .setNightMode(this);

        for (LifecycleListener lifecycleListener : lifecycleListeners) {
            lifecycleListener.onResume();
        }

        getWeather();
        snackbarHelper.dismiss();
        NotificationUtils.dismissAllAlerts(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        for (LifecycleListener lifecycleListener : lifecycleListeners) {
            lifecycleListener.onStop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        for (LifecycleListener lifecycleListener : lifecycleListeners) {
            lifecycleListener.onDestroy();
        }
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);

        onReceiveIntent(intent);
    }

    abstract void onReceiveIntent(Intent intent);

    private void openSettingsIfNotInitialized() {
        if (!isInitialized()) {
            this.startActivity(new Intent(this, SettingsActivity.class), null);
            finish();
        }
    }

    private boolean isInitialized() {
        try {
            return Promise.create((a) -> WeatherPreferences.getInstance(this).isInitialized() &&
                    WeatherDatabase.getInstance(this).locationDao().getCount() > 0).await();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        for (LifecycleListener lifecycleListener : lifecycleListeners) {
            lifecycleListener.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();

        for (LifecycleListener lifecycleListener : lifecycleListeners) {
            lifecycleListener.onLowMemory();
        }
    }

    @Override
    public void addLifecycleListener(LifecycleListener lifecycleListener) {
        this.lifecycleListeners.add(lifecycleListener);
    }

    public static class WeatherViewModel extends AndroidViewModel {
        private MutableLiveData<WeatherModel> weatherModelLiveData;
        private MutableLiveData<OpenCloseState> fullscreenModel;
        private LiveData<List<WeatherDatabase.WeatherLocation>> locationModel;
        private LiveData<WeatherCardType[]> layoutCardModel;
        private LiveData<WeatherCardType[]> forecastLayoutCardModel;

        public WeatherViewModel(@NonNull Application application) {
            super(application);
        }

        public MutableLiveData<WeatherModel> getWeatherModel() {
            if (weatherModelLiveData == null) {
                weatherModelLiveData = new MutableLiveData<>();
            }

            return weatherModelLiveData;
        }

        public MutableLiveData<OpenCloseState> getFullscreenModel() {
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

        public LiveData<WeatherCardType[]> getCurrentLayoutCardsModel() {
            if (layoutCardModel == null) {
                layoutCardModel = WeatherDatabase.getInstance(this.getApplication().getApplicationContext()).cardDao().getEnabledCurrentWeatherCards();
            }

            return layoutCardModel;
        }

        public LiveData<WeatherCardType[]> getForecastLayoutCardsModel() {
            if (forecastLayoutCardModel == null) {
                forecastLayoutCardModel = WeatherDatabase.getInstance(this.getApplication().getApplicationContext()).cardDao().getEnabledForecastWeatherCards();
            }

            return forecastLayoutCardModel;
        }
    }
}
