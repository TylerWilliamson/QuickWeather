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

package com.ominous.quickweather.pref;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ominous.quickweather.R;
import com.ominous.quickweather.data.WeatherDatabase;
import com.ominous.tylerutils.async.Promise;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;

public class WeatherPreferences {
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
            PREFERENCE_GADGETBRIDGE = "gadgetbridge",
            DEFAULT_VALUE = "",
            ENABLED = "enabled",
            DISABLED = "disabled";

    private static WeatherPreferences instance;

    private static boolean isValidProvider = false;

    private final SharedPreferences sharedPreferences;

    private WeatherPreferences(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);

        migrateLocationsToDb(context);
        removeOldPreferences();
    }

    public static WeatherPreferences getInstance(Context context) {
        return instance == null ? instance = new WeatherPreferences(context) : instance;
    }

    public TemperatureUnit getTemperatureUnit() {
        return TemperatureUnit.from(getPreference(PREFERENCE_UNIT_TEMPERATURE), TemperatureUnit.DEFAULT);
    }

    public void setTemperatureUnit(TemperatureUnit temperatureUnit) {
        putPreference(PREFERENCE_UNIT_TEMPERATURE, temperatureUnit.getValue());
    }

    public SpeedUnit getSpeedUnit() {
        return SpeedUnit.from(getPreference(PREFERENCE_UNIT_SPEED), SpeedUnit.DEFAULT);
    }

    public void setSpeedUnit(SpeedUnit speedUnit) {
        putPreference(PREFERENCE_UNIT_SPEED, speedUnit.getValue());
    }

    public String getAPIKey() {
        return getPreference(PREFERENCE_APIKEY);
    }

    public void setAPIKey(String apiKey) {
        putPreference(PREFERENCE_APIKEY, apiKey);
    }

    public Theme getTheme() {
        return Theme.from(getPreference(PREFERENCE_THEME), Theme.DEFAULT);
    }

    public void setTheme(Theme theme) {
        putPreference(PREFERENCE_THEME, theme.getValue());
    }

    @Nullable
    public Boolean getShowAlertNotification() {
        return preferenceToBoolean(getPreference(PREFERENCE_SHOWALERTNOTIF));
    }

    public void setShowAlertNotification(boolean showAlertNotification) {
        putPreference(PREFERENCE_SHOWALERTNOTIF, showAlertNotification ? ENABLED : DISABLED);
    }

    @Nullable
    public Boolean getShowPersistentNotification() {
        return preferenceToBoolean(getPreference(PREFERENCE_SHOWPERSISTNOTIF));
    }

    public void setShowPersistentNotification(boolean showPersistentNotification) {
        putPreference(PREFERENCE_SHOWPERSISTNOTIF, showPersistentNotification ? ENABLED : DISABLED);
    }

    public boolean getShowLocationDisclosure() {
        return !getPreference(PREFERENCE_SHOWLOCATIONDISCLOSURE).equals(DISABLED);
    }

    public void setShowLocationDisclosure(boolean showLocationDisclosure) {
        putPreference(PREFERENCE_SHOWLOCATIONDISCLOSURE, showLocationDisclosure ? ENABLED : DISABLED);
    }

    @Nullable
    public Boolean getGadgetbridgeEnabled() {
        return preferenceToBoolean(getPreference(PREFERENCE_GADGETBRIDGE));
    }

    public void setGadgetbridgeEnabled(boolean gadgetbridgeEnabled) {
        putPreference(PREFERENCE_GADGETBRIDGE, gadgetbridgeEnabled ? ENABLED : DISABLED);
    }

    public ApiVersion getAPIVersion() {
        return ApiVersion.from(getPreference(PREFERENCE_APIVERSION), ApiVersion.DEFAULT);
    }

    public void setAPIVersion(ApiVersion apiVersion) {
        putPreference(PREFERENCE_APIVERSION, apiVersion.getValue());
    }

    public boolean isInitialized() {
        return sharedPreferences.contains(PREFERENCE_APIKEY) &&
                sharedPreferences.contains(PREFERENCE_THEME) &&
                sharedPreferences.contains(PREFERENCE_SHOWALERTNOTIF) &&
                sharedPreferences.contains(PREFERENCE_SHOWPERSISTNOTIF) &&
                sharedPreferences.contains(PREFERENCE_UNIT_SPEED) &&
                sharedPreferences.contains(PREFERENCE_UNIT_TEMPERATURE);
    }

    public boolean isValidProvider() {
        if (!isValidProvider) {
            isValidProvider = getPreference("provider", "OWM").equals("OWM");
        }

        return isValidProvider;
    }

    public boolean shouldShowAlertNotification() {
        return Boolean.TRUE.equals(getShowAlertNotification());
    }

    public boolean shouldShowPersistentNotification() {
        return Boolean.TRUE.equals(getShowPersistentNotification());
    }

    public boolean shouldDoGadgetbridgeBroadcast() {
        return Boolean.TRUE.equals(getGadgetbridgeEnabled());
    }

    public boolean shouldRunBackgroundJob() {
        return shouldShowPersistentNotification() ||
                shouldShowAlertNotification() ||
                shouldDoGadgetbridgeBroadcast();
    }

    public boolean shouldShowNotifications() {
        return shouldShowPersistentNotification() ||
                shouldShowAlertNotification();
    }

    private void migrateLocationsToDb(Context context) {
        if (sharedPreferences.contains("locations")) {
            try {
                Promise.create((a) -> {
                    WeatherDatabase weatherDatabase = WeatherDatabase.getInstance(context);

                    String selectedLocation = getPreference("default_location");

                    try {
                        JSONArray locationsArray = new JSONArray(getPreference("locations", "[]"));

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

    private void removeOldPreferences() {
        for (String key : new String[]{"locations", "default_location", "showannouncement"}) {
            if (sharedPreferences.contains(key)) {
                sharedPreferences.edit().remove(key).apply();
            }
        }

        if (sharedPreferences.contains("provider") && isValidProvider()) {
            sharedPreferences.edit().remove("provider").apply();
        }
    }

    private Boolean preferenceToBoolean(@NonNull String value) {
        switch (value) {
            case ENABLED:
                return true;
            case DISABLED:
                return false;
            default:
                return null;
        }
    }

    private String getPreference(@NonNull String pref) {
        return getPreference(pref, DEFAULT_VALUE);
    }

    private String getPreference(@NonNull String pref, String defaultValue) {
        return sharedPreferences.getString(pref, defaultValue);
    }

    @SuppressLint("ApplySharedPref")
    public void commitChanges() {
        sharedPreferences.edit().commit();
    }

    private void putPreference(@NonNull String pref, @NonNull String value) {
        sharedPreferences.edit().putString(pref, value).apply();
    }
}
