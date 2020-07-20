package com.ominous.quickweather.activity;

import android.app.Application;
import android.util.Log;

import com.ominous.tylerutils.browser.CustomTabs;

import com.ominous.quickweather.util.ColorUtils;
import com.ominous.quickweather.util.GraphUtils;
import com.ominous.quickweather.util.NotificationUtils;
import com.ominous.quickweather.util.WeatherPreferences;
import com.ominous.quickweather.util.WeatherUtils;
import com.ominous.quickweather.weather.Weather;
import com.ominous.quickweather.work.WeatherWorkManager;

import androidx.annotation.NonNull;
import androidx.work.Configuration;

public class QuickWeather extends Application implements Configuration.Provider {
    @Override
    public void onCreate() {
        super.onCreate();

        CustomTabs.getInstance(this);
        GraphUtils.initialize(this);
        WeatherPreferences.initialize(this);
        WeatherUtils.initialize(this);
        Weather.initialize(this);
        NotificationUtils.initialize(this);
        WeatherWorkManager.initialize(this);

        ColorUtils.setNightMode(this);
    }

    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setMinimumLoggingLevel(Log.WARN)
                .build();
    }
}
