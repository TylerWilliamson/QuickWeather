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

package com.ominous.quickweather.data;

import android.content.Context;
import android.location.Location;
import android.util.Pair;

import com.ominous.quickweather.api.OpenWeatherMap;
import com.ominous.quickweather.location.WeatherLocationManager;
import com.ominous.quickweather.util.WeatherPreferences;
import com.ominous.tylerutils.http.HttpException;

import org.json.JSONException;

import java.io.IOException;

//TODO proper name and location
public class WeatherLogic {
    public static WeatherDataContainer getCurrentWeather(Context context, boolean isBackground, boolean obtainLocation) throws
            JSONException, HttpException, IOException, InstantiationException, IllegalAccessException,
            WeatherLocationManager.LocationPermissionNotAvailableException, WeatherLocationManager.LocationDisabledException {
        return getWeather(context, isBackground, obtainLocation, false);
    }

    public static WeatherDataContainer getForecastWeather(Context context, boolean obtainLocation) throws
            JSONException, HttpException, IOException, InstantiationException, IllegalAccessException,
            WeatherLocationManager.LocationPermissionNotAvailableException, WeatherLocationManager.LocationDisabledException {
        return getWeather(context, false, obtainLocation, true);
    }

    private static WeatherDataContainer getWeather(Context context, boolean isBackground, boolean obtainLocation, boolean obtainForecast) throws
            JSONException, HttpException, IOException, InstantiationException, IllegalAccessException,
            WeatherLocationManager.LocationPermissionNotAvailableException, WeatherLocationManager.LocationDisabledException {
        WeatherDataContainer weatherDataContainer = new WeatherDataContainer();

        weatherDataContainer.weatherLocation = WeatherDatabase.getInstance(context).locationDao().getSelected();
        weatherDataContainer.location = WeatherLocationManager.getLocation(context, isBackground);

        if (weatherDataContainer.location == null && obtainLocation) {
            weatherDataContainer.location = WeatherLocationManager.getCurrentLocation(context, false);
        }

        if (weatherDataContainer.location != null) {
            String apiKey = WeatherPreferences.getApiKey();
            Pair<Double, Double> locationPair = new Pair<>(weatherDataContainer.location.getLatitude(), weatherDataContainer.location.getLongitude());

            weatherDataContainer.weatherResponseOneCall = OpenWeatherMap.getWeatherOneCall(apiKey, locationPair);

            if (obtainForecast) {
                weatherDataContainer.weatherResponseForecast = OpenWeatherMap.getWeatherForecast(apiKey, locationPair);
            }
        }

        return weatherDataContainer;
    }

    public static class WeatherDataContainer {
        public WeatherDatabase.WeatherLocation weatherLocation;
        public Location location;
        public WeatherResponseOneCall weatherResponseOneCall;
        public WeatherResponseForecast weatherResponseForecast;
    }
}
