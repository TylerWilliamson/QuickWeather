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

package com.ominous.quickweather.card;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.ominous.quickweather.R;
import com.ominous.quickweather.data.WeatherModel;
import com.ominous.quickweather.data.WeatherResponseForecast;
import com.ominous.quickweather.util.ColorUtils;
import com.ominous.quickweather.util.WeatherUtils;
import com.ominous.tylerutils.util.LocaleUtils;
import com.ominous.tylerutils.util.StringUtils;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class ForecastDetailCardView extends BaseCardView {
    private final TextView forecastTemperature;
    private final TextView forecastTitle;
    private final TextView forecastDescription;
    private final ImageView forecastIcon;
    private final TextView forecastPrecipChance;

    public ForecastDetailCardView(Context context) {
        super(context);

        inflate(context, R.layout.card_forecast_detail, this);

        forecastTemperature = findViewById(R.id.forecast_temperature);
        forecastTitle = findViewById(R.id.forecast_title);
        forecastDescription = findViewById(R.id.forecast_desc);
        forecastIcon = findViewById(R.id.forecast_icon);
        forecastPrecipChance = findViewById(R.id.forecast_precip);
    }

    @Override
    public void update(WeatherModel weatherModel, int position) {
        long thisDay = LocaleUtils.getStartOfDay(weatherModel.date, TimeZone.getTimeZone(weatherModel.responseOneCall.timezone)) / 1000;
        WeatherResponseForecast.ForecastData data = null;

        int alertCount = 0;

        if (weatherModel.responseOneCall.alerts != null) {
            //long thisDay = LocaleUtils.getStartOfDay(weatherModel.date, TimeZone.getTimeZone(weatherModel.responseOneCall.timezone)) / 1000;

            for (int i = 0, l = weatherModel.responseOneCall.alerts.length; i < l; i++) {
                if (weatherModel.responseOneCall.alerts[i].end >= thisDay) {
                    alertCount++;
                }
            }
        }

        for (int i = 0, l = weatherModel.responseForecast.list.length; i < l; i++) {
            if (weatherModel.responseForecast.list[i].dt >= thisDay) {
                data = weatherModel.responseForecast.list[i + position - alertCount - 2];
                i = l;
            }
        }

        if (data != null) {
            forecastIcon.setImageResource(WeatherUtils.getIconFromCode(data.weather[0].icon, data.weather[0].id));
            forecastIcon.setContentDescription(data.weather[0].description);

            forecastTitle.setText(LocaleUtils.formatHourLong(getContext(), Locale.getDefault(), new Date(data.dt * 1000), TimeZone.getTimeZone(weatherModel.responseOneCall.timezone)));

            forecastTemperature.setText(WeatherUtils.getTemperatureString(data.main.temp, 0));
            forecastTemperature.setTextColor(ColorUtils.getColorFromTemperature(data.main.temp, true));

            forecastDescription.setText(StringUtils.capitalizeEachWord(data.weather[0].description));

            if (data.pop > 0) {
                forecastPrecipChance.setText(LocaleUtils.getPercentageString(Locale.getDefault(), data.pop));
                forecastPrecipChance.setTextColor(ColorUtils.getPrecipColor(data.getPrecipitationType()));
            } else {
                forecastPrecipChance.setText(null);
            }
        }
    }

    @Override
    public void onClick(View v) {
        //Nothing
    }
}
