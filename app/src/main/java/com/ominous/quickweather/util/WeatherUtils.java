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
        return switch (unit) {
            case CELSIUS -> (tempFahrenheit - 32) / 1.8;
            case KELVIN -> (tempFahrenheit - 32) / 1.8 + 273.15;
            case FAHRENHEIT -> tempFahrenheit;
            default -> throw new IllegalArgumentException("Unit must not be DEFAULT");
        };

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
            case FTS:
                return speedMph * 22. / 15.;
            case BFT:
                if (speedMph < 1) {
                    return 0;
                } else if (speedMph < 3) {
                    return 1;
                } else if (speedMph < 7) {
                    return 2;
                } else if (speedMph < 12) {
                    return 3;
                } else if (speedMph < 18) {
                    return 4;
                } else if (speedMph < 24) {
                    return 5;
                } else if (speedMph < 31) {
                    return 6;
                } else if (speedMph < 38) {
                    return 7;
                } else if (speedMph < 46) {
                    return 8;
                } else if (speedMph < 54) {
                    return 9;
                } else if (speedMph < 63) {
                    return 10;
                } else if (speedMph < 72) {
                    return 11;
                } else if (speedMph < 80) {
                    return 12;
                } else if (speedMph < 92) {
                    return 13;
                } else if (speedMph < 103) {
                    return 14;
                } else if (speedMph < 114) {
                    return 15;
                } else if (speedMph < 125) {
                    return 16;
                } else {
                    return 17;
                }
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
        return switch (precipType) {
            case MIX -> resources.getString(R.string.weather_precip_mix);
            case RAIN -> resources.getString(R.string.weather_precip_rain);
            case SNOW -> resources.getString(R.string.weather_precip_snow);
        };
    }

    public String getPrecipitationString(DistanceUnit distanceUnit,
                                         double precipIntensity,
                                         PrecipType type,
                                         boolean forAccessibility) {
        int precipStringRes;
        double amount;

        if (forAccessibility) {
            precipStringRes = switch (distanceUnit) {
                case INCH -> {
                    amount = precipIntensity / 25.4;
                    yield R.string.format_precipitation_in_accessibility;
                }
                case MM -> {
                    amount = precipIntensity;
                    yield R.string.format_precipitation_mm_accessibility;
                }
                default ->
                        throw new IllegalArgumentException("Precipitation unit must be MM or IN");
            };
        } else {
            precipStringRes = switch (distanceUnit) {
                case INCH -> {
                    amount = precipIntensity / 25.4;
                    yield R.string.format_precipitation_in;
                }
                case MM -> {
                    amount = precipIntensity;
                    yield R.string.format_precipitation_mm;
                }
                default ->
                        throw new IllegalArgumentException("Precipitation unit must be MM or IN");
            };
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
            unitsRes = switch (unit) {
                case KN -> R.string.format_speed_kn_accessibility;
                case MS -> R.string.format_speed_ms_accessibility;
                case KMH -> R.string.format_speed_kmh_accessibility;
                case FTS -> R.string.format_speed_fts_accessibility;
                case BFT -> R.string.format_speed_bft_accessibility;
                default -> R.string.format_speed_mph_accessibility;
            };
        } else {
            unitsRes = switch (unit) {
                case KN -> R.string.format_speed_kn;
                case MS -> R.string.format_speed_ms;
                case KMH -> R.string.format_speed_kmh;
                case FTS -> R.string.format_speed_fts;
                case BFT -> R.string.format_speed_bft;
                default -> R.string.format_speed_mph;
            };
        }

        //N, NNE, NE, ENE, E, ESE, SE, SSE, S, SSW, SW, WSW, W, WNW, NW, NNW
        final String[] cardinals = forAccessibility ?
                new String[]{
                        resources.getString(R.string.text_direction_north),
                        resources.getString(R.string.text_direction_east),
                        resources.getString(R.string.text_direction_south),
                        resources.getString(R.string.text_direction_west)
                } :
                new String[]{
                        resources.getString(R.string.text_direction_abbreviation_north),
                        resources.getString(R.string.text_direction_abbreviation_east),
                        resources.getString(R.string.text_direction_abbreviation_south),
                        resources.getString(R.string.text_direction_abbreviation_west)
                };

        final String cardinalSpacer = resources.getString(forAccessibility ? R.string.text_direction_spacer : R.string.text_direction_abbreviation_spacer);

        while (degrees < 0) {
            degrees += 360;
        }

        final int bearing = (int) (((degrees % 360) + 11.24) / 22.5);

        StringBuilder directionBuilder = new StringBuilder(3);

        if (bearing % 2 == 1) {
            directionBuilder
                    .append(cardinals[((bearing + 1) % 16) / 4])
                    .append(cardinalSpacer);
        }

        if (bearing % 8 != 4) {
            directionBuilder
                    .append(bearing > 12 || bearing < 4 ? cardinals[0] : cardinals[2])
                    .append(cardinalSpacer);
        }

        if (bearing % 8 != 0) {
            directionBuilder
                    .append(bearing < 8 ? cardinals[1] : cardinals[3])
                    .append(cardinalSpacer);
        }

        return resources.getString(unitsRes, amount, directionBuilder.toString());
    }

    public String getMoonPhaseString(double moonPhase) {
        final int moonPhaseStringRes;

        if (moonPhase < 0.1) {
            moonPhaseStringRes = R.string.text_moon_phase_new;
        } else if (moonPhase < 0.2) {
            moonPhaseStringRes = R.string.text_moon_phase_waxingcrescent;
        } else if (moonPhase < 0.3) {
            moonPhaseStringRes = R.string.text_moon_phase_firstquarter;
        } else if (moonPhase < 0.4) {
            moonPhaseStringRes = R.string.text_moon_phase_waxinggibbous;
        } else if (moonPhase < 0.6) {
            moonPhaseStringRes = R.string.text_moon_phase_full;
        } else if (moonPhase < 0.7) {
            moonPhaseStringRes = R.string.text_moon_phase_waninggibbous;
        } else if (moonPhase < 0.8) {
            moonPhaseStringRes = R.string.text_moon_phase_lastquarter;
        } else if (moonPhase < 0.9) {
            moonPhaseStringRes = R.string.text_moon_phase_waningcrescent;
        } else {
            moonPhaseStringRes = R.string.text_moon_phase_new;
        }

        return resources.getString(moonPhaseStringRes);
    }

    public String getVisibilityString(DistanceUnit unit, double visibility, boolean forAccessibility) {
        int stringRes;
        double amount;

        if (forAccessibility) {
            stringRes = switch (unit) {
                case KM -> {
                    amount = visibility * 0.001;
                    yield R.string.format_distance_km_accessibility;
                }
                case MI -> {
                    amount = visibility * 0.00062137119;
                    yield R.string.format_distance_mi_accessibility;
                }
                case NMI -> {
                    amount = visibility * 0.000539956803;
                    yield R.string.format_distance_nmi_accessibility;
                }
                default ->
                        throw new IllegalArgumentException("Visibility unit must be MI, KM, or NMI");
            };
        } else {
            stringRes = switch (unit) {
                case KM -> {
                    amount = visibility * 0.001;
                    yield R.string.format_distance_km;
                }
                case MI -> {
                    amount = visibility * 0.00062137119;
                    yield R.string.format_distance_mi;
                }
                case NMI -> {
                    amount = visibility * 0.000539956803;
                    yield R.string.format_distance_nmi;
                }
                default ->
                        throw new IllegalArgumentException("Visibility unit must be MI, KM, or NMI");
            };
        }

        return resources.getString(stringRes, amount);
    }
}