package com.ominous.quickweather.pref;

import android.content.SharedPreferences;

import androidx.lifecycle.MutableLiveData;

public class SharedPreferenceLiveData<T extends Enum<T> & IPreferenceEnum> extends MutableLiveData<T> {
    private final SharedPreferences sharedPreferences;
    private final String key;
    private final T defValue;

    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener =
            (sharedPreferences, key) -> {
                if (SharedPreferenceLiveData.this.key.equals(key)) {
                    setValue(convertFromPreference(key, SharedPreferenceLiveData.this.defValue));
                }
            };

    public SharedPreferenceLiveData(SharedPreferences prefs, String key, T defValue) {
        this.sharedPreferences = prefs;
        this.key = key;
        this.defValue = defValue;
    }

    private T convertFromPreference(String key, T defValue) {
        return IPreferenceEnum.from(sharedPreferences.getString(key, defValue.getValue()), defValue);
    }

    @Override
    protected void onActive() {
        super.onActive();
        setValue(convertFromPreference(key, defValue));
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    @Override
    protected void onInactive() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
        super.onInactive();
    }

    @Override
    public void setValue(T value) {
        super.setValue(value);

        sharedPreferences.edit().putString(key, value.getValue()).apply();
    }
}