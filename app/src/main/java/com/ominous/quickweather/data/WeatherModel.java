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

import android.util.Pair;

import java.util.Date;

public class WeatherModel {
    public final WeatherDatabase.WeatherLocation weatherLocation;
    public final CurrentWeather currentWeather;
    public final WeatherStatus status;
    public final String errorMessage;
    public final Exception error;
    public Date date; //TODO handle the Forecast date better
    public final Pair<Double, Double> locationPair;

    public WeatherModel(WeatherStatus status, String errorMessage, Exception error) {
        this.weatherLocation = null;
        this.currentWeather = null;
        this.status = status;
        this.errorMessage = errorMessage;
        this.error = error;
        this.locationPair = null;
    }

    public WeatherModel(CurrentWeather currentWeather,
                        WeatherDatabase.WeatherLocation weatherLocation,
                        Pair<Double, Double> locationPair,
                        WeatherStatus status) {
        this.weatherLocation = weatherLocation;
        this.currentWeather = currentWeather;
        this.status = status;
        this.errorMessage = null;
        this.error = null;
        this.locationPair = locationPair;
    }

    public enum WeatherStatus {
        UPDATING,
        OBTAINING_LOCATION,
        SUCCESS,
        ERROR_OTHER,
        ERROR_LOCATION_UNAVAILABLE,
        ERROR_LOCATION_ACCESS_DISALLOWED,
        ERROR_LOCATION_DISABLED
    }
}