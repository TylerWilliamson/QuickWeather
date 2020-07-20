package com.ominous.quickweather.work;

import android.content.Context;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.ominous.quickweather.util.WeatherPreferences;

import java.util.concurrent.TimeUnit;

public class WeatherWorkManager {
    private static final String TAG = "alertNotificationWork";

    private static WorkManager workManager;

    public static void initialize(Context context) {
        workManager = WorkManager.getInstance(context);
    }

    private static void cancelNotificationWorker() {
        workManager.cancelAllWorkByTag(TAG);
    }

    public static void enqueueNotificationWorker(boolean delayed) {
        cancelNotificationWorker();

        if (WeatherPreferences.getShowAlertNotification().equals(WeatherPreferences.ENABLED) ||
                WeatherPreferences.getShowPersistentNotification().equals(WeatherPreferences.ENABLED)) {

            PeriodicWorkRequest.Builder notifRequestBuilder = new PeriodicWorkRequest
                    .Builder(WeatherWorker.class, 15, TimeUnit.MINUTES)
                    .addTag(TAG);

            if (delayed) {
                notifRequestBuilder
                        .setInitialDelay(15, TimeUnit.MINUTES);
            }

            workManager.enqueueUniquePeriodicWork(
                    TAG,
                    ExistingPeriodicWorkPolicy.REPLACE,
                    notifRequestBuilder.build());
        }
    }
}
