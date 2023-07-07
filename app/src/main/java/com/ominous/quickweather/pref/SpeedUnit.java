package com.ominous.quickweather.pref;

public enum SpeedUnit {
    MPH("mph"),
    MS("m/s"),
    KMH("km/h"),
    KN("kn"),
    DEFAULT("");

    private final String value;

    SpeedUnit(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static SpeedUnit from(String value, SpeedUnit defaultValue) {
        for (SpeedUnit v : values()) {
            if (v.getValue().equals(value)) {
                return v;
            }
        }

        return defaultValue;
    }
}
