/*
 *   Copyright 2019 - 2024 Tyler Williamson
 *
 *   This file is part of QuickWeather.
 *
 *   QuickWeather is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   QuickWeather is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with QuickWeather.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.ominous.quickweather.app;

import android.app.Application;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Configuration;

import com.ominous.quickweather.util.ColorHelper;
import com.ominous.quickweather.util.SSLHelper;

public class QuickWeather extends Application implements Configuration.Provider {
    @Override
    public void onCreate() {
        super.onCreate();

        //We need to instantiate a ColorHelper in the Application to properly set day/night and
        //cache the correct colors. This has to happen in the Application because a WebView is
        //created to set the day/night due to an Android bug
        ColorHelper
                .getInstance(this)
                .setNightMode(this);

        //The root Certificate Authority for Lets Encrypt is expired on older Android devices.
        //This Certificate Authority is used for Open-Meteo. I have bundled the latest root CA,
        //which expires in 2035.
        if (Build.VERSION.SDK_INT <= 24) {
            SSLHelper.addLetsEncryptRootCA(this);
        }
    }

    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setMinimumLoggingLevel(Log.DEBUG)
                .build();
    }
}
