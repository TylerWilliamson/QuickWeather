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

package com.ominous.quickweather.pref;

public enum RadarQuality {
    HIGH("high"),
    LOW("low");

    private final String value;

    RadarQuality(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static RadarQuality from(String value, RadarQuality defaultValue) {
        for (RadarQuality v : values()) {
            if (v.getValue().equals(value)) {
                return v;
            }
        }

        return defaultValue;
    }
}
