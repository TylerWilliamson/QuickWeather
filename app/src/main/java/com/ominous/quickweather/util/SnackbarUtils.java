/*
 *     Copyright 2019 - 2021 Tyler Williamson
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

import android.app.ActivityOptions;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
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

//TODO: Move caching logic here from MainActivity/ForecastActivity
public class SnackbarUtils {
    public static Snackbar makeSnackbar(View view, int textRes) {
        Snackbar snackbar = ViewUtils.makeSnackbar(view, textRes, Snackbar.LENGTH_INDEFINITE)
                .setTextColor(ContextCompat.getColor(view.getContext(), R.color.color_white_emphasis))
                .setActionTextColor(ContextCompat.getColor(view.getContext(), R.color.color_accent));

        TextView messageView = snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text);
        messageView.setTextSize(TypedValue.COMPLEX_UNIT_PX, view.getContext().getResources().getDimension(R.dimen.text_size_regular));
        messageView.setPadding(0, 0, 0, 0);
        messageView.setLineSpacing(view.getContext().getResources().getDimension(R.dimen.margin_quarter), 1f);

        TextView buttonView = snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_action);
        buttonView.setTextSize(TypedValue.COMPLEX_UNIT_PX, view.getContext().getResources().getDimension(R.dimen.text_size_regular));

        return snackbar;
    }

    public static Snackbar notifyObtainingLocation(View view) {
        Snackbar snackbar = makeSnackbar(view, R.string.snackbar_obtaining_location);

        snackbar.show();

        return snackbar;
    }

    public static Snackbar notifyLocationDisabled(View view) {
        Snackbar snackbar = makeSnackbar(view, R.string.error_gps_disabled)
                .setAction(R.string.text_settings, v -> v.getContext().startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)));

        snackbar.show();

        return snackbar;
    }

    public static Snackbar notifyLocPermDenied(View view, ActivityResultLauncher<String[]> requestPermissionLauncher) {
        Snackbar snackbar = makeSnackbar(view, R.string.snackbar_no_location_permission)
                .setAction(R.string.text_settings, v -> WeatherLocationManager.requestLocationPermissions(v.getContext(), requestPermissionLauncher));
        snackbar.show();

        return snackbar;
    }

    public static Snackbar notifyBackLocPermDenied(View view, ActivityResultLauncher<String[]> requestPermissionLauncher) {
        Snackbar snackbar = makeSnackbar(view, R.string.snackbar_background_location)
                .setAction(R.string.text_settings, v -> WeatherLocationManager.requestBackgroundLocation(v.getContext(), requestPermissionLauncher));
        snackbar.show();

        return snackbar;
    }

    public static Snackbar notifyInvalidProvider(View view) {
        Snackbar snackbar = makeSnackbar(view, R.string.snackbar_invalid_provider)
                .setAction(R.string.text_settings, v ->
                        ContextCompat.startActivity(v.getContext(),
                                new Intent(v.getContext(), SettingsActivity.class)
                                        .putExtra(SettingsActivity.EXTRA_SKIP_WELCOME, true),
                                ActivityOptions.makeCustomAnimation(v.getContext(), R.anim.slide_left_in, R.anim.slide_right_out).toBundle())
                );

        snackbar.show();

        return snackbar;
    }

    public static Snackbar notifyNewVersionError(View view) {
        Snackbar snackbar = makeSnackbar(view, R.string.text_error_new_version);

        snackbar.show();

        return snackbar;
    }

    public static Snackbar notifyReleaseError(View view) {
        Snackbar snackbar = makeSnackbar(view, R.string.text_error_getting_release);

        snackbar.show();

        return snackbar;
    }

    public static Snackbar notifyLatestRelease(View view) {
        Snackbar snackbar = makeSnackbar(view, R.string.text_no_new_version);

        snackbar.show();

        return snackbar;
    }

    public static void notifyNewVersion(View view, GithubUtils.GitHubRelease latestRelease) {
        final Uri githubUri = Uri.parse("https://github.com/TylerWilliamson/QuickWeather/releases/latest");
        final Uri googlePlayUri = Uri.parse("https://play.google.com/web/store/apps/details?id=com.ominous.quickweather");
        final Uri fdroidUri = Uri.parse("https://f-droid.org/en/packages/com.ominous.quickweather/");

        CustomTabs customTabs = CustomTabs.getInstance(view.getContext(),
                githubUri,
                googlePlayUri,
                fdroidUri);

        makeSnackbar(view, R.string.text_new_version_available)
                .setAction(R.string.text_open, v ->
                        new TextDialog(v.getContext())
                                .setContent(latestRelease.body)
                                .setTitle(latestRelease.tag_name)
                                .setButton(DialogInterface.BUTTON_POSITIVE, "GitHub", () -> customTabs.launch(v.getContext(), githubUri))
                                .setButton(DialogInterface.BUTTON_NEUTRAL, "Google Play", () -> customTabs.launch(v.getContext(), googlePlayUri))
                                .setButton(DialogInterface.BUTTON_NEGATIVE, "F-Droid", () -> customTabs.launch(v.getContext(), fdroidUri))
                                .show())
                .setDuration(Snackbar.LENGTH_INDEFINITE).show();
    }
}
