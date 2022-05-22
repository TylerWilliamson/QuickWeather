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

package com.ominous.quickweather.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;

import com.ominous.quickweather.R;
import com.ominous.quickweather.data.WeatherDatabase;
import com.ominous.quickweather.dialog.TextDialog;
import com.ominous.quickweather.util.DialogUtils;
import com.ominous.quickweather.util.WeatherPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

public class WeatherLocationManager {
    @SuppressLint("MissingPermission")//Handled by the isLocationEnabled call
    public static Location getCurrentLocation(Context context, boolean isBackground) throws LocationPermissionNotAvailableException, LocationDisabledException {
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
    public static Location getLocation(Context context, boolean isBackground) throws LocationPermissionNotAvailableException, LocationDisabledException {
        WeatherDatabase.WeatherLocation weatherLocation = WeatherDatabase.getInstance(context).locationDao().getSelected();

        if (weatherLocation.isCurrentLocation) {
            if (isLocationPermissionGranted(context) &&
                    (!isBackground || isBackgroundLocationPermissionGranted(context))) {
                LocationManager locationManager = ContextCompat.getSystemService(context, LocationManager.class);

                Location bestLocation = null;
                List<String> providers;

                if (locationManager != null &&
                        (providers = locationManager.getProviders(true)).size() > 0) {
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
        } else {
            Location location = new Location(LocationManager.NETWORK_PROVIDER);
            location.setLatitude(weatherLocation.latitude);
            location.setLongitude(weatherLocation.longitude);

            return location;
        }
    }

    public static boolean isLocationPermissionGranted(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isBackgroundLocationPermissionGranted(Context context) {
        return Build.VERSION.SDK_INT < 29 || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static void showLocationDisclosure(Context context, Runnable onAcceptRunnable) {
        if (WeatherPreferences.getShowLocationDisclosure().equals(WeatherPreferences.ENABLED)) {
            DialogUtils.showLocationDisclosure(context, onAcceptRunnable);
        } else {
            onAcceptRunnable.run();
        }
    }

    public static void requestLocationPermissions(Context context, ActivityResultLauncher<String[]> requestPermissionLauncher) {
        showLocationDisclosure(context, () -> {
            WeatherPreferences.setShowLocationDisclosure(WeatherPreferences.DISABLED);

            requestPermissionLauncher.launch(Build.VERSION.SDK_INT == 29 ?
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION} :
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION});
        });
    }

    //TODO Cleanup Strings
    public static void requestBackgroundLocation(Context context, ActivityResultLauncher<String[]> requestPermissionLauncher) {
        if (Build.VERSION.SDK_INT == 29) {
            requestPermissionLauncher.launch(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION});
        } else if (Build.VERSION.SDK_INT >= 30) {
            PackageManager packageManager = context.getPackageManager();
            CharSequence locationLabel, backgroundLabel, permissionsLabel, autoRevokeLabel;

            backgroundLabel = packageManager.getBackgroundPermissionOptionLabel();

            try {
                locationLabel = packageManager.getPermissionGroupInfo(Manifest.permission_group.LOCATION, 0).loadLabel(packageManager);
            } catch (PackageManager.NameNotFoundException e) {
                locationLabel = "Location";
            }

            try {
                int permissionsLabelResId = packageManager
                        .getResourcesForApplication("com.android.settings")
                        .getIdentifier("com.android.settings:string/permissions_label", null, null);

                permissionsLabel = permissionsLabelResId == 0 ? "Permissions" : context.getPackageManager().getText("com.android.settings", permissionsLabelResId, null);
            } catch (PackageManager.NameNotFoundException e) {
                permissionsLabel = "Permissions";
            }

            try {
                int autoRevokeLabelResId = packageManager
                        .getResourcesForApplication("com.google.android.permissioncontroller")
                        .getIdentifier("com.android.permissioncontroller:string/auto_revoke_label", null, null);

                autoRevokeLabel = autoRevokeLabelResId == 0 ? "Remove permissions if app isn’t used" : context.getPackageManager().getText("com.google.android.permissioncontroller", autoRevokeLabelResId, null);
            } catch (PackageManager.NameNotFoundException e) {
                autoRevokeLabel = "Remove permissions if app isn’t used";
            }

            new TextDialog(context)
                    .setTitle(context.getString(R.string.dialog_background_location_title))
                    .setContent(context.getString(R.string.dialog_background_location, permissionsLabel, locationLabel, backgroundLabel, autoRevokeLabel))
                    .setButton(DialogInterface.BUTTON_POSITIVE, context.getString(R.string.text_settings),
                            () -> context.startActivity(new Intent()
                                    .setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    .addCategory(Intent.CATEGORY_DEFAULT)
                                    .setData(Uri.fromParts("package", context.getPackageName(), null))
                            ))
                    .addCloseButton()
                    .show();
        }
    }

    public static class LocationPermissionNotAvailableException extends Exception {
    }

    public static class LocationDisabledException extends Exception {
    }
}