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

public class OpenMeteoForecast {
    public String timezone;
    public HourlyData hourly;
    public DailyData daily;
    public CurrentData current_weather;

    public static class HourlyData {
        public long[] time;
        public int[] relativehumidity_2m;
        public double[] dewpoint_2m;
        public double[] apparent_temperature;
        public double[] rain;
        public double[] showers;
        public double[] snowfall;
        public double[] temperature_2m;
        public double[] surface_pressure;
        public double[] visibility;
        public double[] uv_index;
        public int[] is_day;
        public int[] weathercode;
        public int[] precipitation_probability;
        public double[] windspeed_10m;
        public int[] winddirection_10m;
    }

    public static class CurrentData {
        public double temperature;
        public double windspeed;
        public int winddirection;
        public int weathercode;
    }

    public static class DailyData {
        public long[] time;
        public int[] weathercode;
        public double[] temperature_2m_max;
        public double[] temperature_2m_min;
        public double[] uv_index_max;
        public double[] rain_sum;
        public double[] showers_sum;
        public double[] snowfall_sum;
        public int[] precipitation_probability_max;
        public double[] windspeed_10m_max;
        public int[] winddirection_10m_dominant;
    }
}
