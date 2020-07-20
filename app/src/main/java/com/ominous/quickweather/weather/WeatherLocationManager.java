package com.ominous.quickweather.weather;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
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

    @SuppressLint("MissingPermission")//Handled by the isLocationEnabled call
    public static void getCurrentLocation(Context context, OnLocationAvailableListener onLocationAvailableListener) throws LocationPermissionNotAvailableException, LocationDisabledException {
        final LocationManager locationManager = ContextCompat.getSystemService(context, LocationManager.class);

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_LOW);

        if (locationManager != null && isLocationEnabled(context)) {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        locationManager.removeUpdates(this);

                        onLocationAvailableListener.onLocationAvailable(location);
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {
                    }

                    @Override
                    public void onProviderEnabled(String provider) {
                    }

                    @Override
                    public void onProviderDisabled(String provider) {
                    }
                });
            } else {
                throw new LocationDisabledException();
            }
        } else {
            throw new LocationPermissionNotAvailableException();
        }
    }

    public static Location getLocation(Context context) throws LocationPermissionNotAvailableException, LocationNotAvailableException, LocationDisabledException {
        LocationManager locationManager = ContextCompat.getSystemService(context, LocationManager.class);

        WeatherPreferences.WeatherLocation weatherLocation = getLocationFromPreferences();
        if (locationManager != null && weatherLocation.location.equals(context.getResources().getString(R.string.text_current_location))) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Location bestLocation = null;

                for (String provider : locationManager.getProviders(true)) {
                    Location newLocation = locationManager.getLastKnownLocation(provider);

                    if (newLocation != null && (bestLocation == null || newLocation.getAccuracy() < bestLocation.getAccuracy())) {
                        bestLocation = newLocation;
                    }
                }

                if (bestLocation != null) {
                    return bestLocation;
                } else {
                    if (!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) &&
                            !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        throw new LocationDisabledException();
                    } else {
                        throw new LocationNotAvailableException();
                    }
                }
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

    public interface OnLocationAvailableListener {
        void onLocationAvailable(Location location);
    }

    public static class LocationPermissionNotAvailableException extends Exception {
    }

    public static class LocationDisabledException extends Exception {
    }

    public static class LocationNotAvailableException extends Exception {
    }

    public static boolean isLocationEnabled(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isBackgroundLocationEnabled(Context context) {
        return Build.VERSION.SDK_INT < 29 || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestLocationPermissions(Activity activity, int requestCode) {
        String[] permissions = Build.VERSION.SDK_INT >= 29 ?
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION} :
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION};

        ActivityCompat.requestPermissions(activity,
                permissions,
                requestCode);
    }
}