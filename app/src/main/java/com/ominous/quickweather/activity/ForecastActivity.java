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

package com.ominous.quickweather.activity;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.ominous.quickweather.R;
import com.ominous.quickweather.data.CurrentWeather;
import com.ominous.quickweather.data.WeatherModel;
import com.ominous.quickweather.util.ColorHelper;
import com.ominous.tylerutils.browser.CustomTabs;
import com.ominous.tylerutils.util.ColorUtils;
import com.ominous.tylerutils.util.LocaleUtils;
import com.ominous.tylerutils.util.WindowUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ForecastActivity extends BaseActivity {
    public final static String EXTRA_DATE = "EXTRA_DATE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        onReceiveIntent(getIntent());

        initViews();
        initViewModel();
    }

    protected void onReceiveIntent(Intent intent) {
        Bundle bundle;
        long timestamp;

        if ((bundle = intent.getExtras()) != null &&
                (timestamp = bundle.getLong(EXTRA_DATE)) != 0) {
            date = new Date(timestamp);
        } else {
            finish();
        }
    }

    @Override
    public void finish() {
        super.finish();
        this.overridePendingTransition(R.anim.slide_left_in, R.anim.slide_right_out);
    }

    @Override
    protected void initViewModel() {
        super.initViewModel();

        weatherViewModel.getForecastLayoutCardsModel().observe(this,
                cards -> weatherCardRecyclerView.setCardSections(cards));
    }

    @Override
    protected void updateWeather(WeatherModel weatherModel) {
        super.updateWeather(weatherModel);

        long thisDate = LocaleUtils.getStartOfDay(date, weatherModel.currentWeather.timezone);

        boolean isToday = false;
        CurrentWeather.DataPoint thisDailyData = null;
        for (int i = 0, l = weatherModel.currentWeather.daily.length; i < l; i++) {
            CurrentWeather.DataPoint dailyData = weatherModel.currentWeather.daily[i];

            if (LocaleUtils.getStartOfDay(new Date(dailyData.dt), weatherModel.currentWeather.timezone) == thisDate) {
                thisDailyData = dailyData;

                isToday = i == 0;
                i = l;
            }
        }

        if (thisDailyData != null) {
            Calendar calendar = Calendar.getInstance(Locale.getDefault());
            calendar.setTimeInMillis(thisDailyData.dt);
            calendar.setTimeZone(weatherModel.currentWeather.timezone);

            toolbar.setTitle(getString(R.string.format_forecast_title,
                    isToday ? getString(R.string.text_today) : calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()),
                    weatherModel.weatherLocation.isCurrentLocation ? getString(R.string.text_current_location) : weatherModel.weatherLocation.name));
            toolbar.setSubtitle(LocaleUtils.formatDateTime(
                    this,
                    Locale.getDefault(),
                    new Date(weatherModel.currentWeather.timestamp),
                    weatherModel.currentWeather.timezone));

            toolbar.setContentDescription(getString(R.string.format_forecast_title,
                    isToday ? getString(R.string.text_today) : calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()),
                    weatherModel.weatherLocation.isCurrentLocation ? getString(R.string.text_current_location) : weatherModel.weatherLocation.name));

            ColorHelper colorHelper = ColorHelper.getInstance(this);

            int color = colorHelper.getColorFromTemperature(
                    (thisDailyData.minTemp + thisDailyData.maxTemp) / 2,
                    false,
                    ColorUtils.isNightModeActive(this));
            int darkColor = ColorUtils.getDarkenedColor(color);
            int textColor = colorHelper.getTextColor(color);

            toolbar.setBackgroundColor(color);
            toolbar.setTitleTextColor(textColor);
            toolbar.setSubtitleTextColor(textColor);

            if (weatherModel.weatherLocation.isCurrentLocation) {
                toolbarMyLocation.setImageTintList(ColorStateList.valueOf(textColor));
                toolbarMyLocation.setVisibility(View.VISIBLE);
            } else {
                toolbarMyLocation.setVisibility(View.GONE);
            }

            Drawable navIcon = toolbar.getNavigationIcon();

            if (navIcon != null) {
                navIcon.setColorFilter(textColor, PorterDuff.Mode.SRC_IN);
                toolbar.setNavigationIcon(navIcon);
            }

            getWindow().setStatusBarColor(darkColor);
            getWindow().setNavigationBarColor(color);

            CustomTabs.getInstance(this).setColor(color);

            WindowUtils.setLightNavBar(getWindow(), textColor == colorHelper.COLOR_TEXT_BLACK);
        }
    }

    @Override
    protected void initViews() {
        super.initViews();

        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener((a) -> getOnBackPressedDispatcher().onBackPressed());
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }
}