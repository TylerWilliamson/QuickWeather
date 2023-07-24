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

package com.ominous.quickweather.util;

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.NonNull;

import com.ominous.quickweather.R;
import com.ominous.quickweather.api.OpenWeatherMap;
import com.ominous.quickweather.data.WeatherResponseOneCall;
import com.ominous.quickweather.pref.DistanceUnit;
import com.ominous.quickweather.pref.SpeedUnit;
import com.ominous.quickweather.pref.TemperatureUnit;
import com.ominous.tylerutils.util.LocaleUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

public class WeatherUtils {
    private static WeatherUtils instance;
    private final HashMap<String, Integer> codeToIcon = new HashMap<>();

    private final Resources resources;

    private WeatherUtils(Context context) {
        this.resources = context.getResources();

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

    public static WeatherUtils getInstance(Context context) {
        if (instance == null) {
            instance = new WeatherUtils(context);
        }

        return instance;
    }

    public int getIconFromCode(String icon, Integer weatherId) {
        Integer resId;

        resId = weatherId != null ? codeToIcon.get(weatherId.toString() + icon.charAt(2)) : null;
        resId = resId == null && icon != null ? codeToIcon.get(icon) : resId;
        resId = resId == null ? R.drawable.thermometer_25 : resId;

        return resId;
    }

    public String getCurrentWeatherDesc(@NonNull WeatherResponseOneCall responseOneCall,
                                        boolean asLongDescription) {
        double precipAmount = responseOneCall.current.getPrecipitationIntensity();

        StringBuilder result = new StringBuilder(responseOneCall.current.weather[0].description);

        for (int i = 1, l = responseOneCall.current.weather.length; i < l; i++) {
            result
                    .append(resources.getString(R.string.format_separator))
                    .append(responseOneCall.current.weather[i].description);
        }

        if (asLongDescription) {
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

            if (responseOneCall.minutely != null && responseOneCall.minutely.length > 0) {
                double startingPrecipitation = responseOneCall.minutely[0].precipitation;
                double endingPrecipitation = responseOneCall.minutely[responseOneCall.minutely.length - 1].precipitation;
                String precipType = getPrecipitationTypeString(responseOneCall.hourly != null && responseOneCall.hourly.length > 0 ? responseOneCall.hourly[0].getPrecipitationType() : OpenWeatherMap.PrecipType.RAIN);

                if (startingPrecipitation > 0 && endingPrecipitation == 0) {
                    for (int i = 0, l = responseOneCall.minutely.length; i < l; i++) {
                        if (responseOneCall.minutely[i].precipitation == 0) {
                            int mins = (int) (responseOneCall.minutely[i].dt - Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis() / 1000) / 60;

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
                    for (int i = 0, l = responseOneCall.minutely.length; i < l; i++) {
                        if (responseOneCall.minutely[i].precipitation > 0) {
                            int mins = (int) (responseOneCall.minutely[i].dt - Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis() / 1000) / 60;

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
        }

        return result.toString();
    }

    public String getForecastLongWeatherDesc(WeatherResponseOneCall.DailyData data) {
        StringBuilder result = new StringBuilder(data.weather[0].description);

        if (data.dew_point >= 60 && data.getPrecipitationIntensity() == 0) {
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
                    .append(resources.getString(R.string.format_precipitation_chance, LocaleUtils.getPercentageString(Locale.getDefault(), data.pop), getPrecipitationTypeString(data.getPrecipitationType())));
        }

        return result.toString();
    }

    public double getTemperature(TemperatureUnit unit, double tempFahrenheit) {
        switch (unit) {
            case CELSIUS:
                return (tempFahrenheit - 32) / 1.8;
            case KELVIN:
                return (tempFahrenheit - 32) / 1.8 + 273.15;
            case FAHRENHEIT:
                return tempFahrenheit;
        }

        throw new IllegalArgumentException("Unit must not be DEFAULT");
    }

    public double getSpeed(SpeedUnit unit, double speedMph) {
        switch (unit) {
            case KMH:
                return speedMph * 1.60934;
            case MS:
                return speedMph * 0.44704;
            case KN:
                return speedMph * 0.86897;
            case MPH:
                return speedMph;
        }

        throw new IllegalArgumentException("Unit must not be DEFAULT");
    }

    public String getPrecipitationTypeString(OpenWeatherMap.PrecipType precipType) {
        switch (precipType) {
            case MIX:
                return resources.getString(R.string.weather_precip_mix);
            case RAIN:
                return resources.getString(R.string.weather_precip_rain);
            case SNOW:
                return resources.getString(R.string.weather_precip_snow);
            default:
                return resources.getString(R.string.text_unknown);
        }
    }

    public String getPrecipitationString(DistanceUnit distanceUnit,
                                         double precipIntensity,
                                         OpenWeatherMap.PrecipType type,
                                         boolean forAccessibility)
    {
        int precipStringRes;
        double amount;

        if (distanceUnit == DistanceUnit.INCH) {
            precipStringRes = forAccessibility ?
                            R.string.format_precipitation_in_accessibility :
                            R.string.format_precipitation_in;
            amount = precipIntensity / 25.4;
        } else {
            precipStringRes = forAccessibility ?
                    R.string.format_precipitation_mm_accessibility :
                    R.string.format_precipitation_mm;
            amount = precipIntensity;
        }

        return resources.getString(precipStringRes,
                amount,
                getPrecipitationTypeString(type == null ? OpenWeatherMap.PrecipType.RAIN : type));
    }

    public String getTemperatureString(TemperatureUnit unit, double temperature, int decimals) {
        return resources.getString(unit == TemperatureUnit.CELSIUS ? R.string.format_temperature_celsius : R.string.format_temperature_fahrenheit,
                LocaleUtils.getDecimalString(Locale.getDefault(), getTemperature(unit, temperature), decimals));
    }

    public String getWindSpeedString(SpeedUnit unit, double windSpeed, int degrees, boolean forAccessibility) {
        double amount = getSpeed(unit, windSpeed);
        int unitsRes;

        if (forAccessibility) {
            switch (unit) {
                case KN:
                    unitsRes = R.string.format_speed_kn_accessibility;
                    break;
                case MS:
                    unitsRes = R.string.format_speed_ms_accessibility;
                    break;
                case KMH:
                    unitsRes = R.string.format_speed_kmh_accessibility;
                    break;
                default:
                    unitsRes = R.string.format_speed_mph_accessibility;
            }
        } else {
            switch (unit) {
                case KN:
                    unitsRes = R.string.format_speed_kn;
                    break;
                case MS:
                    unitsRes = R.string.format_speed_ms;
                    break;
                case KMH:
                    unitsRes = R.string.format_speed_kmh;
                    break;
                default:
                    unitsRes = R.string.format_speed_mph;
            }
        }

        return resources.getString(unitsRes, amount, getWindDirection(degrees, forAccessibility));
    }

    private String getWindDirection(int degrees, boolean forAccessibility) {
        //N, NNE, NE, ENE, E, ESE, SE, SSE, S, SSW, SW, WSW, W, WNW, NW, NNW

        while (degrees < 0) {
            degrees += 360;
        }

        final String[] cardinals = forAccessibility ?
                resources.getStringArray(R.array.text_cardinal_direction) :
                resources.getStringArray(R.array.text_cardinal_direction_abbreviation);

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