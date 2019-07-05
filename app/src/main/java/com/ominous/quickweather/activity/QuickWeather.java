package com.ominous.quickweather.activity;

import android.app.Application;

import com.ominous.quickweather.util.ColorUtils;
import com.ominous.quickweather.util.CustomTabs;
import com.ominous.quickweather.util.GraphUtils;
import com.ominous.quickweather.util.NotificationUtil;
import com.ominous.quickweather.util.WeatherPreferences;
import com.ominous.quickweather.util.WeatherUtils;
import com.ominous.quickweather.weather.Weather;
import com.ominous.quickweather.weather.WeatherWorkManager;

public class QuickWeather extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        CustomTabs.getInstance(this);
        ColorUtils.initialize(this);
        GraphUtils.initialize(this);
        WeatherPreferences.initialize(this);
        WeatherUtils.initialize(this);
        Weather.initialize(this);
        NotificationUtil.initialize(this);
        WeatherWorkManager.initialize(this);

        ColorUtils.setNightMode(this);
    }
}
