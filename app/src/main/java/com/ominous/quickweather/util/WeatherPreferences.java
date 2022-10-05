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

package com.ominous.quickweather.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import com.ominous.quickweather.R;
import com.ominous.quickweather.data.WeatherDatabase;
import com.ominous.tylerutils.async.Promise;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

//TODO boolean preferences should return booleans
public class WeatherPreferences {
    public static final String
            TEMPERATURE_FAHRENHEIT = "fahrenheit",
            TEMPERATURE_CELSIUS = "celsius",
            SPEED_MPH = "mph",
            SPEED_MS = "m/s",
            SPEED_KMH = "km/h",
            SPEED_KN = "kn",
            THEME_LIGHT = "light",
            THEME_DARK = "dark",
            THEME_AUTO = "auto",
            DEFAULT_VALUE = "",
            ENABLED = "enabled",
            DISABLED = "disabled",
            ONECALL_3_0 = "onecall3.0",
            ONECALL_2_5 = "onecall2.5",
            WEATHER_2_5 = "weather2.5";

    private static final String
            PREFERENCES_NAME = "QuickWeather",
            PREFERENCE_UNIT_TEMPERATURE = "temperature",
            PREFERENCE_UNIT_SPEED = "speed",
            PREFERENCE_APIKEY = "apikey",
            PREFERENCE_THEME = "theme",
            PREFERENCE_SHOWALERTNOTIF = "showalertnotif",
            PREFERENCE_SHOWPERSISTNOTIF = "showpersistnotif",
            PREFERENCE_SHOWLOCATIONDISCLOSURE = "showlocationdisclosure",
            PREFERENCE_APIVERSION = "apiversion",
            PREFERENCE_GADGETBRIDGE = "gadgetbridge";

    private static final String[]
            VALID_TEMPERATURE_VALUES = {TEMPERATURE_FAHRENHEIT, TEMPERATURE_CELSIUS},
            VALID_SPEED_VALUES = {SPEED_MPH, SPEED_MS, SPEED_KMH, SPEED_KN},
            VALID_THEME_VALUES = {THEME_LIGHT, THEME_DARK, THEME_AUTO},
            VALID_BOOLEAN_VALUES = {ENABLED, DISABLED},
            VALID_APIVERSION_VALUES = {ONECALL_3_0, ONECALL_2_5, WEATHER_2_5};

    private static boolean isValidProvider = false;

    private static SharedPreferences sharedPreferences;

    public static String getTemperatureUnit() {
        return getPreference(PREFERENCE_UNIT_TEMPERATURE, VALID_TEMPERATURE_VALUES);
    }

    public static void setTemperatureUnit(String temperatureUnit) {
        putPreference(PREFERENCE_UNIT_TEMPERATURE, VALID_TEMPERATURE_VALUES, temperatureUnit);
    }

    public static String getSpeedUnit() {
        return getPreference(PREFERENCE_UNIT_SPEED, VALID_SPEED_VALUES);
    }

    public static void setSpeedUnit(String speedUnit) {
        putPreference(PREFERENCE_UNIT_SPEED, VALID_SPEED_VALUES, speedUnit);
    }

    public static String getApiKey() {
        return getPreference(PREFERENCE_APIKEY, null);
    }

    public static void setApiKey(String apiKey) {
        putPreference(PREFERENCE_APIKEY, null, apiKey);
    }

    public static String getTheme() {
        return getPreference(PREFERENCE_THEME, VALID_THEME_VALUES);
    }

    public static void setTheme(String theme) {
        putPreference(PREFERENCE_THEME, VALID_THEME_VALUES, theme);
    }

    public static String getShowAlertNotification() {
        return getPreference(PREFERENCE_SHOWALERTNOTIF, VALID_BOOLEAN_VALUES);
    }

    public static void setShowAlertNotification(String showAlertNotification) {
        putPreference(PREFERENCE_SHOWALERTNOTIF, VALID_BOOLEAN_VALUES, showAlertNotification);
    }

    public static String getShowPersistentNotification() {
        return getPreference(PREFERENCE_SHOWPERSISTNOTIF, VALID_BOOLEAN_VALUES);
    }

    public static void setShowPersistentNotification(String showPersistentNotification) {
        putPreference(PREFERENCE_SHOWPERSISTNOTIF, VALID_BOOLEAN_VALUES, showPersistentNotification);
    }

    public static String getShowLocationDisclosure() {
        return getPreference(PREFERENCE_SHOWLOCATIONDISCLOSURE, VALID_BOOLEAN_VALUES, ENABLED);
    }

    public static void setShowLocationDisclosure(String showLocationDisclosure) {
        putPreference(PREFERENCE_SHOWLOCATIONDISCLOSURE, VALID_BOOLEAN_VALUES, showLocationDisclosure);
    }

    public static String getGadgetbridgeEnabled() {
        return getPreference(PREFERENCE_GADGETBRIDGE, VALID_BOOLEAN_VALUES);
    }

    public static void setGadgetbridgeEnabled(String gadgetbridgeEnabled) {
        putPreference(PREFERENCE_GADGETBRIDGE, VALID_BOOLEAN_VALUES, gadgetbridgeEnabled);
    }

    public static String getAPIVersion() {
        return getPreference(PREFERENCE_APIVERSION, VALID_APIVERSION_VALUES);
    }

    public static void setAPIVersion(String apiVersion) {
        putPreference(PREFERENCE_APIVERSION, VALID_APIVERSION_VALUES, apiVersion);
    }

    public static boolean isInitialized() {
        return sharedPreferences.contains(PREFERENCE_APIKEY) &&
                sharedPreferences.contains(PREFERENCE_THEME) &&
                sharedPreferences.contains(PREFERENCE_SHOWALERTNOTIF) &&
                sharedPreferences.contains(PREFERENCE_SHOWPERSISTNOTIF) &&
                sharedPreferences.contains(PREFERENCE_UNIT_SPEED) &&
                sharedPreferences.contains(PREFERENCE_UNIT_TEMPERATURE);
    }

    public static void initialize(Context context) {
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);

            migrateLocationsToDb(context);
            removeOldPreferences();
        }
    }

    public static boolean isValidProvider() {
        if (!isValidProvider) {
            isValidProvider = getPreference("provider", null, "OWM").equals("OWM");
        }

        return isValidProvider;
    }

    private static void migrateLocationsToDb(Context context) {
        if (sharedPreferences.contains("locations")) {
            try {
                Promise.create((a) -> {
                    WeatherDatabase weatherDatabase = WeatherDatabase.getInstance(context);

                    String selectedLocation = getPreference("default_location", null);

                    try {
                        JSONArray locationsArray = new JSONArray(getPreference("locations", null, "[]"));

                        boolean defaultLocationFound = false, currentLocationFound = false;
                        for (int i = 0, l = locationsArray.length(); i < l; i++) {
                            JSONObject o = locationsArray.getJSONObject(i);

                            boolean isSelected = false, isCurrentLocation = false;
                            String locationName = o.getString("location");

                            if (!defaultLocationFound && locationName.equals(selectedLocation)) {
                                isSelected = true;
                                defaultLocationFound = true;
                            }

                            if (!currentLocationFound && locationName.equals(context.getString(R.string.text_current_location))) {
                                isCurrentLocation = true;
                                currentLocationFound = true;
                            }

                            weatherDatabase.locationDao().insert(
                                    new WeatherDatabase.WeatherLocation(
                                            0,
                                            o.getDouble("latitude"),
                                            o.getDouble("longitude"),
                                            locationName,
                                            isSelected,
                                            isCurrentLocation,
                                            i));
                        }
                    } catch (JSONException e) {
                        //
                    }
                }).await();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static void removeOldPreferences() {
        for (String key : new String[]{"locations", "default_location", "showannouncement"}) {
            if (sharedPreferences.contains(key)) {
                sharedPreferences.edit().remove(key).apply();
            }
        }

        if (sharedPreferences.contains("provider") && isValidProvider()) {
            sharedPreferences.edit().remove("provider").apply();
        }
    }

    private static String getPreference(@NonNull String pref, @Nullable String[] validValues) {
        return getPreference(pref, validValues, DEFAULT_VALUE);
    }

    private static String getPreference(@NonNull String pref, @Nullable String[] validValues, @NonNull String defaultValue) {
        String value = sharedPreferences.getString(pref, defaultValue);

        return validValues != null && !isValidValue(value, validValues) ? defaultValue : value;
    }

    @SuppressLint("ApplySharedPref")
    public static void commitChanges() {
        sharedPreferences.edit().commit();
    }

    private static void putPreference(@NonNull String pref, @Nullable String[] validValues, @NonNull String value) {
        if (validValues == null || isValidValue(value, validValues)) {
            sharedPreferences.edit().putString(pref, value).apply();
        }
    }

    private static boolean isValidValue(@NonNull String value, @NonNull String[] validValues) {
        for (String validValue : validValues) {
            if (validValue.equals(value)) {
                return true;
            }
        }

        return DEFAULT_VALUE.equals(value);
    }
}
