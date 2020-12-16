package com.ominous.quickweather.work;

import android.content.Context;
import android.location.Location;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkerParameters;

import com.ominous.quickweather.R;
import com.ominous.quickweather.util.NotificationUtils;
import com.ominous.quickweather.util.WeatherPreferences;
import com.ominous.quickweather.weather.WeatherLocationManager;
import com.ominous.tylerutils.work.BaseWorker;
import com.ominous.tylerutils.work.GenericWorker;

import org.json.JSONException;

import java.io.IOException;

public class WeatherWorker extends BaseWorker<GenericWeatherWorker> {
    GenericWeatherWorker worker;

    public WeatherWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        this.worker = getWorker(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            if (worker == null) {
                return Result.failure(new Data.Builder().putString(KEY_ERROR_MESSAGE, "GenericWorker is null").build());
            } else {
                return Result.success(worker.doWork(new GenericWorker.WorkerInterface() {
                    @Override
                    public boolean isCancelled() {
                        return WeatherWorker.this.isStopped();
                    }

                    @Override
                    public void onProgress(int progress, int max) {
                        //Cannot post progress from a Worker
                    }
                }).getData());
            }
        } catch (Throwable t) {
            StringBuilder stackTrace = new StringBuilder();

            for (StackTraceElement ste : t.getStackTrace()) {
                stackTrace.append(ste.toString()).append('\n');
            }

            String errorMessage;

            if (t instanceof InstantiationException || t instanceof IllegalAccessException) {
                errorMessage = getApplicationContext().getString(R.string.error_creating_result);
            } else if (t instanceof JSONException) {
                errorMessage = getApplicationContext().getString(R.string.error_unexpected_api_result);
            } else if (t instanceof IOException) {
                errorMessage = getApplicationContext().getString(R.string.error_connecting_api);
            } else { //Includes HttpException
                errorMessage = t.getMessage();
            }

            NotificationUtils.makeError(
                    getApplicationContext(),
                    getApplicationContext().getString(R.string.error_obtaining_weather),
                    errorMessage);

            return Result.failure(new Data.Builder().putString(KEY_ERROR_MESSAGE, t.getMessage()).putString(KEY_STACK_TRACE, stackTrace.toString()).build());
        }
    }

    @Override
    public GenericWeatherWorker getWorker(Context context) {
        if (worker == null) {
            String errorMessage = null;

            try {
                Location location = WeatherLocationManager.getLocation(context, true);

                worker = new GenericWeatherWorker(context,
                        WeatherPreferences.getProvider(),
                        WeatherPreferences.getApiKey(),
                        new Pair<>(
                                location.getLatitude(),
                                location.getLongitude()),
                        true);
            } catch (WeatherLocationManager.LocationDisabledException e) {
                e.printStackTrace();

                errorMessage = context.getString(R.string.text_enable_gps);
            } catch (WeatherLocationManager.LocationPermissionNotAvailableException e) {
                e.printStackTrace();

                errorMessage = context.getString(R.string.text_no_background_location);
            } catch (WeatherLocationManager.LocationNotAvailableException e) {
                try {
                    WeatherLocationManager.getCurrentLocation(context,
                            location -> worker = new GenericWeatherWorker(context,
                                    WeatherPreferences.getProvider(),
                                    WeatherPreferences.getApiKey(),
                                    new Pair<>(
                                            location.getLatitude(),
                                            location.getLongitude()),
                                    true));
                } catch (WeatherLocationManager.LocationDisabledException | WeatherLocationManager.LocationPermissionNotAvailableException ex) {
                    //Should have been caught previously

                    e.printStackTrace();

                    errorMessage = context.getString(R.string.error_location_unavailable);
                }
            }

            if (errorMessage != null) {
                NotificationUtils.makeError(
                        context,
                        context.getString(R.string.error_current_location),
                        errorMessage);
            }
        }

        return worker;
    }
}
