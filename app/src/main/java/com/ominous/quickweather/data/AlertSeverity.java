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

package com.ominous.quickweather.data;

public enum AlertSeverity {
    WARNING("0"),
    WATCH("1"),
    ADVISORY("2");

    private final String sortKey;

    AlertSeverity(String sortKey) {
        this.sortKey = sortKey;
    }

    public String getSortKey() {
        return sortKey;
    }

    public static AlertSeverity from(String sortKey, AlertSeverity defaultValue) {
        for (AlertSeverity v : values()) {
            if (v.getSortKey().equals(sortKey)) {
                return v;
            }
        }

        return defaultValue;
    }
}
