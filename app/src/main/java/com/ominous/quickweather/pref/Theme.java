package com.ominous.quickweather.pref;

public enum Theme {
    LIGHT("light"),
    DARK("dark"),
    AUTO("auto"),
    DEFAULT("");

    private final String value;

    Theme(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Theme from(String value, Theme defaultValue) {
        for (Theme v : values()) {
            if (v.getValue().equals(value)) {
                return v;
            }
        }

        return defaultValue;
    }
}
