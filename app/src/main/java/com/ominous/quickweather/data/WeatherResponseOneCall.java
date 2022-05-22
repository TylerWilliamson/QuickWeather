/*
 *     Copyright 2019 - 2022 Tyler Williamson
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

import java.io.Serializable;
import java.util.Calendar;
import java.util.TimeZone;

@SuppressWarnings("WeakerAccess,unused")
public class WeatherResponseOneCall {
    public final long timestamp = Calendar.getInstance(TimeZone.getTimeZone("GMT")).getTimeInMillis();
    public double lat;
    public double lon;
    public String timezone;
    public DataPoint current;
    public DailyData[] daily;
    public DataPoint[] hourly;
    public MinuteData[] minutely;
    public Alert[] alerts;

    public static class DataPoint {
        public long dt;
        public double temp;
        public int humidity;
        public double wind_speed;
        public int wind_deg;
        public int pressure;
        public double dew_point;
        public double uvi;
        //public int clouds;
        public double pop;
        public WeatherData[] weather;
        public PrecipData rain;
        public PrecipData snow;

        public double getPrecipitationIntensity() {
            return (this.rain == null ? 0 : this.rain.volume) + (this.snow == null ? 0 : this.snow.volume);
        }

        public OpenWeatherMap.PrecipType getPrecipitationType() {
            return (this.snow == null ? 0 : this.snow.volume) == 0 ? OpenWeatherMap.PrecipType.RAIN :
                    (this.rain == null ? 0 : this.rain.volume) == 0 ? OpenWeatherMap.PrecipType.SNOW :
                            OpenWeatherMap.PrecipType.MIX;
        }
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
                    (sender_name != null && !sender_name.isEmpty() ? "<br>Via " + sender_name : "");
        }

        public String getPlainFormattedDescription() {
            return getHTMLFormattedDescription()
                    .replaceAll("<br>", "\n")
                    .replaceAll("<.+?>", "");
        }

        public OpenWeatherMap.AlertSeverity getSeverity() {
            return event.toLowerCase().contains("warning") ? OpenWeatherMap.AlertSeverity.WARNING :
                    event.toLowerCase().contains("watch") ? OpenWeatherMap.AlertSeverity.WATCH : OpenWeatherMap.AlertSeverity.ADVISORY;
        }
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
        public double rain;
        public double snow;
        public double uvi;

        public double getPrecipitationIntensity() {
            return rain + snow;
        }

        public OpenWeatherMap.PrecipType getPrecipitationType() {
            return snow == 0 ? OpenWeatherMap.PrecipType.RAIN :
                    rain == 0 ? OpenWeatherMap.PrecipType.SNOW :
                            OpenWeatherMap.PrecipType.MIX;
        }
    }

    public static class WeatherData {
        public int id;
        //public String main;
        public String icon;
        public String description;
    }

    public static class PrecipData {
        @JSONFieldName(name = "1h")
        public double volume;
    }
}