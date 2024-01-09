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

package com.ominous.quickweather.data;

import androidx.annotation.StringRes;

import com.ominous.quickweather.R;

public enum WeatherCardType {
    CURRENT_MAIN(R.string.weather_card_type_main),
    CURRENT_FORECAST(R.string.weather_card_type_daily_forecast),
    RADAR(R.string.weather_card_type_radar),
    GRAPH(R.string.weather_card_type_graph),
    ALERT(R.string.weather_card_type_alert),
    FORECAST_DETAIL(R.string.weather_card_type_hourly_forecast),
    FORECAST_MAIN(R.string.weather_card_type_main);

    @StringRes
    private final int descriptionRes;

    WeatherCardType(@StringRes int descriptionRes) {
        this.descriptionRes = descriptionRes;
    }

    @StringRes
    public int getDescriptionRes() {
        return descriptionRes;
    }
}
