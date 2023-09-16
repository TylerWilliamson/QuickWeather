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

package com.ominous.quickweather.api.openmeteo;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import com.ominous.quickweather.R;
import com.ominous.quickweather.data.CurrentWeather;
import com.ominous.quickweather.data.ForecastWeather;
import com.ominous.quickweather.data.PrecipType;
import com.ominous.quickweather.util.WeatherUtils;
import com.ominous.tylerutils.http.HttpException;
import com.ominous.tylerutils.http.HttpRequest;
import com.ominous.tylerutils.util.JsonUtils;
import com.ominous.tylerutils.work.ParallelThreadManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class OpenMeteo {
    private final static String currentApi = "%1$s/v1/forecast?latitude=%2$f&longitude=%3$f&hourly=relativehumidity_2m,dewpoint_2m,apparent_temperature,rain,showers,snowfall,surface_pressure,visibility,winddirection_10m,uv_index,is_day&current_weather=true&temperature_unit=fahrenheit&windspeed_unit=mph&precipitation_unit=inch&timeformat=unixtime&timezone=auto&forecast_days=1";
    private final static String dailyHourlyApi = "%1$s/v1/forecast?latitude=%2$f&longitude=%3$f&hourly=weathercode,precipitation_probability,temperature_2m,rain,showers,snowfall,surface_pressure,dewpoint_2m,relativehumidity_2m,is_day,windspeed_10m,winddirection_10m,uv_index&daily=weathercode,temperature_2m_max,temperature_2m_min,uv_index_max,rain_sum,showers_sum,snowfall_sum,precipitation_probability_max,windspeed_10m_max,winddirection_10m_dominant&temperature_unit=fahrenheit&windspeed_unit=mph&precipitation_unit=inch&timeformat=unixtime&timezone=auto";

    private final static String USER_AGENT = "QuickWeather - https://play.google.com/store/apps/details?id=com.ominous.quickweather";

    private static OpenMeteo instance;

    private OpenMeteo() {

    }

    public static OpenMeteo getInstance() {
        if (instance == null) {
            instance = new OpenMeteo();
        }
        return instance;
    }

    public CurrentWeather getCurrentWeather(Context context,
                                            double latitude,
                                            double longitude,
                                            String apiKey,
                                            String selfHostedInstance)
            throws IOException, JSONException, InstantiationException, IllegalAccessException, HttpException {
        OpenMeteoForecast[] forecasts = new OpenMeteoForecast[2];
        Exception[] exceptions = new Exception[2];

        try {
            ParallelThreadManager.execute(
                    () -> {
                        String url = String.format(Locale.US,
                                currentApi,
                                selfHostedInstance.isEmpty() ? "https://api.open-meteo.com" : selfHostedInstance,
                                latitude,
                                longitude);

                        if (!apiKey.isEmpty()) {
                            url += "&apikey=" + apiKey;
                        }

                        try {
                            forecasts[0] = JsonUtils.deserialize(OpenMeteoForecast.class, new JSONObject(
                                    new HttpRequest(url)
                                            .addHeader("User-Agent", USER_AGENT)
                                            .fetch()
                            ));
                        } catch (IllegalAccessException | InstantiationException | JSONException |
                                 HttpException | IOException e) {
                            exceptions[0] = e;
                        }
                    },
                    () -> {
                        String url1 = String.format(Locale.US,
                                dailyHourlyApi,
                                selfHostedInstance.isEmpty() ? "https://api.open-meteo.com" : selfHostedInstance,
                                latitude,
                                longitude);

                        if (!apiKey.isEmpty()) {
                            url1 += "&apikey=" + apiKey;
                        }
                        try {
                            forecasts[1] = JsonUtils.deserialize(OpenMeteoForecast.class, new JSONObject(
                                    new HttpRequest(url1)
                                            .addHeader("User-Agent", USER_AGENT)
                                            .fetch()
                                    ));
                        } catch (IllegalAccessException | InstantiationException | JSONException |
                                 HttpException | IOException e) {
                            exceptions[1] = e;
                        }
                    }
            );
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        for (Exception e : exceptions) {
            if (e != null) {
                if (e instanceof IOException) {
                    throw (IOException) e;
                } else if (e instanceof JSONException) {
                    throw (JSONException) e;
                } else if (e instanceof InstantiationException) {
                    throw (InstantiationException) e;
                } else if (e instanceof IllegalAccessException) {
                    throw (IllegalAccessException) e;
                } else if (e instanceof HttpException) {
                    throw (HttpException) e;
                } else {
                    throw new RuntimeException(e);
                }
            }
        }

        OpenMeteoForecast openMeteoCurrent = forecasts[0];
        OpenMeteoForecast openMeteoDailyHourly = forecasts[1];

        WeatherUtils weatherUtils = WeatherUtils.getInstance(context);
        long currentTimestamp = Calendar.getInstance(TimeZone.getTimeZone("GMT")).getTimeInMillis();
        int thisHour = 0;

        for (int i = 0, l = openMeteoCurrent.hourly.time.length; i < l; i++) {
            if (currentTimestamp > openMeteoCurrent.hourly.time[i] * 1000) {
                thisHour = i;
            } else {
                break;
            }
        }

        CurrentWeather currentWeather = new CurrentWeather();

        currentWeather.timestamp = currentTimestamp;
        currentWeather.timezone = TimeZone.getTimeZone(openMeteoCurrent.timezone);

        if (openMeteoCurrent.current_weather != null && openMeteoCurrent.hourly != null) {
            WeatherCode weatherCode = WeatherCode.from(openMeteoCurrent.current_weather.weathercode, WeatherCode.ERROR);

            double precipitationIntensity = weatherUtils.getPrecipitationIntensity(
                    openMeteoCurrent.hourly.rain[thisHour] + openMeteoCurrent.hourly.showers[thisHour],
                    openMeteoCurrent.hourly.snowfall[thisHour]) * 25.4;
            PrecipType precipitationType = weatherUtils.getPrecipitationType(
                    openMeteoCurrent.hourly.rain[thisHour] + openMeteoCurrent.hourly.showers[thisHour],
                    openMeteoCurrent.hourly.snowfall[thisHour]);

            currentWeather.current = new CurrentWeather.DataPoint(
                    openMeteoCurrent.hourly.time[thisHour],
                    openMeteoCurrent.current_weather.temperature,
                    openMeteoCurrent.hourly.apparent_temperature[thisHour],
                    Math.min((int) (openMeteoCurrent.hourly.visibility[thisHour] * 0.3048), 10000), //ft to m, cap of 1000m
                    openMeteoCurrent.hourly.relativehumidity_2m[thisHour],
                    openMeteoCurrent.current_weather.windspeed,
                    openMeteoCurrent.current_weather.winddirection,
                    (int) openMeteoCurrent.hourly.surface_pressure[thisHour],
                    openMeteoCurrent.hourly.dewpoint_2m[thisHour],
                    openMeteoCurrent.hourly.uv_index[thisHour],
                    getStandardWeatherCode(weatherCode),
                    getWeatherIconRes(weatherCode, openMeteoCurrent.hourly.is_day[thisHour] == 1),
                    weatherUtils
                            .getWeatherDescription(new String[]{context.getString(getDescriptionResForWeatherCode(weatherCode))}),
                    weatherUtils
                            .getWeatherDescription(new String[]{context.getString(getDescriptionResForWeatherCode(weatherCode))},
                                    openMeteoCurrent.hourly.dewpoint_2m[thisHour],
                                    openMeteoCurrent.current_weather.windspeed,
                                    0,
                                    precipitationIntensity,
                                    precipitationType,
                                    true),
                    precipitationIntensity,
                    precipitationType);
        }

        if (openMeteoDailyHourly.daily != null && openMeteoCurrent.hourly != null) {
            currentWeather.daily = new CurrentWeather.DataPoint[openMeteoDailyHourly.daily.time.length];

            for (int i = 0, l = openMeteoDailyHourly.daily.time.length; i < l; i++) {
                ArrayList<Double> pressureArrayList = new ArrayList<>(24);
                ArrayList<Double> humidityArrayList = new ArrayList<>(24);
                ArrayList<Double> dewPointArrayList = new ArrayList<>(24);

                //TODO calculate the daily pop?
                for (int ii = 0, ll = openMeteoDailyHourly.hourly.time.length; ii < ll && pressureArrayList.size() < 24; ii++) {
                    if (openMeteoDailyHourly.hourly.time[ii] >= openMeteoDailyHourly.daily.time[i] &&
                            (i + 1 >= openMeteoDailyHourly.daily.time.length ||
                                    openMeteoDailyHourly.hourly.time[ii] < openMeteoDailyHourly.daily.time[i + 1])) {
                        pressureArrayList.add(openMeteoDailyHourly.hourly.surface_pressure[ii]);
                        humidityArrayList.add((double) openMeteoDailyHourly.hourly.relativehumidity_2m[ii]);
                        dewPointArrayList.add(openMeteoDailyHourly.hourly.dewpoint_2m[ii]);
                    }
                }

                int dailyPressure = (int) summarizeData(pressureArrayList);
                int dailyHumidity = (int) summarizeData(humidityArrayList);
                double dailyDewPoint = summarizeData(dewPointArrayList);

                double precipitationIntensity = weatherUtils.getPrecipitationIntensity(
                        openMeteoDailyHourly.daily.rain_sum[i] + openMeteoDailyHourly.daily.showers_sum[i],
                        openMeteoDailyHourly.daily.snowfall_sum[i]) * 25.4;
                PrecipType precipitationType = weatherUtils.getPrecipitationType(
                        openMeteoDailyHourly.daily.rain_sum[i] + openMeteoDailyHourly.daily.showers_sum[i],
                        openMeteoDailyHourly.daily.snowfall_sum[i]);

                //TODO calculate the daily weathercode aggregation? currently it chooses the highest value
                WeatherCode dailyWeatherCode = WeatherCode.from(openMeteoDailyHourly.daily.weathercode[i], WeatherCode.ERROR);

                currentWeather.daily[i] = new CurrentWeather.DataPoint(
                        openMeteoDailyHourly.daily.time[i],
                        openMeteoDailyHourly.daily.temperature_2m_max[i],
                        openMeteoDailyHourly.daily.temperature_2m_min[i],
                        dailyHumidity,
                        openMeteoDailyHourly.daily.windspeed_10m_max[i],
                        openMeteoDailyHourly.daily.winddirection_10m_dominant[i],
                        dailyPressure,
                        dailyDewPoint,
                        openMeteoDailyHourly.daily.uv_index_max[i],
                        openMeteoDailyHourly.daily.precipitation_probability_max[i] / 100.,
                        getStandardWeatherCode(dailyWeatherCode),
                        getWeatherIconRes(dailyWeatherCode, true),
                        weatherUtils
                                .getWeatherDescription(new String[]{context.getString(getDescriptionResForWeatherCode(dailyWeatherCode))}),
                        weatherUtils
                                .getWeatherDescription(
                                        new String[]{context.getString(getDescriptionResForWeatherCode(dailyWeatherCode))},
                                        dailyDewPoint,
                                        openMeteoDailyHourly.daily.windspeed_10m_max[i],
                                        openMeteoDailyHourly.daily.precipitation_probability_max[i] / 100.,
                                        precipitationIntensity,
                                        precipitationType,
                                        true),
                        precipitationIntensity,
                        precipitationType
                );

            }
        }

        if (openMeteoCurrent.hourly != null) {
            ArrayList<CurrentWeather.DataPoint> hourlyList = new ArrayList<>();

            for (int i = thisHour, l = openMeteoDailyHourly.hourly.time.length; i < l && (i - thisHour) < 48; i++) {

                hourlyList.add(new CurrentWeather.DataPoint(
                        openMeteoDailyHourly.hourly.time[i],
                        openMeteoDailyHourly.hourly.temperature_2m[i],
                        getStandardWeatherCode(WeatherCode.from(openMeteoDailyHourly.hourly.weathercode[i], WeatherCode.ERROR)),
                        openMeteoDailyHourly.hourly.relativehumidity_2m[i],
                        openMeteoDailyHourly.hourly.windspeed_10m[i],
                        openMeteoDailyHourly.hourly.winddirection_10m[i],
                        openMeteoDailyHourly.hourly.uv_index[i],
                        openMeteoDailyHourly.hourly.precipitation_probability[i] / 100.,
                        weatherUtils.getPrecipitationIntensity(
                                openMeteoDailyHourly.hourly.rain[i] + openMeteoDailyHourly.hourly.showers[i],
                                openMeteoDailyHourly.hourly.snowfall[i]) * 25.4,
                        weatherUtils.getPrecipitationType(
                                openMeteoDailyHourly.hourly.rain[i] + openMeteoDailyHourly.hourly.showers[i],
                                openMeteoDailyHourly.hourly.snowfall[i])));
            }

            currentWeather.hourly = hourlyList.toArray(new CurrentWeather.DataPoint[0]);
        }

        return currentWeather;
    }

    public ForecastWeather getForecastWeather(Context context,
                                              double latitude,
                                              double longitude,
                                              String apiKey,
                                              String selfHostedInstance)
            throws IOException, JSONException, InstantiationException, IllegalAccessException, HttpException {
        String url = String.format(Locale.US,
                dailyHourlyApi,
                selfHostedInstance.isEmpty() ? "https://api.open-meteo.com" : selfHostedInstance,
                latitude,
                longitude);

        if (!apiKey.isEmpty()) {
            url += "&apikey=" + apiKey;
        }

        OpenMeteoForecast openMeteoDailyHourly = JsonUtils.deserialize(OpenMeteoForecast.class, new JSONObject(
                new HttpRequest(url)
                        .addHeader("User-Agent", USER_AGENT)
                        .fetch()));

        WeatherUtils weatherUtils = WeatherUtils.getInstance(context);
        long currentTimestamp = Calendar.getInstance(TimeZone.getTimeZone("GMT")).getTimeInMillis();
        ArrayList<ForecastWeather.ForecastData> forecastDataList = new ArrayList<>(48); // 6 days, every 3 hours

        for (int i = 0, l = openMeteoDailyHourly.hourly.time.length; i < l; i += 3) {
            if (openMeteoDailyHourly.hourly.time[i] * 1000 > currentTimestamp) {
                ForecastWeather.ForecastData forecastData = new ForecastWeather.ForecastData();

                forecastData.dt = openMeteoDailyHourly.hourly.time[i];
                forecastData.temp = openMeteoDailyHourly.hourly.temperature_2m[i];

                WeatherCode hourlyWeatherCode = WeatherCode.from(openMeteoDailyHourly.hourly.weathercode[i], WeatherCode.ERROR);

                forecastData.weatherIconRes = getWeatherIconRes(hourlyWeatherCode, openMeteoDailyHourly.hourly.is_day[i] == 1);
                forecastData.weatherDescription = context.getString(getDescriptionResForWeatherCode(hourlyWeatherCode));
                forecastData.precipitationIntensity = weatherUtils.getPrecipitationIntensity(
                        openMeteoDailyHourly.hourly.rain[i] + openMeteoDailyHourly.hourly.showers[i],
                        openMeteoDailyHourly.hourly.snowfall[i]) * 25.4;
                forecastData.precipitationType = weatherUtils.getPrecipitationType(
                        openMeteoDailyHourly.hourly.rain[i] + openMeteoDailyHourly.hourly.showers[i],
                        openMeteoDailyHourly.hourly.snowfall[i]);
                forecastData.pop = openMeteoDailyHourly.hourly.precipitation_probability[i] / 100.;

                forecastDataList.add(forecastData);
            }
        }

        ForecastWeather forecastWeather = new ForecastWeather();
        forecastWeather.timestamp = currentTimestamp;
        forecastWeather.list = forecastDataList.toArray(new ForecastWeather.ForecastData[0]);

        return forecastWeather;
    }

    public boolean testConnection(String selfHostedInstance, String apiKey) {
        try {
            String url = String.format(Locale.US,
                    currentApi,
                    selfHostedInstance.isEmpty() ? "https://api.open-meteo.com" : selfHostedInstance,
                    33.749,
                    -84.388);

            if (!apiKey.isEmpty()) {
                url += "&apikey=" + apiKey;
            }

            new HttpRequest(url)
                    .addHeader("User-Agent", USER_AGENT)
                    .fetch();

            return true;
        } catch (HttpException | IOException e) {
            //TODO differentiate between connection issue and api key issue
            return false;
        }
    }

    @StringRes
    private int getDescriptionResForWeatherCode(WeatherCode weathercode) {
        switch (weathercode) {
            case CLEARSKY:
                return R.string.openmeteo_clearsky;
            case MAINLYCLEAR:
                return R.string.openmeteo_mainlyclear;
            case PARTLYCLOUDY:
                return R.string.openmeteo_partlycloudy;
            case OVERCAST:
                return R.string.openmeteo_overcast;
            case FOG:
                return R.string.openmeteo_fog;
            case DEPOSITINGTIMEFOG:
                return R.string.openmeteo_depositingrimefog;
            case LIGHTDRIZZLE:
                return R.string.openmeteo_lightdrizzle;
            case MODERATEDRIZZLE:
                return R.string.openmeteo_moderatedrizzle;
            case DENSEDRIZZLE:
                return R.string.openmeteo_densedrizzle;
            case LIGHTRAIN:
                return R.string.openmeteo_lightrain;
            case MODERATERAIN:
                return R.string.openmeteo_moderaterain;
            case HEAVYRAIN:
                return R.string.openmeteo_heavyrain;
            case SLIGHTRAINSHOWERS:
                return R.string.openmeteo_lightrainshower;
            case MODERATERAINSHOWERS:
                return R.string.openmeteo_moderaterainshower;
            case HEAVYRAINSHOWERS:
                return R.string.openmeteo_heavyrainshower;
            case LIGHTFREEZINGDRIZZLE:
                return R.string.openmeteo_lightfreezingdrizzle;
            case MODERATEORDENSEFREEZINGDRIZZLE:
                return R.string.openmeteo_moderatefreezingdrizzle;
            case LIGHTFREEZINGRAIN:
                return R.string.openmeteo_lightfreezingrain;
            case MODERATEORHEAVYFREEZINGRAIN:
                return R.string.openmeteo_heavyfreezingrain;
            case SLIGHTSNOWFALL:
                return R.string.openmeteo_lightsnow;
            case MODERATESNOWFALL:
                return R.string.openmeteo_moderatesnow;
            case HEAVYSNOWFALL:
                return R.string.openmeteo_heavysnow;
            case SNOWGRAINS:
                return R.string.openmeteo_snowgrains;
            case SLIGHTSNOWSHOWERS:
                return R.string.openmeteo_lightsnowshower;
            case HEAVYSNOWSHOWERS:
                return R.string.openmeteo_heavysnowshower;
            case THUNDERSTORMSLIGHTORMODERATE:
                return R.string.openmeteo_thunderstorm;
            case THUNDERSTORMSTRONG:
                return R.string.openmeteo_thunderstormlighthail;
            case THUNDERSTORMHEAVY:
                return R.string.openmeteo_thunderstormheavyhail;
        }

        return -1;
    }

    private int getStandardWeatherCode(WeatherCode weathercode) {
        switch (weathercode) {
            case CLEARSKY:
            case MAINLYCLEAR:
                return 1;
            case PARTLYCLOUDY:
                return 2;
            case OVERCAST:
                return 3;
            case FOG:
            case DEPOSITINGTIMEFOG:
                return 50;
            case LIGHTDRIZZLE:
            case MODERATEDRIZZLE:
            case DENSEDRIZZLE:
            case LIGHTFREEZINGDRIZZLE:
            case MODERATEORDENSEFREEZINGDRIZZLE:
            case SLIGHTRAINSHOWERS:
            case MODERATERAINSHOWERS:
            case HEAVYRAINSHOWERS:
                return 9;
            case LIGHTRAIN:
            case MODERATERAIN:
            case HEAVYRAIN:
            case LIGHTFREEZINGRAIN:
            case MODERATEORHEAVYFREEZINGRAIN:
                return 10;
            case SLIGHTSNOWFALL:
            case MODERATESNOWFALL:
            case HEAVYSNOWFALL:
            case SNOWGRAINS:
            case SLIGHTSNOWSHOWERS:
            case HEAVYSNOWSHOWERS:
                return 13;
            case THUNDERSTORMSLIGHTORMODERATE:
            case THUNDERSTORMSTRONG:
            case THUNDERSTORMHEAVY:
                return 11;
        }
        return 0;
    }

    @DrawableRes
    private int getWeatherIconRes(WeatherCode weathercode, boolean isDay) {
        switch (weathercode) {
            case CLEARSKY:
                return isDay ? R.drawable.sun : R.drawable.moon_25;
            case MAINLYCLEAR:
            case PARTLYCLOUDY:
            case OVERCAST:
                return isDay ? R.drawable.cloud_sun : R.drawable.cloud_moon;
            case FOG:
            case DEPOSITINGTIMEFOG:
                return isDay ? R.drawable.cloud_fog_sun : R.drawable.cloud_fog_moon;
            case LIGHTDRIZZLE:
            case MODERATEDRIZZLE:
            case DENSEDRIZZLE:
            case LIGHTRAIN:
            case MODERATERAIN:
            case HEAVYRAIN:
            case SLIGHTRAINSHOWERS:
            case MODERATERAINSHOWERS:
            case HEAVYRAINSHOWERS:
                return isDay ? R.drawable.cloud_rain_sun : R.drawable.cloud_rain_moon;
            case LIGHTFREEZINGDRIZZLE:
            case MODERATEORDENSEFREEZINGDRIZZLE:
            case LIGHTFREEZINGRAIN:
            case MODERATEORHEAVYFREEZINGRAIN:
                return isDay ? R.drawable.cloud_hail_sun : R.drawable.cloud_hail_moon;
            case SLIGHTSNOWFALL:
            case MODERATESNOWFALL:
            case HEAVYSNOWFALL:
            case SNOWGRAINS:
            case SLIGHTSNOWSHOWERS:
            case HEAVYSNOWSHOWERS:
                return isDay ? R.drawable.cloud_snow_sun : R.drawable.cloud_snow_moon;
            case THUNDERSTORMSLIGHTORMODERATE:
            case THUNDERSTORMSTRONG:
            case THUNDERSTORMHEAVY:
                return isDay ? R.drawable.cloud_rain_lightning_sun : R.drawable.cloud_rain_lightning_moon;
        }

        return R.drawable.ic_error_outline_white_24dp;
    }

    //TODO use median instead of mean?
    private double summarizeData(List<Double> data) {
        double total = 0;

        for (double datum : data) {
            total += datum;
        }

        return total / data.size();
    }

    private enum WeatherCode {
        CLEARSKY(0),
        MAINLYCLEAR(1),
        PARTLYCLOUDY(2),
        OVERCAST(3),
        FOG(45),
        DEPOSITINGTIMEFOG(48),
        LIGHTDRIZZLE(51),
        MODERATEDRIZZLE(53),
        DENSEDRIZZLE(55),
        LIGHTFREEZINGDRIZZLE(56),
        MODERATEORDENSEFREEZINGDRIZZLE(57),
        LIGHTRAIN(61),
        MODERATERAIN(63),
        HEAVYRAIN(65),
        LIGHTFREEZINGRAIN(66),
        MODERATEORHEAVYFREEZINGRAIN(67),
        SLIGHTSNOWFALL(71),
        MODERATESNOWFALL(73),
        HEAVYSNOWFALL(75),
        SNOWGRAINS(77),
        SLIGHTRAINSHOWERS(80),
        MODERATERAINSHOWERS(81),
        HEAVYRAINSHOWERS(82),
        SLIGHTSNOWSHOWERS(85),
        HEAVYSNOWSHOWERS(86),
        THUNDERSTORMSLIGHTORMODERATE(95),
        THUNDERSTORMSTRONG(96),
        THUNDERSTORMHEAVY(99),
        ERROR(-1);

        private final int value;

        WeatherCode(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static WeatherCode from(int value, WeatherCode defaultValue) {
            for (WeatherCode v : values()) {
                if (v.getValue() == value) {
                    return v;
                }
            }

            return defaultValue;
        }

    }

}
