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
import com.ominous.quickweather.util.LocaleUtils;
import com.ominous.quickweather.util.WeatherUtils;
import com.ominous.tylerutils.util.StringUtils;
import com.ominous.tylerutils.view.IconTextView;

import java.util.Locale;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

public class CurrentMainCardView extends BaseCardView {
    private final TextView currentTemperature;
    private final TextView currentDescription;
    private final IconTextView currentWind;
    private final IconTextView currentRain;
    private final IconTextView currentHumidity;
    private final IconTextView currentUVIndex;
    private final IconTextView currentPressure;
    private final IconTextView currentDewPoint;
    private final ImageView currentIcon;
    private final ImageView currentExpand;
    private final TableLayout additionalConditions;
    private final FrameLayout additionalConditionsViewport;
    private int cardHeight = 0;
    private int additionalConditionsHeight = 0;
    private boolean additionalConditionsShown = false;

    public CurrentMainCardView(Context context) {
        super(context);

        inflate(context, R.layout.card_main, this);

        additionalConditionsViewport = findViewById(R.id.current_additional_conditions_viewport);

        currentTemperature = findViewById(R.id.main_temperature);
        currentIcon = findViewById(R.id.main_icon);
        currentDescription = findViewById(R.id.main_description);
        additionalConditions = findViewById(R.id.main_additional_conditions);
        currentExpand = findViewById(R.id.main_expand_icon);
        currentWind = findViewById(R.id.main_wind);
        currentRain = findViewById(R.id.main_rain);
        currentHumidity = findViewById(R.id.main_humidity);
        currentPressure = findViewById(R.id.main_pressure);
        currentUVIndex = findViewById(R.id.main_uvindex);
        currentDewPoint = findViewById(R.id.main_dewpoint);

        currentWind.getImageView().setImageResource(R.drawable.wind);
        currentRain.getImageView().setImageResource(R.drawable.cloud_rain);
        currentHumidity.getImageView().setImageResource(R.drawable.wet);
        currentPressure.getImageView().setImageResource(R.drawable.meter);
        currentUVIndex.getImageView().setImageResource(R.drawable.sun);
        currentDewPoint.getImageView().setImageResource(R.drawable.thermometer_25);

        currentWind.getImageView().setContentDescription(context.getString(R.string.current_wind_desc));
        currentRain.getImageView().setContentDescription(context.getString(R.string.current_precip_desc));
        currentHumidity.getImageView().setContentDescription(context.getString(R.string.current_humidity_desc));
        currentPressure.getImageView().setContentDescription(context.getString(R.string.current_pressure_desc));
        currentUVIndex.getImageView().setContentDescription(context.getString(R.string.current_uvindex_desc));
        currentDewPoint.getImageView().setContentDescription(context.getString(R.string.current_dewpoint_desc));
    }

    @Override
    public void update(WeatherModel weatherModel, int position) {
        currentIcon.setImageResource(WeatherUtils.getIconFromCode(weatherModel.responseOneCall.current.weather[0].icon, weatherModel.responseOneCall.current.weather[0].id));
        currentIcon.setContentDescription(weatherModel.responseOneCall.current.weather[0].description);
        currentTemperature.setText(WeatherUtils.getTemperatureString(weatherModel.responseOneCall.current.temp, 1));
        currentDescription.setText(StringUtils.capitalizeEachWord(WeatherUtils.getCurrentLongWeatherDesc(weatherModel.responseOneCall)));

        currentWind.getTextView().setText(WeatherUtils.getWindSpeedString(weatherModel.responseOneCall.current.wind_speed, weatherModel.responseOneCall.current.wind_deg));
        currentRain.getTextView().setText(WeatherUtils.getPrecipitationString(weatherModel.responseOneCall.current.getPrecipitationIntensity(), weatherModel.responseOneCall.current.getPrecipitationType()));
        currentUVIndex.getTextView().setText(getContext().getString(R.string.format_uvi, weatherModel.responseOneCall.current.uvi));
        currentDewPoint.getTextView().setText(getContext().getString(R.string.format_dewpoint, WeatherUtils.getTemperatureString(weatherModel.responseOneCall.current.dew_point, 1)));
        currentHumidity.getTextView().setText(getContext().getString(R.string.format_humidity, LocaleUtils.getPercentageString(Locale.getDefault(), weatherModel.responseOneCall.current.humidity / 100.0)));
        currentPressure.getTextView().setText(getContext().getString(R.string.format_pressure, weatherModel.responseOneCall.current.pressure));

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

            currentExpand.setRotation(180 * (toClose ? 1 - valueAnimator.getAnimatedFraction() : valueAnimator.getAnimatedFraction()));

            additionalConditions.setTranslationY(translate - additionalConditionsHeight);

            thisParams.height = cardHeight + translateInt;
            this.setLayoutParams(thisParams);

            viewPortParams.height = translateInt;
            additionalConditionsViewport.setLayoutParams(viewPortParams);
        });

        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                currentExpand.setRotation(toClose ? 0 : 180);
            }
        });

        anim.start();
    }
}