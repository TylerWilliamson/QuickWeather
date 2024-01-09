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

package com.ominous.quickweather.api.openweather;

import com.ominous.tylerutils.annotation.JSONFieldName;

import java.util.Calendar;
import java.util.TimeZone;

public class OpenWeatherForecast {
    public final long timestamp = Calendar.getInstance(TimeZone.getTimeZone("GMT")).getTimeInMillis();
    public ForecastData[] list;

    public static class ForecastData {
        public long dt;
        public MainData main;
        public WeatherData[] weather;
        public PrecipData rain;
        public PrecipData snow;
        public double pop;
    }

    public static class MainData {
        public double temp;
    }

    public static class WeatherData {
        public int id;
        public String icon;
        public String description;
    }

    public static class PrecipData {
        @JSONFieldName(name = "3h")
        public double volume;
    }
}
