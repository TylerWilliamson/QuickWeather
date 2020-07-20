package com.ominous.quickweather.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class WeatherPreferences {
    private static final String
            PREFERENCES_NAME = "QuickWeather",
            PREFERENCE_LOCATIONS = "locations",
            PREFERENCE_LOCATIONS_LOCATION = "location",
            PREFERENCE_LOCATIONS_LATITUDE = "latitude",
            PREFERENCE_LOCATIONS_LONGITUDE = "longitude",
            PREFERENCE_LOCATION_DEFAULT = "default_location",
            PREFERENCE_UNIT_TEMPERATURE = "temperature",
            PREFERENCE_UNIT_SPEED = "speed",
            PREFERENCE_APIKEY = "apikey",
            PREFERENCE_THEME = "theme",
            PREFERENCE_SHOWALERTNOTIF = "showalertnotif",
            PREFERENCE_SHOWPERSISTNOTIF = "showpersistnotif",
            PREFERENCE_PROVIDER = "provider",
            PREFERENCE_SHOWANNOUNCEMENT = "showannouncement";

    public static final String
            TEMPERATURE_FAHRENHEIT = "fahrenheit",
            TEMPERATURE_CELSIUS = "celsius",
            SPEED_MPH = "mph",
            SPEED_MS = "m/s",
            SPEED_KMH = "km/h",
            THEME_LIGHT = "light",
            THEME_DARK = "dark",
            THEME_AUTO = "auto",
            PROVIDER_OWM = "OWM",
            PROVIDER_DS = "DS",
            DEFAULT_VALUE = "",
            DEFAULT_ARRAY = "[]",
            ENABLED = "enabled",
            DISABLED = "disabled";

    //TODO: add error checking to gets and sets

    private static SharedPreferences sharedPreferences;

    public static List<WeatherLocation> getLocations() {
        try {
            List<WeatherLocation> locationsList = new ArrayList<>();
            JSONArray locationsArray = new JSONArray(sharedPreferences.getString(PREFERENCE_LOCATIONS, DEFAULT_ARRAY));

            for (int i = 0, l = locationsArray.length(); i < l; i++) {
                JSONObject o = locationsArray.getJSONObject(i);

                locationsList.add(new WeatherLocation(o.getString(PREFERENCE_LOCATIONS_LOCATION), o.getDouble(PREFERENCE_LOCATIONS_LATITUDE), o.getDouble(PREFERENCE_LOCATIONS_LONGITUDE)));
            }
            return locationsList;

        } catch (JSONException e) {
            sharedPreferences.edit().putString(PREFERENCE_LOCATIONS, DEFAULT_ARRAY).apply();
            return new ArrayList<>();
        }
    }

    public static void setLocations(List<WeatherLocation> locations) {
        try {
            JSONArray locationsArray = new JSONArray();

            for (WeatherLocation weatherLocation : locations) {
                locationsArray.put(new JSONObject()
                        .put(PREFERENCE_LOCATIONS_LOCATION, weatherLocation.location)
                        .put(PREFERENCE_LOCATIONS_LATITUDE, weatherLocation.latitude)
                        .put(PREFERENCE_LOCATIONS_LONGITUDE, weatherLocation.longitude)
                );
            }

            sharedPreferences.edit().putString(PREFERENCE_LOCATIONS, locationsArray.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static String getDefaultLocation() {
        String defaultLocation = sharedPreferences.getString(PREFERENCE_LOCATION_DEFAULT, DEFAULT_VALUE);

        List<WeatherLocation> locations;

        if (DEFAULT_VALUE.equals(defaultLocation) && (locations = getLocations()).size() > 0) {
            setDefaultLocation(defaultLocation = locations.get(0).location);
        }

        return defaultLocation;
    }

    public static void setDefaultLocation(String defaultLocation) {
        sharedPreferences.edit().putString(PREFERENCE_LOCATION_DEFAULT, defaultLocation).apply();
    }

    public static String getTemperatureUnit() {
        return sharedPreferences.getString(PREFERENCE_UNIT_TEMPERATURE, DEFAULT_VALUE);
    }

    public static void setTemperatureUnit(String temperatureUnit) {
        sharedPreferences.edit().putString(PREFERENCE_UNIT_TEMPERATURE, temperatureUnit).apply();
    }

    public static String getSpeedUnit() {
        return sharedPreferences.getString(PREFERENCE_UNIT_SPEED, DEFAULT_VALUE);
    }

    public static void setSpeedUnit(String speedUnit) {
        sharedPreferences.edit().putString(PREFERENCE_UNIT_SPEED, speedUnit).apply();
    }

    public static String getApiKey() {
        return sharedPreferences.getString(PREFERENCE_APIKEY, DEFAULT_VALUE);
    }

    public static void setApiKey(String apiKey) {
        sharedPreferences.edit().putString(PREFERENCE_APIKEY, apiKey).apply();
    }

    public static String getTheme() {
        return sharedPreferences.getString(PREFERENCE_THEME, DEFAULT_VALUE);
    }

    public static void setTheme(String theme) {
        sharedPreferences.edit().putString(PREFERENCE_THEME, theme).apply();
    }

    public static void setShowAlertNotification(String showAlertNotification) {
        sharedPreferences.edit().putString(PREFERENCE_SHOWALERTNOTIF, showAlertNotification).apply();
    }

    public static void setShowPersistentNotification(String showPersistentNotification) {
        sharedPreferences.edit().putString(PREFERENCE_SHOWPERSISTNOTIF, showPersistentNotification).apply();
    }

    public static String getShowAlertNotification() {
        return sharedPreferences.getString(PREFERENCE_SHOWALERTNOTIF, DEFAULT_VALUE);
    }

    public static String getShowPersistentNotification() {
        return sharedPreferences.getString(PREFERENCE_SHOWPERSISTNOTIF, DEFAULT_VALUE);
    }

    public static String getProvider() {
        return sharedPreferences.getString(PREFERENCE_PROVIDER, DEFAULT_VALUE);
    }

    public static void setProvider(String provider) {
        sharedPreferences.edit().putString(PREFERENCE_PROVIDER, provider).apply();
    }

    public static String getShowAnnouncement() {
        return sharedPreferences.getString(PREFERENCE_SHOWANNOUNCEMENT, ENABLED);
    }

    public static void setShowAnnouncement(String showAnnouncement) {
        sharedPreferences.edit().putString(PREFERENCE_SHOWANNOUNCEMENT, showAnnouncement).apply();
    }

    public static boolean isInitialized() {
        return sharedPreferences.contains(PREFERENCE_APIKEY);
    }

    public static void initialize(Context context) {
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        }

        if (!isInitialized()) {
            setProvider(PROVIDER_OWM);
        } else if (getProvider().equals(DEFAULT_VALUE)) {
            setProvider(PROVIDER_DS);
        }
    }

    public static class WeatherLocation implements Parcelable {
        public String location;
        public double latitude;
        public double longitude;

        public WeatherLocation(String location, double latitude, double longitude) {
            this.location = location;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        WeatherLocation(Parcel in) {
            this.location = in.readString();
            this.latitude = in.readDouble();
            this.longitude = in.readDouble();
        }

        public static final Parcelable.Creator<WeatherLocation> CREATOR = new Parcelable.Creator<WeatherLocation>() {
            public WeatherLocation createFromParcel(Parcel in) {
                return new WeatherLocation(in);
            }

            public WeatherLocation[] newArray(int size) {
                return new WeatherLocation[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(location);
            dest.writeDouble(latitude);
            dest.writeDouble(longitude);
        }
    }
}
