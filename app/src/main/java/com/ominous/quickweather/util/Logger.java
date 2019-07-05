package com.ominous.quickweather.util;

import android.app.Activity;
import android.util.Log;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;
import com.ominous.quickweather.R;

public class Logger {

    public static void e(Activity activity, String tag, String message, Throwable throwable) {
        View coordinatorLayout = activity.findViewById(R.id.coordinator_layout);

        Logger.e(coordinatorLayout == null ? activity.findViewById(android.R.id.content) : coordinatorLayout, tag, message, throwable);
    }

    private static void e(View view, String tag, String message, Throwable throwable) {
        Log.e(tag, message);
        throwable.printStackTrace();
        Snackbar.make(view, view.getContext().getString(R.string.error_generic, message), Snackbar.LENGTH_LONG).show();
    }
}
