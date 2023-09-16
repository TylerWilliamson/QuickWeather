/*
 *   Copyright 2019 - 2023 Tyler Williamson
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

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.ominous.quickweather.R;
import com.ominous.quickweather.api.openmeteo.OpenMeteo;
import com.ominous.quickweather.api.openweather.OpenWeatherMap;
import com.ominous.quickweather.data.WeatherDatabase;
import com.ominous.quickweather.dialog.LocaleDialog;
import com.ominous.quickweather.dialog.LocationManualDialog;
import com.ominous.quickweather.dialog.LocationMapDialog;
import com.ominous.quickweather.dialog.LocationSearchDialog;
import com.ominous.quickweather.dialog.OnLocationChosenListener;
import com.ominous.quickweather.location.LocationDisabledException;
import com.ominous.quickweather.location.LocationPermissionNotAvailableException;
import com.ominous.quickweather.location.WeatherLocationManager;
import com.ominous.quickweather.pref.Enabled;
import com.ominous.quickweather.pref.OwmApiVersion;
import com.ominous.quickweather.pref.RadarQuality;
import com.ominous.quickweather.pref.SpeedUnit;
import com.ominous.quickweather.pref.TemperatureUnit;
import com.ominous.quickweather.pref.Theme;
import com.ominous.quickweather.pref.WeatherPreferences;
import com.ominous.quickweather.pref.WeatherProvider;
import com.ominous.quickweather.util.ColorHelper;
import com.ominous.quickweather.util.DialogHelper;
import com.ominous.quickweather.util.NotificationUtils;
import com.ominous.quickweather.util.SnackbarHelper;
import com.ominous.quickweather.view.LocationDragListView;
import com.ominous.tylerutils.activity.OnboardingActivity2;
import com.ominous.tylerutils.anim.OpenCloseHandler;
import com.ominous.tylerutils.anim.OpenCloseState;
import com.ominous.tylerutils.async.Promise;
import com.ominous.tylerutils.browser.CustomTabs;
import com.ominous.tylerutils.util.ApiUtils;
import com.ominous.tylerutils.util.ViewUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.os.LocaleListCompat;
import androidx.recyclerview.widget.RecyclerView;

//TODO snackbar error message if no locations, switch to location tab
public class SettingsActivity extends OnboardingActivity2 implements ILifecycleAwareActivity {
    public final static String EXTRA_WEATHERLOCATION = "extra_weatherlocation";
    public final static String EXTRA_GOTOPAGE = "extra_gotopage";

    private DialogHelper dialogHelper;
    private final WeatherLocationManager weatherLocationManager = WeatherLocationManager.getInstance();
    private LifecycleListener lifecycleListener;

    private final ActivityResultLauncher<String> notificationRequestLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), grantedResults -> {
        for (OnboardingContainer onboardingContainer : getOnboardingContainers()) {
            if (onboardingContainer instanceof UnitsPageContainer) {
                ((UnitsPageContainer) onboardingContainer).checkIfNotificationAllowed();
            }
        }
    });

    private final ActivityResultLauncher<String[]> backgroundLocationRequestLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), grantedResults -> {
        for (OnboardingContainer onboardingContainer : getOnboardingContainers()) {
            if (onboardingContainer instanceof UnitsPageContainer) {
                ((UnitsPageContainer) onboardingContainer).checkIfBackgroundLocationEnabled();
            }
        }
    });

    private final ActivityResultLauncher<String[]> hereRequestLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), grantedResults -> {
        Boolean locationResult = grantedResults.get(Manifest.permission.ACCESS_COARSE_LOCATION);

        if (Boolean.TRUE.equals(locationResult)) {
            for (OnboardingContainer onboardingContainer : getOnboardingContainers()) {
                if (onboardingContainer instanceof LocationPageContainer) {
                    ((LocationPageContainer) onboardingContainer).addHere();
                }
            }
        }

        if (Build.VERSION.SDK_INT >= 23 &&
                !Boolean.TRUE.equals(locationResult) &&
                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            dialogHelper.showLocationRationale();
        }
    });

    private final ActivityResultLauncher<String[]> currentLocationRequestLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), grantedResults -> {
        Boolean locationResult = grantedResults.get(Manifest.permission.ACCESS_COARSE_LOCATION);

        for (OnboardingContainer onboardingContainer : getOnboardingContainers()) {
            if (onboardingContainer instanceof LocationPageContainer) {
                LocationPageContainer locationPageContainer = (LocationPageContainer) onboardingContainer;

                locationPageContainer.checkLocationSnackbar();

                if (Boolean.TRUE.equals(locationResult)) {
                    locationPageContainer.addCurrentLocation();
                }
            }
        }

        if (Build.VERSION.SDK_INT >= 23 &&
                !Boolean.TRUE.equals(locationResult) &&
                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            dialogHelper.showLocationRationale();
        }
    });

    @Override
    protected void onStart() {
        super.onStart();

        if (lifecycleListener != null) {
            lifecycleListener.onStart();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initActivity();

        dialogHelper = new DialogHelper(this);

        CustomTabs.getInstance(this).setColor(ContextCompat.getColor(this, R.color.color_accent));

        if (getIntent().hasExtra(EXTRA_WEATHERLOCATION)) {
            this.findViewById(android.R.id.content).post(() -> setCurrentPage(2));
        } else if (getIntent().hasExtra(EXTRA_GOTOPAGE)) {
            this.findViewById(android.R.id.content).post(() -> setCurrentPage(getIntent().getIntExtra(EXTRA_GOTOPAGE, 1)));
        }

        if (lifecycleListener != null) {
            lifecycleListener.onCreate(savedInstanceState);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (lifecycleListener != null) {
            lifecycleListener.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (lifecycleListener != null) {
            lifecycleListener.onResume();
        }
    }

    private void initActivity() {
        ColorHelper.getInstance(this);//Initializing after Activity created to get day/night properly

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
    }

    @Override
    public void onFinish() {
        WeatherPreferences.getInstance(this).commitChanges();

        ContextCompat.startActivity(this, new Intent(this, MainActivity.class), null);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (lifecycleListener != null) {
            lifecycleListener.onStop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (lifecycleListener != null) {
            lifecycleListener.onDestroy();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        if (lifecycleListener != null) {
            lifecycleListener.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();

        if (lifecycleListener != null) {
            lifecycleListener.onLowMemory();
        }
    }

    @Override
    public List<OnboardingContainer> createOnboardingContainers() {
        final ArrayList<OnboardingContainer> containers;

        if (WeatherPreferences.getInstance(this).isInitialized()) {
            containers = new ArrayList<>(3);
        } else {
            containers = new ArrayList<>(4);
            containers.add(new WelcomePageContainer(this));
        }

        containers.add(new ProviderPageContainer(this));
        containers.add(new LocationPageContainer(this));
        containers.add(new UnitsPageContainer(this));

        return containers;
    }

    @Override
    public OnboardingContainer createAdvancedMenuOnboardingContainer() {
        return new AdvancedSettingsContainer(this);
    }

    @Override
    public void finish() {
        super.finish();
        this.overridePendingTransition(R.anim.slide_right_in, R.anim.slide_left_out);
    }

    @Override
    public void setLifecycleListener(LifecycleListener lifecycleListener) {
        this.lifecycleListener = lifecycleListener;
    }

    private static class WelcomePageContainer extends OnboardingContainer {
        public WelcomePageContainer(Context context) {
            super(context);
        }

        @Override
        public int getViewRes() {
            return R.layout.fragment_welcome;
        }

        @Override
        public boolean canAdvanceToNextPage() {
            return true;
        }
    }

    private class LocationPageContainer extends OnboardingContainer implements View.OnClickListener {
        private final static String KEY_LOCATIONS = "locationList";
        private LocationDragListView dragListView;
        private final OnLocationChosenListener onLocationChosenListener = (name, latitude, longitude) -> addLocation(new WeatherDatabase.WeatherLocation(
                0,
                latitude,
                longitude,
                name,
                false,
                false,
                0
        ));
        private MaterialButton currentLocationButton;
        private MaterialButton otherLocationButton;
        private MaterialButton mapButton;
        private MaterialButton thisLocationButton;
        private List<WeatherDatabase.WeatherLocation> locations;
        private LocationAdapterDataObserver locationAdapterDataObserver;
        private boolean hasShownBundledLocation = false;
        private Promise<Void, Void> lastDatabaseUpdate;
        private SnackbarHelper snackbarHelper;
        private LocationSearchDialog locationSearchDialog;
        private LocationManualDialog locationManualDialog;
        private LocationMapDialog locationMapDialog;

        public LocationPageContainer(Context context) {
            super(context);
        }

        @Override
        public int getViewRes() {
            return R.layout.fragment_location;
        }

        @Override
        public boolean canAdvanceToNextPage() {
            return dragListView.getLocationList().size() > 0;
        }

        @Override
        public void onCreateView(View v) {
            dragListView = v.findViewById(R.id.drag_list_view);
            currentLocationButton = v.findViewById(R.id.button_current_location);
            otherLocationButton = v.findViewById(R.id.button_other_location);
            mapButton = v.findViewById(R.id.button_map);
            thisLocationButton = v.findViewById(R.id.button_here);

            snackbarHelper = new SnackbarHelper(v.findViewById(R.id.viewpager_coordinator));

            locationManualDialog = new LocationManualDialog(v.getContext());
            locationSearchDialog = new LocationSearchDialog(v.getContext());
            locationMapDialog = new LocationMapDialog(v.getContext(), SettingsActivity.this);
        }

        @Override
        public void onBindView(View v) {
            Promise.create((a) -> {
                if (locations == null) {
                    locations = WeatherDatabase.getInstance(v.getContext()).locationDao().getAllWeatherLocations();
                }

                SettingsActivity.this.runOnUiThread(() -> {
                    dragListView.setLocationList(locations);

                    if (locationAdapterDataObserver == null) {
                        locationAdapterDataObserver = new LocationAdapterDataObserver();
                        dragListView.getAdapter().registerAdapterDataObserver(locationAdapterDataObserver);
                    }
                });
            });

            currentLocationButton.setOnClickListener(this);
            otherLocationButton.setOnClickListener(this);
            mapButton.setOnClickListener(this);
            thisLocationButton.setOnClickListener(this);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onSaveInstanceState(@NonNull Bundle outState) {
            outState.putParcelableArrayList(KEY_LOCATIONS, (ArrayList<WeatherDatabase.WeatherLocation>) dragListView.getAdapter().getItemList());
        }

        @Override
        public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
            locations = savedInstanceState.getParcelableArrayList(KEY_LOCATIONS);
        }

        private void addCurrentLocation() {
            if (!isCurrentLocationSelected()) {
                addLocation(new WeatherDatabase.WeatherLocation(
                        0,
                        0,
                        0,
                        getContext().getString(R.string.text_current_location),
                        false,
                        true,
                        0
                ));
            }
        }

        private void addHere() {
            if (weatherLocationManager.isLocationPermissionGranted(getContext())) {
                thisLocationButton.setEnabled(false);
                snackbarHelper.notifyObtainingLocation();

                Promise.create((a) -> {
                    Location l = weatherLocationManager.obtainCurrentLocation(getContext(), false);

                    SettingsActivity.this.runOnUiThread(() -> {
                        snackbarHelper.dismiss();
                        thisLocationButton.setEnabled(true);
                    });

                    return l;
                }, e -> {
                    if (e instanceof LocationPermissionNotAvailableException) {
                        snackbarHelper.notifyLocPermDenied(SettingsActivity.this.hereRequestLauncher);
                    } else if (e instanceof LocationDisabledException) {
                        snackbarHelper.notifyLocDisabled();
                    } else {
                        snackbarHelper.notifyNullLoc();
                    }
                }).then((l) -> {
                    if (l != null) {
                        runOnUiThread(() ->
                                locationManualDialog.show(new WeatherDatabase.WeatherLocation(l.getLatitude(), l.getLongitude(), null), onLocationChosenListener));
                    } else {
                        snackbarHelper.notifyNullLoc();
                    }
                });
            } else {
                weatherLocationManager.showLocationDisclosure(getContext(), () -> {
                    WeatherPreferences.getInstance(getContext()).setShowLocationDisclosure(Enabled.DISABLED);

                    weatherLocationManager.requestLocationPermissions(getContext(), SettingsActivity.this.hereRequestLauncher);
                });
            }
        }

        private void addLocation(WeatherDatabase.WeatherLocation weatherLocation) {
            dragListView.addLocation(weatherLocation);
        }

        private void setCurrentLocationEnabled(boolean enabled) {
            currentLocationButton.setEnabled(enabled);
        }

        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        public boolean isCurrentLocationSelected() {
            for (WeatherDatabase.WeatherLocation weatherLocation : dragListView.getLocationList()) {
                if (weatherLocation.isCurrentLocation) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public void onPageSelected() {
            checkLocationSnackbar();

            WeatherDatabase.WeatherLocation weatherLocation = !hasShownBundledLocation &&
                    SettingsActivity.this.getIntent() != null ?
                    SettingsActivity.this.getIntent().getParcelableExtra(EXTRA_WEATHERLOCATION) : null;

            hasShownBundledLocation = true;

            if (weatherLocation != null) {
                locationManualDialog.show(weatherLocation, onLocationChosenListener);
            }
        }

        @Override
        public void onPageDeselected() {
            dismissSnackbar();
        }

        public void checkLocationSnackbar() {
            if (currentLocationButton != null && snackbarHelper != null) {
                if (!currentLocationButton.isEnabled() && !weatherLocationManager.isLocationPermissionGranted(getContext())) {
                    snackbarHelper.notifyLocPermDenied(SettingsActivity.this.currentLocationRequestLauncher);
                } else {
                    dismissSnackbar();
                }
            }
        }

        private void dismissSnackbar() {
            if (snackbarHelper != null) {
                snackbarHelper.dismiss();
            }
        }

        @Override
        public void onClick(final View v) {
            if (v.getId() == R.id.button_current_location) {
                if (!isCurrentLocationSelected()) {
                    if (weatherLocationManager.isLocationPermissionGranted(v.getContext())) {
                        addCurrentLocation();
                        v.setEnabled(false);
                    } else {
                        weatherLocationManager.showLocationDisclosure(v.getContext(), () -> {
                            WeatherPreferences.getInstance(getContext()).setShowLocationDisclosure(Enabled.DISABLED);

                            weatherLocationManager.requestLocationPermissions(v.getContext(), SettingsActivity.this.currentLocationRequestLauncher);
                        });
                    }
                }
            } else if (v.getId() == R.id.button_other_location) {
                locationSearchDialog.show(onLocationChosenListener);
            } else if (v.getId() == R.id.button_map) {
                locationMapDialog.show(onLocationChosenListener);
            } else if (v.getId() == R.id.button_here) {
                addHere();
            }
        }

        private void updateLocations() {
            Promise.VoidPromiseCallable<Void> callable = (a) -> {
                WeatherDatabase.WeatherLocationDao weatherDatabaseDao = WeatherDatabase.getInstance(getContext()).locationDao();

                List<WeatherDatabase.WeatherLocation> newWeatherLocations = dragListView.getLocationList();
                List<WeatherDatabase.WeatherLocation> oldWeatherLocations = weatherDatabaseDao.getAllWeatherLocations();

                for (WeatherDatabase.WeatherLocation oldWeatherLocation : oldWeatherLocations) {
                    boolean wasFound = false;

                    for (WeatherDatabase.WeatherLocation newWeatherLocation : newWeatherLocations) {
                        wasFound = wasFound || oldWeatherLocation.id == newWeatherLocation.id;
                    }

                    if (!wasFound) {
                        weatherDatabaseDao.delete(oldWeatherLocation);
                    }
                }

                boolean selectedExists = false;

                for (WeatherDatabase.WeatherLocation weatherLocation : newWeatherLocations) {
                    selectedExists = selectedExists || weatherLocation.isSelected;
                }

                if (!selectedExists && newWeatherLocations.size() > 0) {
                    newWeatherLocations.get(0).isSelected = true;
                }

                for (int i = 0, l = newWeatherLocations.size(); i < l; i++) {
                    WeatherDatabase.WeatherLocation weatherLocation = newWeatherLocations.get(i);
                    weatherLocation.order = i;

                    if (weatherLocation.id > 0) {
                        weatherDatabaseDao.update(weatherLocation);
                    } else {
                        int id = (int) weatherDatabaseDao.insert(weatherLocation);

                        newWeatherLocations.set(i,
                                new WeatherDatabase.WeatherLocation(
                                        id,
                                        weatherLocation.latitude,
                                        weatherLocation.longitude,
                                        weatherLocation.name,
                                        weatherLocation.isSelected,
                                        weatherLocation.isCurrentLocation,
                                        weatherLocation.order));
                    }
                }
            };

            //TODO: Louder errors
            if (lastDatabaseUpdate == null || !(lastDatabaseUpdate.getState() == Promise.PromiseState.STARTED ||
                    lastDatabaseUpdate.getState() == Promise.PromiseState.NOT_STARTED)) {
                lastDatabaseUpdate = Promise.create(callable, Throwable::printStackTrace);
            } else {
                lastDatabaseUpdate = lastDatabaseUpdate.then(callable, Throwable::printStackTrace);
            }
        }

        @Override
        public void onFinish() {
            if (lastDatabaseUpdate != null) {
                try {
                    lastDatabaseUpdate.await();
                } catch (ExecutionException | InterruptedException e) {
                    //
                }
            }
        }

        //TODO Smarter Database Updates - Currently doUpdate gets called multiple times per update
        private class LocationAdapterDataObserver extends RecyclerView.AdapterDataObserver {
            LocationAdapterDataObserver() {
                doUpdate();
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                doUpdate();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                doUpdate();
            }

            @Override
            public void onItemRangeChanged(int positionStart, int itemCount) {
                doUpdate();
            }

            @Override
            public void onItemRangeChanged(int positionStart, int itemCount, @Nullable Object payload) {
                doUpdate();
            }

            @Override
            public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                doUpdate();
            }

            @Override
            public void onChanged() {
                doUpdate();
            }

            private void doUpdate() {
                setCurrentLocationEnabled(!isCurrentLocationSelected());

                notifyViewPager();

                updateLocations();
            }
        }
    }

    private class UnitsPageContainer extends OnboardingContainer {
        private final static String KEY_TEMPERATURE = "temperature";
        private final static String KEY_SPEED = "speed";
        private final static String KEY_THEME = "theme";
        private final static String KEY_ALERTNOTIF = "alertnotif";
        private final static String KEY_PERSISTNOTIF = "persistnotif";
        private final static String KEY_GADGETBRIDGE = "gadgetbridge";
        private UnitsButtonGroup<Enabled> alertsButtonGroup;
        private UnitsButtonGroup<Enabled> persistentButtonGroup;
        private Enabled gadgetbridgeEnabled = null;
        private Enabled alertNotifEnabled = null;
        private Enabled persistNotifEnabled = null;

        private SpeedUnit speed = null;
        private Theme theme = null;
        private TemperatureUnit temperature = null;
        private SnackbarHelper snackbarHelper;

        public UnitsPageContainer(Context context) {
            super(context);
        }

        @Override
        public int getViewRes() {
            return R.layout.fragment_units;
        }

        @Override
        public void onCreateView(View v) {
            snackbarHelper = new SnackbarHelper(v.findViewById(R.id.viewpager_coordinator));
        }

        @Override
        public void onBindView(View v) {
            if (speed == null) {
                speed = WeatherPreferences.getInstance(getContext()).getSpeedUnit();
            }

            if (temperature == null) {
                temperature = WeatherPreferences.getInstance(getContext()).getTemperatureUnit();
            }

            if (theme == null) {
                theme = WeatherPreferences.getInstance(getContext()).getTheme();
            }

            if (alertNotifEnabled == null) {
                alertNotifEnabled = WeatherPreferences.getInstance(getContext()).getShowAlertNotification();
            }

            if (persistNotifEnabled == null) {
                persistNotifEnabled = WeatherPreferences.getInstance(getContext()).getShowPersistentNotification();
            }

            if (gadgetbridgeEnabled == null) {
                gadgetbridgeEnabled = WeatherPreferences.getInstance(getContext()).getGadgetbridgeEnabled();
            }

            new UnitsButtonGroup<TemperatureUnit>(v, temperature -> {
                WeatherPreferences.getInstance(getContext()).setTemperatureUnit(this.temperature = temperature);
                notifyViewPagerConditionally();
                checkIfBackgroundLocationEnabled();
            })
                    .addButton(R.id.button_fahrenheit, TemperatureUnit.FAHRENHEIT)
                    .addButton(R.id.button_celsius, TemperatureUnit.CELSIUS)
                    .selectButton(temperature);

            new UnitsButtonGroup<SpeedUnit>(v, speed -> {
                WeatherPreferences.getInstance(getContext()).setSpeedUnit(this.speed = speed);
                notifyViewPagerConditionally();
                checkIfBackgroundLocationEnabled();
            })
                    .addButton(R.id.button_mph, SpeedUnit.MPH)
                    .addButton(R.id.button_kmh, SpeedUnit.KMH)
                    .addButton(R.id.button_ms, SpeedUnit.MS)
                    .addButton(R.id.button_kn, SpeedUnit.KN)
                    .selectButton(speed);

            new UnitsButtonGroup<Theme>(v, theme -> {
                checkIfBackgroundLocationEnabled();
                if (this.theme != theme) {
                    WeatherPreferences.getInstance(getContext()).setTheme(this.theme = theme);
                    notifyViewPagerConditionally();
                    ColorHelper
                            .getInstance(getContext())
                            .setNightMode(getContext());
                }
            })
                    .addButton(R.id.button_theme_light, Theme.LIGHT)
                    .addButton(R.id.button_theme_dark, Theme.DARK)
                    .addButton(R.id.button_theme_auto, Theme.AUTO)
                    .selectButton(theme);

            alertsButtonGroup = new UnitsButtonGroup<Enabled>(v, alertNotifEnabled -> {
                WeatherPreferences.getInstance(getContext()).setShowAlertNotification(this.alertNotifEnabled = alertNotifEnabled);
                notifyViewPagerConditionally();
                checkIfBackgroundLocationEnabled();

                if (Build.VERSION.SDK_INT >= 33 && alertNotifEnabled == Enabled.ENABLED) {
                    notificationRequestLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                }
            })
                    .addButton(R.id.button_alert_notif_enabled, Enabled.ENABLED)
                    .addButton(R.id.button_alert_notif_disabled, Enabled.DISABLED);

            alertsButtonGroup.selectButton(alertNotifEnabled);

            persistentButtonGroup = new UnitsButtonGroup<Enabled>(v, persistNotifEnabled -> {
                WeatherPreferences.getInstance(getContext()).setShowPersistentNotification(this.persistNotifEnabled = persistNotifEnabled);
                notifyViewPagerConditionally();
                checkIfBackgroundLocationEnabled();

                if (Build.VERSION.SDK_INT >= 33 && persistNotifEnabled == Enabled.ENABLED) {
                    notificationRequestLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                }
            })
                    .addButton(R.id.button_weather_notif_enabled, Enabled.ENABLED)
                    .addButton(R.id.button_weather_notif_disabled, Enabled.DISABLED);

            persistentButtonGroup.selectButton(persistNotifEnabled);

            new UnitsButtonGroup<Enabled>(v, gadgetbridgeEnabled -> {
                WeatherPreferences.getInstance(getContext()).setGadgetbridgeEnabled(this.gadgetbridgeEnabled = gadgetbridgeEnabled);
                notifyViewPagerConditionally();
                checkIfBackgroundLocationEnabled();
            })
                    .addButton(R.id.button_gadgetbridge_enabled, Enabled.ENABLED)
                    .addButton(R.id.button_gadgetbridge_disabled, Enabled.DISABLED)
                    .selectButton(gadgetbridgeEnabled);

            notifyViewPagerConditionally();
        }

        @Override
        public void onSaveInstanceState(@NonNull Bundle outState) {
            outState.putString(KEY_TEMPERATURE, temperature.getValue());
            outState.putString(KEY_SPEED, speed.getValue());
            outState.putString(KEY_THEME, theme.getValue());
            outState.putString(KEY_ALERTNOTIF, alertNotifEnabled.getValue());
            outState.putString(KEY_PERSISTNOTIF, persistNotifEnabled.getValue());
            outState.putString(KEY_GADGETBRIDGE, gadgetbridgeEnabled.getValue());
        }

        @Override
        public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
            temperature = TemperatureUnit.from(savedInstanceState.getString(KEY_TEMPERATURE), TemperatureUnit.DEFAULT);
            speed = SpeedUnit.from(savedInstanceState.getString(KEY_SPEED), SpeedUnit.DEFAULT);
            theme = Theme.from(savedInstanceState.getString(KEY_THEME), Theme.DEFAULT);
            alertNotifEnabled = Enabled.from(savedInstanceState.getString(KEY_ALERTNOTIF), Enabled.DEFAULT);
            persistNotifEnabled = Enabled.from(savedInstanceState.getString(KEY_PERSISTNOTIF), Enabled.DEFAULT);
            gadgetbridgeEnabled = Enabled.from(savedInstanceState.getString(KEY_GADGETBRIDGE), Enabled.DEFAULT);
        }

        private void notifyViewPagerConditionally() {
            if (canAdvanceToNextPage()) {
                notifyViewPager();
            }
        }

        @Override
        public void onPageSelected() {
            checkIfBackgroundLocationEnabled();

            WeatherProvider weatherProvider = WeatherPreferences.getInstance(getContext()).getWeatherProvider();

            if (alertsButtonGroup != null) {
                if (weatherProvider == WeatherProvider.OPENMETEO) {
                    alertsButtonGroup.setEnabled(false);
                    alertsButtonGroup.selectButton(Enabled.DISABLED);
                } else {
                    alertsButtonGroup.setEnabled(true);
                }
            }
        }

        @Override
        public void onPageDeselected() {
            dismissSnackbar();
        }

        @Override
        public boolean canAdvanceToNextPage() {
            return temperature != TemperatureUnit.DEFAULT &&
                    speed != SpeedUnit.DEFAULT &&
                    theme != Theme.DEFAULT &&
                    alertNotifEnabled != null &&
                    persistNotifEnabled != null &&
                    gadgetbridgeEnabled != null;
        }

        public void checkIfBackgroundLocationEnabled() {
            Promise.create(a -> {
                if (WeatherDatabase.getInstance(getContext()).locationDao().isCurrentLocationSelected() &&
                        WeatherPreferences.getInstance(getContext()).shouldRunBackgroundJob()) {
                    if (!weatherLocationManager.isBackgroundLocationPermissionGranted(getContext())) {
                        SettingsActivity.this.runOnUiThread(() ->
                                snackbarHelper.notifyBackLocPermDenied(SettingsActivity.this.backgroundLocationRequestLauncher, WeatherPreferences.getInstance(getContext()).shouldShowNotifications()));
                    }
                } else {
                    SettingsActivity.this.runOnUiThread(this::dismissSnackbar);
                }
            });
        }

        public void checkIfNotificationAllowed() {
            if (!NotificationUtils.canShowNotifications(getContext())) {
                persistentButtonGroup.selectButton(Enabled.DISABLED);
                alertsButtonGroup.selectButton(Enabled.DISABLED);
            }
        }

        private void dismissSnackbar() {
            if (snackbarHelper != null) {
                snackbarHelper.dismiss();
            }
        }
    }

    private class ProviderPageContainer extends OnboardingContainer implements View.OnClickListener {
        private final static String KEY_PROVIDER = "provider";
        private final static String KEY_OWM_APIKEY = "owmApiKey";
        private final static String KEY_OWM_APIKEY_STATE = "owmApiKeyState";
        private final static String KEY_OPENMETEO_APIKEY = "openmeteoApiKey";
        private final static String KEY_OPENMETEO_INSTANCE = "openmeteoInstance";
        private final static String KEY_OPENMETEO_STATE = "openmeteoState";

        private ImageView owmIcon;
        private ImageView openMeteoIcon;
        private LinearLayout owmTitle;
        private LinearLayout openMeteoTitle;
        private ConstraintLayout owmFrame;
        private ConstraintLayout openMeteoFrame;
        private Space spacer;

        private TextInputEditText owmApiKeyEditText;
        private TextInputLayout owmApiKeyEditTextLayout;
        private MaterialButton owmTestApiKeyButton;
        private LinearProgressIndicator owmTestApiProgressIndicator;

        private TextInputEditText openmeteoApiKeyEditText;
        private TextInputLayout openmeteoApiKeyEditTextLayout;
        private TextInputEditText openmeteoInstanceEditText;
        private TextInputLayout openmeteoInstanceEditTextLayout;
        private MaterialButton openmeteoTestConnectionButton;
        private LinearProgressIndicator openmeteoTestConnectionProgressIndicator;

        private WeatherProvider weatherProvider = WeatherProvider.DEFAULT;
        private ApiKeyState owmApiKeyState = ApiKeyState.NULL;
        private ApiKeyState openmeteoState = ApiKeyState.NULL;

        private SnackbarHelper snackbarHelper;

        private OpenCloseHandler providerOpenCloseHandler;

        private ColorStateList greenColorStateList;
        private ColorStateList defaultColorStateList;

        public ProviderPageContainer(Context context) {
            super(context);
        }

        @Override
        public void onCreateView(View v) {
            owmApiKeyEditText = v.findViewById(R.id.onboarding_owm_apikey_edittext);
            owmApiKeyEditTextLayout = v.findViewById(R.id.onboarding_owm_apikey_edittext_layout);
            owmTestApiKeyButton = v.findViewById(R.id.test_owm_api_key);
            owmTestApiProgressIndicator = v.findViewById(R.id.onboarding_owm_apikey_progress);

            openmeteoApiKeyEditText = v.findViewById(R.id.onboarding_openmeteo_apikey_edittext);
            openmeteoApiKeyEditTextLayout = v.findViewById(R.id.onboarding_openmeteo_apikey_edittext_layout);
            openmeteoInstanceEditText = v.findViewById(R.id.onboarding_openmeteo_instance_edittext);
            openmeteoInstanceEditTextLayout = v.findViewById(R.id.onboarding_openmeteo_instance_edittext_layout);
            openmeteoTestConnectionButton = v.findViewById(R.id.test_openmeteo_connection);
            openmeteoTestConnectionProgressIndicator = v.findViewById(R.id.onboarding_openmeteo_apikey_progress);

            owmTitle = v.findViewById(R.id.provider_owm_title);
            owmFrame = v.findViewById(R.id.provider_owm_frame);
            owmIcon = v.findViewById(R.id.provider_owm_icon);
            openMeteoTitle = v.findViewById(R.id.provider_openmeteo_title);
            openMeteoFrame = v.findViewById(R.id.provider_openmeteo_frame);
            openMeteoIcon = v.findViewById(R.id.provider_openmeteo_icon);
            spacer = v.findViewById(R.id.spacer);

            ViewUtils.setAccessibilityInfo(openMeteoTitle, getString(R.string.onboarding_provider_title_click_action), null);
            ViewUtils.setAccessibilityInfo(owmTitle, getString(R.string.onboarding_provider_title_click_action), null);

            setUpAnimators();
        }

        @Override
        public void onBindView(View v) {
            if (getContext() != null) {
                ViewUtils.setEditTextCursorColor(owmApiKeyEditText, ContextCompat.getColor(getContext(), R.color.color_accent_text));
            }

            snackbarHelper = new SnackbarHelper(owmApiKeyEditTextLayout);

            WeatherPreferences weatherPreferences = WeatherPreferences.getInstance(getContext());

            weatherProvider = weatherPreferences.getWeatherProvider();

            if (owmApiKeyState == ApiKeyState.NULL) {
                String apiKey = weatherPreferences.getOWMAPIKey();

                if (apiKey.isEmpty()) {
                    //TODO Test api key, dont assume
                    setOwmApiKeyState(ApiKeyState.NEUTRAL);
                } else {
                    owmApiKeyEditText.setText(apiKey);

                    setOwmApiKeyState(ApiKeyState.PASS);
                }
            } else {
                setOwmApiKeyState(owmApiKeyState);
            }

            if (openmeteoState == ApiKeyState.NULL) {
                String apiKey = weatherPreferences.getOpenMeteoAPIKey();
                String instance = weatherPreferences.getOpenMeteoInstance();

                //TODO Test api key + instance, dont assume
                openmeteoInstanceEditText.setText(instance);
                openmeteoApiKeyEditText.setText(apiKey);
                setOpenMeteoApiKeyState(ApiKeyState.PASS);
            }

            owmApiKeyEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    setOwmApiKeyState(ApiKeyState.NEUTRAL);
                }
            });
            owmApiKeyEditText.setOnFocusChangeListener((v1, hasFocus) -> updateOwmApiKeyColors(owmApiKeyState));
            owmApiKeyEditText.setOnEditorActionListener((textView, i, keyEvent) -> {
                owmTestApiKeyButton.performClick();
                return true;
            });

            TextWatcher openMeteoTextWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    setOpenMeteoApiKeyState(ApiKeyState.NEUTRAL);
                }
            };

            View.OnFocusChangeListener openMeteoOnFocusChangeListener = (v1, hasFocus) -> updateOpenMeteoApiKeyColors(openmeteoState);
            TextView.OnEditorActionListener openMeteoEditorActionListener = (textView, i, keyEvent) -> {
                openmeteoTestConnectionButton.performClick();
                return true;
            };

            openmeteoApiKeyEditText.addTextChangedListener(openMeteoTextWatcher);
            openmeteoInstanceEditText.addTextChangedListener(openMeteoTextWatcher);
            openmeteoApiKeyEditText.setOnFocusChangeListener(openMeteoOnFocusChangeListener);
            openmeteoInstanceEditText.setOnFocusChangeListener(openMeteoOnFocusChangeListener);
            openmeteoApiKeyEditText.setOnEditorActionListener(openMeteoEditorActionListener);
            openmeteoInstanceEditText.setOnEditorActionListener(openMeteoEditorActionListener);

            owmTestApiKeyButton.setOnClickListener(this);
            openmeteoTestConnectionButton.setOnClickListener(this);
            owmFrame.setOnClickListener(this);
            openMeteoFrame.setOnClickListener(this);

            owmTitle.setOnClickListener(view -> providerOpenCloseHandler.open());
            openMeteoTitle.setOnClickListener(view -> providerOpenCloseHandler.close());

            providerOpenCloseHandler.setState(OpenCloseState.NULL);

            switch (weatherProvider) {
                case OPENMETEO:
                    providerOpenCloseHandler.close(true);
                    break;
                case OPENWEATHERMAP:
                    providerOpenCloseHandler.open(true);
                    break;
            }
        }

        private void setUpAnimators() {
            Drawable openTitleBackground = ContextCompat.getDrawable(getContext(), R.drawable.provider_title_open_background);
            Drawable closedTitleBackground = ContextCompat.getDrawable(getContext(), R.drawable.provider_title_closed_background);

            ValueAnimator owmAnimator = ValueAnimator.ofFloat(0, 1).setDuration(500);

            owmAnimator.addUpdateListener(valueAnimator -> {
                LinearLayout.LayoutParams owmLayoutParams = (LinearLayout.LayoutParams) owmFrame.getLayoutParams();
                owmLayoutParams.weight = valueAnimator.getAnimatedFraction();
                owmFrame.setLayoutParams(owmLayoutParams);

                LinearLayout.LayoutParams spacerLayoutParams = (LinearLayout.LayoutParams) spacer.getLayoutParams();

                if (spacerLayoutParams.weight > 0) {
                    spacerLayoutParams.weight = 1 - valueAnimator.getAnimatedFraction();
                    spacer.setLayoutParams(spacerLayoutParams);
                } else {
                    LinearLayout.LayoutParams openMeteoLayoutParams = (LinearLayout.LayoutParams) openMeteoFrame.getLayoutParams();
                    openMeteoLayoutParams.weight = 1 - valueAnimator.getAnimatedFraction();
                    openMeteoFrame.setLayoutParams(openMeteoLayoutParams);
                }
            });

            owmAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    owmIcon.setImageResource(R.drawable.ic_gps_fixed_white_24dp);
                    openMeteoIcon.setImageResource(R.drawable.ic_gps_not_fixed_white_24dp);
                    owmTitle.setBackground(openTitleBackground);
                }

                @Override
                public void onAnimationEnd(@NonNull Animator animation, boolean isReverse) {
                    weatherProvider = WeatherProvider.OPENWEATHERMAP;
                    WeatherPreferences.getInstance(getContext()).setWeatherProvider(weatherProvider);
                    openMeteoTitle.setBackground(closedTitleBackground);
                }
            });

            ValueAnimator openmeteoAnimator = ValueAnimator.ofFloat(0, 1).setDuration(500);

            openmeteoAnimator.addUpdateListener(valueAnimator -> {
                LinearLayout.LayoutParams openmeteoLayoutParams = (LinearLayout.LayoutParams) openMeteoFrame.getLayoutParams();
                openmeteoLayoutParams.weight = valueAnimator.getAnimatedFraction();
                openMeteoFrame.setLayoutParams(openmeteoLayoutParams);

                LinearLayout.LayoutParams spacerLayoutParams = (LinearLayout.LayoutParams) spacer.getLayoutParams();

                if (spacerLayoutParams.weight > 0) {
                    spacerLayoutParams.weight = 1 - valueAnimator.getAnimatedFraction();
                    spacer.setLayoutParams(spacerLayoutParams);
                } else {
                    LinearLayout.LayoutParams owmLayoutParams = (LinearLayout.LayoutParams) owmFrame.getLayoutParams();
                    owmLayoutParams.weight = 1 - valueAnimator.getAnimatedFraction();
                    owmFrame.setLayoutParams(owmLayoutParams);
                }
            });

            openmeteoAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    openMeteoIcon.setImageResource(R.drawable.ic_gps_fixed_white_24dp);
                    owmIcon.setImageResource(R.drawable.ic_gps_not_fixed_white_24dp);
                    openMeteoTitle.setBackground(openTitleBackground);
                }

                @Override
                public void onAnimationEnd(@NonNull Animator animation, boolean isReverse) {
                    weatherProvider = WeatherProvider.OPENMETEO;
                    WeatherPreferences.getInstance(getContext()).setWeatherProvider(weatherProvider);
                    owmTitle.setBackground(closedTitleBackground);
                }
            });

            providerOpenCloseHandler = new OpenCloseHandler(owmAnimator, openmeteoAnimator);
        }

        @Override
        public void onSaveInstanceState(@NonNull Bundle outState) {
            outState.putInt(KEY_PROVIDER, weatherProvider.ordinal());
            outState.putString(KEY_OWM_APIKEY, ViewUtils.editTextToString(owmApiKeyEditText));
            outState.putInt(KEY_OWM_APIKEY_STATE, owmApiKeyState.ordinal());
            outState.putString(KEY_OPENMETEO_APIKEY, ViewUtils.editTextToString(openmeteoApiKeyEditText));
            outState.putString(KEY_OPENMETEO_INSTANCE, ViewUtils.editTextToString(openmeteoInstanceEditText));
            outState.putInt(KEY_OPENMETEO_STATE, openmeteoState.ordinal());
        }

        @Override
        public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
            weatherProvider = WeatherProvider.values()[savedInstanceState.getInt(KEY_PROVIDER)];
            owmApiKeyEditText.setText(savedInstanceState.getString(KEY_OWM_APIKEY));
            owmApiKeyState = ApiKeyState.values()[savedInstanceState.getInt(KEY_OWM_APIKEY_STATE)];
            openmeteoApiKeyEditText.setText(savedInstanceState.getString(KEY_OPENMETEO_APIKEY));
            openmeteoInstanceEditText.setText(savedInstanceState.getString(KEY_OPENMETEO_INSTANCE));
            openmeteoState = ApiKeyState.values()[savedInstanceState.getInt(KEY_OPENMETEO_STATE)];
        }

        @Override
        public void onPageSelected() {
            if (owmApiKeyEditText != null) {
                if (weatherProvider == WeatherProvider.OPENWEATHERMAP) {
                    updateOwmApiKeyColors(owmApiKeyState);
                } else if (weatherProvider == WeatherProvider.OPENMETEO) {
                    updateOpenMeteoApiKeyColors(openmeteoState);
                }
            }
        }

        @Override
        public void onPageDeselected() {
            if (owmApiKeyEditText != null) {
                if (weatherProvider == WeatherProvider.OPENWEATHERMAP) {
                    updateOwmApiKeyColors(owmApiKeyState);
                } else if (weatherProvider == WeatherProvider.OPENMETEO) {
                    updateOpenMeteoApiKeyColors(openmeteoState);
                }
            }
        }

        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.test_owm_api_key) {
                String apiKeyText = ViewUtils.editTextToString(owmApiKeyEditText).replaceAll("[^0-9A-Za-z]", "");

                if (apiKeyText.length() > 0) {
                    owmTestApiKeyButton.setEnabled(false);
                    owmTestApiProgressIndicator.show();

                    owmApiKeyEditText.setEnabled(false);
                    owmApiKeyEditText.clearFocus();

                    Promise.create((a) -> {
                                OwmApiVersion apiVersion = OpenWeatherMap.getInstance()
                                        .determineApiVersion(apiKeyText);

                                SettingsActivity.this.runOnUiThread(() -> {
                                    owmTestApiProgressIndicator.hide();

                                    if (apiVersion == null) {
                                        setOwmApiKeyState(ApiKeyState.BAD_API_KEY);
                                    } else if (apiVersion == OwmApiVersion.WEATHER_2_5) {
                                        setOwmApiKeyState(ApiKeyState.NO_ONECALL);
                                    } else {
                                        setOwmApiKeyState(ApiKeyState.PASS);

                                        WeatherPreferences weatherPreferences = WeatherPreferences.getInstance(getContext());

                                        weatherPreferences.setWeatherProvider(WeatherProvider.OPENWEATHERMAP);
                                        weatherPreferences.setOWMAPIKey(apiKeyText);
                                        weatherPreferences.setOwmApiVersion(apiVersion);
                                    }
                                });
                            },
                            (t) -> SettingsActivity.this.runOnUiThread(() -> {
                                        owmTestApiProgressIndicator.hide();
                                        owmTestApiKeyButton.setEnabled(true);
                                        owmApiKeyEditText.setEnabled(true);

                                        snackbarHelper.logError("API Key Test Error: " + t.getMessage(), t);
                                    }
                            ));
                }
            } else if (v.getId() == R.id.test_openmeteo_connection) {
                String selfHostedInstance = ViewUtils.editTextToString(openmeteoInstanceEditText);
                String apiKey = ViewUtils.editTextToString(openmeteoApiKeyEditText);

                openmeteoTestConnectionButton.setEnabled(false);
                openmeteoTestConnectionProgressIndicator.show();

                openmeteoApiKeyEditText.setEnabled(false);
                openmeteoApiKeyEditText.clearFocus();

                openmeteoInstanceEditText.setEnabled(false);
                openmeteoInstanceEditText.clearFocus();

                Promise.create((a) -> {
                            boolean result = OpenMeteo.getInstance()
                                    .testConnection(selfHostedInstance, apiKey);

                            SettingsActivity.this.runOnUiThread(() -> {
                                openmeteoTestConnectionProgressIndicator.hide();

                                if (result) {
                                    setOpenMeteoApiKeyState(ApiKeyState.PASS);
                                    WeatherPreferences weatherPreferences = WeatherPreferences.getInstance(getContext());

                                    weatherPreferences.setWeatherProvider(WeatherProvider.OPENMETEO);
                                    weatherPreferences.setOpenMeteoAPIKey(apiKey);
                                    weatherPreferences.setOpenMeteoInstance(selfHostedInstance);
                                } else {
                                    setOpenMeteoApiKeyState(ApiKeyState.BAD_API_KEY);
                                }
                            });
                        },
                        (t) -> SettingsActivity.this.runOnUiThread(() -> {
                                    openmeteoTestConnectionProgressIndicator.hide();
                                    openmeteoTestConnectionButton.setEnabled(true);
                                    openmeteoApiKeyEditText.setEnabled(true);
                                    openmeteoInstanceEditText.setEnabled(true);

                                    snackbarHelper.logError("Connection Test Error: " + t.getMessage(), t);
                                }
                        ));
            } else if (v.getId() == R.id.provider_owm_frame) {
                ViewUtils.toggleKeyboardState(owmApiKeyEditText, false);
                owmApiKeyEditText.clearFocus();
            } else if (v.getId() == R.id.provider_openmeteo_frame) {
                ViewUtils.toggleKeyboardState(openmeteoApiKeyEditText, false);
                openmeteoApiKeyEditText.clearFocus();
                ViewUtils.toggleKeyboardState(openmeteoInstanceEditText, false);
                openmeteoInstanceEditText.clearFocus();
            }
        }

        @Override
        public int getViewRes() {
            return R.layout.fragment_provider;
        }

        @Override
        public boolean canAdvanceToNextPage() {
            return (weatherProvider == WeatherProvider.OPENWEATHERMAP
                    && owmApiKeyState == ApiKeyState.PASS) ||
                    (weatherProvider == WeatherProvider.OPENMETEO &&
                            openmeteoState == ApiKeyState.PASS);
        }

        private void updateEditTextColors(ApiKeyState state, TextInputLayout layout, TextInputEditText editText) {
            int greenTextColor = getResources().getColor(R.color.color_green);

            if (greenColorStateList == null) {
                greenColorStateList = new ColorStateList(
                        new int[][]{
                                new int[]{-android.R.attr.state_focused},
                                new int[]{android.R.attr.state_focused}
                        },
                        new int[]{
                                greenTextColor,
                                greenTextColor
                        }
                );
            }

            if (defaultColorStateList == null) {
                int primaryTextColor = getResources().getColor(R.color.text_primary_emphasis);

                defaultColorStateList = new ColorStateList(
                        new int[][]{
                                new int[]{-android.R.attr.state_focused},
                                new int[]{android.R.attr.state_focused}
                        },
                        new int[]{
                                primaryTextColor,
                                primaryTextColor
                        }
                );
            }

            if (state == ApiKeyState.PASS) {
                ViewUtils.setDrawable(editText, R.drawable.ic_done_white_24dp, greenTextColor, ViewUtils.FLAG_END);

                layout.setBoxStrokeColorStateList(greenColorStateList);
                layout.setHintTextColor(greenColorStateList);
                layout.setDefaultHintTextColor(greenColorStateList);
            } else {
                editText.setCompoundDrawables(null, null, null, null);

                layout.setBoxStrokeColorStateList(defaultColorStateList);
                layout.setHintTextColor(defaultColorStateList);
                layout.setDefaultHintTextColor(defaultColorStateList);
            }
        }

        private void updateOwmApiKeyColors(ApiKeyState state) {
            switch (state) {
                case BAD_API_KEY:
                    owmApiKeyEditTextLayout.setError(getString(R.string.text_invalid_api_key));
                    break;
                case NO_ONECALL:
                    owmApiKeyEditTextLayout.setError(getString(R.string.text_invalid_subscription));
                    break;
                default:
                    owmApiKeyEditTextLayout.setError(null);

                    updateEditTextColors(state, owmApiKeyEditTextLayout, owmApiKeyEditText);
            }
        }

        private void updateOpenMeteoApiKeyColors(ApiKeyState state) {
            if (Objects.requireNonNull(state) == ApiKeyState.BAD_API_KEY) {
                openmeteoInstanceEditTextLayout.setError(getString(R.string.text_invalid_api_key_or_instance));
                openmeteoApiKeyEditTextLayout.setError(getString(R.string.text_invalid_api_key_or_instance));
            } else {
                openmeteoInstanceEditTextLayout.setError(null);
                openmeteoApiKeyEditTextLayout.setError(null);

                updateEditTextColors(state, openmeteoInstanceEditTextLayout, openmeteoInstanceEditText);
                updateEditTextColors(state, openmeteoApiKeyEditTextLayout, openmeteoApiKeyEditText);
            }
        }

        private void setOwmApiKeyState(ApiKeyState state) {
            owmApiKeyState = state;

            updateOwmApiKeyColors(state);

            owmTestApiKeyButton.setEnabled(state == ApiKeyState.NEUTRAL);

            owmApiKeyEditText.setEnabled(true);

            notifyViewPager();
        }

        private void setOpenMeteoApiKeyState(ApiKeyState state) {
            openmeteoState = state;

            updateOpenMeteoApiKeyColors(state);

            openmeteoTestConnectionButton.setEnabled(state == ApiKeyState.NEUTRAL);

            openmeteoApiKeyEditText.setEnabled(true);
            openmeteoInstanceEditText.setEnabled(true);

            notifyViewPager();
        }
    }

    private class AdvancedSettingsContainer extends OnboardingContainer {
        private final static String KEY_RADARQUALITY = "radarquality";
        private final static String KEY_REOPEN_ADVANCED_MENU = "reopen";
        private RadarQuality radarQuality = null;
        private MaterialButton buttonLanguage;

        private boolean shouldReopenAdvancedMenu = false;

        public AdvancedSettingsContainer(Context context) {
            super(context);
        }

        @Override
        public int getViewRes() {
            return R.layout.fragment_advanced_settings;
        }

        @Override
        public void onCreateView(View v) {
            buttonLanguage = v.findViewById(R.id.button_app_language);
        }

        @Override
        public void onBindView(View v) {
            if (radarQuality == null) {
                radarQuality = WeatherPreferences.getInstance(getContext()).getRadarQuality();
            }

            new UnitsButtonGroup<RadarQuality>(v, radarQuality ->
                    WeatherPreferences
                            .getInstance(getContext())
                            .setRadarQuality(this.radarQuality = radarQuality))
                    .addButton(R.id.button_radar_high, RadarQuality.HIGH)
                    .addButton(R.id.button_radar_low, RadarQuality.LOW)
                    .selectButton(radarQuality);

            LocaleListCompat llc = AppCompatDelegate.getApplicationLocales();
            Locale currentLocale = llc.size() == 0 ? null : llc.get(0);

            setLanguageButtonText(currentLocale);

            buttonLanguage.setOnClickListener(view ->
                    new LocaleDialog(getContext(), currentLocale)
                            .show(locale -> {
                                setLanguageButtonText(locale);

                                shouldReopenAdvancedMenu = true;

                                AppCompatDelegate.setApplicationLocales(
                                        locale == null ?
                                                LocaleListCompat.getEmptyLocaleList() :
                                                LocaleListCompat.create(locale));
                            }));
        }

        @Override
        public void onSaveInstanceState(@NonNull Bundle outState) {
            outState.putString(KEY_RADARQUALITY, radarQuality.getValue());

            outState.putBoolean(KEY_REOPEN_ADVANCED_MENU, shouldReopenAdvancedMenu);
        }

        @Override
        public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
            radarQuality = RadarQuality.from(savedInstanceState.getString(KEY_RADARQUALITY), RadarQuality.HIGH);

            if (savedInstanceState.getBoolean(KEY_REOPEN_ADVANCED_MENU)) {
                buttonLanguage.post(() -> openAdvancedMenu(true));
            }
        }

        @Override
        public void onPageSelected() {

        }

        @Override
        public void onPageDeselected() {

        }

        @Override
        public boolean canAdvanceToNextPage() {
            return false;
        }

        private void setLanguageButtonText(Locale locale) {
            buttonLanguage.setText(locale == null ?
                    ApiUtils.getStringResourceFromApplication(
                            getContext().getPackageManager(),
                            "com.android.settings",
                            "preference_of_system_locale_summary",
                            "System default") :
                    locale.getDisplayName(locale));
        }
    }

    private enum ApiKeyState {
        NULL,
        NEUTRAL,
        PASS,
        BAD_API_KEY,
        NO_ONECALL
    }

    private static class UnitsButtonGroup<T> implements View.OnClickListener {
        private final HashMap<T, View> valueViewMap = new HashMap<>();

        private final View container;
        private final OnUnitsButtonSelected<T> onUnitsButtonSelected;

        private T currentValue = null;

        public UnitsButtonGroup(View container, @NonNull OnUnitsButtonSelected<T> onUnitsButtonSelected) {
            this.container = container;
            this.onUnitsButtonSelected = onUnitsButtonSelected;
        }

        public UnitsButtonGroup<T> addButton(@IdRes int buttonResId, T value) {
            View view = container.findViewById(buttonResId);
            view.setOnClickListener(this);

            valueViewMap.put(value, view);

            return this;
        }

        public void selectButton(T value) {
            selectButton(valueViewMap.get(value), value);
        }

        private void selectButton(View v, T value) {
            if (currentValue == null || !currentValue.equals(value)) {
                currentValue = value;

                for (T key : valueViewMap.keySet()) {
                    View button = valueViewMap.get(key);

                    if (button != null) {
                        button.setSelected(false);

                        if (value == null && v != null && v.getId() == button.getId()) {
                            value = key;
                        }
                    }
                }

                if (v != null) {
                    v.setSelected(true);

                    onUnitsButtonSelected.onUnitsButtonSelected(value);
                }
            }
        }

        public void setEnabled(boolean enabled) {
            for (T entry : valueViewMap.keySet()) {
                View v = valueViewMap.get(entry);

                if (v != null) {
                    v.setEnabled(enabled);
                }
            }
        }

        @Override
        public void onClick(View v) {
            selectButton(v, null);
        }
    }

    private interface OnUnitsButtonSelected<T> {
        void onUnitsButtonSelected(T value);
    }
}
