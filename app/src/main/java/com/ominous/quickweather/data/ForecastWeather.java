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

import androidx.annotation.DrawableRes;

public class ForecastWeather {
    public long timestamp;
    public ForecastData[] list;

    public static class ForecastData {
        public long dt;
        public double temp;

        @DrawableRes
        public int weatherIconRes;
        public String weatherDescription;
        public double precipitationIntensity;
        public PrecipType precipitationType;
        public double pop;
    }
}
