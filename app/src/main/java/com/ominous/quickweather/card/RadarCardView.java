/*
 *   Copyright 2019 - 2025 Tyler Williamson
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
import android.view.ViewGroup;

import com.ominous.quickweather.R;
import com.ominous.quickweather.activity.BaseActivity;
import com.ominous.quickweather.data.WeatherModel;
import com.ominous.quickweather.view.WeatherMapView;
import com.ominous.tylerutils.util.ViewUtils;

public class RadarCardView extends BaseCardView {
    private final WeatherMapView weatherMapView;

    public RadarCardView(Context context) {
        this(context, null);
    }

    public RadarCardView(Context context, WeatherMapView weatherMapView) {
        super(context);


        this.weatherMapView = weatherMapView;

        init(context);
    }

    private void init(Context context) {
        setContentDescription(context.getString(R.string.card_radar_desc));

        ViewUtils.setAccessibilityInfo(this, null, null);
    }

    public WeatherMapView getRadarView() {
        return weatherMapView;
    }

    @Override
    public void update(WeatherModel weatherModel, int position) {
        if (weatherMapView.getParent() != null) {
            ((ViewGroup) weatherMapView.getParent()).removeView(weatherMapView);
        }

        addView(weatherMapView);
        weatherMapView.setLayoutParams(new BaseCardView.LayoutParams(LayoutParams.MATCH_PARENT, getResources().getDimensionPixelSize(R.dimen.radar_height)));
        weatherMapView.update(weatherModel.locationPair.first, weatherModel.locationPair.second);
    }

    public void attachToActivity(BaseActivity activity) {
        weatherMapView.attachToActivity(activity);
    }

    @Override
    public void onClick(View v) {
        //Nothing
    }

    public void setOnFullscreenClickedListener(OnFullscreenClickedListener onFullscreenClickedListener) {
        weatherMapView.setOnFullscreenClickedListener(onFullscreenClickedListener);
    }

    public interface OnFullscreenClickedListener {
        void onFullscreenClicked(boolean expand);
    }
}