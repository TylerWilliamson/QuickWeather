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

package com.ominous.quickweather.data;

import android.location.Location;

import java.util.Date;

public class WeatherModel {
    public WeatherDatabase.WeatherLocation weatherLocation;
    public Location location;
    public WeatherResponseOneCall responseOneCall;
    public WeatherResponseForecast responseForecast;
    public WeatherStatus status;
    public String errorMessage = null;
    public Date date;

    public WeatherModel() {

    }

    public WeatherModel(WeatherResponseOneCall responseOneCall, WeatherResponseForecast responseForecast, WeatherStatus status, String errorMessage, Date date) {
        this.responseOneCall = responseOneCall;
        this.status = status;
        this.errorMessage = errorMessage;
        this.responseForecast = responseForecast;
        this.date = date;
    }

    public enum WeatherStatus {
        UPDATING,
        OBTAINING_LOCATION,
        SUCCESS,
        ERROR_OTHER,
        ERROR_LOCATION_ACCESS_DISALLOWED,
        ERROR_LOCATION_DISABLED
    }
}