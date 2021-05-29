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

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.ominous.quickweather.R;
import com.ominous.quickweather.util.ColorUtils;
import com.ominous.quickweather.util.WeatherUtils;
import com.ominous.quickweather.weather.WeatherResponse;
import com.ominous.tylerutils.util.StringUtils;

import java.util.Calendar;
import java.util.Locale;

public class ForecastCardView extends BaseCardView {
    public ForecastCardView(Context context) {
        super(context);

        inflate(context, R.layout.card_forecast, this);
    }

    @Override
    public void update(WeatherResponse response, int position) {
        int day = position - (3 + (response.alerts == null ? 0 : response.alerts.length));

        WeatherResponse.DataPoint data = response.daily.data[day];

        TextView forecastTemperatureMin = findViewById(R.id.forecast_temperature_min);
        TextView forecastTemperatureMax = findViewById(R.id.forecast_temperature_max);
        TextView forecastTitle = findViewById(R.id.forecast_title);
        TextView forecastDescription = findViewById(R.id.forecast_desc);
        ImageView forecastIcon  = findViewById(R.id.forecast_icon);

        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.add(Calendar.DATE,day);

        forecastIcon            .setImageResource(WeatherUtils.getIconFromCode(data.icon));
        forecastIcon            .setContentDescription(data.summary);

        forecastTitle           .setText(day == 0 ? getContext().getString(R.string.text_today) : calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()));
        forecastTemperatureMax  .setText(WeatherUtils.getTemperatureString(data.temperatureMax,0));
        forecastTemperatureMax  .setTextColor(ColorUtils.getColorFromTemperature(data.temperatureMax,true));

        forecastTemperatureMin  .setText(WeatherUtils.getTemperatureString(data.temperatureMin,0));
        forecastTemperatureMin  .setTextColor(ColorUtils.getColorFromTemperature(data.temperatureMin,true));

        forecastIcon            .setImageResource(WeatherUtils.getIconFromCode(data.icon));
        forecastIcon            .setContentDescription(data.summary);
        forecastDescription     .setText(StringUtils.capitalizeEachWord(data.summary));
    }

    @Override
    public void onClick(View v) {
        //Nothing
    }
}
