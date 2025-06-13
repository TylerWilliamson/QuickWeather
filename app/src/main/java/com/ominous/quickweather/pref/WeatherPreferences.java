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
    private final static String PREFERENCES_NAME = "QuickWeather";
    private final static String PREFERENCE_UNIT_TEMPERATURE = "temperature";
    private final static String PREFERENCE_UNIT_SPEED = "speed";
    private final static String PREFERENCE_OWM_APIKEY = "apikey";
    private final static String PREFERENCE_OPENMETEO_APIKEY = "openmeteoapikey";
    private final static String PREFERENCE_OPENMETEO_INSTANCE = "openmeteoinstance";
    private final static String PREFERENCE_THEME = "theme";
    private final static String PREFERENCE_SHOWALERTNOTIF = "showalertnotif";
    private final static String PREFERENCE_SHOWPERSISTNOTIF = "showpersistnotif";
    private final static String PREFERENCE_SHOWLOCATIONDISCLOSURE = "showlocationdisclosure";
    private final static String PREFERENCE_OWM_APIVERSION = "apiversion";
    private final static String PREFERENCE_PROVIDER = "weatherprovider";
    private final static String PREFERENCE_GADGETBRIDGE = "gadgetbridge";
    private final static String PREFERENCE_RADARQUALITY = "radarquality";
    private final static String PREFERENCE_RADARTHEME = "radartheme";
    private final static String PREFERENCE_EXPANDEDDETAILS = "expandeddetails";
    private final static String PREFERENCE_LT_APIKEY = "ltapikey";
    private final static String PREFERENCE_LT_INSTANCE = "ltinstance";

    private final static String DEFAULT_VALUE = "";

    private static WeatherPreferences instance;

    private final SharedPreferences sharedPreferences;

    private WeatherPreferences(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);

        migrateLocationsToDb(context);
        migrateHideRadarToDb(context);
        removeOldPreferences();
        checkForMissingProvider();
    }

    public static WeatherPreferences getInstance(Context context) {
        if (instance == null) {
            instance = new WeatherPreferences(context);
        }
        return instance;
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

    public DistanceUnit getDistanceUnit(boolean isShort) {
        if (isShort) {
            return switch (getSpeedUnit()) {
                case DEFAULT -> DistanceUnit.DEFAULT;
                case MPH, FTS -> DistanceUnit.INCH;
                default -> DistanceUnit.MM;
            };
        } else {
            return switch (getSpeedUnit()) {
                case DEFAULT -> DistanceUnit.DEFAULT;
                case MPH, FTS -> DistanceUnit.MI;
                case KN -> DistanceUnit.NMI;
                default -> DistanceUnit.KM;
            };
        }
    }

    public void setSpeedUnit(SpeedUnit speedUnit) {
        putPreference(PREFERENCE_UNIT_SPEED, speedUnit.getValue());
    }

    public RadarTheme getRadarTheme() {
        return RadarTheme.from(getPreference(PREFERENCE_RADARTHEME), RadarTheme.UNIVERSAL_BLUE);
    }

    public void setRadarTheme(@NonNull RadarTheme radarTheme) {
        putPreference(PREFERENCE_RADARTHEME, radarTheme.getValue());
    }

    public String getOWMAPIKey() {
        return getPreference(PREFERENCE_OWM_APIKEY);
    }

    public void setOWMAPIKey(String owmApiKey) {
        putPreference(PREFERENCE_OWM_APIKEY, owmApiKey);
    }

    public String getOpenMeteoAPIKey() {
        return getPreference(PREFERENCE_OPENMETEO_APIKEY);
    }

    public void setOpenMeteoAPIKey(String openMeteoApiKey) {
        putPreference(PREFERENCE_OPENMETEO_APIKEY, openMeteoApiKey);
    }

    public String getOpenMeteoInstance() {
        return getPreference(PREFERENCE_OPENMETEO_INSTANCE);
    }

    public void setOpenMeteoInstance(String openMeteoInstance) {
        putPreference(PREFERENCE_OPENMETEO_INSTANCE, openMeteoInstance);
    }

    public Theme getTheme() {
        return Theme.from(getPreference(PREFERENCE_THEME), Theme.DEFAULT);
    }

    public void setTheme(Theme theme) {
        putPreference(PREFERENCE_THEME, theme.getValue());
    }

    public Enabled getShowAlertNotification() {
        return Enabled.from(getPreference(PREFERENCE_SHOWALERTNOTIF), Enabled.DEFAULT);
    }

    public void setShowAlertNotification(Enabled showAlertNotification) {
        putPreference(PREFERENCE_SHOWALERTNOTIF, showAlertNotification.getValue());
    }

    public Enabled getShowPersistentNotification() {
        return Enabled.from(getPreference(PREFERENCE_SHOWPERSISTNOTIF), Enabled.DEFAULT);
    }

    public void setShowPersistentNotification(Enabled showPersistentNotification) {
        putPreference(PREFERENCE_SHOWPERSISTNOTIF, showPersistentNotification.getValue());
    }

    public Enabled getShowLocationDisclosure() {
        return Enabled.from(getPreference(PREFERENCE_SHOWLOCATIONDISCLOSURE), Enabled.DEFAULT);
    }

    public void setShowLocationDisclosure(Enabled showLocationDisclosure) {
        putPreference(PREFERENCE_SHOWLOCATIONDISCLOSURE, showLocationDisclosure.getValue());
    }

    @Nullable
    public Enabled getGadgetbridgeEnabled() {
        return Enabled.from(getPreference(PREFERENCE_GADGETBRIDGE), Enabled.DEFAULT);
    }

    public void setGadgetbridgeEnabled(Enabled gadgetbridgeEnabled) {
        putPreference(PREFERENCE_GADGETBRIDGE, gadgetbridgeEnabled.getValue());
    }

    public WeatherProvider getWeatherProvider() {
        return WeatherProvider.from(getPreference(PREFERENCE_PROVIDER), WeatherProvider.DEFAULT);
    }

    public void setWeatherProvider(WeatherProvider weatherProvider) {
        putPreference(PREFERENCE_PROVIDER, weatherProvider.getValue());
    }

    //TODO Deprecate after 2024-07-01, Only ONECALL_3_0 valid
    public OwmApiVersion getOwmApiVersion() {
        return OwmApiVersion.from(getPreference(PREFERENCE_OWM_APIVERSION), OwmApiVersion.DEFAULT);
    }

    public void setOwmApiVersion(OwmApiVersion owmApiVersion) {
        putPreference(PREFERENCE_OWM_APIVERSION, owmApiVersion.getValue());
    }

    public RadarQuality getRadarQuality() {
        return RadarQuality.from(getPreference(PREFERENCE_RADARQUALITY, RadarQuality.HIGH.getValue()), RadarQuality.HIGH);
    }

    public void setRadarQuality(RadarQuality radarQuality) {
        putPreference(PREFERENCE_RADARQUALITY, radarQuality.getValue());
    }

    public Enabled getExpandedDetails() {
        return Enabled.from(getPreference(PREFERENCE_EXPANDEDDETAILS, Enabled.DISABLED.getValue()), Enabled.DISABLED);
    }

    public void setExpandedDetails(Enabled enabled) {
        putPreference(PREFERENCE_EXPANDEDDETAILS, enabled.getValue());
    }

    public String getLTApiKey() {
        return getPreference(PREFERENCE_LT_APIKEY);
    }

    public void setLTApiKey(String ltApiKey) {
        putPreference(PREFERENCE_LT_APIKEY, ltApiKey);
    }

    public String getLTInstance() {
        return getPreference(PREFERENCE_LT_INSTANCE);
    }

    public void setLTInstance(String instance) {
        putPreference(PREFERENCE_LT_INSTANCE, instance);
    }

    public boolean isInitialized() {
        return sharedPreferences.contains(PREFERENCE_PROVIDER) &&
                (getWeatherProvider() == WeatherProvider.OPENMETEO || sharedPreferences.contains(PREFERENCE_OWM_APIKEY)) &&
                sharedPreferences.contains(PREFERENCE_THEME) &&
                sharedPreferences.contains(PREFERENCE_SHOWALERTNOTIF) &&
                sharedPreferences.contains(PREFERENCE_SHOWPERSISTNOTIF) &&
                sharedPreferences.contains(PREFERENCE_UNIT_SPEED) &&
                sharedPreferences.contains(PREFERENCE_UNIT_TEMPERATURE);
    }

    public boolean canTranslateAlerts() {
        return !getLTApiKey().isEmpty() || !getLTInstance().isEmpty();
    }

    public boolean shouldShowAlertNotification() {
        return getShowAlertNotification() == Enabled.ENABLED;
    }

    public boolean shouldShowPersistentNotification() {
        return getShowPersistentNotification() == Enabled.ENABLED;
    }

    public boolean shouldDoGadgetbridgeBroadcast() {
        return getGadgetbridgeEnabled() == Enabled.ENABLED;
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

    private void migrateHideRadarToDb(Context context) {
        if (getPreference(PREFERENCE_RADARQUALITY, DEFAULT_VALUE).equals("disabled")) {
            try {
                Promise.create(context)
                        .then(c -> {
                            WeatherDatabase.getInstance(c).cardDao().disableRadar();

                            setRadarQuality(RadarQuality.HIGH);
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

        if (sharedPreferences.contains("provider") &&
                getPreference("provider", "OWM").equals("OWM")) {
            sharedPreferences.edit().remove("provider").apply();
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

    private void checkForMissingProvider() {
        if (sharedPreferences.contains(PREFERENCE_OWM_APIKEY) &&
                !sharedPreferences.contains(PREFERENCE_PROVIDER)) {
            setWeatherProvider(WeatherProvider.OPENWEATHERMAP);
        }
    }
}
