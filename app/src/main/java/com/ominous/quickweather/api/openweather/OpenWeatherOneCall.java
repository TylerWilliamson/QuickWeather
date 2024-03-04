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

import java.io.Serializable;

@SuppressWarnings("WeakerAccess,unused")
public class OpenWeatherOneCall {
    public String timezone;
    public DataPoint current;
    public DailyData[] daily;
    public DataPoint[] hourly;
    public MinuteData[] minutely;
    public Alert[] alerts;

    public static class DataPoint {
        public long dt;
        public double temp;
        public double feels_like;
        public int visibility;
        public int humidity;
        public double wind_speed;
        public int wind_deg;
        public int pressure;
        public double dew_point;
        public double uvi;
        public double pop;
        public WeatherData[] weather;
        public PrecipData rain;
        public PrecipData snow;
    }

    public static class MinuteData {
        public long dt;
        public double precipitation;
    }

    public static class Alert implements Serializable {
        public String sender_name;
        public String event;
        public long start;
        public long end;
        public String description;
    }

    public static class DailyTemp {
        public double min;
        public double max;
    }

    public static class DailyData {
        public long dt;
        public DailyTemp temp;
        public int humidity;
        public int pressure;
        public double dew_point;
        public double wind_speed;
        public int wind_deg;
        public double pop;
        public WeatherData[] weather;
        public int weatherCode;
        public double rain;
        public double snow;
        public double uvi;
        public long sunrise;
        public long sunset;
        public long moonrise;
        public long moonset;
        public double moon_phase;
    }

    public static class WeatherData {
        public int id;
        public String icon;
        public String description;
    }

    public static class PrecipData {
        @JSONFieldName(name = "1h")
        public double volume;
    }
}