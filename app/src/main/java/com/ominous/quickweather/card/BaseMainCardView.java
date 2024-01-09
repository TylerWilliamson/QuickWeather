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

import android.animation.ValueAnimator;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.ominous.quickweather.R;
import com.ominous.quickweather.data.WeatherModel;
import com.ominous.tylerutils.anim.OpenCloseHandler;
import com.ominous.tylerutils.anim.OpenCloseState;
import com.ominous.tylerutils.util.ViewUtils;
import com.ominous.tylerutils.view.IconTextView;

import androidx.constraintlayout.widget.ConstraintLayout;

public class BaseMainCardView extends BaseCardView {
    protected final TextView mainTemperature;
    protected final TextView mainDescription;
    protected final IconTextView windIconTextView;
    protected final IconTextView rainIconTextView;
    protected final IconTextView humidityIconTextView;
    protected final IconTextView uvIndexIconTextView;
    protected final IconTextView pressureIconTextView;
    protected final IconTextView dewPointIconTextView;
    protected final IconTextView feelsLikeIconTextView;
    protected final IconTextView visibilityIconTextView;
    protected final ImageView mainIcon;
    protected final ImageView mainExpandIcon;
    protected final GridLayout additionalConditions;
    protected final ConstraintLayout additionalConditionsViewport;
    private int additionalConditionsHeight = 0;
    private ValueAnimator animatorClose;
    private ValueAnimator animatorOpen;
    private OpenCloseHandler openCloseHandler;
    private ConstraintLayout.LayoutParams viewPortParams;

    public BaseMainCardView(Context context) {
        super(context);

        inflate(context, R.layout.card_main, this);

        additionalConditionsViewport = findViewById(R.id.current_additional_conditions_viewport);

        mainTemperature = findViewById(R.id.current_main_temperature);
        mainIcon = findViewById(R.id.current_main_icon);
        mainDescription = findViewById(R.id.main_description);

        additionalConditions = findViewById(R.id.main_additional_conditions);
        mainExpandIcon = findViewById(R.id.main_expand_icon);
        windIconTextView = findViewById(R.id.main_wind);
        rainIconTextView = findViewById(R.id.main_rain);
        humidityIconTextView = findViewById(R.id.main_humidity);
        pressureIconTextView = findViewById(R.id.main_pressure);
        uvIndexIconTextView = findViewById(R.id.main_uvindex);
        dewPointIconTextView = findViewById(R.id.main_dewpoint);
        feelsLikeIconTextView = findViewById(R.id.main_feelslike);
        visibilityIconTextView = findViewById(R.id.main_visibility);

        windIconTextView.getImageView().setImageResource(R.drawable.wind);
        rainIconTextView.getImageView().setImageResource(R.drawable.cloud_rain);
        humidityIconTextView.getImageView().setImageResource(R.drawable.wet);
        pressureIconTextView.getImageView().setImageResource(R.drawable.meter);
        uvIndexIconTextView.getImageView().setImageResource(R.drawable.sun);
        dewPointIconTextView.getImageView().setImageResource(R.drawable.thermometer_25);
        feelsLikeIconTextView.getImageView().setImageResource(R.drawable.thermometer_25);
        visibilityIconTextView.getImageView().setImageResource(R.drawable.cloud_sun);

        ViewUtils.setAccessibilityInfo(this, null, null);
    }

    @Override
    public void update(WeatherModel weatherModel, int position) {
        this.post(() -> {
            if (additionalConditionsHeight == 0) {
                additionalConditionsHeight = additionalConditions.getMeasuredHeight();
            }

            ViewGroup.LayoutParams params = additionalConditions.getLayoutParams();
            params.height = additionalConditionsHeight;
            additionalConditions.setLayoutParams(params);

            viewPortParams = (ConstraintLayout.LayoutParams) additionalConditionsViewport.getLayoutParams();
            viewPortParams.height = 1;
            additionalConditionsViewport.setLayoutParams(viewPortParams);

            animatorClose = ValueAnimator.ofFloat(additionalConditionsHeight, 0);
            animatorClose.addUpdateListener(valueAnimator ->
                    doTranslate((Float) valueAnimator.getAnimatedValue()));
            animatorClose.setDuration(400);

            animatorOpen = ValueAnimator.ofFloat(0, additionalConditionsHeight);
            animatorOpen.addUpdateListener(valueAnimator ->
                    doTranslate((Float) valueAnimator.getAnimatedValue()));
            animatorOpen.setDuration(400);

            openCloseHandler = new OpenCloseHandler(animatorOpen, animatorClose);

            openCloseHandler.setState(OpenCloseState.CLOSED);
            doTranslate(0);
        });
    }

    @Override
    public void onClick(View v) {
        if (openCloseHandler.getState() == OpenCloseState.OPEN || openCloseHandler.getState() == OpenCloseState.OPENING) {
            openCloseHandler.close();
        } else {
            openCloseHandler.open();
        }
    }

    private void doTranslate(float translate) {
        int translateInt = (int) translate;

        mainExpandIcon.setRotation(180 * (translate / additionalConditionsHeight));

        viewPortParams.height = translateInt;
        additionalConditionsViewport.setLayoutParams(viewPortParams);

        if (translateInt == 0) {
            additionalConditions.setVisibility(View.GONE);
        } else if (translateInt > 0 && additionalConditions.getVisibility() == View.GONE) {
            additionalConditions.setVisibility(View.VISIBLE);
        }
    }
}