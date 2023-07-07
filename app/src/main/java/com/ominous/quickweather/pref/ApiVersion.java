package com.ominous.quickweather.pref;

public enum ApiVersion {
    ONECALL_3_0("onecall3.0"),
    ONECALL_2_5("onecall2.5"),
    WEATHER_2_5("weather2.5"),
    DEFAULT("");

    private final String value;

    ApiVersion(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ApiVersion from(String value, ApiVersion defaultValue) {
        for (ApiVersion v : values()) {
            if (v.getValue().equals(value)) {
                return v;
            }
        }

        return defaultValue;
    }
}
