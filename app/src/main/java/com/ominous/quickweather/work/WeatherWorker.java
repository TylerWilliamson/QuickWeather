/*
 *     Copyright 2019 - 2022 Tyler Williamson
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

import com.ominous.quickweather.R;
import com.ominous.quickweather.data.WeatherLogic;
import com.ominous.quickweather.data.WeatherResponseOneCall;
import com.ominous.quickweather.location.WeatherLocationManager;
import com.ominous.quickweather.util.NotificationUtils;
import com.ominous.quickweather.util.WeatherPreferences;
import com.ominous.tylerutils.http.HttpException;

import org.json.JSONException;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class WeatherWorker extends Worker {
    public static final String KEY_ERROR_MESSAGE = "key_error_message", KEY_STACK_TRACE = "key_stack_trace";

    public WeatherWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    private static String getStackTrace(StackTraceElement[] stackTraceElements) {
        StringBuilder stackTrace = new StringBuilder();

        for (StackTraceElement ste : stackTraceElements) {
            stackTrace.append(ste.toString()).append('\n');
        }

        return stackTrace.toString();
    }

    @NonNull
    @Override
    public Result doWork() {
        WeatherWorkManager.enqueueNotificationWorker(true);

        String errorMessage, stackTrace;
        boolean shouldRetry;

        try {
            WeatherLogic.WeatherDataContainer weatherDataContainer = WeatherLogic.getCurrentWeather(getApplicationContext(), true, true);

            if (weatherDataContainer.location == null) {
                return Result.failure(new Data.Builder().putString(KEY_ERROR_MESSAGE, getApplicationContext().getString(R.string.error_current_location)).build());
            } else if (weatherDataContainer.weatherResponseOneCall == null) {
                return Result.failure(new Data.Builder().putString(KEY_ERROR_MESSAGE, getApplicationContext().getString(R.string.error_null_response)).build());
            }

            if (weatherDataContainer.weatherResponseOneCall.alerts != null && WeatherPreferences.getShowAlertNotification().equals(WeatherPreferences.ENABLED)) {
                for (WeatherResponseOneCall.Alert alert : weatherDataContainer.weatherResponseOneCall.alerts) {
                    NotificationUtils.makeAlert(getApplicationContext(), alert);
                }
            }

            if (WeatherPreferences.getShowPersistentNotification().equals(WeatherPreferences.ENABLED)) {
                NotificationUtils.updatePersistentNotification(getApplicationContext(), weatherDataContainer.weatherLocation, weatherDataContainer.weatherResponseOneCall);
            }

            //TODO Worker Success data?
            return Result.success(Data.EMPTY);
        } catch (JSONException e) {
            errorMessage = getApplicationContext().getString(R.string.error_unexpected_api_result);
            stackTrace = getStackTrace(e.getStackTrace());
            shouldRetry = true;
        } catch (IllegalAccessException | InstantiationException e) {
            errorMessage = getApplicationContext().getString(R.string.error_creating_result);
            stackTrace = getStackTrace(e.getStackTrace());
            shouldRetry = false;
        } catch (WeatherLocationManager.LocationDisabledException e) {
            errorMessage = getApplicationContext().getString(R.string.error_gps_disabled);
            stackTrace = getStackTrace(e.getStackTrace());
            shouldRetry = false;
        } catch (WeatherLocationManager.LocationPermissionNotAvailableException e) {
            errorMessage = getApplicationContext().getString(R.string.snackbar_background_location);
            stackTrace = getStackTrace(e.getStackTrace());
            shouldRetry = false;
        } catch (HttpException e) {
            errorMessage = e.getMessage();
            stackTrace = getStackTrace(e.getStackTrace());
            shouldRetry = true;
            //TODO handle different HTTP error codes
        } catch (IOException e) {
            errorMessage = getApplicationContext().getString(R.string.error_connecting_api);
            stackTrace = getStackTrace(e.getStackTrace());
            shouldRetry = true;
        }

        if (shouldRetry && getRunAttemptCount() < 3) {
            return Result.retry();
        } else {
            NotificationUtils.makeError(
                    getApplicationContext(),
                    getApplicationContext().getString(R.string.error_obtaining_weather),
                    errorMessage);

            return Result.failure(new Data.Builder().putString(KEY_ERROR_MESSAGE, errorMessage).putString(KEY_STACK_TRACE, stackTrace).build());
        }
    }
}
