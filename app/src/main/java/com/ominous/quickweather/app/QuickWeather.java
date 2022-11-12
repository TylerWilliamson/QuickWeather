/*
 *     Copyright 2019 - 2022 Tyler Williamson
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

package com.ominous.quickweather.app;

import android.app.Application;
import android.util.Log;

import com.ominous.quickweather.util.WeatherUtils;
import com.ominous.quickweather.web.CachedWebServer;
import com.ominous.quickweather.work.WeatherWorkManager;
import com.ominous.tylerutils.browser.CustomTabs;

import androidx.annotation.NonNull;
import androidx.work.Configuration;

public class QuickWeather extends Application implements Configuration.Provider {
    private static CachedWebServer cachedWebServer;

    @Override
    public void onCreate() {
        super.onCreate();

        CustomTabs.getInstance(this);
        WeatherUtils.initialize(this);
        WeatherWorkManager.initialize(this);

        cachedWebServer = new CachedWebServer(4234);
        cachedWebServer.start();
    }

    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setMinimumLoggingLevel(Log.WARN)
                .build();
    }

    public static CachedWebServer getCachedWebServer() {
        return cachedWebServer;
    }
}
