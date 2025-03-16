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

package com.ominous.quickweather.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.ominous.quickweather.pref.Enabled;
import com.ominous.quickweather.pref.WeatherPreferences;
import com.ominous.quickweather.util.DialogHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public enum WeatherLocationManager {
    INSTANCE;

    @SuppressWarnings("SameReturnValue")
    public static WeatherLocationManager getInstance() {
        return INSTANCE;
    }

    //TODO pass DialogHelper to calls
    @SuppressLint("MissingPermission")//Handled by the isLocationEnabled call
    public Location obtainCurrentLocation(Context context, boolean isBackground) throws LocationPermissionNotAvailableException, LocationDisabledException {
        final Location location = new Location(LocationManager.GPS_PROVIDER);
        final LocationManager locationManager = ContextCompat.getSystemService(context, LocationManager.class);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final ArrayList<LocationListener> locationListeners = new ArrayList<>();
        boolean providersAvailable = false;

        if (locationManager != null && isLocationPermissionGranted(context)
                && (!isBackground || isBackgroundLocationPermissionGranted(context))) {
            for (String provider : locationManager.getProviders(true)) {
                if (locationManager.isProviderEnabled(provider)) {
                    providersAvailable = true;

                    LocationListener locationListener = new LocationListener() {
                        @Override
                        public void onLocationChanged(@NonNull Location l) {
                            location.set(l);
                            countDownLatch.countDown();
                        }

                        @Override
                        public void onStatusChanged(String provider, int status, Bundle extras) {
                        }

                        @Override
                        public void onProviderEnabled(@NonNull String provider) {
                        }

                        @Override
                        public void onProviderDisabled(@NonNull String provider) {
                        }
                    };

                    locationListeners.add(locationListener);

                    locationManager.requestLocationUpdates(provider, 500, 0, locationListener, Looper.getMainLooper());
                }
            }

            if (!providersAvailable) {
                throw new LocationDisabledException();
            }
        } else {
            throw new LocationPermissionNotAvailableException();
        }

        try {
            boolean succeeded = countDownLatch.await(15, TimeUnit.MINUTES);

            for (LocationListener locationListener : locationListeners) {
                locationManager.removeUpdates(locationListener);
            }

            return succeeded ? location : null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Nullable
    @SuppressLint("MissingPermission")//Handled by the isLocationEnabled call
    public Location getLastKnownLocation(Context context, boolean isBackground) throws LocationPermissionNotAvailableException, LocationDisabledException {
        if (isLocationPermissionGranted(context) &&
                (!isBackground || isBackgroundLocationPermissionGranted(context))) {
            LocationManager locationManager = ContextCompat.getSystemService(context, LocationManager.class);

            Location bestLocation = null;
            List<String> providers;

            if (locationManager != null &&
                    !(providers = locationManager.getProviders(true)).isEmpty()) {
                for (String provider : providers) {
                    Location newLocation = locationManager.getLastKnownLocation(provider);

                    if (newLocation != null && (bestLocation == null || newLocation.getAccuracy() < bestLocation.getAccuracy())) {
                        bestLocation = newLocation;
                    }
                }

                return bestLocation;
            } else {
                throw new LocationDisabledException();
            }
        } else {
            throw new LocationPermissionNotAvailableException();
        }
    }

    public boolean isLocationPermissionGranted(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public boolean isBackgroundLocationPermissionGranted(Context context) {
        return Build.VERSION.SDK_INT < 29 || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public void showLocationDisclosure(Context context, Runnable onAcceptRunnable) {
        if (WeatherPreferences.getInstance(context).getShowLocationDisclosure() != Enabled.DISABLED) {
            new DialogHelper(context).showLocationDisclosure(onAcceptRunnable);
        } else {
            onAcceptRunnable.run();
        }
    }

    public void requestLocationPermissions(Context context, ActivityResultLauncher<String[]> requestPermissionLauncher) {
        showLocationDisclosure(context, () -> {
            WeatherPreferences.getInstance(context).setShowLocationDisclosure(Enabled.DISABLED);

            requestPermissionLauncher.launch(Build.VERSION.SDK_INT == 29 ?
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION} :
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION});
        });
    }

    public void requestBackgroundLocation(Context context, ActivityResultLauncher<String[]> requestPermissionLauncher) {
        if (Build.VERSION.SDK_INT == 29) {
            requestPermissionLauncher.launch(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION});
        } else if (Build.VERSION.SDK_INT >= 30) {
            requestPermissionLauncher.launch(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION});

            new DialogHelper(context).showBackgroundLocationInstructionsDialog();
        }
    }

}