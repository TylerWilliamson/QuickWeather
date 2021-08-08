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

import android.app.Activity;
import android.util.Log;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;
import com.ominous.quickweather.R;
import com.ominous.tylerutils.util.ViewUtils;

import androidx.core.content.ContextCompat;

public class Logger {

    public static void e(Activity activity, String tag, String message, Throwable throwable) {
        View coordinatorLayout = activity.findViewById(R.id.coordinator_layout);

        Logger.e(coordinatorLayout == null ? activity.findViewById(android.R.id.content) : coordinatorLayout, tag, message, throwable);
    }

    public static void e(View view, String tag, String message, Throwable throwable) {
        Log.e(tag, message);

        if (throwable != null) {
            throwable.printStackTrace();
        }

        Snackbar snackbar = ViewUtils.makeSnackbar(view, view.getContext().getString(R.string.error_generic, message), Snackbar.LENGTH_LONG);
        snackbar.setTextColor(ContextCompat.getColor(view.getContext(), R.color.color_white));
        snackbar.show();
    }
}
