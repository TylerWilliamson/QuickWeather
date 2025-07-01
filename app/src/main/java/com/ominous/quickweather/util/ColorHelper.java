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

package com.ominous.quickweather.util;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.util.SparseIntArray;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.ominous.quickweather.R;
import com.ominous.quickweather.data.PrecipType;
import com.ominous.quickweather.pref.WeatherPreferences;
import com.ominous.tylerutils.util.ColorUtils;

public class ColorHelper {
    @ColorInt
    public final int COLOR_TEXT_BLACK;
    @ColorInt
    public final int COLOR_TEXT_WHITE;

    @ColorInt
    private final int COLOR_RAIN;
    @ColorInt
    private final int COLOR_SNOW;
    @ColorInt
    private final int COLOR_MIX;

    private final SparseIntArray adjustedTemperatureColors;
    private final SparseIntArray adjustedTemperatureColorsDark;
    private final SparseIntArray temperatureColors;

    private static ColorHelper instance;

    private ColorHelper(Context context) {
        setNightMode(context);

        final int pink = ContextCompat.getColor(context, R.color.color_pink);
        final int purpleLight = ContextCompat.getColor(context, R.color.color_purple_light);
        final int blueLight = ContextCompat.getColor(context, R.color.color_blue_light);
        final int blue = ContextCompat.getColor(context, R.color.color_blue);
        final int green = ContextCompat.getColor(context, R.color.color_green);
        final int yellow = ContextCompat.getColor(context, R.color.color_yellow);
        final int orange = ContextCompat.getColor(context, R.color.color_orange);
        final int red = ContextCompat.getColor(context, R.color.color_red);

        final int adjPink = getAdjustedColor(context, pink, false);
        final int adjPurpleLight = getAdjustedColor(context, purpleLight, false);
        final int adjBlueLight = getAdjustedColor(context, blueLight, false);
        final int adjBlue = getAdjustedColor(context, blue, false);
        final int adjGreen = getAdjustedColor(context, green, false);
        final int adjYellow = getAdjustedColor(context, yellow, false);
        final int adjOrange = getAdjustedColor(context, orange, false);
        final int adjRed = getAdjustedColor(context, red, false);

        final int adjPinkDark = getAdjustedColor(context, pink, true);
        final int adjPurpleLightDark = getAdjustedColor(context, purpleLight, true);
        final int adjBlueLightDark = getAdjustedColor(context, blueLight, true);
        final int adjBlueDark = getAdjustedColor(context, blue, true);
        final int adjGreenDark = getAdjustedColor(context, green, true);
        final int adjYellowDark = getAdjustedColor(context, yellow, true);
        final int adjOrangeDark = getAdjustedColor(context, orange, true);
        final int adjRedDark = getAdjustedColor(context, red, true);

        adjustedTemperatureColors = new SparseIntArray(10);
        adjustedTemperatureColors.append(20, adjPink);
        adjustedTemperatureColors.append(30, adjPurpleLight);
        adjustedTemperatureColors.append(40, adjBlueLight);
        adjustedTemperatureColors.append(50, adjBlue);
        adjustedTemperatureColors.append(60, adjGreen);
        adjustedTemperatureColors.append(70, adjYellow);
        adjustedTemperatureColors.append(80, adjOrange);
        adjustedTemperatureColors.append(90, adjRed);
        adjustedTemperatureColors.append(100, adjPink);
        adjustedTemperatureColors.append(110, adjPurpleLight);

        adjustedTemperatureColorsDark = new SparseIntArray(10);
        adjustedTemperatureColorsDark.append(20, adjPinkDark);
        adjustedTemperatureColorsDark.append(30, adjPurpleLightDark);
        adjustedTemperatureColorsDark.append(40, adjBlueLightDark);
        adjustedTemperatureColorsDark.append(50, adjBlueDark);
        adjustedTemperatureColorsDark.append(60, adjGreenDark);
        adjustedTemperatureColorsDark.append(70, adjYellowDark);
        adjustedTemperatureColorsDark.append(80, adjOrangeDark);
        adjustedTemperatureColorsDark.append(90, adjRedDark);
        adjustedTemperatureColorsDark.append(100, adjPinkDark);
        adjustedTemperatureColorsDark.append(110, adjPurpleLightDark);

        temperatureColors = new SparseIntArray(10);
        temperatureColors.append(20, pink);
        temperatureColors.append(30, purpleLight);
        temperatureColors.append(40, blueLight);
        temperatureColors.append(50, blue);
        temperatureColors.append(60, green);
        temperatureColors.append(70, yellow);
        temperatureColors.append(80, orange);
        temperatureColors.append(90, red);
        temperatureColors.append(100, pink);
        temperatureColors.append(110, purpleLight);

        COLOR_RAIN = adjBlueLight;
        COLOR_MIX = adjPink;
        COLOR_SNOW = ContextCompat.getColor(context, R.color.color_grey_99);

        COLOR_TEXT_BLACK = ContextCompat.getColor(context, R.color.color_black);
        COLOR_TEXT_WHITE = ContextCompat.getColor(context, R.color.color_white);
    }

    public static ColorHelper getInstance(Context context) {
        if (instance == null) {
            instance = new ColorHelper(context);
        }

        return instance;
    }

    @ColorInt
    private int getAdjustedColor(Context context, @ColorInt int color, boolean isDarkBackground) {
        double contrastRatio = ColorUtils.getContrastRatio(
                color,
                ContextCompat.getColor(context, isDarkBackground ? R.color.color_grey_21 : R.color.color_white));

        if (contrastRatio < 3 && isDarkBackground) {
            return ColorUtils.adjustBrightness(color, 2 - Math.sqrt(contrastRatio / 3));
        } else if (contrastRatio < 3) {
            return ColorUtils.adjustBrightness(color, Math.sqrt(contrastRatio / 3));
        } else {
            return color;
        }
    }

    public void setNightMode(Context context) {
        int mode = switch (WeatherPreferences.getInstance(context).getTheme()) {
            case DARK -> AppCompatDelegate.MODE_NIGHT_YES;
            case LIGHT -> AppCompatDelegate.MODE_NIGHT_NO;
            default -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        };

        ColorUtils.setNightMode(context, mode);
    }

    //Temperature is Fahrenheit
    @ColorInt
    public int getColorFromTemperature(double temperature,
                                       boolean adjusted,
                                       boolean isDarkModeActive) {
        final int minTemp = temperatureColors.keyAt(0);
        final int maxTemp = temperatureColors.keyAt(temperatureColors.size() - 1);

        temperature = temperature < maxTemp ? temperature > minTemp ? temperature : minTemp : maxTemp;

        final int low = (int) (temperature / 10) * 10;
        final int high = low == maxTemp ? maxTemp : low + 10;
        final int lowColor = getColorFromTemperatureArrays(low, adjusted, isDarkModeActive);
        final int highColor = getColorFromTemperatureArrays(high, adjusted, isDarkModeActive);

        return ColorUtils.blendColors(lowColor, highColor, (temperature % 10) * 10);
    }

    @ColorInt
    private int getColorFromTemperatureArrays(int temperature, boolean adjusted, boolean isDarkModeActive) {
        if (adjusted) {
            if (isDarkModeActive) {
                return adjustedTemperatureColorsDark.get(temperature);
            } else {
                return adjustedTemperatureColors.get(temperature);
            }
        } else {
            return temperatureColors.get(temperature);
        }
    }

    @ColorInt
    public int getPrecipColor(PrecipType type) {
        return switch (type) {
            case MIX -> COLOR_MIX;
            case SNOW -> COLOR_SNOW;
            default -> COLOR_RAIN;
        };
    }

    @ColorInt
    public int getTextColor(@ColorInt int backgroundColor) {
        return ColorUtils.isColorBright(backgroundColor) ? COLOR_TEXT_BLACK : COLOR_TEXT_WHITE;
    }

    public ColorStateList getNotificationTextColor(Context context) {
        final ColorStateList textColors;

        try (TypedArray a = context.obtainStyledAttributes(
                android.R.style.TextAppearance_Material_Notification_Title,
                new int[]{android.R.attr.textColor})) {
            textColors = a.getColorStateList(0);
        }

        return textColors;
    }
}
