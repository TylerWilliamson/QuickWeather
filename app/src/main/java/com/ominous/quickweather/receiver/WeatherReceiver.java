package com.ominous.quickweather.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.ominous.quickweather.activity.MainActivity;
import com.ominous.quickweather.data.WeatherDatabase;
import com.ominous.quickweather.weather.Weather;
import com.ominous.quickweather.weather.WeatherWorkManager;

import java.util.Calendar;

public class WeatherReceiver extends BroadcastReceiver {
    public static final String EXTRA_ALERT = "EXTRA_ALERT",
            ACTION_OPENALERT = "com.ominous.quickweather.ACTION_OPENALERT",
            ACTION_DISMISSALERT = "com.ominous.quickweather.ACTION_DISMISSALERT";

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle;
        Weather.WeatherResponse.Alert alert;

        if ((bundle = intent.getExtras()) != null && (alert = (Weather.WeatherResponse.Alert) bundle.getSerializable(EXTRA_ALERT)) != null) {
            if (intent.getAction() != null) {
                switch (intent.getAction()) {
                    case ACTION_OPENALERT:
                        context.startActivity(
                                new Intent(context, MainActivity.class)
                                        .putExtra(MainActivity.EXTRA_ALERTURI, alert.uri)
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
            WeatherWorkManager.enqueueNotificationWorker();
        }
    }
}
