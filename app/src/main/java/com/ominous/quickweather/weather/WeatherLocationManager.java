package com.ominous.quickweather.weather;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;

import androidx.core.content.ContextCompat;

import com.ominous.quickweather.R;
import com.ominous.quickweather.util.WeatherPreferences;

import java.util.List;

public class WeatherLocationManager {

    public static WeatherPreferences.WeatherLocation getLocationFromPreferences() {
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

    public static Location getLocation(Context context) throws LocationPermissionNotAvailableException {
        LocationManager locationManager = ContextCompat.getSystemService(context,LocationManager.class);

        WeatherPreferences.WeatherLocation weatherLocation = getLocationFromPreferences();
        if (locationManager != null && weatherLocation.location.equals(context.getResources().getString(R.string.text_current_location))) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                return locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            } else {
                throw new LocationPermissionNotAvailableException();
            }
        } else {
            Location location = new Location(LocationManager.NETWORK_PROVIDER);
            location.setLatitude(weatherLocation.latitude);
            location.setLongitude(weatherLocation.longitude);

            return location;
        }
    }

    public static class LocationPermissionNotAvailableException extends Exception {}
}
