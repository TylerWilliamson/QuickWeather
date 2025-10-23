/*
 *   Copyright 2019 - 2025 Tyler Williamson
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

//TODO airQuality
public class Gadgetbridge {
    private final static String ACTION_GENERIC_WEATHER = "nodomain.freeyourgadget.gadgetbridge.ACTION_GENERIC_WEATHER";
    private final static String EXTRA_WEATHER_JSON = "WeatherJson";
    private final static String EXTRA_WEATHER_GZ = "WeatherGz";

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

            weatherJson.put("timestamp", currentWeather.timestamp / 1000L); //seconds
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
            weatherJson.put("precipProbability", currentWeather.daily[0].pop);
            weatherJson.put("dewPoint", Math.round(weatherUtils.getTemperature(TemperatureUnit.KELVIN, currentWeather.current.dewPoint)));
            weatherJson.put("pressure", currentWeather.current.pressure);
            weatherJson.put("visibility", currentWeather.current.visibility);
            weatherJson.put("sunRise", currentWeather.daily[0].sunrise / 1000L);
            weatherJson.put("sunSet", currentWeather.daily[0].sunset / 1000L);
            weatherJson.put("moonRise", currentWeather.daily[0].moonrise / 1000L);
            weatherJson.put("moonSet", currentWeather.daily[0].moonset / 1000L);
            weatherJson.put("moonPhase", Math.round(currentWeather.daily[0].moonPhase * 360));// 0-360, "new moon" at 0
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
                dailyJsonData.put("precipProbability", currentWeather.daily[i].pop);
                dailyJsonData.put("windSpeed", weatherUtils.getSpeed(SpeedUnit.KMH, currentWeather.daily[i].windSpeed));
                dailyJsonData.put("windDirection", currentWeather.daily[i].windDeg);
                dailyJsonData.put("sunRise", currentWeather.daily[i].sunrise / 1000L);
                dailyJsonData.put("sunSet", currentWeather.daily[i].sunset / 1000L);
                dailyJsonData.put("moonRise", currentWeather.daily[i].moonrise / 1000L);
                dailyJsonData.put("moonSet", currentWeather.daily[i].moonset / 1000L);
                dailyJsonData.put("moonPhase", Math.round(currentWeather.daily[i].moonPhase * 360));// 0-360, "new moon" at 0

                weatherForecasts.put(dailyJsonData);
            }

            weatherJson.put("forecasts", weatherForecasts);

            JSONArray hourlyForecasts = new JSONArray();

            for (int i = 1; i < currentWeather.hourly.length; i++) {
                JSONObject hourlyJsonData = new JSONObject();

                hourlyJsonData.put("timestamp", currentWeather.hourly[i].dt / 1000L); //seconds
                hourlyJsonData.put("temp", Math.round(weatherUtils.getTemperature(TemperatureUnit.KELVIN, currentWeather.hourly[i].temp)));
                hourlyJsonData.put("conditionCode", currentWeather.hourly[i].weatherCode);
                hourlyJsonData.put("humidity", currentWeather.hourly[i].humidity);
                hourlyJsonData.put("windSpeed", weatherUtils.getSpeed(SpeedUnit.KMH, currentWeather.hourly[i].windSpeed));
                hourlyJsonData.put("windDirection", currentWeather.hourly[i].windDeg);
                hourlyJsonData.put("uvIndex", currentWeather.hourly[i].uvi);
                hourlyJsonData.put("precipProbability", currentWeather.hourly[i].pop);

                hourlyForecasts.put(hourlyJsonData);
            }

            weatherJson.put("hourly", hourlyForecasts);

            context.sendBroadcast(
                    new Intent(ACTION_GENERIC_WEATHER)
                            .putExtra(EXTRA_WEATHER_JSON, weatherJson.toString())
                            .putExtra(EXTRA_WEATHER_GZ, encodeWeatherGz(weatherJson))
                            .setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES));
        } catch (JSONException | IOException e) {
            //
        }
    }

    public static byte[] encodeWeatherGz(JSONObject weatherJson) throws IOException {
        JSONArray weathers = new JSONArray();
        weathers.put(weatherJson);
        String weathersText = weathers.toString();

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
            try (OutputStreamWriter writer = new OutputStreamWriter(gzip, StandardCharsets.UTF_8)) {
                writer.append(weathersText);
            }
        }
        return bytes.toByteArray();
    }
}
