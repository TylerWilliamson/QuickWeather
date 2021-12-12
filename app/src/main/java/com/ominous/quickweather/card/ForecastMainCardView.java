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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;

import com.ominous.quickweather.R;
import com.ominous.quickweather.data.WeatherModel;
import com.ominous.quickweather.data.WeatherResponseOneCall;
import com.ominous.quickweather.util.LocaleUtils;
import com.ominous.quickweather.util.WeatherUtils;
import com.ominous.tylerutils.util.StringUtils;
import com.ominous.tylerutils.view.IconTextView;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

public class ForecastMainCardView extends BaseCardView {
    private final TextView forecastTemperature;
    private final TextView forecastDescription;
    private final IconTextView forecastWind;
    private final IconTextView forecastRain;
    private final IconTextView forecastHumidity;
    private final IconTextView forecastUVIndex;
    private final IconTextView forecastPressure;
    private final IconTextView forecastDewPoint;
    private final ImageView forecastIcon;
    private final ImageView forecastExpandIcon;
    private final TableLayout additionalConditions;
    private final FrameLayout additionalConditionsViewport;
    private int cardHeight = 0;
    private int additionalConditionsHeight = 0;
    private boolean additionalConditionsShown = false;

    public ForecastMainCardView(Context context) {
        super(context);

        inflate(context, R.layout.card_main, this);

        additionalConditionsViewport = findViewById(R.id.current_additional_conditions_viewport);

        forecastTemperature = findViewById(R.id.main_temperature);
        forecastIcon = findViewById(R.id.main_icon);
        forecastDescription = findViewById(R.id.main_description);
        additionalConditions = findViewById(R.id.main_additional_conditions);
        forecastExpandIcon = findViewById(R.id.main_expand_icon);
        forecastWind = findViewById(R.id.main_wind);
        forecastRain = findViewById(R.id.main_rain);
        forecastHumidity = findViewById(R.id.main_humidity);
        forecastPressure = findViewById(R.id.main_pressure);
        forecastUVIndex = findViewById(R.id.main_uvindex);
        forecastDewPoint = findViewById(R.id.main_dewpoint);

        forecastWind.getImageView().setImageResource(R.drawable.wind);
        forecastRain.getImageView().setImageResource(R.drawable.cloud_rain);
        forecastHumidity.getImageView().setImageResource(R.drawable.wet);
        forecastPressure.getImageView().setImageResource(R.drawable.meter);
        forecastUVIndex.getImageView().setImageResource(R.drawable.sun);
        forecastDewPoint.getImageView().setImageResource(R.drawable.thermometer_25);

        forecastWind.getImageView().setContentDescription(context.getString(R.string.current_wind_desc));
        forecastRain.getImageView().setContentDescription(context.getString(R.string.current_precip_desc));
        forecastHumidity.getImageView().setContentDescription(context.getString(R.string.current_humidity_desc));
        forecastPressure.getImageView().setContentDescription(context.getString(R.string.current_pressure_desc));
        forecastUVIndex.getImageView().setContentDescription(context.getString(R.string.current_uvindex_desc));
        forecastDewPoint.getImageView().setContentDescription(context.getString(R.string.current_dewpoint_desc));
    }

    @Override
    public void update(WeatherModel weatherModel, int position) {
        long thisDate = LocaleUtils.getStartOfDay(weatherModel.date, TimeZone.getTimeZone(weatherModel.responseOneCall.timezone));
        WeatherResponseOneCall.DailyData thisDailyData = null;
        for (int i = 0, l = weatherModel.responseOneCall.daily.length; i < l; i++) {
            WeatherResponseOneCall.DailyData dailyData = weatherModel.responseOneCall.daily[i];

            if (LocaleUtils.getStartOfDay(new Date(dailyData.dt * 1000), TimeZone.getTimeZone(weatherModel.responseOneCall.timezone)) == thisDate) {
                thisDailyData = dailyData;
                i = l;
            }
        }

        if (thisDailyData != null) {
            forecastIcon.setImageResource(WeatherUtils.getIconFromCode(thisDailyData.weather[0].icon, thisDailyData.weather[0].id));
            forecastIcon.setContentDescription(thisDailyData.weather[0].description);

            forecastTemperature.setText(getContext().getString(R.string.format_forecast_temp, WeatherUtils.getTemperatureString(thisDailyData.temp.max, 0), WeatherUtils.getTemperatureString(thisDailyData.temp.min, 0)));
            forecastDescription.setText(StringUtils.capitalizeEachWord(WeatherUtils.getForecastLongWeatherDesc(thisDailyData)));

            forecastWind.getTextView().setText(WeatherUtils.getWindSpeedString(thisDailyData.wind_speed, thisDailyData.wind_deg));
            forecastRain.getTextView().setText(WeatherUtils.getPrecipitationString(thisDailyData.getPrecipitationIntensity(), thisDailyData.getPrecipitationType()));
            forecastUVIndex.getTextView().setText(getContext().getString(R.string.format_uvi, thisDailyData.uvi));
            forecastDewPoint.getTextView().setText(getContext().getString(R.string.format_dewpoint, WeatherUtils.getTemperatureString(thisDailyData.dew_point, 1)));
            forecastHumidity.getTextView().setText(getContext().getString(R.string.format_humidity, LocaleUtils.getPercentageString(Locale.getDefault(), thisDailyData.humidity / 100.0)));
            forecastPressure.getTextView().setText(getContext().getString(R.string.format_pressure, thisDailyData.pressure));
        }

        this.post(() -> {
            if (additionalConditionsHeight == 0) {
                additionalConditionsHeight = additionalConditions.getMeasuredHeight();
                cardHeight = getMeasuredHeight() - additionalConditionsHeight + getResources().getDimensionPixelSize(R.dimen.margin_standard);

                ViewGroup.LayoutParams params = additionalConditions.getLayoutParams();
                params.height = additionalConditionsHeight;
                additionalConditions.setLayoutParams(params);
            }

            this.doTranslate(true, 0);
        });
    }

    @Override
    public void onClick(View v) {
        this.doTranslate(additionalConditionsShown, 400);
    }

    private void doTranslate(boolean toClose, int duration) {
        additionalConditionsShown = !toClose;

        final RecyclerView.LayoutParams thisParams = (RecyclerView.LayoutParams) this.getLayoutParams();
        final ConstraintLayout.LayoutParams viewPortParams = (ConstraintLayout.LayoutParams) additionalConditionsViewport.getLayoutParams();

        ValueAnimator anim = ValueAnimator.ofFloat(toClose ? additionalConditionsHeight : 0, toClose ? 0 : additionalConditionsHeight);
        anim.setDuration(duration);
        anim.addUpdateListener(valueAnimator -> {
            float translate = (Float) valueAnimator.getAnimatedValue();
            int translateInt = (int) translate;

            forecastExpandIcon.setRotation(180 * (toClose ? 1 - valueAnimator.getAnimatedFraction() : valueAnimator.getAnimatedFraction()));

            additionalConditions.setTranslationY(translate - additionalConditionsHeight);

            thisParams.height = cardHeight + translateInt;
            this.setLayoutParams(thisParams);

            viewPortParams.height = translateInt;
            additionalConditionsViewport.setLayoutParams(viewPortParams);
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                forecastExpandIcon.setRotation(toClose ? 0 : 180);
            }
        });
        anim.start();
    }
}