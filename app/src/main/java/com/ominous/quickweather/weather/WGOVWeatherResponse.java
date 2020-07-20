package com.ominous.quickweather.weather;

@SuppressWarnings("WeakerAccess")
public class WGOVWeatherResponse {

    public Feature[] features;

    public static class Feature {
        public String id;
        public Properties properties;
    }

    public static class Properties {
        public String ends;
        public String expires;
        public String event;
        public String description;
        public String instruction;
    }
}
