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

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.ominous.quickweather.R;
import com.ominous.quickweather.data.WeatherModel;
import com.ominous.quickweather.data.WeatherResponseForecast;
import com.ominous.quickweather.pref.TemperatureUnit;
import com.ominous.quickweather.pref.WeatherPreferences;
import com.ominous.quickweather.util.ColorHelper;
import com.ominous.quickweather.util.WeatherUtils;
import com.ominous.tylerutils.util.ColorUtils;
import com.ominous.tylerutils.util.LocaleUtils;
import com.ominous.tylerutils.util.StringUtils;
import com.ominous.tylerutils.util.ViewUtils;

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

        ViewUtils.setAccessibilityInfo(this, null, null);
    }

    @Override
    public void update(WeatherModel weatherModel, int position) {
        WeatherUtils weatherUtils = WeatherUtils.getInstance(getContext());
        TemperatureUnit temperatureUnit = WeatherPreferences.getInstance(getContext()).getTemperatureUnit();
        ColorHelper colorHelper = ColorHelper.getInstance(getContext());

        long thisDay = LocaleUtils.getStartOfDay(weatherModel.date, TimeZone.getTimeZone(weatherModel.responseOneCall.timezone)) / 1000;
        WeatherResponseForecast.ForecastData data = null;

        int alertCount = 0;

        if (weatherModel.responseOneCall.alerts != null) {
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
            String hourText = LocaleUtils.formatHourLong(getContext(), Locale.getDefault(), new Date(data.dt * 1000), TimeZone.getTimeZone(weatherModel.responseOneCall.timezone));

            forecastIcon.setImageResource(weatherUtils.getIconFromCode(data.weather[0].icon, data.weather[0].id));

            forecastTitle.setText(hourText);

            forecastTemperature.setText(weatherUtils.getTemperatureString(temperatureUnit, data.main.temp, 0));
            forecastTemperature.setTextColor(colorHelper.getColorFromTemperature(data.main.temp, true, ColorUtils.isNightModeActive(getContext())));

            forecastDescription.setText(StringUtils.capitalizeEachWord(data.weather[0].description));

            if (data.pop > 0) {
                forecastPrecipChance.setText(LocaleUtils.getPercentageString(Locale.getDefault(), data.pop));
                forecastPrecipChance.setTextColor(colorHelper.getPrecipColor(data.getPrecipitationType()));
            } else {
                forecastPrecipChance.setText(null);
            }

            setContentDescription(getContext().getString(R.string.format_forecast_detail_desc,
                    hourText,
                    data.weather[0].description,
                    weatherUtils.getTemperatureString(temperatureUnit, data.main.temp, 0),
                    getContext().getString(R.string.format_precipitation_chance, LocaleUtils.getPercentageString(Locale.getDefault(), data.pop), weatherUtils.getPrecipitationTypeString(data.getPrecipitationType()))
            ));
        }
    }

    @Override
    public void onClick(View v) {
        //Nothing
    }
}
