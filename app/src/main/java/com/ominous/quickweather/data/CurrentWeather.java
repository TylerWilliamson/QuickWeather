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

package com.ominous.quickweather.data;

import java.io.Serializable;
import java.util.TimeZone;

import androidx.annotation.DrawableRes;

public class CurrentWeather {
    public long timestamp;
    public TimeZone timezone;
    public DataPoint current;
    public DataPoint[] daily;
    public DataPoint[] hourly;
    public DataPoint[] trihourly;
    public Alert[] alerts;

    public static class DataPoint {
        public final long dt;
        public double minTemp; // Fahrenheit
        public double maxTemp; // Fahrenheit
        public double temp; // Fahrenheit
        public double feelsLike; // Fahrenheit
        public int visibility; // meters, max of 10000
        public int humidity; // percent, 0-100
        public double windSpeed; // mph
        public int windDeg; // bearing, degrees from north
        public int pressure; // hPa
        public double dewPoint; // Fahrenheit
        public double uvi; // index, 0-11+
        public int pop; // percent, 0-100
        public int weatherCode; //OpenWeatherMap weather code
        @DrawableRes
        public int weatherIconRes;
        public String weatherDescription;
        public String weatherLongDescription;
        public final double precipitationIntensity; // mm
        public final PrecipType precipitationType;

        //current
        public DataPoint(
                long dt,
                double temp,
                double feelsLike,
                int visibility,
                int humidity,
                double windSpeed,
                int windDeg,
                int pressure,
                double dewPoint,
                double uvi,
                int weatherCode,
                int weatherIconRes,
                String weatherDescription,
                String weatherLongDescription,
                double precipitationIntensity,
                PrecipType precipitationType) {
            this.dt = dt;
            this.temp = temp;
            this.feelsLike = feelsLike;
            this.visibility = visibility;
            this.humidity = humidity;
            this.windSpeed = windSpeed;
            this.windDeg = windDeg;
            this.pressure = pressure;
            this.dewPoint = dewPoint;
            this.uvi = uvi;
            this.weatherCode = weatherCode;
            this.weatherIconRes = weatherIconRes;
            this.weatherDescription = weatherDescription;
            this.weatherLongDescription = weatherLongDescription;
            this.precipitationIntensity = precipitationIntensity;
            this.precipitationType = precipitationType;
        }

        //daily
        public DataPoint(
                long dt,
                double maxTemp,
                double minTemp,
                int humidity,
                double windSpeed,
                int windDeg,
                int pressure,
                double dewPoint,
                double uvi,
                int pop,
                int weatherCode,
                int weatherIconRes,
                String weatherDescription,
                String weatherLongDescription,
                double precipitationIntensity,
                PrecipType precipitationType) {
            this.dt = dt;
            this.maxTemp = maxTemp;
            this.minTemp = minTemp;
            this.humidity = humidity;
            this.windSpeed = windSpeed;
            this.windDeg = windDeg;
            this.pressure = pressure;
            this.dewPoint = dewPoint;
            this.uvi = uvi;
            this.pop = pop;
            this.weatherCode = weatherCode;
            this.weatherIconRes = weatherIconRes;
            this.weatherDescription = weatherDescription;
            this.weatherLongDescription = weatherLongDescription;
            this.precipitationIntensity = precipitationIntensity;
            this.precipitationType = precipitationType;
        }

        //hourly
        public DataPoint(
                long dt,
                double temp,
                int weatherCode,
                int humidity,
                double windSpeed,
                int windDeg,
                double uvi,
                int pop,
                double precipitationIntensity,
                PrecipType precipitationType) {
            this.dt = dt;
            this.temp = temp;
            this.weatherCode = weatherCode;
            this.humidity = humidity;
            this.windSpeed = windSpeed;
            this.windDeg = windDeg;
            this.uvi = uvi;
            this.pop = pop;
            this.precipitationIntensity = precipitationIntensity;
            this.precipitationType = precipitationType;
        }


        //trihourly
        public DataPoint(
                long dt,
                double temp,
                int weatherIconRes,
                String weatherDescription,
                int pop,
                double precipitationIntensity,
                PrecipType precipitationType) {
            this.dt = dt;
            this.temp = temp;
            this.weatherIconRes = weatherIconRes;
            this.weatherDescription = weatherDescription;
            this.pop = pop;
            this.precipitationIntensity = precipitationIntensity;
            this.precipitationType = precipitationType;
        }
    }

    //TODO clean up alert
    public static class Alert implements Serializable {
        public String senderName;
        public String event;
        public long start;
        public long end;
        public String description;

        public String getUri() {
            return event + ' ' + start;
        }

        public int getId() {
            return getUri().hashCode();
        }

        public String getHTMLFormattedDescription() {
            return description
                    .replaceAll("\\n\\*", "<br>*")
                    .replaceAll("\\n\\.", "<br>.")
                    .replaceAll("\\n", " ") +
                    (senderName != null && !senderName.isEmpty() ? "<br>Via " + senderName : "");
        }

        public String getPlainFormattedDescription() {
            return getHTMLFormattedDescription()
                    .replaceAll("<br>", "\n")
                    .replaceAll("<.+?>", "");
        }

        public AlertSeverity getSeverity() {
            return event.toLowerCase().contains("warning") ? AlertSeverity.WARNING :
                    event.toLowerCase().contains("watch") ? AlertSeverity.WATCH : AlertSeverity.ADVISORY;
        }
    }
}