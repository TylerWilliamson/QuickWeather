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

package com.ominous.quickweather.work;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.core.content.ContextCompat;
import androidx.work.BackoffPolicy;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.ominous.quickweather.pref.WeatherPreferences;
import com.ominous.quickweather.util.DialogHelper;

import java.util.concurrent.TimeUnit;

public class WeatherWorkManager {
    private final static String TAG = "alertNotificationWork";

    private final Context context;

    public WeatherWorkManager(Context context) {
        this.context = context;
    }

    public void enqueueNotificationWorker(boolean delayed) {
        WorkManager workManager = WorkManager.getInstance(context);
        WeatherPreferences weatherPreferences = WeatherPreferences.getInstance(context);

        if (weatherPreferences.shouldRunBackgroundJob()) {
            PeriodicWorkRequest.Builder notifRequestBuilder = new PeriodicWorkRequest
                    .Builder(WeatherWorker.class, 2, TimeUnit.HOURS)
                    .setBackoffCriteria(
                            BackoffPolicy.LINEAR,
                            3,
                            TimeUnit.MINUTES)
                    .addTag(TAG);

            if (delayed) {
                notifRequestBuilder
                        .setInitialDelay(2, TimeUnit.HOURS);
            }

            workManager.enqueueUniquePeriodicWork(
                    TAG,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    notifRequestBuilder.build());
        } else {
            workManager.cancelAllWorkByTag(TAG);
        }
    }

    public boolean isNotIgnoringBatteryOptimizations() {
        PowerManager powerManager = ContextCompat.getSystemService(context, PowerManager.class);

        return Build.VERSION.SDK_INT >= 23 && (powerManager == null ||
                !powerManager.isIgnoringBatteryOptimizations(context.getPackageName()));
    }

    @SuppressLint("BatteryLife")
    public void requestIgnoreBatteryOptimization(DialogHelper dialogHelper) {
        if (Build.VERSION.SDK_INT >= 23) {
            if (isNotIgnoringBatteryOptimizations()) {
                dialogHelper.showBatteryOptimizationDialog(() -> {
                    Intent intent;

                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                            == PackageManager.PERMISSION_GRANTED) {
                        intent = new Intent(
                                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                Uri.fromParts("package", context.getPackageName(), null));
                    } else {
                        intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                    }

                    try {
                        context.startActivity(intent, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }
}
