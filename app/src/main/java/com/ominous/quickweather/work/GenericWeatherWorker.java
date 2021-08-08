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
import android.util.Pair;

import com.ominous.quickweather.util.NotificationUtils;
import com.ominous.quickweather.util.WeatherPreferences;
import com.ominous.quickweather.weather.Weather;
import com.ominous.quickweather.weather.WeatherResponse;
import com.ominous.tylerutils.work.GenericWorker;

import androidx.work.Data;

public class GenericWeatherWorker extends GenericWorker<WeatherResults> {
    private final String apiKey;
    private final Pair<Double, Double> locationKey;
    private final boolean updateAlerts;
    private final String provider;

    public GenericWeatherWorker(Context context, String provider, String apiKey, Pair<Double, Double> locationKey, boolean updateAlerts) {
        super(context);

        this.apiKey = apiKey;
        this.locationKey = locationKey;
        this.updateAlerts = updateAlerts;
        this.provider = provider;
    }

    @Override
    public WeatherResults doWork(WorkerInterface workerInterface) throws Throwable {
        WeatherResponse weatherResponse = Weather.getWeather(provider, apiKey, locationKey);

        if (weatherResponse != null) {
            if (updateAlerts && weatherResponse.alerts != null && WeatherPreferences.getShowAlertNotification().equals(WeatherPreferences.ENABLED)) {
                for (WeatherResponse.Alert alert : weatherResponse.alerts) {
                    NotificationUtils.makeAlert(getContext(), alert);
                }
            }

            if (WeatherPreferences.getShowPersistentNotification().equals(WeatherPreferences.ENABLED)) {
                NotificationUtils.updatePersistentNotification(getContext(), weatherResponse.currently);
            }
        }

        return new WeatherResults(
                Data.EMPTY,//TODO data?
                weatherResponse);
    }
}
