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

package com.ominous.quickweather.card;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.TextView;

import com.ominous.quickweather.R;
import com.ominous.quickweather.activity.ForecastActivity;
import com.ominous.quickweather.data.WeatherModel;
import com.ominous.quickweather.data.WeatherResponseOneCall;
import com.ominous.quickweather.pref.TemperatureUnit;
import com.ominous.quickweather.pref.WeatherPreferences;
import com.ominous.quickweather.util.ColorHelper;
import com.ominous.quickweather.util.WeatherUtils;
import com.ominous.tylerutils.util.ColorUtils;
import com.ominous.tylerutils.util.StringUtils;
import com.ominous.tylerutils.util.ViewUtils;

import java.util.Calendar;
import java.util.Locale;

public class CurrentDetailCardView extends BaseCardView {
    private final TextView forecastTemperatureMin;
    private final TextView forecastTemperatureMax;
    private final TextView forecastTitle;
    private final TextView forecastDescription;
    private final ImageView forecastIcon;
    private final Calendar calendar = Calendar.getInstance(Locale.getDefault());
    private final HorizontalScrollView scrollView;

    public CurrentDetailCardView(Context context) {
        super(context);

        inflate(context, R.layout.card_current_forecast, this);

        forecastTemperatureMin = findViewById(R.id.forecast_temperature_min);
        forecastTemperatureMax = findViewById(R.id.forecast_temperature_max);
        forecastTitle = findViewById(R.id.forecast_title);
        forecastDescription = findViewById(R.id.forecast_desc);
        forecastIcon = findViewById(R.id.forecast_icon);
        scrollView = findViewById(R.id.scrollview);

        TextView forecastTemperatureMinSpacer = findViewById(R.id.forecast_temperature_min_spacer);
        TextView forecastTemperatureMaxSpacer = findViewById(R.id.forecast_temperature_max_spacer);

        WeatherUtils weatherUtils = WeatherUtils.getInstance(getContext());
        TemperatureUnit temperatureUnit = WeatherPreferences.getInstance(getContext()).getTemperatureUnit();
        String spacerText = weatherUtils.getTemperatureString(temperatureUnit, 100, 0);

        forecastTemperatureMinSpacer.setText(spacerText);
        forecastTemperatureMaxSpacer.setText(spacerText);

        ViewUtils.setAccessibilityInfo(this, context.getString(R.string.format_label_open, context.getString(R.string.forecast_desc)), null);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scrollView.onTouchEvent(event);

        return super.onTouchEvent(event);
    }

    @Override
    public void update(WeatherModel weatherModel, int position) {
        WeatherUtils weatherUtils = WeatherUtils.getInstance(getContext());
        ColorHelper colorHelper = ColorHelper.getInstance(getContext());
        TemperatureUnit temperatureUnit = WeatherPreferences.getInstance(getContext()).getTemperatureUnit();
        boolean isDarkModeActive = ColorUtils.isNightModeActive(getContext());

        int day = position - (3 + (weatherModel.responseOneCall.alerts == null ? 0 : weatherModel.responseOneCall.alerts.length));

        WeatherResponseOneCall.DailyData data = weatherModel.responseOneCall.daily[day];

        calendar.setTimeInMillis(data.dt * 1000);

        forecastIcon.setImageResource(weatherUtils.getIconFromCode(data.weather[0].icon, data.weather[0].id));

        forecastTitle.setText(day == 0 ? getContext().getString(R.string.text_today) : calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()));

        forecastTemperatureMax.setText(weatherUtils.getTemperatureString(temperatureUnit, data.temp.max, 0));
        forecastTemperatureMax.setTextColor(colorHelper.getColorFromTemperature(data.temp.max, true, isDarkModeActive));

        forecastTemperatureMin.setText(weatherUtils.getTemperatureString(temperatureUnit, data.temp.min, 0));
        forecastTemperatureMin.setTextColor(colorHelper.getColorFromTemperature(data.temp.min, true, isDarkModeActive));

        forecastDescription.setText(StringUtils.capitalizeEachWord(data.weather[0].description));

        setContentDescription(getContext().getString(R.string.format_current_forecast_desc,
                day == 0 ? getContext().getString(R.string.text_today) : calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()),
                data.weather[0].description,
                weatherUtils.getTemperatureString(temperatureUnit, data.temp.max, 0),
                weatherUtils.getTemperatureString(temperatureUnit, data.temp.min, 0)
        ));
    }

    @Override
    public void onClick(View v) {
        getContext().startActivity(
                new Intent(getContext(), ForecastActivity.class)
                        .putExtra(ForecastActivity.EXTRA_DATE, calendar.getTimeInMillis()),
                ActivityOptions.makeCustomAnimation(getContext(), R.anim.slide_right_in, R.anim.slide_left_out).toBundle());
    }
}
