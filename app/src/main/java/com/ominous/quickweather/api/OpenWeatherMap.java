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

package com.ominous.quickweather.api;

import androidx.annotation.NonNull;

import com.ominous.quickweather.data.WeatherResponseForecast;
import com.ominous.quickweather.data.WeatherResponseOneCall;
import com.ominous.quickweather.util.WeatherPreferences;
import com.ominous.tylerutils.http.HttpException;
import com.ominous.tylerutils.http.HttpRequest;
import com.ominous.tylerutils.util.JsonUtils;
import com.ominous.tylerutils.work.ParallelThreadManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

public class OpenWeatherMap {
    private static final String uriFormatOneCall = "https://api.openweathermap.org/data/%5$s/onecall?appid=%1$s&lat=%2$f&lon=%3$f&lang=%4$s&units=imperial";
    private static final String uriFormatForecast = "https://api.openweathermap.org/data/2.5/forecast?appid=%1$s&lat=%2$f&lon=%3$f&lang=%4$s&units=imperial";
    private static final String uriFormatWeather = "https://api.openweathermap.org/data/2.5/weather?appid=%1$s&lat=%2$f&lon=%3$f&lang=%4$s&units=imperial";

    public static WeatherResponseOneCall getWeatherOneCall(@NonNull WeatherPreferences.ApiVersion version, String apiKey, double latitude, double longitude)
            throws IOException, JSONException, InstantiationException, IllegalAccessException, HttpException {
        if (version == WeatherPreferences.ApiVersion.WEATHER_2_5) {
            throw new OpenWeatherMapException("Invalid OpenWeatherMap API Version - OneCall is required");
        }

        return JsonUtils.deserialize(WeatherResponseOneCall.class, new JSONObject(
                new HttpRequest(
                        String.format(Locale.US, uriFormatOneCall, apiKey,
                                latitude,
                                longitude,
                                getLang(Locale.getDefault()),
                                version == WeatherPreferences.ApiVersion.ONECALL_2_5 ? "2.5" : "3.0"))
                        .addHeader("User-Agent", "QuickWeather - https://play.google.com/store/apps/details?id=com.ominous.quickweather")
                        .fetch()));
    }

    public static WeatherResponseForecast getWeatherForecast(String apiKey, double latitude, double longitude)
            throws IOException, JSONException, InstantiationException, IllegalAccessException, HttpException {
        return JsonUtils.deserialize(WeatherResponseForecast.class, new JSONObject(
                new HttpRequest(
                        String.format(Locale.US, uriFormatForecast, apiKey,
                                latitude,
                                longitude,
                                getLang(Locale.getDefault())))
                        .addHeader("User-Agent", "QuickWeather - https://play.google.com/store/apps/details?id=com.ominous.quickweather")
                        .fetch()));
    }

    public static String getLang(Locale locale) {
        String lang = locale.getLanguage();

        if (lang.equals("pt") && locale.getCountry().equals("BR")) {
            lang = "pt_br";
        } else if (locale.equals(Locale.CHINESE) || locale.equals(Locale.SIMPLIFIED_CHINESE)) {
            lang = "zh_cn";
        } else if (locale.equals(Locale.TRADITIONAL_CHINESE)) {
            lang = "zh_tw";
        }

        return lang.isEmpty() ? "en" : lang;
    }

    public static WeatherPreferences.ApiVersion determineApiVersion(String apiKey) throws OpenWeatherMapException {
        final HashMap<WeatherPreferences.ApiVersion, Boolean> results = new HashMap<>();

        try {
            ParallelThreadManager.execute(
                    () -> {
                        try {
                            new HttpRequest(
                                    String.format(Locale.US, uriFormatOneCall, apiKey,
                                            33.749,
                                            -84.388,
                                            getLang(Locale.getDefault()),
                                            "2.5"))
                                    .addHeader("User-Agent", "QuickWeather - https://play.google.com/store/apps/details?id=com.ominous.quickweather")
                                    .fetch();

                            results.put(WeatherPreferences.ApiVersion.ONECALL_2_5, true);
                        } catch (HttpException e) {
                            results.put(WeatherPreferences.ApiVersion.ONECALL_2_5, false);
                        } catch (IOException e) {
                            results.put(WeatherPreferences.ApiVersion.ONECALL_2_5, null);
                        }
                    },
                    () -> {
                        try {
                            new HttpRequest(
                                    String.format(Locale.US, uriFormatOneCall, apiKey,
                                            33.749,
                                            -84.388,
                                            getLang(Locale.getDefault()),
                                            "3.0"))
                                    .addHeader("User-Agent", "QuickWeather - https://play.google.com/store/apps/details?id=com.ominous.quickweather")
                                    .fetch();

                            results.put(WeatherPreferences.ApiVersion.ONECALL_3_0, true);
                        } catch (HttpException e) {
                            results.put(WeatherPreferences.ApiVersion.ONECALL_3_0, false);
                        } catch (IOException e) {
                            results.put(WeatherPreferences.ApiVersion.ONECALL_3_0, null);
                        }
                    },
                    () -> {
                        try {
                            new HttpRequest(
                                    String.format(Locale.US, uriFormatWeather, apiKey,
                                            33.749,
                                            -84.388,
                                            getLang(Locale.getDefault())
                                    ))
                                    .addHeader("User-Agent", "QuickWeather - https://play.google.com/store/apps/details?id=com.ominous.quickweather")
                                    .fetch();

                            results.put(WeatherPreferences.ApiVersion.WEATHER_2_5, true);
                        } catch (HttpException e) {
                            results.put(WeatherPreferences.ApiVersion.WEATHER_2_5, false);
                        } catch (IOException e) {
                            results.put(WeatherPreferences.ApiVersion.WEATHER_2_5, null);
                        }
                    }
            );
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (results.get(WeatherPreferences.ApiVersion.ONECALL_2_5) == null ||
                results.get(WeatherPreferences.ApiVersion.ONECALL_3_0) == null ||
                results.get(WeatherPreferences.ApiVersion.WEATHER_2_5) == null
        ) {
            throw new OpenWeatherMapException("Could not connect to OpenWeatherMap");
        }

        for (WeatherPreferences.ApiVersion option : WeatherPreferences.ApiVersion.values()) {
            if (Boolean.TRUE.equals(results.get(option))) {
                return option;
            }
        }

        return null;
    }

    public enum PrecipType {
        RAIN,
        MIX,
        SNOW
    }

    public enum AlertSeverity {
        ADVISORY,
        WATCH,
        WARNING
    }

    public static class OpenWeatherMapException extends RuntimeException {
        public OpenWeatherMapException(String message) {
            super(message);
        }
    }
}
