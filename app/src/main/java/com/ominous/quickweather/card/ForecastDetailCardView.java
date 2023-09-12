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
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.TextView;

import com.ominous.quickweather.R;
import com.ominous.quickweather.data.WeatherModel;
import com.ominous.quickweather.data.ForecastWeather;
import com.ominous.quickweather.pref.TemperatureUnit;
import com.ominous.quickweather.pref.WeatherPreferences;
import com.ominous.quickweather.util.ColorHelper;
import com.ominous.quickweather.util.WeatherUtils;
import com.ominous.tylerutils.util.ColorUtils;
import com.ominous.tylerutils.util.LocaleUtils;
import com.ominous.tylerutils.util.ViewUtils;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

//TODO BaseDetailCard
//TODO Swipe != Click
public class ForecastDetailCardView extends BaseCardView {
    private final TextView forecastTemperature;
    private final TextView forecastTitle;
    private final TextView forecastDescription;
    private final TextView forecastPrecipChance;
    private final ImageView forecastIcon;
    private final HorizontalScrollView scrollView;

    public ForecastDetailCardView(Context context) {
        super(context);

        inflate(context, R.layout.card_forecast_detail, this);

        forecastTemperature = findViewById(R.id.forecast_temperature);
        forecastTitle = findViewById(R.id.forecast_title);
        forecastDescription = findViewById(R.id.forecast_desc);
        forecastIcon = findViewById(R.id.forecast_icon);
        forecastPrecipChance = findViewById(R.id.forecast_precip);
        scrollView = findViewById(R.id.scrollview);

        TextView forecastTemperatureSpacer = findViewById(R.id.forecast_temperature_spacer);
        TextView forecastPrecipChanceSpacer = findViewById(R.id.forecast_precip_spacer);
        TextView forecastTitleSpacer = findViewById(R.id.forecast_title_spacer);

        WeatherUtils weatherUtils = WeatherUtils.getInstance(getContext());
        TemperatureUnit temperatureUnit = WeatherPreferences.getInstance(getContext()).getTemperatureUnit();

        forecastTemperatureSpacer.setText(weatherUtils.getTemperatureString(temperatureUnit, 100, 0));
        forecastPrecipChanceSpacer.setText(LocaleUtils.getPercentageString(Locale.getDefault(), 1f));
        forecastTitleSpacer.setText(LocaleUtils.formatHourLong(getContext(),
                Locale.getDefault(),
                new Date(1681603199000L), // 2023-04-15 23:59:59 GMT
                TimeZone.getTimeZone("GMT")));

        ViewUtils.setAccessibilityInfo(this, null, null);
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
        TemperatureUnit temperatureUnit = WeatherPreferences.getInstance(getContext()).getTemperatureUnit();
        ColorHelper colorHelper = ColorHelper.getInstance(getContext());

        long thisDay = LocaleUtils.getStartOfDay(weatherModel.date, weatherModel.currentWeather.timezone) / 1000;
        ForecastWeather.ForecastData data = null;

        int alertCount = 0;

        if (weatherModel.currentWeather.alerts != null) {
            for (int i = 0, l = weatherModel.currentWeather.alerts.length; i < l; i++) {
                if (weatherModel.currentWeather.alerts[i].end >= thisDay) {
                    alertCount++;
                }
            }
        }

        for (int i = 0, l = weatherModel.forecastWeather.list.length; i < l; i++) {
            if (weatherModel.forecastWeather.list[i].dt >= thisDay) {
                data = weatherModel.forecastWeather.list[i + position - alertCount - 2];
                i = l;
            }
        }

        if (data != null) {
            String hourText = LocaleUtils.formatHourLong(getContext(), Locale.getDefault(), new Date(data.dt * 1000), weatherModel.currentWeather.timezone);

            forecastIcon.setImageResource(data.weatherIconRes);

            forecastTitle.setText(hourText);

            forecastTemperature.setText(weatherUtils.getTemperatureString(temperatureUnit, data.temp, 0));
            forecastTemperature.setTextColor(colorHelper.getColorFromTemperature(data.temp, true, ColorUtils.isNightModeActive(getContext())));

            forecastDescription.setText(data.weatherDescription);

            if (data.pop > 0) {
                forecastPrecipChance.setText(LocaleUtils.getPercentageString(Locale.getDefault(), data.pop));
                forecastPrecipChance.setTextColor(colorHelper.getPrecipColor(data.precipitationType));
            } else {
                forecastPrecipChance.setText(null);
            }

            setContentDescription(getContext().getString(R.string.format_forecast_detail_desc,
                    hourText,
                    data.weatherDescription,
                    weatherUtils.getTemperatureString(temperatureUnit, data.temp, 0),
                    getContext().getString(R.string.format_precipitation_chance, LocaleUtils.getPercentageString(Locale.getDefault(), data.pop), weatherUtils.getPrecipitationTypeString(data.precipitationType))
            ));
        }
    }

    @Override
    public void onClick(View v) {
        //Nothing
    }
}
