/*
 *   Copyright 2019 - 2023 Tyler Williamson
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

package com.ominous.quickweather.activity;

import android.app.ActivityManager;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.ominous.quickweather.R;
import com.ominous.quickweather.data.WeatherDatabase;
import com.ominous.quickweather.pref.WeatherPreferences;
import com.ominous.quickweather.util.ColorHelper;
import com.ominous.quickweather.web.CachedWebServer;
import com.ominous.tylerutils.async.Promise;

import java.util.concurrent.ExecutionException;

public abstract class BaseActivity extends AppCompatActivity {
    private LifecycleListener lifecycleListener = null;

    private static CachedWebServer cachedWebServer;


    @Override
    protected void onStart() {
        super.onStart();

        if (lifecycleListener != null) {
            lifecycleListener.onStart();
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (cachedWebServer == null) {
            cachedWebServer = CachedWebServer.getInstance();
            cachedWebServer.start();
        }

        openSettingsIfNotInitialized();

        ColorHelper
                .getInstance(this)
                .setNightMode(this);

        setTaskDescription(
                Build.VERSION.SDK_INT >= 28 ?
                        new ActivityManager.TaskDescription(
                                getString(R.string.app_name),
                                R.mipmap.ic_launcher_round,
                                ContextCompat.getColor(this, R.color.color_app_accent)) :
                        new ActivityManager.TaskDescription(
                                getString(R.string.app_name),
                                BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_round),
                                ContextCompat.getColor(this, R.color.color_app_accent)));

        if (lifecycleListener != null) {
            lifecycleListener.onCreate(savedInstanceState);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (lifecycleListener != null) {
            lifecycleListener.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        openSettingsIfNotInitialized();

        ColorHelper
                .getInstance(this)
                .setNightMode(this);

        if (lifecycleListener != null) {
            lifecycleListener.onResume();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (lifecycleListener != null) {
            lifecycleListener.onStop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (lifecycleListener != null) {
            lifecycleListener.onDestroy();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        onReceiveIntent(intent);
    }

    abstract void onReceiveIntent(Intent intent);

    private void openSettingsIfNotInitialized() {
        if (!isInitialized()) {
            ContextCompat.startActivity(this, new Intent(this, SettingsActivity.class), null);
            finish();
        }
    }

    private boolean isInitialized() {
        try {
            return Promise.create((a) -> WeatherPreferences.getInstance(this).isInitialized() &&
                    WeatherDatabase.getInstance(this).locationDao().getCount() > 0).await();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        if (lifecycleListener != null) {
            lifecycleListener.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();

        if (lifecycleListener != null) {
            lifecycleListener.onLowMemory();
        }
    }

    public void setLifecycleListener(LifecycleListener lifecycleListener) {
        this.lifecycleListener = lifecycleListener;
    }

    public interface LifecycleListener {
        void onStart();

        void onCreate(Bundle savedInstanceState);

        void onResume();

        void onPause();

        void onStop();

        void onDestroy();

        void onSaveInstanceState(Bundle outState);

        void onLowMemory();
    }
}
