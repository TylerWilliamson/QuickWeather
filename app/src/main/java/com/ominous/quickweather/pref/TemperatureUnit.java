package com.ominous.quickweather.pref;

public enum TemperatureUnit {
    KELVIN("kelvin"),
    FAHRENHEIT("fahrenheit"),
    CELSIUS("celsius"),
    DEFAULT("");

    private final String value;

    TemperatureUnit(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static TemperatureUnit from(String value, TemperatureUnit defaultValue) {
        for (TemperatureUnit v : values()) {
            if (v.getValue().equals(value)) {
                return v;
            }
        }

        return defaultValue;
    }
}
