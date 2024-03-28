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

package com.ominous.quickweather.card;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.TextView;

import com.ominous.quickweather.R;
import com.ominous.quickweather.pref.WeatherPreferences;
import com.ominous.quickweather.util.ColorHelper;
import com.ominous.quickweather.util.WeatherUtils;

public abstract class BaseDetailCardView extends BaseCardView {
    protected final TextView forecastItem1;
    protected final TextView forecastItem1Spacer;
    protected final TextView forecastItem2;
    protected final TextView forecastItem2Spacer;
    protected final TextView forecastTitle;
    protected final TextView forecastTitleSpacer;

    protected final TextView forecastDescription;
    protected final ImageView forecastIcon;
    protected final HorizontalScrollView scrollView;

    protected final ColorHelper colorHelper;
    protected final WeatherUtils weatherUtils;
    protected final WeatherPreferences weatherPreferences;

    private float initialX;

    public BaseDetailCardView(Context context) {
        super(context);

        inflate(context, R.layout.card_detail, this);

        forecastItem1 = findViewById(R.id.forecast_item1);
        forecastItem1Spacer = findViewById(R.id.forecast_item1_spacer);
        forecastItem2 = findViewById(R.id.forecast_item2);
        forecastItem2Spacer = findViewById(R.id.forecast_item2_spacer);
        forecastTitle = findViewById(R.id.forecast_title);
        forecastTitleSpacer = findViewById(R.id.forecast_title_spacer);

        forecastDescription = findViewById(R.id.forecast_desc);
        forecastIcon = findViewById(R.id.forecast_icon);
        scrollView = findViewById(R.id.scrollview);

        colorHelper = ColorHelper.getInstance(getContext());
        weatherUtils = WeatherUtils.getInstance(getContext());
        weatherPreferences = WeatherPreferences.getInstance(getContext());
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scrollView.onTouchEvent(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                if (Math.abs(event.getX() - initialX) > ViewConfiguration.get(getContext()).getScaledTouchSlop()) {
                    //Swipe detected, do not click
                    return true;
                }
            case MotionEvent.ACTION_DOWN:
                initialX = event.getX();
            default:
                return super.onTouchEvent(event);
        }
    }
}
