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

package com.ominous.quickweather.card;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.ominous.quickweather.R;
import com.ominous.quickweather.activity.ForecastActivity;
import com.ominous.quickweather.data.WeatherModel;
import com.ominous.quickweather.data.WeatherResponseOneCall;
import com.ominous.quickweather.util.ColorUtils;
import com.ominous.quickweather.util.WeatherUtils;
import com.ominous.tylerutils.util.StringUtils;

import java.util.Calendar;
import java.util.Locale;

public class CurrentForecastCardView extends BaseCardView {
    private final TextView forecastTemperatureMin;
    private final TextView forecastTemperatureMax;
    private final TextView forecastTitle;
    private final TextView forecastDescription;
    private final ImageView forecastIcon;
    private final Calendar calendar = Calendar.getInstance(Locale.getDefault());

    public CurrentForecastCardView(Context context) {
        super(context);

        inflate(context, R.layout.card_current_forecast, this);

        forecastTemperatureMin = findViewById(R.id.forecast_temperature_min);
        forecastTemperatureMax = findViewById(R.id.forecast_temperature_max);
        forecastTitle = findViewById(R.id.forecast_title);
        forecastDescription = findViewById(R.id.forecast_desc);
        forecastIcon = findViewById(R.id.forecast_icon);
    }

    @Override
    public void update(WeatherModel weatherModel, int position) {
        int day = position - (3 + (weatherModel.responseOneCall.alerts == null ? 0 : weatherModel.responseOneCall.alerts.length));

        WeatherResponseOneCall.DailyData data = weatherModel.responseOneCall.daily[day];

        calendar.setTimeInMillis(data.dt * 1000);

        forecastIcon.setImageResource(WeatherUtils.getIconFromCode(data.weather[0].icon, data.weather[0].id));
        forecastIcon.setContentDescription(data.weather[0].description);

        forecastTitle.setText(day == 0 ? getContext().getString(R.string.text_today) : calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()));

        forecastTemperatureMax.setText(WeatherUtils.getTemperatureString(data.temp.max, 0));
        forecastTemperatureMax.setTextColor(ColorUtils.getColorFromTemperature(data.temp.max, true));

        forecastTemperatureMin.setText(WeatherUtils.getTemperatureString(data.temp.min, 0));
        forecastTemperatureMin.setTextColor(ColorUtils.getColorFromTemperature(data.temp.min, true));

        forecastDescription.setText(StringUtils.capitalizeEachWord(data.weather[0].description));
    }

    @Override
    public void onClick(View v) {
        getContext().startActivity(
                new Intent(getContext(), ForecastActivity.class)
                        .putExtra(ForecastActivity.EXTRA_DATE, calendar.getTimeInMillis()),
                ActivityOptions.makeCustomAnimation(getContext(), R.anim.slide_right_in, R.anim.slide_left_out).toBundle());
    }
}
