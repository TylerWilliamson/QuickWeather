package com.ominous.quickweather.weather;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.ominous.quickweather.R;
import com.ominous.quickweather.dialog.TextDialog;
import com.ominous.quickweather.util.DialogUtils;
import com.ominous.quickweather.util.WeatherPreferences;

import java.util.List;

public class WeatherLocationManager {

    //TODO error checking if locations list is null or empty
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
        criteria.setAccuracy(Criteria.NO_REQUIREMENT);

        if (locationManager != null && isLocationEnabled(context)) {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        locationManager.removeUpdates(this);

                        onLocationAvailableListener.onLocationAvailable(location);
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
                }, Looper.getMainLooper());
            } else {
                throw new LocationDisabledException();
            }
        } else {
            throw new LocationPermissionNotAvailableException();
        }
    }

    @SuppressLint("MissingPermission")//Handled by the isLocationEnabled call
    public static Location getLocation(Context context, boolean isBackground) throws LocationPermissionNotAvailableException, LocationNotAvailableException, LocationDisabledException {
        LocationManager locationManager = ContextCompat.getSystemService(context, LocationManager.class);

        WeatherPreferences.WeatherLocation weatherLocation = getLocationFromPreferences();
        if (locationManager != null && weatherLocation.location.equals(context.getResources().getString(R.string.text_current_location))) {
            if (isLocationEnabled(context) && (!isBackground || isBackgroundLocationEnabled(context))) {
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
                    .setTitle(context.getString(R.string.dialog_background_location))
                    .setContent(context.getString(R.string.dialog_background_location_instructions, permissionsLabel, locationLabel, backgroundLabel, autoRevokeLabel))
                    .setButton(DialogInterface.BUTTON_POSITIVE, "Open Settings",
                            () -> context.startActivity(new Intent()
                                    .setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    .addCategory(Intent.CATEGORY_DEFAULT)
                                    .setData(Uri.fromParts("package", context.getPackageName(), null))
                            ))
                    .addCloseButton()
                    .show();
        }
    }
}