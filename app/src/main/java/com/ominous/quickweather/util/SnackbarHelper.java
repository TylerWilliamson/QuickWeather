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

package com.ominous.quickweather.util;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.ominous.quickweather.R;
import com.ominous.quickweather.location.WeatherLocationManager;
import com.ominous.quickweather.work.WeatherWorkManager;
import com.ominous.tylerutils.plugins.GithubUtils;
import com.ominous.tylerutils.util.ViewUtils;

//TODO: More logging
//TODO: Support multiple snackbars somehow
public class SnackbarHelper {
    private final static String TAG = "Logger";
    private final Snackbar snackbar;
    private final DialogHelper dialogHelper;

    private final WeatherWorkManager weatherWorkManager;
    private final WeatherLocationManager weatherLocationManager;

    public SnackbarHelper(View view) {
        snackbar = ViewUtils.makeSnackbar(view, android.R.string.ok, Snackbar.LENGTH_INDEFINITE)
                .setTextMaxLines(5)
                .setTextColor(ContextCompat.getColor(view.getContext(), R.color.color_white_emphasis))
                .setActionTextColor(ContextCompat.getColor(view.getContext(), R.color.color_accent_text));

        TextView messageView = snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text);
        messageView.setTextSize(TypedValue.COMPLEX_UNIT_PX, view.getContext().getResources().getDimension(R.dimen.text_size_regular));
        messageView.setPadding(0, 0, 0, 0);
        messageView.setLineSpacing(view.getContext().getResources().getDimension(R.dimen.margin_quarter), 1f);

        TextView buttonView = snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_action);
        buttonView.setTextSize(TypedValue.COMPLEX_UNIT_PX, view.getContext().getResources().getDimension(R.dimen.text_size_regular));

        dialogHelper = new DialogHelper(view.getContext());
        weatherWorkManager = new WeatherWorkManager(view.getContext());
        weatherLocationManager = new WeatherLocationManager(view.getContext());
    }

    public void logError(String errorMessage, Throwable t) {
        Log.e(TAG, errorMessage, t);
    }

    public void logError(String errorMessage) {
        logError(errorMessage, null);
    }

    private void updateSnackbar(CharSequence text, int duration, int buttonTextRes, View.OnClickListener buttonOnClickListener) {
        snackbar
                .setText(text)
                .setDuration(duration);

        if (buttonTextRes == 0 || buttonOnClickListener == null) {
            snackbar.setAction(null, null);
        } else {
            snackbar.setAction(snackbar.getContext().getText(buttonTextRes), buttonOnClickListener);
        }

        snackbar.show();
    }

    private void updateSnackbar(int textRes, int duration, int buttonTextRes, View.OnClickListener buttonOnClickListener) {
        updateSnackbar(snackbar.getContext().getText(textRes), duration, buttonTextRes, buttonOnClickListener);
    }

    public void notifyObtainingLocation() {
        updateSnackbar(R.string.snackbar_obtaining_location,
                Snackbar.LENGTH_INDEFINITE,
                0,
                null);
    }

    public void notifyLocDisabled() {
        updateSnackbar(R.string.error_gps_disabled,
                Snackbar.LENGTH_INDEFINITE,
                R.string.text_settings,
                v -> v.getContext().startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)));
    }

    public void notifyNullLoc() {
        notifyError(R.string.error_null_location, null);
    }

    public void notifyLocPermDenied(ActivityResultLauncher<String[]> requestPermissionLauncher) {
        updateSnackbar(R.string.snackbar_no_location_permission,
                Snackbar.LENGTH_INDEFINITE,
                R.string.text_settings,
                v -> weatherLocationManager.requestLocationPermissions(dialogHelper, requestPermissionLauncher));
    }

    public void notifyBackLocPermDenied(ActivityResultLauncher<String[]> requestPermissionLauncher, boolean notificationsEnabled) {
        updateSnackbar(notificationsEnabled ? R.string.snackbar_background_location_notifications : R.string.snackbar_background_location_gadgetbridge,
                Snackbar.LENGTH_INDEFINITE,
                R.string.text_settings,
                v -> weatherLocationManager.requestBackgroundLocation(dialogHelper, requestPermissionLauncher));
    }

    public void notifyNotificationPermissionDenied(ActivityResultLauncher<String> requestPermissionLauncher) {
        if (Build.VERSION.SDK_INT >= 33) {
            updateSnackbar(snackbar.getContext().getString(R.string.snackbar_notification_permission),
                    Snackbar.LENGTH_INDEFINITE,
                    R.string.text_settings,
                    v -> requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS));
        }
    }

    public void notifyNoNewVersion() {
        updateSnackbar(R.string.text_no_new_version,
                Snackbar.LENGTH_SHORT,
                0,
                null);
    }

    public void notifyNewVersion(GithubUtils.GitHubRelease latestRelease) {
        updateSnackbar(R.string.text_new_version_available,
                Snackbar.LENGTH_INDEFINITE,
                R.string.text_open,
                v -> dialogHelper.showNewVersionDialog(latestRelease.body, latestRelease.tag_name));
    }

    public void notifyBatteryOptimization() {
        updateSnackbar(R.string.snackbar_battery_optimization,
                Snackbar.LENGTH_INDEFINITE,
                R.string.text_settings,
                v -> weatherWorkManager.requestIgnoreBatteryOptimization(dialogHelper));
    }

    public void notifyError(String error, Throwable t) {
        logError(error, t);

        updateSnackbar(error, Snackbar.LENGTH_SHORT, 0, null);
    }

    public void notifyError(@StringRes int errorMessageRes, Throwable t) {
        notifyError(snackbar.getContext().getString(errorMessageRes), t);
    }

    public void dismiss() {
        snackbar.dismiss();
    }
}
