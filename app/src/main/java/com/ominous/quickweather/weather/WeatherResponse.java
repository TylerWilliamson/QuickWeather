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

package com.ominous.quickweather.weather;

import java.io.Serializable;

@SuppressWarnings("WeakerAccess")
public class WeatherResponse {
    public double latitude;
    public double longitude;
    public String timezone;
    public DataPoint currently;
    public DataBlock daily;
    public DataBlock hourly;
    public Alert[] alerts;

    public static class DataBlock { //https://darksky.net/dev/docs#data-block
        public DataPoint[] data;
    }

    public static class Alert implements Serializable { //https://darksky.net/dev/docs#alerts
        public static final String TEXT_WATCH = "watch", TEXT_WARNING = "warning", TEXT_ADVISORY = "advisory";

        public String title;
        public String severity;
        public String description;
        public String uri;
        //public long time; //no see
        public long expires;

        public int getId() {
            return uri.hashCode();
        }
    }

    public static class DataPoint { //https://darksky.net/dev/docs#data-point
        public static final String PRECIP_RAIN = "rain", PRECIP_SNOW = "snow", PRECIP_MIX = "sleet";

        public long time; //no see
        public String summary;
        public String icon;
        public String precipType = PRECIP_RAIN;
        public double precipIntensity;
        public double temperature;
        public double temperatureMax;
        public double temperatureMin;
        public double humidity;
        public double uvIndex;
        //public double cloudCover;
        public double pressure;
        public double windSpeed;
        public double dewPoint;
        public int windBearing;
    }
}
