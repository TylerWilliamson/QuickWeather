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

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.ominous.quickweather.R;
import com.ominous.quickweather.activity.ForecastActivity;
import com.ominous.quickweather.data.CurrentWeather;
import com.ominous.quickweather.data.WeatherModel;
import com.ominous.quickweather.pref.TemperatureUnit;
import com.ominous.tylerutils.util.ColorUtils;
import com.ominous.tylerutils.util.ViewUtils;

import java.util.Calendar;
import java.util.Locale;

public class CurrentDetailCardView extends BaseDetailCardView {
    private long currentDate = 0;

    public CurrentDetailCardView(Context context) {
        super(context);

        TemperatureUnit temperatureUnit = weatherPreferences.getTemperatureUnit();
        String spacerText = weatherUtils.getTemperatureString(temperatureUnit, 100, 0);

        forecastItem2Spacer.setText(spacerText);
        forecastItem1Spacer.setText(spacerText);
        forecastTitleSpacer.setText(R.string.text_today);

        ViewUtils.setAccessibilityInfo(this, context.getString(R.string.format_label_open, context.getString(R.string.forecast_desc)), null);
    }

    @Override
    public void update(WeatherModel weatherModel, int position) {
        TemperatureUnit temperatureUnit = weatherPreferences.getTemperatureUnit();
        boolean isDarkModeActive = ColorUtils.isNightModeActive(getContext());

        CurrentWeather.DataPoint data = weatherModel.currentWeather.daily[position];

        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTimeZone(weatherModel.currentWeather.timezone);

        currentDate = data.dt * 1000;
        calendar.setTimeInMillis(currentDate);

        forecastIcon.setImageResource(data.weatherIconRes);

        forecastTitle.setText(position == 0 ? getContext().getString(R.string.text_today) : calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()));

        forecastItem1.setText(weatherUtils.getTemperatureString(temperatureUnit, data.maxTemp, 0));
        forecastItem1.setTextColor(colorHelper.getColorFromTemperature(data.maxTemp, true, isDarkModeActive));

        forecastItem2.setText(weatherUtils.getTemperatureString(temperatureUnit, data.minTemp, 0));
        forecastItem2.setTextColor(colorHelper.getColorFromTemperature(data.minTemp, true, isDarkModeActive));

        forecastDescription.setText(data.weatherDescription);

        setContentDescription(getContext().getString(R.string.format_current_forecast_desc,
                position == 0 ? getContext().getString(R.string.text_today) : calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()),
                data.weatherDescription,
                weatherUtils.getTemperatureString(temperatureUnit, data.maxTemp, 0),
                weatherUtils.getTemperatureString(temperatureUnit, data.minTemp, 0)
        ));
    }

    @Override
    public void onClick(View v) {
        getContext().startActivity(
                new Intent(getContext(), ForecastActivity.class)
                        .putExtra(ForecastActivity.EXTRA_DATE, currentDate),
                ActivityOptions.makeCustomAnimation(getContext(), R.anim.slide_right_in, R.anim.slide_left_out).toBundle());
    }
}
