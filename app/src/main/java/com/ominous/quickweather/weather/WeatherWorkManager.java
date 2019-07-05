package com.ominous.quickweather.weather;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ListenableWorker;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkerParameters;
import androidx.work.impl.utils.futures.SettableFuture;

import com.google.common.util.concurrent.ListenableFuture;
import com.ominous.quickweather.util.NotificationUtil;
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

    public static void enqueueNotificationWorker() {
        cancelNotificationWorker();

        if (WeatherPreferences.getShowAlertNotification().equals(WeatherPreferences.ENABLED) ||
                WeatherPreferences.getShowPersistentNotification().equals(WeatherPreferences.ENABLED)) {
            workManager.enqueueUniquePeriodicWork(
                            TAG,
                            ExistingPeriodicWorkPolicy.REPLACE,
                            new PeriodicWorkRequest
                                    .Builder(NotificationWorker.class, 15, TimeUnit.MINUTES)
                                    .addTag(TAG)
                                    .build());
        }
    }


    public static class NotificationWorker extends ListenableWorker {
        private Context context;

        public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
            super(context, workerParams);

            this.context = context;
        }

        @NonNull
        @Override
        @SuppressLint("RestrictedApi") //TODO: Remove SuppressLint when RestrictTo tag removed
        public ListenableFuture<Result> startWork() {
            final SettableFuture<Result> future = SettableFuture.create();

            try {
                Location location = WeatherLocationManager.getLocation(context);

                Weather.getWeather(
                        WeatherPreferences.getApiKey(),
                        location.getLatitude(),
                        location.getLongitude(),
                        new Weather.WeatherListener() {
                            @Override
                            public void onWeatherRetrieved(Weather.WeatherResponse weatherResponse) {

                                if (weatherResponse.alerts != null && WeatherPreferences.getShowAlertNotification().equals(WeatherPreferences.ENABLED)) {
                                    for (Weather.WeatherResponse.Alert alert : weatherResponse.alerts) {
                                        NotificationUtil.makeAlert(context, alert);
                                    }
                                }

                                if (WeatherPreferences.getShowPersistentNotification().equals(WeatherPreferences.ENABLED)) {
                                    NotificationUtil.updatePersistentNotification(context, weatherResponse.currently);
                                }

                                future.set(Result.success());
                            }

                            @Override
                            public void onWeatherError(String error, Throwable throwable) {
                                future.setException(throwable);
                                future.set(Result.failure());
                            }
                        });

            } catch (WeatherLocationManager.LocationPermissionNotAvailableException e) {
                future.setException(e);
                future.set(Result.failure());
            }

            return future;
        }
    }
}
