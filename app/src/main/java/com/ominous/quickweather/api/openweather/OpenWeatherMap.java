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

package com.ominous.quickweather.api.openweather;

import android.content.Context;
import android.content.res.Resources;

import com.ominous.quickweather.R;
import com.ominous.quickweather.data.CurrentWeather;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

public class OpenWeatherMap {
    private final static String uriFormatOneCall = "https://api.openweathermap.org/data/%5$s/onecall?appid=%1$s&lat=%2$f&lon=%3$f&lang=%4$s&units=imperial";
    private final static String uriFormatForecast = "https://api.openweathermap.org/data/2.5/forecast?appid=%1$s&lat=%2$f&lon=%3$f&lang=%4$s&units=imperial";
    private final static String uriFormatWeather = "https://api.openweathermap.org/data/2.5/weather?appid=%1$s&lat=%2$f&lon=%3$f&lang=%4$s&units=imperial";

    private final static String USER_AGENT = "QuickWeather - https://play.google.com/store/apps/details?id=com.ominous.quickweather";

    private static OpenWeatherMap instance;

    private OpenWeatherMap() {
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
                                            "3.0"))
                                    .addHeader("User-Agent", USER_AGENT)
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
                                    .addHeader("User-Agent", USER_AGENT)
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

        if (results.get(OwmApiVersion.ONECALL_3_0) == null ||
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
        if (icon != null) {
            int resId = weatherId != null ? getIconRes(weatherId.toString() + icon.charAt(2)) : R.drawable.ic_error_outline_white_24dp;

            return resId == R.drawable.ic_error_outline_white_24dp ? getIconRes(icon) : resId;
        } else {
            return R.drawable.ic_error_outline_white_24dp;
        }

    }

    public CurrentWeather getCurrentWeather(
            Context context,
            @NonNull OwmApiVersion apiVersion,
            double latitude,
            double longitude,
            String apiKey)
            throws IOException, JSONException, InstantiationException, IllegalAccessException, HttpException {
        if (apiVersion == OwmApiVersion.ONECALL_3_0) {
            return getCurrentWeatherFromOneCall(context, latitude, longitude, apiKey);
        }
        throw new IllegalArgumentException("WeatherProvider must be ONECALL_3_0");
    }

    //TODO Test Forecast when only subscribed to One Call
    private CurrentWeather getCurrentWeatherFromOneCall(
            Context context,
            double latitude,
            double longitude,
            String apiKey)
            throws IOException, JSONException, InstantiationException, IllegalAccessException, HttpException {
        HashMap<Integer, JSONObject> results = new HashMap<>();
        ArrayList<Exception> exceptions = new ArrayList<>();

        try {
            ParallelThreadManager.execute(
                    () -> {
                        try {
                            results.put(1, new JSONObject(new HttpRequest(
                                    String.format(Locale.US, uriFormatOneCall, apiKey,
                                            latitude,
                                            longitude,
                                            getLang(Locale.getDefault()),
                                            "3.0"))
                                    .addHeader("User-Agent", USER_AGENT)
                                    .fetch()));
                        } catch (HttpException | IOException | JSONException e) {
                            exceptions.add(e);
                        }
                    },
                    () -> {
                        try {
                            results.put(2, new JSONObject(new HttpRequest(
                                    String.format(Locale.US, uriFormatForecast, apiKey,
                                            latitude,
                                            longitude,
                                            getLang(Locale.getDefault())))
                                    .addHeader("User-Agent", USER_AGENT)
                                    .fetch()));
                        } catch (HttpException | IOException | JSONException e) {
                            exceptions.add(e);
                        }
                    }
                );
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (!exceptions.isEmpty()) {
            Exception lastException = exceptions.get(0);

            if (lastException instanceof HttpException) {
                throw (HttpException) lastException;
            } else if (lastException instanceof IOException) {
                throw (IOException) lastException;
            } else if (lastException instanceof JSONException) {
                throw (JSONException) lastException;
            }
        }

        OpenWeatherOneCall openWeatherOneCall = JsonUtils.deserialize(
                OpenWeatherOneCall.class,
                results.get(1));

        OpenWeatherForecast openWeatherForecast = JsonUtils.deserialize(
                OpenWeatherForecast.class,
                results.get(2));

        WeatherUtils weatherUtils = WeatherUtils.getInstance(context);
        CurrentWeather currentWeather = new CurrentWeather();

        currentWeather.timezone = TimeZone.getTimeZone(openWeatherOneCall.timezone);
        currentWeather.timestamp = Calendar.getInstance(currentWeather.timezone).getTimeInMillis();
        currentWeather.latitude = latitude;
        currentWeather.longitude = longitude;

        if (openWeatherOneCall.current != null) {
            int weatherIconRes;
            int weatherCode;
            String weatherDescription;
            String weatherLongDescription;

            if (openWeatherOneCall.current.weather != null) {
                weatherIconRes = getIconFromCode(openWeatherOneCall.current.weather[0].icon,
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
                    openWeatherOneCall.current.dt * 1000L,
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
                    weatherIconRes = getIconFromCode(openWeatherOneCall.daily[i].weather[0].icon,
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
                                    (int) (openWeatherOneCall.daily[i].pop * 100),
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
                        openWeatherOneCall.daily[i].dt * 1000L,
                        openWeatherOneCall.daily[i].temp.max,
                        openWeatherOneCall.daily[i].temp.min,
                        openWeatherOneCall.daily[i].humidity,
                        openWeatherOneCall.daily[i].wind_speed,
                        openWeatherOneCall.daily[i].wind_deg,
                        openWeatherOneCall.daily[i].pressure,
                        openWeatherOneCall.daily[i].dew_point,
                        openWeatherOneCall.daily[i].uvi,
                        (int) (openWeatherOneCall.daily[i].pop * 100),
                        weatherCode,
                        weatherIconRes,
                        weatherDescription,
                        weatherLongDescription,
                        precipitationIntensity,
                        precipitationType,
                        openWeatherOneCall.daily[i].sunrise * 1000L,
                        openWeatherOneCall.daily[i].sunset * 1000L,
                        openWeatherOneCall.daily[i].moonrise * 1000L,
                        openWeatherOneCall.daily[i].moonset * 1000L,
                        openWeatherOneCall.daily[i].moon_phase);
            }
        }

        if (openWeatherOneCall.hourly != null) {
            currentWeather.hourly = new CurrentWeather.DataPoint[openWeatherOneCall.hourly.length];

            for (int i = 0, l = openWeatherOneCall.hourly.length; i < l; i++) {
                currentWeather.hourly[i] = new CurrentWeather.DataPoint(
                        openWeatherOneCall.hourly[i].dt * 1000L,
                        openWeatherOneCall.hourly[i].temp,
                        openWeatherOneCall.hourly[i].weather != null && openWeatherOneCall.hourly[i].weather.length > 1 ?
                                openWeatherOneCall.hourly[i].weather[0].id : 0,
                        openWeatherOneCall.hourly[i].humidity,
                        openWeatherOneCall.hourly[i].wind_speed,
                        openWeatherOneCall.hourly[i].wind_deg,
                        openWeatherOneCall.hourly[i].uvi,
                        (int) (openWeatherOneCall.hourly[i].pop * 100),
                        weatherUtils.getPrecipitationIntensity(
                                openWeatherOneCall.hourly[i].rain == null ? 0 : openWeatherOneCall.hourly[i].rain.volume,
                                openWeatherOneCall.hourly[i].snow == null ? 0 : openWeatherOneCall.hourly[i].snow.volume),
                        weatherUtils.getPrecipitationType(
                                openWeatherOneCall.hourly[i].rain == null ? 0 : openWeatherOneCall.hourly[i].rain.volume,
                                openWeatherOneCall.hourly[i].snow == null ? 0 : openWeatherOneCall.hourly[i].snow.volume));
            }
        }

        if (openWeatherOneCall.alerts != null) {
            ArrayList<CurrentWeather.Alert> alertList = new ArrayList<>();

            for (int i = 0, l = openWeatherOneCall.alerts.length; i < l; i++) {
                if (!openWeatherOneCall.alerts[i].event.toLowerCase().contains("amber alert") &&
                        !openWeatherOneCall.alerts[i].event.toLowerCase().contains("this_message_is_for_test_purposes_only")) {
                    CurrentWeather.Alert alert = new CurrentWeather.Alert();

                    alert.senderName = openWeatherOneCall.alerts[i].sender_name;
                    alert.event = openWeatherOneCall.alerts[i].event;
                    alert.start = openWeatherOneCall.alerts[i].start;
                    alert.end = openWeatherOneCall.alerts[i].end;
                    alert.description = openWeatherOneCall.alerts[i].description;

                    alertList.add(alert);
                }
            }

            currentWeather.alerts = alertList.toArray(new CurrentWeather.Alert[]{});
        }

        if (openWeatherForecast.list != null) {
            currentWeather.trihourly = new CurrentWeather.DataPoint[openWeatherForecast.list.length];

            for (int i = 0, l = openWeatherForecast.list.length; i < l; i++) {
                int weatherIconRes;
                String weatherDescription;

                if (openWeatherForecast.list[i].weather != null) {
                    weatherIconRes =
                            getIconFromCode(
                                    openWeatherForecast.list[i].weather[0].icon,
                                    openWeatherForecast.list[i].weather[0].id);

                    String[] weatherDescriptions = new String[openWeatherForecast.list[i].weather.length];

                    for (int ii = 0; ii < openWeatherForecast.list[i].weather.length; ii++) {
                        weatherDescriptions[ii] = openWeatherForecast.list[i].weather[ii].description;
                    }

                    weatherDescription = weatherUtils.getWeatherDescription(weatherDescriptions);
                } else {
                    weatherIconRes = R.drawable.ic_error_outline_white_24dp;
                    weatherDescription = context.getString(R.string.text_error);
                }


                currentWeather.trihourly[i] = new CurrentWeather.DataPoint(
                        openWeatherForecast.list[i].dt * 1000L,
                        openWeatherForecast.list[i].main != null ? openWeatherForecast.list[i].main.temp : 0,
                        weatherIconRes,
                        weatherDescription,
                        (int) (openWeatherForecast.list[i].pop * 100),
                        weatherUtils.getPrecipitationIntensity(
                                openWeatherForecast.list[i].rain == null ? 0 : openWeatherForecast.list[i].rain.volume,
                                openWeatherForecast.list[i].snow == null ? 0 : openWeatherForecast.list[i].snow.volume),
                        weatherUtils.getPrecipitationType(
                                openWeatherForecast.list[i].rain == null ? 0 : openWeatherForecast.list[i].rain.volume,
                                openWeatherForecast.list[i].snow == null ? 0 : openWeatherForecast.list[i].snow.volume)
                );
            }
        }

        return currentWeather;
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

    @DrawableRes
    private int getIconRes(String weathercode) {
        switch (weathercode) {
            case "01d":
                return R.drawable.sun;
            case "01n":
                return R.drawable.moon_25;
            case "02d":
            case "03d":
            case "04d":
                return R.drawable.cloud_sun;
            case "02n":
            case "03n":
            case "04n":
                return R.drawable.cloud_moon;
            case "09d":
                return R.drawable.cloud_drizzle_sun;
            case "09n":
                return R.drawable.cloud_drizzle_moon;
            case "10d":
                return R.drawable.cloud_rain_sun;
            case "10n":
                return R.drawable.cloud_rain_moon;
            case "11d":
                return R.drawable.cloud_rain_lightning_sun;
            case "11n":
                return R.drawable.cloud_rain_lightning_moon;
            case "13d":
                return R.drawable.cloud_snow_sun;
            case "13n":
                return R.drawable.cloud_snow_moon;
            case "50d":
                return R.drawable.cloud_fog_sun;
            case "50n":
                return R.drawable.cloud_fog_moon;
            case "611d":
            case "612d":
            case "613d":
                return R.drawable.cloud_hail_sun;
            case "611n":
            case "612n":
            case "613n":
                return R.drawable.cloud_hail_moon;
            case "781d":
            case "781n":
                return R.drawable.tornado;
            default:
                return R.drawable.ic_error_outline_white_24dp;
        }
    }

    public static class OpenWeatherMapException extends RuntimeException {
        public OpenWeatherMapException(String message) {
            super(message);
        }
    }
}
