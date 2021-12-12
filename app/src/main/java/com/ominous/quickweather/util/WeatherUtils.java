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

package com.ominous.quickweather.util;

import android.content.Context;
import android.content.res.Resources;

import com.ominous.quickweather.R;
import com.ominous.quickweather.data.WeatherResponseOneCall;
import com.ominous.quickweather.weather.Weather;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import androidx.annotation.NonNull;

public class WeatherUtils {
    private static Resources resources;
    private static HashMap<String, Integer> codeToIcon;

    public static void initialize(Context context) {
        resources = context.getResources();

        codeToIcon = new HashMap<>();
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

    public static int getIconFromCode(String icon, Integer weatherId) {
        Integer resId;

        resId = weatherId != null ? codeToIcon.get(weatherId.toString() + icon.charAt(2)) : null;
        resId = resId == null && icon != null ? codeToIcon.get(icon) : resId;
        resId = resId == null ? R.drawable.thermometer_25 : resId;

        return resId;
    }

    public static String getCurrentShortWeatherDesc(@NonNull WeatherResponseOneCall responseOneCall) {
        StringBuilder resultStringBuilder = new StringBuilder(responseOneCall.current.weather[0].description);

        for (int i = 1, l = responseOneCall.current.weather.length; i < l; i++) {
            resultStringBuilder
                    .append(resources.getString(R.string.format_separator))
                    .append(responseOneCall.current.weather[i].description);
        }

        return resultStringBuilder.toString();
    }

    public static String getCurrentLongWeatherDesc(@NonNull WeatherResponseOneCall responseOneCall) {
        StringBuilder result = new StringBuilder(getCurrentShortWeatherDesc(responseOneCall));
        String precipType;
        double precipAmount = responseOneCall.current.getPrecipitationIntensity();

        switch (responseOneCall.hourly[0].getPrecipitationType()) {
            case MIX:
                precipType = resources.getString(R.string.weather_precip_mix);
                break;
            case SNOW:
                precipType = resources.getString(R.string.weather_precip_snow);
                break;
            default:
                precipType = resources.getString(R.string.weather_precip_rain);
                break;
        }

        if (responseOneCall.current.dew_point >= 60 && precipAmount == 0) {
            result
                    .append(resources.getString(R.string.format_separator))
                    .append(resources.getString(R.string.weather_desc_humid));
        } else if (responseOneCall.current.dew_point <= 35) {
            result
                    .append(resources.getString(R.string.format_separator))
                    .append(resources.getString(R.string.weather_desc_dry));
        }

        if (responseOneCall.current.wind_speed > 25.3) {
            result
                    .append(resources.getString(R.string.format_separator))
                    .append(resources.getString(R.string.weather_desc_strongwinds));
        } else if (responseOneCall.current.wind_speed > 8.05) {
            result
                    .append(resources.getString(R.string.format_separator))
                    .append(resources.getString(R.string.weather_desc_breezy));
        }

        if (responseOneCall.minutely != null) {
            if (responseOneCall.minutely[0].precipitation > 0 &&
                    responseOneCall.minutely[60].precipitation == 0) {
                for (int i = 0, l = responseOneCall.minutely.length; i < l; i++) {
                    if (responseOneCall.minutely[i].precipitation == 0) {
                        int mins = (int) (responseOneCall.minutely[i].dt - Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis() / 1000) / 60;

                        mins = (int) BigDecimal.valueOf(mins / 5.).setScale(0, BigDecimal.ROUND_HALF_UP).doubleValue() * 5;

                        if (mins > 0) {
                            result
                                    .append(resources.getString(R.string.format_separator))
                                    .append(resources.getString(R.string.format_precipitation_startend, precipType, "ending in", mins));
                        }

                        break;
                    }
                }
            } else if (responseOneCall.minutely[0].precipitation == 0 &&
                    responseOneCall.minutely[60].precipitation > 0) {
                for (int i = 0, l = responseOneCall.minutely.length; i < l; i++) {
                    if (responseOneCall.minutely[i].precipitation > 0) {
                        int mins = (int) (responseOneCall.minutely[i].dt - Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis() / 1000) / 60;

                        mins = (int) BigDecimal.valueOf(mins / 5.).setScale(0, BigDecimal.ROUND_HALF_UP).doubleValue() * 5;

                        if (mins > 0) {
                            result
                                    .append(resources.getString(R.string.format_separator))
                                    .append(resources.getString(R.string.format_precipitation_startend, precipType, "in", mins));
                        }

                        break;
                    }
                }
            }
        }

        return result.toString();
    }

    public static String getForecastLongWeatherDesc(WeatherResponseOneCall.DailyData data) {
        StringBuilder result = new StringBuilder(data.weather[0].description);

        if (data.dew_point >= 60) {
            result
                    .append(resources.getString(R.string.format_separator))
                    .append(resources.getString(R.string.weather_desc_humid));
        } else if (data.dew_point <= 35) {
            result
                    .append(resources.getString(R.string.format_separator))
                    .append(resources.getString(R.string.weather_desc_dry));
        }

        if (data.wind_speed > 25.3) {
            result
                    .append(resources.getString(R.string.format_separator))
                    .append(resources.getString(R.string.weather_desc_strongwinds));
        } else if (data.wind_speed > 8.05) {
            result
                    .append(resources.getString(R.string.format_separator))
                    .append(resources.getString(R.string.weather_desc_breezy));
        }

        if (data.pop > 0) {
            result
                    .append(resources.getString(R.string.format_separator))
                    .append(resources.getString(R.string.format_precipitation_chance, LocaleUtils.getPercentageString(Locale.getDefault(), data.pop), data.getPrecipitationType()));
        }

        return result.toString();
    }

    public static double getConvertedTemperature(double tempFahrenheit) {
        return WeatherPreferences.getTemperatureUnit().equals(WeatherPreferences.TEMPERATURE_CELSIUS) ? (tempFahrenheit - 32) / 1.8 : tempFahrenheit;
    }

    public static String getPrecipitationString(double precipIntensity, Weather.PrecipType type) {
        boolean isImperial = WeatherPreferences.getSpeedUnit().equals(WeatherPreferences.SPEED_MPH);

        if (type == null) {
            type = Weather.PrecipType.RAIN;
        }

        return LocaleUtils.getDecimalString(Locale.getDefault(), isImperial ? precipIntensity / 25.4 : precipIntensity, 2) +
                (isImperial ? " in " : " mm ") +
                (type == Weather.PrecipType.RAIN ? resources.getString(R.string.weather_precip_rain) :
                        type == Weather.PrecipType.SNOW ? resources.getString(R.string.weather_precip_snow) :
                                resources.getString(R.string.weather_precip_mix));
    }

    public static String getTemperatureString(double temperature, int decimals) {
        return LocaleUtils.getDecimalString(Locale.getDefault(), getConvertedTemperature(temperature), decimals) + "\u00B0" + (WeatherPreferences.getTemperatureUnit().equals(WeatherPreferences.TEMPERATURE_CELSIUS) ? 'C' : 'F');
    }

    public static String getWindSpeedString(double windSpeed, int degrees) {
        String units = WeatherPreferences.getSpeedUnit();

        return LocaleUtils.getDecimalString(Locale.getDefault(), units.equals(WeatherPreferences.SPEED_KMH) ? windSpeed * 1.60934 : units.equals(WeatherPreferences.SPEED_MS) ? windSpeed * 0.44704 : windSpeed, 1) + " " +
                units + " " +
                getWindDirection(degrees);
    }

    private static String getWindDirection(int degrees) {
        //N, NNE, NE, ENE, E, ESE, SE, SSE, S, SSW, SW, WSW, W, WNW, NW, NNW

        while (degrees < 0) {
            degrees += 360;
        }

        final char[] cardinals = {'N', 'E', 'S', 'W'};

        final int bearing = (int) (((degrees % 360) + 11.24) / 22.5);

        StringBuilder directionBuilder = new StringBuilder(3);

        if (bearing % 2 == 1) {
            directionBuilder.append(cardinals[((bearing + 1) % 16) / 4]);
        }

        if (bearing % 8 != 4) {
            directionBuilder.append(bearing > 12 || bearing < 4 ? cardinals[0] : cardinals[2]);
        }

        if (bearing % 8 != 0) {
            directionBuilder.append(bearing < 8 ? cardinals[1] : cardinals[3]);
        }

        return directionBuilder.toString();
    }
}
