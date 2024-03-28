/*
 *   Copyright 2019 - 2024 Tyler Williamson
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

import com.ominous.quickweather.R;
import com.ominous.quickweather.data.PrecipType;
import com.ominous.quickweather.pref.DistanceUnit;
import com.ominous.quickweather.pref.SpeedUnit;
import com.ominous.quickweather.pref.TemperatureUnit;
import com.ominous.tylerutils.util.LocaleUtils;
import com.ominous.tylerutils.util.StringUtils;

import java.util.Locale;

public class WeatherUtils {
    private static WeatherUtils instance;

    private final Resources resources;

    private WeatherUtils(Context context) {
        this.resources = context.getResources();
    }

    public static WeatherUtils getInstance(Context context) {
        if (instance == null) {
            instance = new WeatherUtils(context);
        }

        return instance;
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

    public String getWeatherDescription(String[] weatherDescriptions) {
        return getWeatherDescription(weatherDescriptions, 0, 0, 0, 0, null, false);
    }

    public String getWeatherDescription(String[] weatherDescription,
                                         double dewPoint,
                                         double windSpeed,
                                         int pop,
                                         double precipitationIntensity,
                                         PrecipType precipitationType,
                                         boolean asLongDescription) {
        StringBuilder result = new StringBuilder(weatherDescription[0]);

        for (int i = 1, l = weatherDescription.length; i < l; i++) {
            result
                    .append(resources.getString(R.string.format_separator))
                    .append(weatherDescription[i]);
        }

        if (asLongDescription) {
            if (dewPoint >= 60 && precipitationIntensity <= 0.01) {
                result
                        .append(resources.getString(R.string.format_separator))
                        .append(resources.getString(R.string.weather_desc_humid));
            } else if (dewPoint <= 35) {
                result
                        .append(resources.getString(R.string.format_separator))
                        .append(resources.getString(R.string.weather_desc_dry));
            }

            if (windSpeed > 25.3) {
                result
                        .append(resources.getString(R.string.format_separator))
                        .append(resources.getString(R.string.weather_desc_strongwinds));
            } else if (windSpeed > 8.05) {
                result
                        .append(resources.getString(R.string.format_separator))
                        .append(resources.getString(R.string.weather_desc_breezy));
            }

            if (pop > 0) {
                result
                        .append(resources.getString(R.string.format_separator))
                        .append(resources.getString(R.string.format_precipitation_chance,
                                LocaleUtils.getPercentageString(Locale.getDefault(), pop / 100.),
                                getPrecipitationTypeString(precipitationType)));
            }
        }

        return StringUtils.capitalizeEachWord(result.toString());
    }

    public double getPrecipitationIntensity(double rain, double snow) {
        return rain + snow;
    }

    public PrecipType getPrecipitationType(double rain, double snow) {
        return snow > 0 ?
                rain > 0 ?
                        PrecipType.MIX : PrecipType.SNOW : PrecipType.RAIN;
    }

    public String getPrecipitationTypeString(PrecipType precipType) {
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
                                         PrecipType type,
                                         boolean forAccessibility) {
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
                getPrecipitationTypeString(type == null ? PrecipType.RAIN : type));
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

        //N, NNE, NE, ENE, E, ESE, SE, SSE, S, SSW, SW, WSW, W, WNW, NW, NNW
        final String[] cardinals = forAccessibility ?
                resources.getStringArray(R.array.text_cardinal_direction) :
                resources.getStringArray(R.array.text_cardinal_direction_abbreviation);

        while (degrees < 0) {
            degrees += 360;
        }

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

        return resources.getString(unitsRes, amount, directionBuilder.toString());
    }

    public String getMoonPhaseString(double moonPhase) {
        int moonPhaseIndex;

        if (moonPhase < 0.1) {
            moonPhaseIndex = 0;
        } else if (moonPhase < 0.2) {
            moonPhaseIndex = 1;
        } else if (moonPhase < 0.3) {
            moonPhaseIndex = 2;
        } else if (moonPhase < 0.4) {
            moonPhaseIndex = 3;
        } else if (moonPhase < 0.6) {
            moonPhaseIndex = 4;
        } else if (moonPhase < 0.7) {
            moonPhaseIndex = 5;
        } else if (moonPhase < 0.8) {
            moonPhaseIndex = 6;
        } else if (moonPhase < 0.9) {
            moonPhaseIndex = 7;
        } else {
            moonPhaseIndex = 0;
        }

        return resources.getStringArray(R.array.text_moon_phases)[moonPhaseIndex];
    }
}