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

package com.ominous.quickweather.work;

import android.content.Context;
import android.location.Location;
import android.util.Pair;

import com.ominous.quickweather.R;
import com.ominous.quickweather.util.NotificationUtils;
import com.ominous.quickweather.util.WeatherPreferences;
import com.ominous.quickweather.weather.WeatherLocationManager;
import com.ominous.tylerutils.work.BaseWorker;
import com.ominous.tylerutils.work.GenericWorker;

import org.json.JSONException;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkerParameters;

public class WeatherWorker extends BaseWorker<GenericWeatherWorker> {
    GenericWeatherWorker worker;

    public WeatherWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        this.worker = getWorker(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        WeatherWorkManager.enqueueNotificationWorker(true);

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

                if (location == null) {
                    //TODO: worker can be null due to race conditions
                    WeatherLocationManager.getCurrentLocation(context,
                            l -> worker = new GenericWeatherWorker(context,
                                    WeatherPreferences.getProvider(),
                                    WeatherPreferences.getApiKey(),
                                    new Pair<>(
                                            l.getLatitude(),
                                            l.getLongitude()),
                                    true));
                } else {
                    worker = new GenericWeatherWorker(context,
                            WeatherPreferences.getProvider(),
                            WeatherPreferences.getApiKey(),
                            new Pair<>(
                                    location.getLatitude(),
                                    location.getLongitude()),
                            true);
                }
            } catch (WeatherLocationManager.LocationDisabledException e) {
                e.printStackTrace();

                errorMessage = context.getString(R.string.error_gps_disabled);
            } catch (WeatherLocationManager.LocationPermissionNotAvailableException e) {
                e.printStackTrace();

                errorMessage = context.getString(R.string.snackbar_background_location);
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
