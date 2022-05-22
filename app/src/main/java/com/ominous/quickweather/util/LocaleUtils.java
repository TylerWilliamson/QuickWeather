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

package com.ominous.quickweather.util;

import android.os.Build;

import java.text.ParseException;
import java.util.Locale;

public class LocaleUtils extends com.ominous.tylerutils.util.LocaleUtils {

    public static String getOWMLang(Locale locale) {
        String lang = locale.getLanguage();

        if (lang.equals("pt") && locale.getCountry().equals("BR")) {
            lang = "pt_br";
        } else if (locale.equals(Locale.CHINESE) || locale.equals(Locale.SIMPLIFIED_CHINESE)) {
            lang = "zh_cn";
        } else if (locale.equals(Locale.TRADITIONAL_CHINESE)) {
            lang = "zh_tw";
        }

        return lang.isEmpty() ? "en" : lang;
    }

    public static double parseDouble(Locale locale, String doubleString) {
        try {
            if (Build.VERSION.SDK_INT >= 24) {
                return android.icu.text.NumberFormat.getInstance(locale)
                        .parse(doubleString == null ? "0" : doubleString)
                        .doubleValue();
            } else {
                Number number = java.text.NumberFormat.getInstance(locale)
                        .parse(doubleString == null ? "0" : doubleString);
                return number == null ? 0 : number.doubleValue();
            }
        } catch (ParseException e) {
            return 0;
        }
    }
}
