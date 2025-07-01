package com.ominous.quickweather.pref;

public interface IPreferenceEnum {
    static <T extends Enum<T> & IPreferenceEnum> T from(String value, T defaultValue) {
        Object valuesObj = defaultValue.getClass().getEnumConstants();

        if (valuesObj != null) {
            try {
                //noinspection unchecked
                T[] values = (T[]) valuesObj;

                for (T v : values) {
                    if (v.getValue().equals(value)) {
                        return v;
                    }
                }
            } catch (ClassCastException e) {
                return defaultValue;
            }
        }

        return defaultValue;
    }

    String getValue();
}
