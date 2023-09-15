/*
 *   Copyright 2019 - 2023 Tyler Williamson
 *
 *   This file is part of QuickWeather.
 *
 *   QuickWeather is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   QuickWeather is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with QuickWeather.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.ominous.quickweather.api;

import android.content.Context;
import android.content.Intent;

import com.ominous.quickweather.data.WeatherDatabase;
import com.ominous.quickweather.data.CurrentWeather;
import com.ominous.quickweather.pref.SpeedUnit;
import com.ominous.quickweather.pref.TemperatureUnit;
import com.ominous.quickweather.util.WeatherUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

public class Gadgetbridge {
    private final static String ACTION_GENERIC_WEATHER = "nodomain.freeyourgadget.gadgetbridge.ACTION_GENERIC_WEATHER";
    private final static String EXTRA_WEATHER_JSON = "WeatherJson";

    private static Gadgetbridge instance;

    private Gadgetbridge() {
    }

    public static Gadgetbridge getInstance() {
        if (instance == null) {
            instance = new Gadgetbridge();
        }

        return instance;
    }

    public void broadcastWeather(Context context,
                                 WeatherDatabase.WeatherLocation weatherLocation,
                                 CurrentWeather currentWeather) {
        try {
            WeatherUtils weatherUtils = WeatherUtils.getInstance(context);

            JSONObject weatherJson = new JSONObject();

            weatherJson.put("timestamp", (int) (Calendar.getInstance().getTimeInMillis() / 1000));
            weatherJson.put("location", weatherLocation.name);
            weatherJson.put("currentTemp", Math.round(weatherUtils.getTemperature(TemperatureUnit.KELVIN, currentWeather.current.temp)));
            weatherJson.put("todayMinTemp", Math.round(weatherUtils.getTemperature(TemperatureUnit.KELVIN, currentWeather.daily[0].minTemp)));
            weatherJson.put("todayMaxTemp", Math.round(weatherUtils.getTemperature(TemperatureUnit.KELVIN, currentWeather.daily[0].maxTemp)));
            weatherJson.put("currentCondition", currentWeather.current.weatherLongDescription);
            weatherJson.put("currentConditionCode", currentWeather.current.weatherCode);
            weatherJson.put("currentHumidity", currentWeather.current.humidity);
            weatherJson.put("windSpeed", weatherUtils.getSpeed(SpeedUnit.KMH, currentWeather.current.windSpeed));
            weatherJson.put("windDirection", currentWeather.current.windDeg);
            weatherJson.put("uvIndex", currentWeather.current.uvi);
            weatherJson.put("precipProbability", Math.round(currentWeather.daily[0].pop * 100));
            weatherJson.put("dewPoint", Math.round(weatherUtils.getTemperature(TemperatureUnit.KELVIN, currentWeather.current.dewPoint)));
            weatherJson.put("pressure", currentWeather.current.pressure);
            weatherJson.put("visibility", currentWeather.current.visibility);
            weatherJson.put("latitude", (float) weatherLocation.latitude);
            weatherJson.put("longitude", (float) weatherLocation.longitude);
            weatherJson.put("feelsLikeTemp", Math.round(weatherUtils.getTemperature(TemperatureUnit.KELVIN, currentWeather.current.feelsLike)));
            weatherJson.put("isCurrentLocation", weatherLocation.isCurrentLocation ? 1 : 0);

            JSONArray weatherForecasts = new JSONArray();

            for (int i = 1; i < currentWeather.daily.length; i++) {
                JSONObject dailyJsonData = new JSONObject();

                dailyJsonData.put("conditionCode", currentWeather.daily[i].weatherCode);
                dailyJsonData.put("humidity", currentWeather.daily[i].humidity);
                dailyJsonData.put("maxTemp", Math.round(weatherUtils.getTemperature(TemperatureUnit.KELVIN, currentWeather.daily[i].maxTemp)));
                dailyJsonData.put("minTemp", Math.round(weatherUtils.getTemperature(TemperatureUnit.KELVIN, currentWeather.daily[i].minTemp)));
                dailyJsonData.put("uvIndex", currentWeather.daily[i].uvi);
                dailyJsonData.put("precipProbability", Math.round(currentWeather.daily[i].pop * 100));
                dailyJsonData.put("windSpeed", weatherUtils.getSpeed(SpeedUnit.KMH, currentWeather.daily[i].windSpeed));
                dailyJsonData.put("windDirection", Math.round(currentWeather.daily[i].windDeg));

                weatherForecasts.put(dailyJsonData);
            }

            weatherJson.put("forecasts", weatherForecasts);

            JSONArray hourlyForecasts = new JSONArray();

            for (int i = 1; i < currentWeather.hourly.length; i++) {
                JSONObject hourlyJsonData = new JSONObject();

                hourlyJsonData.put("timestamp", currentWeather.hourly[i].dt);
                hourlyJsonData.put("temp", Math.round(weatherUtils.getTemperature(TemperatureUnit.KELVIN, currentWeather.hourly[i].temp)));
                hourlyJsonData.put("conditionCode", currentWeather.hourly[i].weatherCode);
                hourlyJsonData.put("humidity", currentWeather.hourly[i].humidity);
                hourlyJsonData.put("windSpeed", weatherUtils.getSpeed(SpeedUnit.KMH, currentWeather.hourly[i].windSpeed));
                hourlyJsonData.put("windDirection", Math.round(currentWeather.hourly[i].windDeg));
                hourlyJsonData.put("uvIndex", currentWeather.hourly[i].uvi);
                hourlyJsonData.put("precipProbability", Math.round(currentWeather.hourly[i].pop * 100));

                hourlyForecasts.put(hourlyJsonData);
            }

            weatherJson.put("hourly", hourlyForecasts);

            context.sendBroadcast(
                    new Intent(ACTION_GENERIC_WEATHER)
                            .putExtra(EXTRA_WEATHER_JSON, weatherJson.toString())
                            .setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES));
        } catch (JSONException e) {
            //
        }
    }
}
