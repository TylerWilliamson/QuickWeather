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

import android.Manifest;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.ominous.quickweather.R;
import com.ominous.quickweather.api.OpenWeatherMap;
import com.ominous.quickweather.data.WeatherDatabase;
import com.ominous.quickweather.dialog.LocationManualDialog;
import com.ominous.quickweather.dialog.LocationMapDialog;
import com.ominous.quickweather.dialog.LocationSearchDialog;
import com.ominous.quickweather.dialog.OnLocationChosenListener;
import com.ominous.quickweather.location.WeatherLocationManager;
import com.ominous.quickweather.util.ColorUtils;
import com.ominous.quickweather.util.DialogHelper;
import com.ominous.quickweather.util.NotificationUtils;
import com.ominous.quickweather.util.SnackbarHelper;
import com.ominous.quickweather.util.WeatherPreferences;
import com.ominous.quickweather.view.LocationDragListView;
import com.ominous.quickweather.web.FileWebServer;
import com.ominous.tylerutils.activity.OnboardingActivity;
import com.ominous.tylerutils.async.Promise;
import com.ominous.tylerutils.browser.CustomTabs;
import com.ominous.tylerutils.util.ViewUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

//TODO update dark mode onClick somehow
//TODO snackbar error message if no locations, switch to location tab
public class SettingsActivity extends OnboardingActivity {
    public final static String EXTRA_WEATHERLOCATION = "extra_weatherlocation";
    private static WeakReference<SettingsActivity> instance;
    private final ActivityResultLauncher<String> notificationRequestLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), grantedResults -> {
        for (Fragment fragment : getInstantiatedFragments()) {
            if (fragment instanceof UnitsFragment) {
                ((UnitsFragment) fragment).checkIfNotificationAllowed();
            }
        }
    });
    private FileWebServer fileWebServer;
    private DialogHelper dialogHelper;

    private static SettingsActivity getInstance() {
        return instance.get();
    }

    private static void setInstance(SettingsActivity settingsActivity) {
        instance = new WeakReference<>(settingsActivity);
    }    private final ActivityResultLauncher<String[]> currentLocationRequestLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), grantedResults -> {
        Boolean locationResult = grantedResults.get(Manifest.permission.ACCESS_COARSE_LOCATION);

        for (Fragment fragment : getInstantiatedFragments()) {
            if (fragment instanceof LocationFragment) {
                LocationFragment locationFragment = (LocationFragment) fragment;

                locationFragment.checkLocationSnackbar();

                if (Boolean.TRUE.equals(locationResult)) {
                    locationFragment.addCurrentLocation();
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setInstance(this);

        initActivity();

        fileWebServer = new FileWebServer(this, 4234);
        dialogHelper = new DialogHelper(this);

        CustomTabs.getInstance(this).setColor(ContextCompat.getColor(this, R.color.color_accent));

        if (getIntent().hasExtra(EXTRA_WEATHERLOCATION)) {
            this.findViewById(android.R.id.content).post(() -> setCurrentPage(2));
        }
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
    }

    @Override
    public void onFinish() {
        WeatherPreferences.commitChanges();

        ContextCompat.startActivity(this, new Intent(this, MainActivity.class), null);
        doExitAnimation();
    }    private final ActivityResultLauncher<String[]> hereRequestLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), grantedResults -> {
        Boolean locationResult = grantedResults.get(Manifest.permission.ACCESS_COARSE_LOCATION);

        if (Boolean.TRUE.equals(locationResult)) {
            for (Fragment fragment : getInstantiatedFragments()) {
                if (fragment instanceof LocationFragment) {
                    ((LocationFragment) fragment).addHere();
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
    protected void onResume() {
        super.onResume();

        fileWebServer.start();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (fileWebServer != null) {
            fileWebServer.stop();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        doExitAnimation();
    }    private final ActivityResultLauncher<String[]> backgroundLocationRequestLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), grantedResults -> {
        for (Fragment fragment : getInstantiatedFragments()) {
            if (fragment instanceof UnitsFragment) {
                ((UnitsFragment) fragment).checkIfBackgroundLocationEnabled();
            }
        }
    });

    private void doExitAnimation() {
        this.overridePendingTransition(R.anim.slide_right_in, R.anim.slide_left_out);
    }

    @Override
    public void addFragments() {
        if (!WeatherPreferences.isInitialized()) {
            this.addFragment(WelcomeFragment.class);
        }
        this.addFragment(ApiKeyFragment.class);
        this.addFragment(LocationFragment.class);
        this.addFragment(UnitsFragment.class);
    }

    public static class WelcomeFragment extends OnboardingFragment {
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
            notifyViewPager(true);

            return inflater.inflate(R.layout.fragment_welcome, parent, false);
        }

        @Override
        public void onFinish() {

        }
    }

    //TODO RippleBackground on items
    public static class LocationFragment extends OnboardingFragment implements View.OnClickListener {
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

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (savedInstanceState != null) {
                locations = savedInstanceState.getParcelableArrayList(KEY_LOCATIONS);
            }

            locationManualDialog = new LocationManualDialog(getContext());
            locationSearchDialog = new LocationSearchDialog(getContext());
            locationMapDialog = new LocationMapDialog(getContext());
        }

        @Override
        public View onCreateView(@NonNull final LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_location, parent, false);

            dragListView = v.findViewById(R.id.drag_list_view);
            currentLocationButton = v.findViewById(R.id.button_current_location);
            otherLocationButton = v.findViewById(R.id.button_other_location);
            mapButton = v.findViewById(R.id.button_map);
            thisLocationButton = v.findViewById(R.id.button_here);

            snackbarHelper = new SnackbarHelper(v.findViewById(R.id.viewpager_coordinator));

            return v;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onSaveInstanceState(@NonNull Bundle outBundle) {
            super.onSaveInstanceState(outBundle);

            outBundle.putParcelableArrayList(KEY_LOCATIONS, (ArrayList<WeatherDatabase.WeatherLocation>) dragListView.getAdapter().getItemList());
        }

        @Override
        public void onStart() {
            super.onStart();

            Promise.create((a) -> {
                if (locations == null) {
                    locations = WeatherDatabase.getInstance(getContext()).locationDao().getAllWeatherLocations();
                }

                new Handler(Looper.getMainLooper()).post(() -> {
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

        private void addCurrentLocation() {
            if (!isCurrentLocationSelected()) {
                addLocation(new WeatherDatabase.WeatherLocation(
                        0,
                        0,
                        0,
                        this.getString(R.string.text_current_location),
                        false,
                        true,
                        0
                ));
            }
        }

        private void addHere() {
            if (WeatherLocationManager.isLocationPermissionGranted(getContext())) {
                thisLocationButton.setEnabled(false);
                snackbarHelper.notifyObtainingLocation();

                Promise.create((a) -> {
                    Location l = WeatherLocationManager.getCurrentLocation(SettingsActivity.getInstance(), false);

                    SettingsActivity.getInstance().runOnUiThread(() -> {
                        snackbarHelper.dismiss();
                        thisLocationButton.setEnabled(true);
                    });

                    return l;
                }, e -> {
                    if (e instanceof WeatherLocationManager.LocationPermissionNotAvailableException) {
                        snackbarHelper.notifyLocPermDenied(SettingsActivity.getInstance().hereRequestLauncher);
                    } else if (e instanceof WeatherLocationManager.LocationDisabledException) {
                        snackbarHelper.notifyLocDisabled();
                    } else {
                        snackbarHelper.notifyNullLoc();
                    }
                }).then((l) -> {
                    if (l != null) {
                        new Handler(Looper.getMainLooper()).post(() ->
                                locationManualDialog.show(new WeatherDatabase.WeatherLocation(l.getLatitude(), l.getLongitude(), null), onLocationChosenListener));
                    } else {
                        snackbarHelper.notifyNullLoc();
                    }
                });

            } else {
                WeatherLocationManager.showLocationDisclosure(SettingsActivity.getInstance(), () -> {
                    WeatherPreferences.setShowLocationDisclosure(WeatherPreferences.DISABLED);

                    WeatherLocationManager.requestLocationPermissions(SettingsActivity.getInstance(), SettingsActivity.getInstance().hereRequestLauncher);
                });
            }
        }

        private void addLocation(WeatherDatabase.WeatherLocation weatherLocation) {
            dragListView.addLocation(weatherLocation);
        }

        private void setCurrentLocationEnabled(boolean enabled) {
            currentLocationButton.setEnabled(enabled);
        }

        public boolean isCurrentLocationSelected() {
            for (WeatherDatabase.WeatherLocation weatherLocation : dragListView.getLocationList()) {
                if (weatherLocation.isCurrentLocation) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public void onPageDeselected() {
            dismissSnackbar();
        }

        @Override
        public void onResume() {
            super.onResume();

            checkLocationSnackbar();
        }

        @Override
        public void onPageSelected() {
            checkLocationSnackbar();

            WeatherDatabase.WeatherLocation weatherLocation = !hasShownBundledLocation &&
                    SettingsActivity.getInstance().getIntent() != null ?
                    SettingsActivity.getInstance().getIntent().getParcelableExtra(EXTRA_WEATHERLOCATION) : null;

            hasShownBundledLocation = true;

            if (weatherLocation != null) {
                locationManualDialog.show(weatherLocation, onLocationChosenListener);
            }
        }

        public void checkLocationSnackbar() {
            if (!currentLocationButton.isEnabled() && !WeatherLocationManager.isLocationPermissionGranted(requireContext())) {
                snackbarHelper.notifyLocPermDenied(SettingsActivity.getInstance().currentLocationRequestLauncher);
            } else {
                dismissSnackbar();
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
                    if (WeatherLocationManager.isLocationPermissionGranted(v.getContext())) {
                        addCurrentLocation();
                        v.setEnabled(false);
                    } else {
                        WeatherLocationManager.showLocationDisclosure(v.getContext(), () -> {
                            WeatherPreferences.setShowLocationDisclosure(WeatherPreferences.DISABLED);

                            WeatherLocationManager.requestLocationPermissions(v.getContext(), SettingsActivity.getInstance().currentLocationRequestLauncher);
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
            if (lastDatabaseUpdate == null || !(lastDatabaseUpdate.getState().equals(Promise.PromiseState.STARTED) || lastDatabaseUpdate.getState().equals(Promise.PromiseState.NOT_STARTED))) {
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

                notifyViewPager(dragListView.getLocationList().size() > 0);

                updateLocations();
            }
        }
    }

    public static class UnitsFragment extends OnboardingFragment implements View.OnClickListener {
        private static final String KEY_TEMPERATURE = "temperature", KEY_SPEED = "speed", KEY_THEME = "theme", KEY_ALERTNOTIF = "alertnotif", KEY_PERSISTNOTIF = "persistnotif";
        private MaterialButton
                buttonFahrenheit, buttonCelsius,
                buttonMph, buttonKmh, buttonMs, buttonKn,
                buttonThemeLight, buttonThemeDark, buttonThemeAuto,
                buttonNotifAlertEnabled, buttonNotifAlertDisabled,
                buttonNotifPersistEnabled, buttonNotifPersistDisabled;
        private String temperature = null, speed = null, theme = null, alertNotifEnabled = null, persistNotifEnabled = null;
        private SnackbarHelper snackbarHelper;

        @Override
        public void onResume() {
            super.onResume();

            checkIfBackgroundLocationEnabled();
        }

        @Override
        public View onCreateView(@NonNull final LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_units, parent, false);

            buttonFahrenheit = v.findViewById(R.id.button_fahrenheit);
            buttonCelsius = v.findViewById(R.id.button_celsius);
            buttonMph = v.findViewById(R.id.button_mph);
            buttonKmh = v.findViewById(R.id.button_kmh);
            buttonMs = v.findViewById(R.id.button_ms);
            buttonKn = v.findViewById(R.id.button_kn);
            buttonThemeLight = v.findViewById(R.id.button_theme_light);
            buttonThemeDark = v.findViewById(R.id.button_theme_dark);
            buttonThemeAuto = v.findViewById(R.id.button_theme_auto);
            buttonNotifAlertEnabled = v.findViewById(R.id.button_alert_notif_enabled);
            buttonNotifAlertDisabled = v.findViewById(R.id.button_alert_notif_disabled);
            buttonNotifPersistEnabled = v.findViewById(R.id.button_weather_notif_enabled);
            buttonNotifPersistDisabled = v.findViewById(R.id.button_weather_notif_disabled);

            snackbarHelper = new SnackbarHelper(v.findViewById(R.id.viewpager_coordinator));

            return v;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (savedInstanceState != null) {
                temperature = savedInstanceState.getString(KEY_TEMPERATURE);
                speed = savedInstanceState.getString(KEY_SPEED);
                theme = savedInstanceState.getString(KEY_THEME);
                alertNotifEnabled = savedInstanceState.getString(KEY_ALERTNOTIF);
                persistNotifEnabled = savedInstanceState.getString(KEY_PERSISTNOTIF);
            }
        }

        @Override
        public void onSaveInstanceState(@NonNull Bundle outBundle) {
            super.onSaveInstanceState(outBundle);

            outBundle.putString(KEY_TEMPERATURE, temperature);
            outBundle.putString(KEY_SPEED, speed);
            outBundle.putString(KEY_THEME, theme);
            outBundle.putString(KEY_ALERTNOTIF, alertNotifEnabled);
            outBundle.putString(KEY_PERSISTNOTIF, persistNotifEnabled);
        }

        @Override
        public void onStart() {
            super.onStart();

            buttonFahrenheit.setOnClickListener(this);
            buttonCelsius.setOnClickListener(this);
            buttonMph.setOnClickListener(this);
            buttonKmh.setOnClickListener(this);
            buttonMs.setOnClickListener(this);
            buttonKn.setOnClickListener(this);
            buttonThemeLight.setOnClickListener(this);
            buttonThemeDark.setOnClickListener(this);
            buttonThemeAuto.setOnClickListener(this);
            buttonNotifAlertEnabled.setOnClickListener(this);
            buttonNotifAlertDisabled.setOnClickListener(this);
            buttonNotifPersistEnabled.setOnClickListener(this);
            buttonNotifPersistDisabled.setOnClickListener(this);

            buttonFahrenheit.setTag(WeatherPreferences.TEMPERATURE_FAHRENHEIT);
            buttonCelsius.setTag(WeatherPreferences.TEMPERATURE_CELSIUS);
            buttonMph.setTag(WeatherPreferences.SPEED_MPH);
            buttonKmh.setTag(WeatherPreferences.SPEED_KMH);
            buttonMs.setTag(WeatherPreferences.SPEED_MS);
            buttonKn.setTag(WeatherPreferences.SPEED_KN);
            buttonThemeLight.setTag(WeatherPreferences.THEME_LIGHT);
            buttonThemeDark.setTag(WeatherPreferences.THEME_DARK);
            buttonThemeAuto.setTag(WeatherPreferences.THEME_AUTO);
            buttonNotifAlertEnabled.setTag(WeatherPreferences.ENABLED);
            buttonNotifAlertDisabled.setTag(WeatherPreferences.DISABLED);
            buttonNotifPersistEnabled.setTag(WeatherPreferences.ENABLED);
            buttonNotifPersistDisabled.setTag(WeatherPreferences.DISABLED);

            if (speed == null) {
                speed = WeatherPreferences.getSpeedUnit();
            }

            if (temperature == null) {
                temperature = WeatherPreferences.getTemperatureUnit();
            }

            if (theme == null) {
                theme = WeatherPreferences.getTheme();
            }

            if (alertNotifEnabled == null) {
                alertNotifEnabled = WeatherPreferences.getShowAlertNotification();
            }

            if (persistNotifEnabled == null) {
                persistNotifEnabled = WeatherPreferences.getShowPersistentNotification();
            }

            switch (speed) {
                case WeatherPreferences.SPEED_KMH:
                    buttonKmh.setSelected(true);
                    break;
                case WeatherPreferences.SPEED_MPH:
                    buttonMph.setSelected(true);
                    break;
                case WeatherPreferences.SPEED_MS:
                    buttonMs.setSelected(true);
                    break;
                case WeatherPreferences.SPEED_KN:
                    buttonKn.setSelected(true);
                    break;
            }

            switch (temperature) {
                case WeatherPreferences.TEMPERATURE_CELSIUS:
                    buttonCelsius.setSelected(true);
                    break;
                case WeatherPreferences.TEMPERATURE_FAHRENHEIT:
                    buttonFahrenheit.setSelected(true);
                    break;
            }

            switch (theme) {
                case WeatherPreferences.THEME_LIGHT:
                    buttonThemeLight.setSelected(true);
                    break;
                case WeatherPreferences.THEME_DARK:
                    buttonThemeDark.setSelected(true);
                    break;
                case WeatherPreferences.THEME_AUTO:
                    buttonThemeAuto.setSelected(true);
                    break;
            }

            switch (alertNotifEnabled) {
                case WeatherPreferences.ENABLED:
                    buttonNotifAlertEnabled.setSelected(true);
                    break;
                case WeatherPreferences.DISABLED:
                    buttonNotifAlertDisabled.setSelected(true);
                    break;
            }

            switch (persistNotifEnabled) {
                case WeatherPreferences.ENABLED:
                    buttonNotifPersistEnabled.setSelected(true);
                    break;
                case WeatherPreferences.DISABLED:
                    buttonNotifPersistDisabled.setSelected(true);
                    break;
            }

            if (!temperature.isEmpty() && !speed.isEmpty() && !theme.isEmpty() && !alertNotifEnabled.isEmpty() && !persistNotifEnabled.isEmpty()) {
                notifyViewPager(true);
            }
        }

        @Override
        public void onFinish() {
            WeatherPreferences.commitChanges();
        }

        @Override
        public void onClick(View v) {
            int viewId = v.getId();

            //Because APPARENTLY switch-statements with resource IDs are too hard for Android Studio
            if (viewId == R.id.button_fahrenheit ||
                    viewId == R.id.button_celsius) {
                buttonFahrenheit.setSelected(false);
                buttonCelsius.setSelected(false);

                temperature = v.getTag().toString();

                WeatherPreferences.setTemperatureUnit(temperature);
            } else if (viewId == R.id.button_mph ||
                    viewId == R.id.button_kmh ||
                    viewId == R.id.button_ms ||
                    viewId == R.id.button_kn) {
                buttonKmh.setSelected(false);
                buttonMph.setSelected(false);
                buttonMs.setSelected(false);
                buttonKn.setSelected(false);

                speed = v.getTag().toString();

                WeatherPreferences.setSpeedUnit(speed);
            } else if (viewId == R.id.button_theme_auto ||
                    viewId == R.id.button_theme_light ||
                    viewId == R.id.button_theme_dark) {
                buttonThemeLight.setSelected(false);
                buttonThemeDark.setSelected(false);
                buttonThemeAuto.setSelected(false);

                theme = v.getTag().toString();
                WeatherPreferences.setTheme(theme);

                //TODO handle uiMode config change
                //Error is due to recreating the activity, need to update TylerUtils
                //ColorUtils.setNightMode(getContext());
            } else if (viewId == R.id.button_weather_notif_enabled ||
                    viewId == R.id.button_weather_notif_disabled) {
                buttonNotifPersistEnabled.setSelected(false);
                buttonNotifPersistDisabled.setSelected(false);

                persistNotifEnabled = v.getTag().toString();

                WeatherPreferences.setShowPersistentNotification(persistNotifEnabled);
            } else if (viewId == R.id.button_alert_notif_disabled ||
                    viewId == R.id.button_alert_notif_enabled) {
                buttonNotifAlertEnabled.setSelected(false);
                buttonNotifAlertDisabled.setSelected(false);

                alertNotifEnabled = v.getTag().toString();

                WeatherPreferences.setShowAlertNotification(alertNotifEnabled);
            }

            if (getActivity() != null && Build.VERSION.SDK_INT >= 33 && (viewId == R.id.button_alert_notif_enabled ||
                    viewId == R.id.button_weather_notif_enabled)) {
                ((SettingsActivity) getActivity()).notificationRequestLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }

            v.setSelected(true);
            checkIfBackgroundLocationEnabled();

            if (!temperature.isEmpty() && !speed.isEmpty() && !theme.isEmpty() && !alertNotifEnabled.isEmpty() && !persistNotifEnabled.isEmpty()) {
                notifyViewPager(true);
            }
        }

        @Override
        public void onPageDeselected() {
            dismissSnackbar();
        }

        private void dismissSnackbar() {
            if (snackbarHelper != null) {
                snackbarHelper.dismiss();
            }
        }

        @Override
        public void onPageSelected() {
            checkIfBackgroundLocationEnabled();
        }

        public void checkIfBackgroundLocationEnabled() {
            boolean isCurrentLocationSelected = false;

            for (Fragment fragment : SettingsActivity.getInstance().getInstantiatedFragments()) {
                if (fragment instanceof LocationFragment) {
                    LocationFragment locationFragment = (LocationFragment) fragment;

                    isCurrentLocationSelected = locationFragment.isCurrentLocationSelected();
                }
            }

            if (isCurrentLocationSelected && (buttonNotifAlertEnabled.isSelected() || buttonNotifPersistEnabled.isSelected())) {
                if (!WeatherLocationManager.isBackgroundLocationPermissionGranted(getContext())) {
                    snackbarHelper.notifyBackLocPermDenied(SettingsActivity.getInstance().backgroundLocationRequestLauncher);
                }
            } else {
                dismissSnackbar();
            }
        }

        public void checkIfNotificationAllowed() {
            if (!NotificationUtils.canShowNotifications(getContext())) {
                if (buttonNotifAlertEnabled.isSelected()) {
                    buttonNotifAlertEnabled.setSelected(false);
                    buttonNotifAlertDisabled.setSelected(true);

                    alertNotifEnabled = buttonNotifAlertDisabled.getTag().toString();

                    WeatherPreferences.setShowAlertNotification(alertNotifEnabled);
                }

                if (buttonNotifPersistEnabled.isSelected()) {
                    buttonNotifPersistEnabled.setSelected(false);
                    buttonNotifPersistDisabled.setSelected(true);

                    persistNotifEnabled = buttonNotifPersistDisabled.getTag().toString();

                    WeatherPreferences.setShowPersistentNotification(persistNotifEnabled);
                }
            }
        }
    }

    public static class ApiKeyFragment extends OnboardingFragment implements View.OnClickListener, TextWatcher, View.OnFocusChangeListener {
        private static final int STATE_NULL = -1, STATE_NEUTRAL = 0, STATE_PASS = 1, STATE_BAD_API_KEY = 2, STATE_NO_ONECALL = 3;
        private static final String KEY_APIKEY = "apiKey", KEY_APIKEYSTATE = "apiKeyState";
        private final int[][] colorStates = new int[][]{
                new int[]{-android.R.attr.state_focused},
                new int[]{android.R.attr.state_focused}
        };
        private TextInputEditText apiKeyEditText;
        private TextInputLayout apiKeyEditTextLayout;
        private MaterialButton testApiKeyButton;
        private LinearProgressIndicator testApiProgressIndicator;
        private View container;
        private int apiKeyState = STATE_NULL;
        private boolean apiKeyFocused = true;
        private SnackbarHelper snackbarHelper;

        @Override
        public View onCreateView(@NonNull final LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
            container = inflater.inflate(R.layout.fragment_apikey, parent, false);

            apiKeyEditText = container.findViewById(R.id.onboarding_apikey_edittext);
            apiKeyEditTextLayout = container.findViewById(R.id.onboarding_apikey_edittext_layout);
            testApiKeyButton = container.findViewById(R.id.test_api_key);
            testApiProgressIndicator = container.findViewById(R.id.onboarding_apikey_progress);

            return container;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (savedInstanceState != null) {
                apiKeyEditText.setText(savedInstanceState.getString(KEY_APIKEY));
                apiKeyState = savedInstanceState.getInt(KEY_APIKEYSTATE);
            }
        }

        @Override
        public void onSaveInstanceState(@NonNull Bundle outBundle) {
            super.onSaveInstanceState(outBundle);

            outBundle.putString(KEY_APIKEY, ViewUtils.editTextToString(apiKeyEditText));
            outBundle.putInt(KEY_APIKEYSTATE, apiKeyState);
        }

        @Override
        public void onStart() {
            super.onStart();

            if (getContext() != null) {
                ViewUtils.setEditTextCursorColor(apiKeyEditText, ContextCompat.getColor(getContext(), R.color.color_accent_text));
            }

            snackbarHelper = new SnackbarHelper(apiKeyEditTextLayout);

            if (apiKeyState == STATE_NULL) {
                String apiKey = WeatherPreferences.getApiKey();

                if (apiKey.equals(WeatherPreferences.DEFAULT_VALUE)) {
                    //TODO Test api key, dont assume
                    setApiKeyState(STATE_NEUTRAL);
                } else {
                    apiKeyEditText.setText(apiKey);

                    setApiKeyState(STATE_PASS);
                }
            } else {
                setApiKeyState(apiKeyState);
            }

            apiKeyEditText.addTextChangedListener(this);
            apiKeyEditText.setOnFocusChangeListener(this);
            apiKeyEditText.setOnEditorActionListener((textView, i, keyEvent) -> {
                testApiKeyButton.performClick();
                return true;
            });

            container.setOnClickListener(this);

            testApiKeyButton.setOnClickListener(this);
        }

        @Override
        public void onPageSelected() {
            apiKeyEditText.requestFocus();

            updateApiKeyColors(apiKeyState);

            ViewUtils.toggleKeyboardState(apiKeyEditText, true);
        }

        @Override
        public void onPageDeselected() {
            if (apiKeyEditText != null) {
                apiKeyEditText.clearFocus();

                updateApiKeyColors(apiKeyState);

                ViewUtils.toggleKeyboardState(apiKeyEditText, false);
            }
        }

        @Override
        public void onFinish() {
            WeatherPreferences.commitChanges();
        }

        private void updateApiKeyColors(int state) {
            switch (state) {
                case STATE_BAD_API_KEY:
                    apiKeyEditTextLayout.setError(getString(R.string.text_invalid_api_key));
                    break;
                case STATE_NO_ONECALL:
                    apiKeyEditTextLayout.setError(getString(R.string.text_invalid_subscription));
                    break;
                default:
                    apiKeyEditTextLayout.setError(null);

                    int textColorRes, editTextDrawableRes;

                    if (state == STATE_PASS) {
                        textColorRes = R.color.color_green;
                        editTextDrawableRes = R.drawable.ic_done_white_24dp;
                    } else {
                        textColorRes = R.color.text_primary_emphasis;
                        editTextDrawableRes = 0;

                        apiKeyEditText.post(() -> apiKeyEditText.setCompoundDrawables(null, null, null, null));
                    }

                    int coloredTextColor = getResources().getColor(textColorRes);
                    int greyTextColor = getResources().getColor(R.color.text_primary_disabled);

                    ColorStateList textColor = new ColorStateList(
                            colorStates,
                            new int[]{
                                    greyTextColor,
                                    coloredTextColor
                            }
                    );

                    apiKeyEditTextLayout.setBoxStrokeColor(coloredTextColor);
                    apiKeyEditTextLayout.setHintTextColor(textColor);

                    if (state == STATE_PASS) {
                        apiKeyEditText.post(() -> ViewUtils.setDrawable(apiKeyEditText, editTextDrawableRes, apiKeyFocused ? coloredTextColor : greyTextColor, ViewUtils.FLAG_END));
                    }
            }
        }

        private void setApiKeyState(int state) {
            apiKeyState = state;

            updateApiKeyColors(state);

            testApiKeyButton.setEnabled(state == STATE_NEUTRAL);

            apiKeyEditText.setEnabled(true);

            notifyViewPager(state == STATE_PASS);
        }

        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.test_api_key) {
                String apiKeyText = ViewUtils.editTextToString(apiKeyEditText).replaceAll("[^0-9A-Za-z]", "");

                if (apiKeyText.length() > 0) {
                    testApiKeyButton.setEnabled(false);
                    testApiProgressIndicator.show();

                    apiKeyEditText.setEnabled(false);
                    apiKeyEditText.clearFocus();

                    Promise.create((a) -> {
                                OpenWeatherMap.APIVersion apiVersion = OpenWeatherMap.determineApiVersion(apiKeyText);

                                SettingsActivity.getInstance().runOnUiThread(() -> {

                                    testApiProgressIndicator.hide();

                                    if (apiVersion == null) {
                                        setApiKeyState(STATE_BAD_API_KEY);
                                    } else if (apiVersion == OpenWeatherMap.APIVersion.WEATHER_2_5) {
                                        setApiKeyState(STATE_NO_ONECALL);
                                    } else {
                                        setApiKeyState(STATE_PASS);
                                        WeatherPreferences.setApiKey(apiKeyText);
                                        WeatherPreferences.setAPIVersion(apiVersion.equals(OpenWeatherMap.APIVersion.ONECALL_3_0) ? WeatherPreferences.ONECALL_3_0 : WeatherPreferences.ONECALL_2_5);
                                    }
                                });

                            },
                            (t) -> SettingsActivity.getInstance().runOnUiThread(() -> {
                                        testApiProgressIndicator.hide();
                                        testApiKeyButton.setEnabled(true);
                                        apiKeyEditText.setEnabled(true);

                                        snackbarHelper.logError("API Key Test Error: " + t.getMessage(), t);
                                    }
                            ));
                } else {
                    //if the user clicks outside the edittext (on the container), clear the focus
                    apiKeyEditText.clearFocus();
                }
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            setApiKeyState(STATE_NEUTRAL);
        }

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            apiKeyFocused = hasFocus;
            updateApiKeyColors(apiKeyState);
        }
    }








}
