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
import android.util.Pair;

import com.ominous.quickweather.api.OpenWeatherMap;
import com.ominous.quickweather.location.WeatherLocationManager;
import com.ominous.quickweather.util.WeatherPreferences;
import com.ominous.tylerutils.http.HttpException;

import org.json.JSONException;

import java.io.IOException;

//TODO proper name and location
public class WeatherLogic {
    public static WeatherModel getCurrentWeather(Context context, boolean isBackground, boolean obtainLocation) throws
            JSONException, HttpException, IOException, InstantiationException, IllegalAccessException,
            WeatherLocationManager.LocationPermissionNotAvailableException, WeatherLocationManager.LocationDisabledException {
        return getWeather(context, isBackground, obtainLocation, false);
    }

    public static WeatherModel getForecastWeather(Context context, boolean obtainLocation) throws
            JSONException, HttpException, IOException, InstantiationException, IllegalAccessException,
            WeatherLocationManager.LocationPermissionNotAvailableException, WeatherLocationManager.LocationDisabledException {
        return getWeather(context, false, obtainLocation, true);
    }

    private static WeatherModel getWeather(Context context, boolean isBackground, boolean obtainLocation, boolean obtainForecast) throws
            JSONException, HttpException, IOException, InstantiationException, IllegalAccessException,
            WeatherLocationManager.LocationPermissionNotAvailableException, WeatherLocationManager.LocationDisabledException {
        WeatherModel weatherModel = new WeatherModel();

        weatherModel.weatherLocation = WeatherDatabase.getInstance(context).locationDao().getSelected();
        weatherModel.location = WeatherLocationManager.getLocation(context, isBackground);

        if (weatherModel.location == null && obtainLocation) {
            weatherModel.location = WeatherLocationManager.getCurrentLocation(context, false);
        }

        if (weatherModel.location != null) {
            String apiKey = WeatherPreferences.getApiKey();
            String apiVersionString = WeatherPreferences.getAPIVersion();

            OpenWeatherMap.APIVersion apiVersion = apiVersionString.equals(WeatherPreferences.ONECALL_3_0) ? OpenWeatherMap.APIVersion.ONECALL_3_0 : OpenWeatherMap.APIVersion.ONECALL_2_5;

            if (apiVersionString.equals(WeatherPreferences.DEFAULT_VALUE)) {
                WeatherPreferences.setAPIVersion(WeatherPreferences.ONECALL_2_5);
            }

            Pair<Double, Double> locationPair = new Pair<>(weatherModel.location.getLatitude(), weatherModel.location.getLongitude());

            weatherModel.responseOneCall = OpenWeatherMap.getWeatherOneCall(apiVersion, apiKey, locationPair);

            if (obtainForecast) {
                weatherModel.responseForecast = OpenWeatherMap.getWeatherForecast(apiKey, locationPair);
            }
        }

        return weatherModel;
    }
}
