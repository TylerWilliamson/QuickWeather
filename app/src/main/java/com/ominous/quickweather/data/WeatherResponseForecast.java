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

package com.ominous.quickweather.data;

import com.ominous.quickweather.api.OpenWeatherMap;
import com.ominous.tylerutils.annotation.JSONFieldName;

import java.util.Calendar;
import java.util.TimeZone;

@SuppressWarnings("WeakerAccess,unused")
public class WeatherResponseForecast {
    public final long timestamp = Calendar.getInstance(TimeZone.getTimeZone("GMT")).getTimeInMillis();
    public ForecastData[] list;

    public static class ForecastData {
        public long dt;
        public MainData main;
        public WeatherData[] weather;
        public PrecipData rain;
        public PrecipData snow;
        public double pop;

        public double getPrecipitationIntensity() {
            return (this.rain == null ? 0 : this.rain.volume) + (this.snow == null ? 0 : this.snow.volume);
        }

        public OpenWeatherMap.PrecipType getPrecipitationType() {
            return (this.snow == null ? 0 : this.snow.volume) == 0 ? OpenWeatherMap.PrecipType.RAIN :
                    (this.rain == null ? 0 : this.rain.volume) == 0 ? OpenWeatherMap.PrecipType.SNOW :
                            OpenWeatherMap.PrecipType.MIX;
        }
    }

    public static class MainData {
        public double temp;
    }

    public static class WeatherData {
        public int id;
        //public String main;
        public String icon;
        public String description;
    }

    public static class PrecipData {
        @JSONFieldName(name = "3h")
        public double volume;
    }
}
