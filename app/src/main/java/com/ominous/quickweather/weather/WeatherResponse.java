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
        public double cloudCover;
        public double pressure;
        public double windSpeed;
        public double dewPoint;
        public int windBearing;
    }
}
