/*
 *   Copyright 2019 - 2023 Tyler Williamson
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

import android.content.Context;

import androidx.work.BackoffPolicy;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.ominous.quickweather.pref.WeatherPreferences;

import java.util.concurrent.TimeUnit;

public class WeatherWorkManager {
    private final static String TAG = "alertNotificationWork";

    public static void enqueueNotificationWorker(Context context, boolean delayed) {
        WorkManager workManager = WorkManager.getInstance(context);
        WeatherPreferences weatherPreferences = WeatherPreferences.getInstance(context);

        if (weatherPreferences.shouldRunBackgroundJob()) {
            PeriodicWorkRequest.Builder notifRequestBuilder = new PeriodicWorkRequest
                    .Builder(WeatherWorker.class, 15, TimeUnit.MINUTES)
                    .setBackoffCriteria(
                            BackoffPolicy.LINEAR,
                            3,
                            TimeUnit.MINUTES)
                    .addTag(TAG);

            if (delayed) {
                notifRequestBuilder
                        .setInitialDelay(15, TimeUnit.MINUTES);
            }

            workManager.enqueueUniquePeriodicWork(
                    TAG,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    notifRequestBuilder.build());
        } else {
            workManager.cancelAllWorkByTag(TAG);
        }
    }
}
