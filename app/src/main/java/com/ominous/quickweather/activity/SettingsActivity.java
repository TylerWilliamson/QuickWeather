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

import android.Manifest;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.ominous.quickweather.R;
import com.ominous.quickweather.data.WeatherDatabase;
import com.ominous.quickweather.dialog.LocationDialog;
import com.ominous.quickweather.util.DialogUtils;
import com.ominous.quickweather.util.Logger;
import com.ominous.quickweather.util.SnackbarUtils;
import com.ominous.quickweather.util.WeatherPreferences;
import com.ominous.quickweather.view.LocationDragListView;
import com.ominous.quickweather.weather.Weather;
import com.ominous.quickweather.weather.WeatherLocationManager;
import com.ominous.tylerutils.activity.OnboardingActivity;
import com.ominous.tylerutils.async.Promise;
import com.ominous.tylerutils.browser.CustomTabs;
import com.ominous.tylerutils.util.StringUtils;
import com.ominous.tylerutils.util.ViewUtils;
import com.ominous.tylerutils.view.LinkedTextView;
import com.woxthebox.draglistview.DragListView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

//TODO update dark mode onClick somehow
public class SettingsActivity extends OnboardingActivity {
    public final static String EXTRA_SKIP_WELCOME = "extra_skip_welcome";
    public final static String EXTRA_WEATHERLOCATION = "extra_weatherlocation";
    private final static String TAG = "SettingsActivity";

    private final ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), grantedResults -> {
        for (String key : grantedResults.keySet()) {
            switch (key) {
                case Manifest.permission.ACCESS_COARSE_LOCATION:
                    for (Fragment fragment : getInstantiatedFragments()) {
                        if (fragment instanceof LocationFragment) {
                            LocationFragment locationFragment = (LocationFragment) fragment;

                            locationFragment.checkLocationSnackbar();

                            if (Boolean.TRUE.equals(grantedResults.get(key))) {
                                locationFragment.addCurrentLocation();
                            }
                        }
                    }

                    if (Build.VERSION.SDK_INT >= 23 &&
                            !Boolean.TRUE.equals(grantedResults.get(key)) &&
                            shouldShowRequestPermissionRationale(key)) {
                        DialogUtils.showLocationRationale(this);
                    }
                    break;
                case Manifest.permission.ACCESS_BACKGROUND_LOCATION:
                    for (Fragment fragment : getInstantiatedFragments()) {
                        if (fragment instanceof UnitsFragment) {
                            ((UnitsFragment) fragment).checkIfBackgroundLocationEnabled();
                        }
                    }
                    break;
            }
        }
    });

    private ActivityResultLauncher<String[]> getRequestPermissionLauncher() {
        return requestPermissionLauncher;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CustomTabs.getInstance(this).setColor(ContextCompat.getColor(this, R.color.color_accent));

        if (getIntent().hasExtra(EXTRA_WEATHERLOCATION)) {
            this.findViewById(android.R.id.content).post(() -> setCurrentPage(2));
        }
    }

    @Override
    public void onFinish() {
        WeatherPreferences.commitChanges();

        ContextCompat.startActivity(this, new Intent(this, MainActivity.class), null);
        doExitAnimation();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        doExitAnimation();
    }

    private void doExitAnimation() {
        this.overridePendingTransition(R.anim.slide_right_in, R.anim.slide_left_out);
    }

    @Override
    public void addFragments() {
        if (this.getIntent().getExtras() == null || !this.getIntent().getExtras().getBoolean(EXTRA_SKIP_WELCOME, false)) {
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
        private MaterialButton currentLocationButton, otherLocationButton;
        private List<WeatherDatabase.WeatherLocation> locations;
        private LocationAdapterDataObserver locationAdapterDataObserver;
        private Snackbar locationDisabledSnackbar;
        private LinkedTextView privacyPolicyTextView;
        private LocationDialog locationDialog;
        private boolean hasShownBundledLocation = false;
        private Promise<Void, Void> lastDatabaseUpdate;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (savedInstanceState != null) {
                locations = savedInstanceState.getParcelableArrayList(KEY_LOCATIONS);
            }

            locationDialog = new LocationDialog(getContext(), new LocationDialog.OnLocationChosenListener() {
                @Override
                public void onLocationChosen(String name, double latitude, double longitude) {
                    addLocation(new WeatherDatabase.WeatherLocation(
                            0,
                            latitude,
                            longitude,
                            name,
                            false,
                            false,
                            0
                    ));
                }

                @Override
                public void onGeoCoderError(Throwable throwable) {
                    Logger.e(requireActivity(), TAG, LocationFragment.this.getString(R.string.error_connecting_geocoder), throwable);
                }
            });
        }

        @Override
        public View onCreateView(@NonNull final LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_location, parent, false);

            dragListView = v.findViewById(R.id.drag_list_view);
            currentLocationButton = v.findViewById(R.id.button_current_location);
            otherLocationButton = v.findViewById(R.id.button_other_location);
            privacyPolicyTextView = v.findViewById(R.id.privacy_text_view);

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
            }).then((a) -> {
                new Handler(Looper.getMainLooper()).post(() -> {
                    dragListView.setAdapterFromList(locations);

                    if (locationAdapterDataObserver == null) {
                        locationAdapterDataObserver = new LocationAdapterDataObserver();
                    }

                    dragListView.getAdapter().registerAdapterDataObserver(locationAdapterDataObserver);
                });
            });

            dragListView.setAdapterFromList(new ArrayList<>());

            dragListView.setDragListListener(new DragListView.DragListListener() {
                @Override
                public void onItemDragStarted(int position) {
                }

                @Override
                public void onItemDragging(int itemPosition, float x, float y) {
                }

                @Override
                public void onItemDragEnded(int fromPosition, int toPosition) {
                    updateLocations();
                }
            });

            currentLocationButton.setOnClickListener(this);
            otherLocationButton.setOnClickListener(this);

            privacyPolicyTextView.setText(StringUtils.fromHtml(this.getString(R.string.text_privacy_policy)));
        }

        private void addCurrentLocation() {
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

        private void addLocation(WeatherDatabase.WeatherLocation weatherLocation) {
            dragListView.addLocation(weatherLocation);

            updateLocations();
        }

        private void setCurrentLocationEnabled(boolean enabled) {
            currentLocationButton.setEnabled(enabled);
        }

        public boolean isCurrentLocationSelected() {
            for (WeatherDatabase.WeatherLocation weatherLocation : dragListView.getItemList()) {
                if (weatherLocation.isCurrentLocation) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public void onPageDeselected() {
            dismissLocationSnackbar();
        }

        public void dismissLocationSnackbar() {
            if (locationDisabledSnackbar != null) {
                locationDisabledSnackbar.dismiss();
            }
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
                    getActivity() != null &&
                    getActivity().getIntent() != null ?
                    getActivity().getIntent().getParcelableExtra(EXTRA_WEATHERLOCATION) : null;

            hasShownBundledLocation = true;

            if (weatherLocation != null) {
                locationDialog.showEditDialog(weatherLocation);
            }
        }

        public void checkLocationSnackbar() {
            if (!currentLocationButton.isEnabled() && !WeatherLocationManager.isLocationPermissionGranted(requireContext())) {
                locationDisabledSnackbar = SnackbarUtils.notifyLocPermDenied(requireActivity().findViewById(R.id.viewpager_coordinator), ((SettingsActivity) requireActivity()).getRequestPermissionLauncher());
            } else {
                dismissLocationSnackbar();
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

                            WeatherLocationManager.requestLocationPermissions(v.getContext(), ((SettingsActivity) requireActivity()).getRequestPermissionLauncher());
                        });
                    }
                }
            } else {
                locationDialog.showSearchDialog();
            }
        }

        private void updateLocations() {
            Promise.VoidPromiseCallable<Void> callable = (a) -> {
                WeatherDatabase.WeatherLocationDao weatherDatabaseDao = WeatherDatabase.getInstance(getContext()).locationDao();

                List<WeatherDatabase.WeatherLocation> newWeatherLocations = dragListView.getItemList();
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

                if (!selectedExists) {
                    newWeatherLocations.get(0).isSelected = true;
                }

                int order = 0;
                for (WeatherDatabase.WeatherLocation weatherLocation : newWeatherLocations) {
                    weatherLocation.order = order++;

                    if (weatherLocation.id > 0) {
                        weatherDatabaseDao.update(weatherLocation);
                    } else {
                        weatherDatabaseDao.insert(weatherLocation);
                    }
                }
            };

            if (lastDatabaseUpdate == null) {
                lastDatabaseUpdate = Promise.create(callable);
            } else {
                lastDatabaseUpdate.then(callable);
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

            private void doUpdate() {
                setCurrentLocationEnabled(!isCurrentLocationSelected());

                notifyViewPager(dragListView.getItemList().size() > 0);
            }
        }
    }

    public static class UnitsFragment extends OnboardingFragment implements View.OnClickListener {
        private static final String KEY_TEMPERATURE = "temperature", KEY_SPEED = "speed", KEY_THEME = "theme", KEY_ALERTNOTIF = "alertnotif", KEY_PERSISTNOTIF = "persistnotif";
        private MaterialButton
                buttonFahrenheit, buttonCelsius,
                buttonMph, buttonKmh, buttonMs,
                buttonThemeLight, buttonThemeDark, buttonThemeAuto,
                buttonNotifAlertEnabled, buttonNotifAlertDisabled,
                buttonNotifPersistEnabled, buttonNotifPersistDisabled;
        private CoordinatorLayout coordinatorLayout;
        private String temperature = null, speed = null, theme = null, alertNotifEnabled = null, persistNotifEnabled = null;
        private Snackbar locationDisabledSnackbar;

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
            buttonThemeLight = v.findViewById(R.id.button_theme_light);
            buttonThemeDark = v.findViewById(R.id.button_theme_dark);
            buttonThemeAuto = v.findViewById(R.id.button_theme_auto);
            buttonNotifAlertEnabled = v.findViewById(R.id.button_alert_notif_enabled);
            buttonNotifAlertDisabled = v.findViewById(R.id.button_alert_notif_disabled);
            buttonNotifPersistEnabled = v.findViewById(R.id.button_weather_notif_enabled);
            buttonNotifPersistDisabled = v.findViewById(R.id.button_weather_notif_disabled);
            coordinatorLayout = v.findViewById(R.id.viewpager_coordinator);

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
                    viewId == R.id.button_ms) {
                buttonKmh.setSelected(false);
                buttonMph.setSelected(false);
                buttonMs.setSelected(false);

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

        @Override
        public void onPageSelected() {
            checkIfBackgroundLocationEnabled();
        }

        public void dismissSnackbar() {
            if (locationDisabledSnackbar != null) {
                locationDisabledSnackbar.dismiss();
            }
        }

        public void checkIfBackgroundLocationEnabled() {
            boolean isCurrentLocationSelected = false;

            SettingsActivity activity = (SettingsActivity) getActivity();

            if (activity != null) {
                for (Fragment fragment : ((SettingsActivity) getActivity()).getInstantiatedFragments()) {
                    if (fragment instanceof LocationFragment) {
                        LocationFragment locationFragment = (LocationFragment) fragment;

                        isCurrentLocationSelected = locationFragment.isCurrentLocationSelected();
                    }
                }
            }

            if (isCurrentLocationSelected && (buttonNotifAlertEnabled.isSelected() || buttonNotifPersistEnabled.isSelected())) {
                if (!WeatherLocationManager.isBackgroundLocationPermissionGranted(getActivity())) {
                    locationDisabledSnackbar = SnackbarUtils.notifyBackLocPermDenied(coordinatorLayout, ((SettingsActivity) requireActivity()).getRequestPermissionLauncher());
                }
            } else {
                dismissSnackbar();
            }
        }
    }

    public static class ApiKeyFragment extends OnboardingFragment implements View.OnClickListener, TextWatcher, View.OnFocusChangeListener {
        private static final int STATE_NULL = -1, STATE_NEUTRAL = 0, STATE_PASS = 1, STATE_FAIL = 2;
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

            ViewUtils.setEditTextCursorColor(apiKeyEditText, ContextCompat.getColor(getContext(), R.color.color_accent_text));

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
            if (state == STATE_FAIL) {
                apiKeyEditTextLayout.setError(getString(R.string.text_invalid_api_key));
            } else {
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

        //TODO: Cleanup flow of methods
        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.test_api_key) {
                String apiKeyText = ViewUtils.editTextToString(apiKeyEditText);

                if (apiKeyText.length() > 0) {
                    testApiKeyButton.setEnabled(false);
                    apiKeyEditText.setEnabled(false);
                    testApiProgressIndicator.show();

                    Promise.create(
                            (a) -> {
                                //Welcome to Atlanta!
                                return Weather.getWeatherOneCall(apiKeyText, new Pair<>(33.749, -84.388), false);
                            },
                            (t) -> requireActivity().runOnUiThread(() -> {
                                testApiProgressIndicator.hide();
                                if (t.getMessage() != null && (t.getMessage().contains("403") || t.getMessage().contains("401"))) {
                                    setApiKeyState(STATE_FAIL);
                                } else {
                                    testApiKeyButton.setEnabled(true);
                                    apiKeyEditText.setEnabled(true);
                                    Logger.e(testApiKeyButton, TAG, t.getMessage(), t);
                                }
                            }))
                            .then((a) -> {
                                requireActivity().runOnUiThread(() -> {
                                    testApiProgressIndicator.hide();
                                    setApiKeyState(STATE_PASS);

                                    WeatherPreferences.setApiKey(ViewUtils.editTextToString(apiKeyEditText));
                                });
                            });

                    apiKeyEditText.clearFocus();
                }
            } else {
                apiKeyEditText.clearFocus();
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
