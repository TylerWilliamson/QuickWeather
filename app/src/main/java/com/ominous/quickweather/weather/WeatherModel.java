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

package com.ominous.quickweather.weather;

public class WeatherModel {

    public final WeatherResponse response;
    public final WeatherStatus status;
    public final String errorMessage;

    public WeatherModel(WeatherResponse response, WeatherStatus status, String errorMessage) {
        this.response = response;
        this.status = status;
        this.errorMessage = errorMessage;
    }

    public WeatherModel(WeatherResponse response) {
        this.response = response;
        this.status = WeatherStatus.SUCCESS;
        this.errorMessage = null;
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
