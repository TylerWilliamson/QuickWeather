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

package com.ominous.quickweather.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.ominous.quickweather.activity.MainActivity;
import com.ominous.quickweather.data.WeatherDatabase;
import com.ominous.quickweather.weather.WeatherResponse;
import com.ominous.quickweather.work.WeatherWorkManager;

import java.util.Calendar;

public class WeatherReceiver extends BroadcastReceiver {
    public static final String EXTRA_ALERT = "EXTRA_ALERT",
            ACTION_OPENALERT = "com.ominous.quickweather.ACTION_OPENALERT",
            ACTION_DISMISSALERT = "com.ominous.quickweather.ACTION_DISMISSALERT";

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle;
        WeatherResponse.Alert alert;

        if ((bundle = intent.getExtras()) != null && (alert = (WeatherResponse.Alert) bundle.getSerializable(EXTRA_ALERT)) != null) {
            if (intent.getAction() != null) {
                switch (intent.getAction()) {
                    case ACTION_OPENALERT:
                        context.startActivity(
                                new Intent(context, MainActivity.class)
                                        .putExtra(MainActivity.EXTRA_ALERT, alert)
                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

                    case ACTION_DISMISSALERT:
                        WeatherDatabase.WeatherNotificationDao notifcationDao = WeatherDatabase.getInstance(context).notificationDao();

                        notifcationDao
                                .insert(new WeatherDatabase.WeatherNotification(
                                        alert.getId(),
                                        alert.uri,
                                        alert.expires
                                ));

                        notifcationDao
                                .deleteExpired(Calendar.getInstance().getTimeInMillis());

                        break;
                }
            }
        }

        if (intent.getAction() != null && (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) || intent.getAction().equals(Intent.ACTION_PACKAGE_REPLACED))) {
            WeatherWorkManager.enqueueNotificationWorker(false);
        }
    }
}
