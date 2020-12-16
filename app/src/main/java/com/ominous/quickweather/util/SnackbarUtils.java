package com.ominous.quickweather.util;

import android.app.Activity;
import android.content.Intent;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.ominous.quickweather.R;
import com.ominous.quickweather.activity.SettingsActivity;
import com.ominous.quickweather.weather.WeatherLocationManager;
import com.ominous.tylerutils.util.ViewUtils;

public class SnackbarUtils {
    private static Snackbar makeSnackbar(View view, int textRes) {
        Snackbar snackbar = ViewUtils.makeSnackbar(view, textRes, Snackbar.LENGTH_INDEFINITE)
                .setTextColor(ContextCompat.getColor(view.getContext(),R.color.color_white_emphasis))
                .setActionTextColor(ContextCompat.getColor(view.getContext(), R.color.color_accent_emphasis));

        TextView messageView = snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text);
        messageView.setTextSize(TypedValue.COMPLEX_UNIT_PX, view.getContext().getResources().getDimension(R.dimen.text_size_regular));
        messageView.setPadding(0,0,0,0);
        messageView.setLineSpacing(view.getContext().getResources().getDimension(R.dimen.margin_quarter),1f);

        TextView buttonView = snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_action);
        buttonView.setTextSize(TypedValue.COMPLEX_UNIT_PX, view.getContext().getResources().getDimension(R.dimen.text_size_regular));

        return snackbar;
    }

    public static Snackbar notifyObtainingLocation(View view) {
        Snackbar snackbar = makeSnackbar(view, R.string.text_obtaining_location);

        snackbar.show();

        return snackbar;
    }

    public static Snackbar notifyLocationDisabled(View view) {
        Snackbar snackbar = makeSnackbar(view, R.string.text_enable_gps)
                .setAction(R.string.text_settings, v -> v.getContext().startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)));

        snackbar.show();

        return snackbar;
    }

    public static Snackbar notifyLocPermDenied(View view, ActivityResultLauncher<String[]> requestPermissionLauncher) {
        Snackbar snackbar = makeSnackbar(view, R.string.text_no_location_permission)
                .setAction(R.string.text_settings, v -> WeatherLocationManager.requestLocationPermissions(v.getContext(),requestPermissionLauncher));
        snackbar.show();

        return snackbar;
    }

    public static Snackbar notifyBackLocPermDenied(View view, ActivityResultLauncher<String[]> requestPermissionLauncher) {
        Snackbar snackbar = makeSnackbar(view, R.string.text_no_background_location)
                .setAction(R.string.text_settings, v -> WeatherLocationManager.requestBackgroundLocation(v.getContext(),requestPermissionLauncher));
        snackbar.show();

        return snackbar;
    }

    public static Snackbar notifySwitchToOWM(View view, Activity activity) {
        Snackbar snackbar = makeSnackbar(view, R.string.text_transition_snackbar)
                .setAction("Switch", v -> {
                    ContextCompat.startActivity(v.getContext(),new Intent(v.getContext(), SettingsActivity.class).putExtra(SettingsActivity.EXTRA_SKIP_WELCOME, true),null);
                    activity.overridePendingTransition(R.anim.slide_left_in, R.anim.slide_right_out);
                });

        snackbar.show();

        return snackbar;
    }
}
