/*
 *     Copyright 2019 - 2021 Tyler Williamson
 *
 *     This file is part of QuickWeather.
 *
 *     QuickWeather is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     QuickWeather is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with QuickWeather.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.ominous.quickweather.activity;

import android.app.Application;
import android.util.Log;

import com.ominous.tylerutils.browser.CustomTabs;

import com.ominous.quickweather.util.ColorUtils;
import com.ominous.quickweather.util.GraphUtils;
import com.ominous.quickweather.util.NotificationUtils;
import com.ominous.quickweather.util.WeatherPreferences;
import com.ominous.quickweather.util.WeatherUtils;
import com.ominous.quickweather.work.WeatherWorkManager;

import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.Configuration;

public class QuickWeather extends Application implements Configuration.Provider {
    //TODO implement this better with a lifecycle
    private static WeakReference<MainActivity> mainActivityRef;
    private static ViewModelProvider viewModelProvider;

    @Override
    public void onCreate() {
        super.onCreate();

        CustomTabs.getInstance(this);
        GraphUtils.initialize(this);
        WeatherPreferences.initialize(this);
        WeatherUtils.initialize(this);
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

    public static MainActivity getMainActivity() {
        return mainActivityRef.get();
    }

    public static void setMainActivity(MainActivity mainActivity) {
        QuickWeather.mainActivityRef = new WeakReference<>(mainActivity);
    }

    public static ViewModelProvider getViewModelProvider() {
        if (viewModelProvider == null) {
            viewModelProvider = new ViewModelProvider(getMainActivity());
        }

        return viewModelProvider;
    }
}
