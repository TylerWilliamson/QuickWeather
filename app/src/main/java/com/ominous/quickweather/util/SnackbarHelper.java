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

import android.Manifest;
import android.app.ActivityOptions;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.ominous.quickweather.R;
import com.ominous.quickweather.activity.SettingsActivity;
import com.ominous.quickweather.dialog.TextDialog;
import com.ominous.quickweather.location.WeatherLocationManager;
import com.ominous.tylerutils.browser.CustomTabs;
import com.ominous.tylerutils.plugins.GithubUtils;
import com.ominous.tylerutils.util.ViewUtils;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.content.ContextCompat;

//TODO: More logging
//TODO: Support multiple snackbars somehow
public class SnackbarHelper {
    private final static String TAG = "Logger";
    private final Snackbar snackbar;

    public SnackbarHelper(View view) {
        snackbar = ViewUtils.makeSnackbar(view, android.R.string.ok, Snackbar.LENGTH_INDEFINITE)
                .setTextColor(ContextCompat.getColor(view.getContext(), R.color.color_white_emphasis))
                .setActionTextColor(ContextCompat.getColor(view.getContext(), R.color.color_accent));

        TextView messageView = snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text);
        messageView.setTextSize(TypedValue.COMPLEX_UNIT_PX, view.getContext().getResources().getDimension(R.dimen.text_size_regular));
        messageView.setPadding(0, 0, 0, 0);
        messageView.setLineSpacing(view.getContext().getResources().getDimension(R.dimen.margin_quarter), 1f);

        TextView buttonView = snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_action);
        buttonView.setTextSize(TypedValue.COMPLEX_UNIT_PX, view.getContext().getResources().getDimension(R.dimen.text_size_regular));
    }

    public void logError(String errorMessage, Throwable t) {
        Log.e(TAG, errorMessage, t);
    }

    public void logError(String errorMessage) {
        Log.e(TAG, errorMessage);
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

    //TODO use NotifyNullLoc in other places
    public void notifyNullLoc() {
        logError("Null Location Received");

        updateSnackbar(R.string.error_null_location,
                Snackbar.LENGTH_SHORT,
                0,
                null
        );
    }

    public void notifyLocPermDenied(ActivityResultLauncher<String[]> requestPermissionLauncher) {
        updateSnackbar(R.string.snackbar_no_location_permission,
                Snackbar.LENGTH_INDEFINITE,
                R.string.text_settings,
                v -> WeatherLocationManager.requestLocationPermissions(v.getContext(), requestPermissionLauncher));
    }

    public void notifyBackLocPermDenied(ActivityResultLauncher<String[]> requestPermissionLauncher) {
        updateSnackbar(R.string.snackbar_background_location,
                Snackbar.LENGTH_INDEFINITE,
                R.string.text_settings,
                v -> WeatherLocationManager.requestBackgroundLocation(v.getContext(), requestPermissionLauncher));
    }

    public void notifyInvalidProvider() {
        updateSnackbar(R.string.snackbar_invalid_provider,
                Snackbar.LENGTH_INDEFINITE,
                R.string.text_settings,
                v -> ContextCompat.startActivity(v.getContext(),
                        new Intent(v.getContext(), SettingsActivity.class),
                        ActivityOptions.makeCustomAnimation(v.getContext(), R.anim.slide_left_in, R.anim.slide_right_out).toBundle()));


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
        final Uri githubUri = Uri.parse("https://github.com/TylerWilliamson/QuickWeather/releases/latest");
        final Uri googlePlayUri = Uri.parse("https://play.google.com/web/store/apps/details?id=com.ominous.quickweather");
        final Uri fdroidUri = Uri.parse("https://f-droid.org/en/packages/com.ominous.quickweather/");

        final CustomTabs customTabs = CustomTabs.getInstance(snackbar.getContext(),
                githubUri,
                googlePlayUri,
                fdroidUri);

        final Resources resources = snackbar.getContext().getResources();

        updateSnackbar(R.string.text_new_version_available,
                Snackbar.LENGTH_INDEFINITE,
                R.string.text_open,
                v -> new TextDialog(v.getContext())
                        .setContent(latestRelease.body)
                        .setTitle(latestRelease.tag_name)
                        .setButton(DialogInterface.BUTTON_POSITIVE, resources.getString(R.string.text_github), () -> customTabs.launch(v.getContext(), githubUri))
                        .setButton(DialogInterface.BUTTON_NEUTRAL, resources.getString(R.string.text_googleplay), () -> customTabs.launch(v.getContext(), googlePlayUri))
                        .setButton(DialogInterface.BUTTON_NEGATIVE, resources.getString(R.string.text_fdroid), () -> customTabs.launch(v.getContext(), fdroidUri))
                        .show());
    }

    public void notifyError(String error, Throwable t) {
        logError(error, t);

        updateSnackbar(error, Snackbar.LENGTH_SHORT, 0, null);
    }

    public void dismiss() {
        snackbar.dismiss();
    }
}
