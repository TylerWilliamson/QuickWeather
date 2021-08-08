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

import com.ominous.quickweather.data.WeatherDatabase;
import com.ominous.quickweather.weather.WeatherResponse;
import com.ominous.quickweather.work.WeatherWorkManager;

public class WeatherReceiver extends BroadcastReceiver {
    public static final String EXTRA_ALERT = "EXTRA_ALERT",
            ACTION_DISMISSALERT = "com.ominous.quickweather.ACTION_DISMISSALERT";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action;

        if ((action = intent.getAction()) != null) {
            switch (action) {
                case ACTION_DISMISSALERT:
                    Bundle bundle;
                    WeatherResponse.Alert alert;
                    if ((bundle = intent.getExtras()) != null &&
                            (alert = (WeatherResponse.Alert) bundle.getSerializable(EXTRA_ALERT)) != null) {
                        WeatherDatabase.getInstance(context).insertAlert(alert);
                    }
                    break;
                case Intent.ACTION_BOOT_COMPLETED:
                case Intent.ACTION_PACKAGE_REPLACED:
                    WeatherWorkManager.enqueueNotificationWorker(false);
                    break;
            }
        }
    }
}
