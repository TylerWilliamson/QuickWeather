package com.ominous.quickweather.activity;

import android.app.Application;

import com.ominous.quickweather.util.ColorUtils;
import com.ominous.quickweather.util.CustomTabs;
import com.ominous.quickweather.util.WeatherPreferences;
import com.ominous.quickweather.util.WeatherUtils;

public class QuickWeather extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        CustomTabs.getInstance(this);
        ColorUtils.initialize(this);
        WeatherPreferences.initialize(this);
        WeatherUtils.initialize(this);

        ColorUtils.setNightMode(this);
    }
}
