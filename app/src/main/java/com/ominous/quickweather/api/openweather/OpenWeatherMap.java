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

package com.ominous.quickweather.api.openweather;

import android.content.Context;
import android.content.res.Resources;

import com.ominous.quickweather.R;
import com.ominous.quickweather.data.CurrentWeather;
import com.ominous.quickweather.data.ForecastWeather;
import com.ominous.quickweather.data.PrecipType;
import com.ominous.quickweather.pref.OwmApiVersion;
import com.ominous.quickweather.util.WeatherUtils;
import com.ominous.tylerutils.http.HttpException;
import com.ominous.tylerutils.http.HttpRequest;
import com.ominous.tylerutils.util.JsonUtils;
import com.ominous.tylerutils.util.StringUtils;
import com.ominous.tylerutils.work.ParallelThreadManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import androidx.annotation.NonNull;

public class OpenWeatherMap {
    private final static String uriFormatOneCall = "https://api.openweathermap.org/data/%5$s/onecall?appid=%1$s&lat=%2$f&lon=%3$f&lang=%4$s&units=imperial";
    private final static String uriFormatForecast = "https://api.openweathermap.org/data/2.5/forecast?appid=%1$s&lat=%2$f&lon=%3$f&lang=%4$s&units=imperial";
    private final static String uriFormatWeather = "https://api.openweathermap.org/data/2.5/weather?appid=%1$s&lat=%2$f&lon=%3$f&lang=%4$s&units=imperial";

    private final HashMap<String, Integer> codeToIcon = new HashMap<>();

    private static OpenWeatherMap instance;

    private OpenWeatherMap() {
        //TODO convert to switch
        codeToIcon.put("01d", R.drawable.sun);
        codeToIcon.put("01n", R.drawable.moon_25);
        codeToIcon.put("02d", R.drawable.cloud_sun);
        codeToIcon.put("02n", R.drawable.cloud_moon);
        codeToIcon.put("03d", R.drawable.cloud_sun);
        codeToIcon.put("03n", R.drawable.cloud_moon);
        codeToIcon.put("04d", R.drawable.cloud_sun);
        codeToIcon.put("04n", R.drawable.cloud_moon);
        codeToIcon.put("09d", R.drawable.cloud_drizzle_sun);
        codeToIcon.put("09n", R.drawable.cloud_drizzle_moon);
        codeToIcon.put("10d", R.drawable.cloud_rain_sun);
        codeToIcon.put("10n", R.drawable.cloud_rain_moon);
        codeToIcon.put("11d", R.drawable.cloud_rain_lightning_sun);
        codeToIcon.put("11n", R.drawable.cloud_rain_lightning_moon);
        codeToIcon.put("13d", R.drawable.cloud_snow_sun);
        codeToIcon.put("13n", R.drawable.cloud_snow_moon);
        codeToIcon.put("50d", R.drawable.cloud_fog_sun);
        codeToIcon.put("50n", R.drawable.cloud_fog_moon);

        codeToIcon.put("611d", R.drawable.cloud_hail_sun);
        codeToIcon.put("611n", R.drawable.cloud_hail_moon);
        codeToIcon.put("612d", R.drawable.cloud_hail_sun);
        codeToIcon.put("612n", R.drawable.cloud_hail_moon);
        codeToIcon.put("613d", R.drawable.cloud_hail_sun);
        codeToIcon.put("613n", R.drawable.cloud_hail_moon);
        codeToIcon.put("781d", R.drawable.tornado);
        codeToIcon.put("781n", R.drawable.tornado);
    }

    public static OpenWeatherMap getInstance() {
        if (instance == null) {
            instance = new OpenWeatherMap();
        }

        return instance;
    }

    public String getLang(Locale locale) {
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

    public OwmApiVersion determineApiVersion(String apiKey) throws OpenWeatherMapException {
        final HashMap<OwmApiVersion, Boolean> results = new HashMap<>();

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

                            results.put(OwmApiVersion.ONECALL_2_5, true);
                        } catch (HttpException e) {
                            results.put(OwmApiVersion.ONECALL_2_5, false);
                        } catch (IOException e) {
                            results.put(OwmApiVersion.ONECALL_2_5, null);
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

                            results.put(OwmApiVersion.ONECALL_3_0, true);
                        } catch (HttpException e) {
                            results.put(OwmApiVersion.ONECALL_3_0, false);
                        } catch (IOException e) {
                            results.put(OwmApiVersion.ONECALL_3_0, null);
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

                            results.put(OwmApiVersion.WEATHER_2_5, true);
                        } catch (HttpException e) {
                            results.put(OwmApiVersion.WEATHER_2_5, false);
                        } catch (IOException e) {
                            results.put(OwmApiVersion.WEATHER_2_5, null);
                        }
                    }
            );
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (results.get(OwmApiVersion.ONECALL_2_5) == null ||
                results.get(OwmApiVersion.ONECALL_3_0) == null ||
                results.get(OwmApiVersion.WEATHER_2_5) == null
        ) {
            throw new OpenWeatherMapException("Could not connect to OpenWeatherMap");
        }

        for (OwmApiVersion option : OwmApiVersion.values()) {
            if (Boolean.TRUE.equals(results.get(option))) {
                return option;
            }
        }

        return null;
    }

    public int getIconFromCode(String icon, Integer weatherId) {
        Integer resId;

        resId = weatherId != null ? codeToIcon.get(weatherId.toString() + icon.charAt(2)) : null;
        resId = resId == null && icon != null ? codeToIcon.get(icon) : resId;
        resId = resId == null ? R.drawable.thermometer_25 : resId;

        return resId;
    }

    public CurrentWeather getCurrentWeather(
            Context context,
            @NonNull OwmApiVersion apiVersion,
            double latitude,
            double longitude,
            String apiKey)
            throws IOException, JSONException, InstantiationException, IllegalAccessException, HttpException {
        if (apiVersion == OwmApiVersion.ONECALL_2_5 ||
                apiVersion == OwmApiVersion.ONECALL_3_0) {
            return getCurrentWeatherFromOneCall(context, apiVersion, latitude, longitude, apiKey);
        }
        throw new IllegalArgumentException("WeatherProvider must be ONECALL_2_5 or ONECALL_3_0");
    }

    private CurrentWeather getCurrentWeatherFromOneCall(
            Context context,
            @NonNull OwmApiVersion apiVersion,
            double latitude,
            double longitude,
            String apiKey)
            throws IOException, JSONException, InstantiationException, IllegalAccessException, HttpException {
        OpenWeatherOneCall openWeatherOneCall = JsonUtils.deserialize(OpenWeatherOneCall.class, new JSONObject(
                new HttpRequest(
                        String.format(Locale.US, uriFormatOneCall, apiKey,
                                latitude,
                                longitude,
                                getLang(Locale.getDefault()),
                                apiVersion == OwmApiVersion.ONECALL_2_5 ? "2.5" : "3.0"))
                        .addHeader("User-Agent", "QuickWeather - https://play.google.com/store/apps/details?id=com.ominous.quickweather")
                        .fetch()));

        WeatherUtils weatherUtils = WeatherUtils.getInstance(context);
        CurrentWeather currentWeather = new CurrentWeather();

        currentWeather.timestamp = Calendar.getInstance(TimeZone.getTimeZone("GMT")).getTimeInMillis();
        currentWeather.timezone = TimeZone.getTimeZone(openWeatherOneCall.timezone);

        if (openWeatherOneCall.current != null) {
            int weatherIconRes;
            int weatherCode;
            String weatherDescription;
            String weatherLongDescription;

            if (openWeatherOneCall.current.weather != null) {
                weatherIconRes = OpenWeatherMap.getInstance()
                        .getIconFromCode(openWeatherOneCall.current.weather[0].icon,
                                openWeatherOneCall.current.weather[0].id);

                weatherCode = openWeatherOneCall.current.weather[0].id;

                String[] weatherDescriptions = new String[openWeatherOneCall.current.weather.length];

                for (int i = 0; i < openWeatherOneCall.current.weather.length; i++) {
                    weatherDescriptions[i] = openWeatherOneCall.current.weather[i].description;
                }

                weatherDescription = WeatherUtils.getInstance(context)
                        .getWeatherDescription(weatherDescriptions);
                weatherLongDescription =
                        getCurrentWeatherLongDescription(context,
                                openWeatherOneCall,
                                weatherDescription);
            } else {
                weatherIconRes = R.drawable.ic_error_outline_white_24dp;
                weatherCode = -1;
                weatherDescription = context.getString(R.string.text_error);
                weatherLongDescription = context.getString(R.string.text_error);
            }

            currentWeather.current = new CurrentWeather.DataPoint(
                    openWeatherOneCall.current.dt,
                    openWeatherOneCall.current.temp,
                    openWeatherOneCall.current.feels_like,
                    openWeatherOneCall.current.visibility,
                    openWeatherOneCall.current.humidity,
                    openWeatherOneCall.current.wind_speed,
                    openWeatherOneCall.current.wind_deg,
                    openWeatherOneCall.current.pressure,
                    openWeatherOneCall.current.dew_point,
                    openWeatherOneCall.current.uvi,
                    weatherCode,
                    weatherIconRes,
                    weatherDescription,
                    weatherLongDescription,
                    weatherUtils.getPrecipitationIntensity(
                            openWeatherOneCall.current.rain == null ? 0 : openWeatherOneCall.current.rain.volume,
                            openWeatherOneCall.current.snow == null ? 0 : openWeatherOneCall.current.snow.volume),
                    weatherUtils.getPrecipitationType(
                            openWeatherOneCall.current.rain == null ? 0 : openWeatherOneCall.current.rain.volume,
                            openWeatherOneCall.current.snow == null ? 0 : openWeatherOneCall.current.snow.volume));
        }

        if (openWeatherOneCall.daily != null) {
            currentWeather.daily = new CurrentWeather.DataPoint[openWeatherOneCall.daily.length];

            for (int i = 0, l = openWeatherOneCall.daily.length; i < l; i++) {
                double precipitationIntensity = weatherUtils.getPrecipitationIntensity(
                        openWeatherOneCall.daily[i].rain,
                        openWeatherOneCall.daily[i].snow);
                PrecipType precipitationType = weatherUtils.getPrecipitationType(
                        openWeatherOneCall.daily[i].rain,
                        openWeatherOneCall.daily[i].snow);

                int weatherIconRes;
                int weatherCode;
                String weatherDescription;
                String weatherLongDescription;

                if (openWeatherOneCall.daily[i].weather != null) {
                    weatherIconRes = OpenWeatherMap.getInstance()
                            .getIconFromCode(openWeatherOneCall.daily[i].weather[0].icon,
                                    openWeatherOneCall.daily[i].weather[0].id);

                    weatherCode = openWeatherOneCall.daily[i].weather[0].id;

                    String[] weatherDescriptions = new String[openWeatherOneCall.daily[i].weather.length];

                    for (int ii = 0; ii < openWeatherOneCall.daily[i].weather.length; ii++) {
                        weatherDescriptions[ii] = openWeatherOneCall.daily[i].weather[ii].description;
                    }

                    weatherDescription = WeatherUtils.getInstance(context)
                            .getWeatherDescription(weatherDescriptions);
                    weatherLongDescription = WeatherUtils.getInstance(context)
                            .getWeatherDescription(weatherDescriptions,
                                    openWeatherOneCall.daily[i].dew_point,
                                    openWeatherOneCall.daily[i].wind_speed,
                                    openWeatherOneCall.daily[i].pop,
                                    precipitationIntensity,
                                    precipitationType,
                                    true);
                } else {
                    weatherIconRes = R.drawable.ic_error_outline_white_24dp;
                    weatherCode = -1;
                    weatherDescription = context.getString(R.string.text_error);
                    weatherLongDescription = context.getString(R.string.text_error);
                }

                currentWeather.daily[i] = new CurrentWeather.DataPoint(
                        openWeatherOneCall.daily[i].dt,
                        openWeatherOneCall.daily[i].temp.max,
                        openWeatherOneCall.daily[i].temp.min,
                        openWeatherOneCall.daily[i].humidity,
                        openWeatherOneCall.daily[i].wind_speed,
                        openWeatherOneCall.daily[i].wind_deg,
                        openWeatherOneCall.daily[i].pressure,
                        openWeatherOneCall.daily[i].dew_point,
                        openWeatherOneCall.daily[i].uvi,
                        openWeatherOneCall.daily[i].pop,
                        weatherCode,
                        weatherIconRes,
                        weatherDescription,
                        weatherLongDescription,
                        precipitationIntensity,
                        precipitationType);
            }
        }

        if (openWeatherOneCall.hourly != null) {
            currentWeather.hourly = new CurrentWeather.DataPoint[openWeatherOneCall.hourly.length];

            for (int i = 0, l = openWeatherOneCall.hourly.length; i < l; i++) {
                currentWeather.hourly[i] = new CurrentWeather.DataPoint(
                        openWeatherOneCall.hourly[i].dt,
                        openWeatherOneCall.hourly[i].temp,
                        weatherUtils.getPrecipitationIntensity(
                                openWeatherOneCall.hourly[i].rain == null ? 0 : openWeatherOneCall.hourly[i].rain.volume,
                                openWeatherOneCall.hourly[i].snow == null ? 0 : openWeatherOneCall.hourly[i].snow.volume),
                        weatherUtils.getPrecipitationType(
                                openWeatherOneCall.hourly[i].rain == null ? 0 : openWeatherOneCall.hourly[i].rain.volume,
                                openWeatherOneCall.hourly[i].snow == null ? 0 : openWeatherOneCall.hourly[i].snow.volume));
            }
        }

        if (openWeatherOneCall.alerts != null) {
            currentWeather.alerts = new CurrentWeather.Alert[openWeatherOneCall.alerts.length];

            for (int i = 0, l = openWeatherOneCall.alerts.length; i < l; i++) {
                currentWeather.alerts[i] = new CurrentWeather.Alert();

                currentWeather.alerts[i].senderName = openWeatherOneCall.alerts[i].sender_name;
                currentWeather.alerts[i].event = openWeatherOneCall.alerts[i].event;
                currentWeather.alerts[i].start = openWeatherOneCall.alerts[i].start;
                currentWeather.alerts[i].end = openWeatherOneCall.alerts[i].end;
                currentWeather.alerts[i].description = openWeatherOneCall.alerts[i].description;
            }
        }

        return currentWeather;
    }

    public ForecastWeather getForecastWeather(
            Context context,
            double latitude,
            double longitude,
            String apiKey)
            throws IOException, JSONException, InstantiationException, IllegalAccessException, HttpException {
        OpenWeatherForecast openWeatherForecast = JsonUtils.deserialize(OpenWeatherForecast.class, new JSONObject(
                new HttpRequest(
                        String.format(Locale.US, uriFormatForecast, apiKey,
                                latitude,
                                longitude,
                                getLang(Locale.getDefault())))
                        .addHeader("User-Agent", "QuickWeather - https://play.google.com/store/apps/details?id=com.ominous.quickweather")
                        .fetch()));

        WeatherUtils weatherUtils = WeatherUtils.getInstance(context);
        ForecastWeather forecastWeather = new ForecastWeather();

        forecastWeather.timestamp = openWeatherForecast.timestamp;

        if (openWeatherForecast.list != null) {
            forecastWeather.list = new ForecastWeather.ForecastData[openWeatherForecast.list.length];

            for (int i = 0, l = openWeatherForecast.list.length; i < l; i++) {
                forecastWeather.list[i] = new ForecastWeather.ForecastData();

                forecastWeather.list[i].dt = openWeatherForecast.list[i].dt;
                forecastWeather.list[i].pop = openWeatherForecast.list[i].pop;

                if (openWeatherForecast.list[i].main != null) {
                    forecastWeather.list[i].temp = openWeatherForecast.list[i].main.temp;
                }

                if (openWeatherForecast.list[i].weather != null) {
                    forecastWeather.list[i].weatherIconRes =
                            OpenWeatherMap.getInstance().getIconFromCode(
                                    openWeatherForecast.list[i].weather[0].icon,
                                    openWeatherForecast.list[i].weather[0].id);

                    String[] weatherDescriptions = new String[openWeatherForecast.list[i].weather.length];

                    for (int ii = 0; ii < openWeatherForecast.list[i].weather.length; ii++) {
                        weatherDescriptions[ii] = openWeatherForecast.list[i].weather[ii].description;
                    }

                    forecastWeather.list[i].weatherDescription = weatherUtils.getWeatherDescription(weatherDescriptions);
                }

                forecastWeather.list[i].precipitationIntensity = weatherUtils.getPrecipitationIntensity(
                        openWeatherForecast.list[i].rain == null ? 0 : openWeatherForecast.list[i].rain.volume,
                        openWeatherForecast.list[i].snow == null ? 0 : openWeatherForecast.list[i].snow.volume);

                forecastWeather.list[i].precipitationType = weatherUtils.getPrecipitationType(
                        openWeatherForecast.list[i].rain == null ? 0 : openWeatherForecast.list[i].rain.volume,
                        openWeatherForecast.list[i].snow == null ? 0 : openWeatherForecast.list[i].snow.volume);
            }
        }

        return forecastWeather;
    }

    private String getCurrentWeatherLongDescription(Context context,
                                                    OpenWeatherOneCall openWeatherOneCall,
                                                    String weatherDescription) {
        WeatherUtils weatherUtils = WeatherUtils.getInstance(context);
        Resources resources = context.getResources();

        StringBuilder result = new StringBuilder(weatherDescription);

        if (openWeatherOneCall.minutely != null && openWeatherOneCall.minutely.length > 0) {
            double startingPrecipitation = openWeatherOneCall.minutely[0].precipitation;
            double endingPrecipitation = openWeatherOneCall.minutely[openWeatherOneCall.minutely.length - 1].precipitation;
            String precipType = WeatherUtils.getInstance(context)
                    .getPrecipitationTypeString(openWeatherOneCall.hourly != null && openWeatherOneCall.hourly.length > 0 ?
                            weatherUtils.getPrecipitationType(
                                    openWeatherOneCall.hourly[0].rain == null ? 0 : openWeatherOneCall.hourly[0].rain.volume,
                                    openWeatherOneCall.hourly[0].snow == null ? 0 : openWeatherOneCall.hourly[0].snow.volume) : PrecipType.RAIN);

            if (startingPrecipitation > 0 && endingPrecipitation == 0) {
                for (int i = 0, l = openWeatherOneCall.minutely.length; i < l; i++) {
                    if (openWeatherOneCall.minutely[i].precipitation == 0) {
                        int mins = (int) (openWeatherOneCall.minutely[i].dt - Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis() / 1000) / 60;

                        mins = (int) BigDecimal.valueOf(mins / 5.).setScale(0, RoundingMode.HALF_UP).doubleValue() * 5;

                        if (mins > 0) {
                            result
                                    .append(resources.getString(R.string.format_separator))
                                    .append(resources.getString(R.string.format_precipitation_end, precipType, mins));
                        }

                        break;
                    }
                }
            } else if (startingPrecipitation == 0 &&
                    endingPrecipitation > 0) {
                for (int i = 0, l = openWeatherOneCall.minutely.length; i < l; i++) {
                    if (openWeatherOneCall.minutely[i].precipitation > 0) {
                        int mins = (int) (openWeatherOneCall.minutely[i].dt - Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis() / 1000) / 60;

                        mins = (int) BigDecimal.valueOf(mins / 5.).setScale(0, RoundingMode.HALF_UP).doubleValue() * 5;

                        if (mins > 0) {
                            result
                                    .append(resources.getString(R.string.format_separator))
                                    .append(resources.getString(R.string.format_precipitation_start, precipType, mins));
                        }

                        break;
                    }
                }
            }
        }

        return StringUtils.capitalizeEachWord(result.toString());
    }

    public static class OpenWeatherMapException extends RuntimeException {
        public OpenWeatherMapException(String message) {
            super(message);
        }
    }
}
