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

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.ominous.quickweather.R;
import com.ominous.quickweather.data.WeatherModel;
import com.ominous.quickweather.data.WeatherResponseOneCall;
import com.ominous.quickweather.pref.SpeedUnit;
import com.ominous.quickweather.pref.TemperatureUnit;
import com.ominous.quickweather.pref.WeatherPreferences;
import com.ominous.quickweather.util.WeatherUtils;
import com.ominous.tylerutils.util.LocaleUtils;
import com.ominous.tylerutils.util.StringUtils;
import com.ominous.tylerutils.util.ViewUtils;
import com.ominous.tylerutils.view.IconTextView;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

//TODO Add visibility + cloud cover
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
    private final Calendar calendar = Calendar.getInstance(Locale.getDefault());
    private int cardHeight = 0;
    private int additionalConditionsHeight = 0;
    private boolean additionalConditionsShown = false;

    public ForecastMainCardView(Context context) {
        super(context);

        inflate(context, R.layout.card_current_main, this);

        additionalConditionsViewport = findViewById(R.id.current_additional_conditions_viewport);

        forecastTemperature = findViewById(R.id.current_main_temperature);
        forecastIcon = findViewById(R.id.current_main_icon);
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

        ViewUtils.setAccessibilityInfo(this, null, null);
    }

    @Override
    public void update(WeatherModel weatherModel, int position) {
        int day = -1;
        long thisDate = LocaleUtils.getStartOfDay(weatherModel.date, TimeZone.getTimeZone(weatherModel.responseOneCall.timezone));
        WeatherResponseOneCall.DailyData thisDailyData = null;
        for (int i = 0, l = weatherModel.responseOneCall.daily.length; i < l; i++) {
            WeatherResponseOneCall.DailyData dailyData = weatherModel.responseOneCall.daily[i];

            if (LocaleUtils.getStartOfDay(new Date(dailyData.dt * 1000), TimeZone.getTimeZone(weatherModel.responseOneCall.timezone)) == thisDate) {
                thisDailyData = dailyData;
                day = i;
                i = l;
            }
        }

        if (thisDailyData != null) {
            calendar.setTimeInMillis(thisDailyData.dt * 1000);

            WeatherUtils weatherUtils = WeatherUtils.getInstance(getContext());
            WeatherPreferences weatherPreferences = WeatherPreferences.getInstance(getContext());
            TemperatureUnit temperatureUnit = weatherPreferences.getTemperatureUnit();
            SpeedUnit speedUnit = weatherPreferences.getSpeedUnit();

            String weatherString = StringUtils.capitalizeEachWord(weatherUtils.getForecastLongWeatherDesc(thisDailyData));
            String maxTemperatureString = weatherUtils.getTemperatureString(temperatureUnit, thisDailyData.temp.max, 0);
            String minTemperatureString = weatherUtils.getTemperatureString(temperatureUnit, thisDailyData.temp.min, 0);
            String dewPointString = weatherUtils.getTemperatureString(temperatureUnit, thisDailyData.dew_point, 1);
            String humidityString = LocaleUtils.getPercentageString(Locale.getDefault(), thisDailyData.humidity / 100.0);
            String pressureString = getContext().getString(R.string.format_pressure, thisDailyData.pressure);
            String uvIndexString = getContext().getString(R.string.format_uvi, thisDailyData.uvi);

            forecastIcon.setImageResource(weatherUtils.getIconFromCode(thisDailyData.weather[0].icon, thisDailyData.weather[0].id));

            forecastTemperature.setText(getContext().getString(R.string.format_forecast_temp, maxTemperatureString, minTemperatureString));
            forecastDescription.setText(weatherString);

            forecastWind.getTextView().setText(weatherUtils.getWindSpeedString(speedUnit, thisDailyData.wind_speed, thisDailyData.wind_deg, false));
            forecastRain.getTextView().setText(weatherUtils.getPrecipitationString(speedUnit, thisDailyData.getPrecipitationIntensity(), thisDailyData.getPrecipitationType(), false));
            forecastUVIndex.getTextView().setText(uvIndexString);
            forecastDewPoint.getTextView().setText(getContext().getString(R.string.format_dewpoint, dewPointString));
            forecastHumidity.getTextView().setText(getContext().getString(R.string.format_humidity, humidityString));
            forecastPressure.getTextView().setText(pressureString);

            setContentDescription(getContext().getString(R.string.format_forecast_desc,
                    day == 0 ? getContext().getString(R.string.text_today) : calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()),
                    weatherString,
                    maxTemperatureString,
                    minTemperatureString,
                    weatherUtils.getPrecipitationString(speedUnit, thisDailyData.getPrecipitationIntensity(), thisDailyData.getPrecipitationType(), true),
                    weatherUtils.getWindSpeedString(speedUnit, thisDailyData.wind_speed, thisDailyData.wind_deg, true),
                    humidityString,
                    pressureString,
                    dewPointString,
                    uvIndexString
            ));
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