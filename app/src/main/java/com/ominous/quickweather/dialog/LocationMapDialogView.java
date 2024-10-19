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

package com.ominous.quickweather.dialog;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ominous.quickweather.R;
import com.ominous.quickweather.activity.ILifecycleAwareActivity;
import com.ominous.quickweather.data.WeatherDatabase;
import com.ominous.quickweather.view.WeatherMapView;

public class LocationMapDialogView extends FrameLayout {
    private WeatherDatabase.WeatherLocation weatherLocation =
            new WeatherDatabase.WeatherLocation(0, 0, null);

    private final WeatherMapView weatherMapView;

    public LocationMapDialogView(@NonNull Context context) {
        this(context, null, 0, 0);
    }

    public LocationMapDialogView(@NonNull Context context, @Nullable @org.jetbrains.annotations.Nullable AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public LocationMapDialogView(@NonNull Context context, @Nullable @org.jetbrains.annotations.Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public LocationMapDialogView(@NonNull Context context, @Nullable @org.jetbrains.annotations.Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        LayoutInflater.from(context).inflate(R.layout.dialog_map, this, true);

        weatherMapView = findViewById(R.id.map);

        weatherMapView.setOnMapClickListener(point -> {
            setWeatherLocation(point.getLatitude(), point.getLongitude());

            return true;
        });
    }

    public void attachToActivity(ILifecycleAwareActivity lifecycleAwareActivity) {
        weatherMapView.attachToActivity(lifecycleAwareActivity);
    }

    public WeatherDatabase.WeatherLocation getWeatherLocation() {
        return weatherLocation;
    }

    public void setWeatherLocation(double lat, double lon) {
        weatherLocation = new WeatherDatabase.WeatherLocation(lat, lon, null);
    }
}
