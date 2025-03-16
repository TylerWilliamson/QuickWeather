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

    //TODO Possible bug when it changes from light to dark mode and the colors are cached
    private ColorHelper(Context context) {
        setNightMode(context);

        final int color_pink = ContextCompat.getColor(context, R.color.color_pink);
        final int color_purple_light = ContextCompat.getColor(context, R.color.color_purple_light);
        final int color_blue_light = ContextCompat.getColor(context, R.color.color_blue_light);
        final int color_blue = ContextCompat.getColor(context, R.color.color_blue);
        final int color_green = ContextCompat.getColor(context, R.color.color_green);
        final int color_yellow = ContextCompat.getColor(context, R.color.color_yellow);
        final int color_orange = ContextCompat.getColor(context, R.color.color_orange);
        final int color_red = ContextCompat.getColor(context, R.color.color_red);

        final int adj_color_pink = getAdjustedColor(context, color_pink, false);
        final int adj_color_purple_light = getAdjustedColor(context, color_purple_light, false);
        final int adj_color_blue_light = getAdjustedColor(context, color_blue_light, false);
        final int adj_color_blue = getAdjustedColor(context, color_blue, false);
        final int adj_color_green = getAdjustedColor(context, color_green, false);
        final int adj_color_yellow = getAdjustedColor(context, color_yellow, false);
        final int adj_color_orange = getAdjustedColor(context, color_orange, false);
        final int adj_color_red = getAdjustedColor(context, color_red, false);

        final int adj_color_pink_dark = getAdjustedColor(context, color_pink, true);
        final int adj_color_purple_light_dark = getAdjustedColor(context, color_purple_light, true);
        final int adj_color_blue_light_dark = getAdjustedColor(context, color_blue_light, true);
        final int adj_color_blue_dark = getAdjustedColor(context, color_blue, true);
        final int adj_color_green_dark = getAdjustedColor(context, color_green, true);
        final int adj_color_yellow_dark = getAdjustedColor(context, color_yellow, true);
        final int adj_color_orange_dark = getAdjustedColor(context, color_orange, true);
        final int adj_color_red_dark = getAdjustedColor(context, color_red, true);

        adjustedTemperatureColors = new SparseIntArray(10);
        adjustedTemperatureColors.append(20, adj_color_pink);
        adjustedTemperatureColors.append(30, adj_color_purple_light);
        adjustedTemperatureColors.append(40, adj_color_blue_light);
        adjustedTemperatureColors.append(50, adj_color_blue);
        adjustedTemperatureColors.append(60, adj_color_green);
        adjustedTemperatureColors.append(70, adj_color_yellow);
        adjustedTemperatureColors.append(80, adj_color_orange);
        adjustedTemperatureColors.append(90, adj_color_red);
        adjustedTemperatureColors.append(100, adj_color_pink);
        adjustedTemperatureColors.append(110, adj_color_purple_light);

        adjustedTemperatureColorsDark = new SparseIntArray(10);
        adjustedTemperatureColorsDark.append(20, adj_color_pink_dark);
        adjustedTemperatureColorsDark.append(30, adj_color_purple_light_dark);
        adjustedTemperatureColorsDark.append(40, adj_color_blue_light_dark);
        adjustedTemperatureColorsDark.append(50, adj_color_blue_dark);
        adjustedTemperatureColorsDark.append(60, adj_color_green_dark);
        adjustedTemperatureColorsDark.append(70, adj_color_yellow_dark);
        adjustedTemperatureColorsDark.append(80, adj_color_orange_dark);
        adjustedTemperatureColorsDark.append(90, adj_color_red_dark);
        adjustedTemperatureColorsDark.append(100, adj_color_pink_dark);
        adjustedTemperatureColorsDark.append(110, adj_color_purple_light_dark);

        temperatureColors = new SparseIntArray(10);
        temperatureColors.append(20, color_pink);
        temperatureColors.append(30, color_purple_light);
        temperatureColors.append(40, color_blue_light);
        temperatureColors.append(50, color_blue);
        temperatureColors.append(60, color_green);
        temperatureColors.append(70, color_yellow);
        temperatureColors.append(80, color_orange);
        temperatureColors.append(90, color_red);
        temperatureColors.append(100, color_pink);
        temperatureColors.append(110, color_purple_light);

        COLOR_RAIN = adj_color_blue_light;
        COLOR_MIX = adj_color_pink;
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
                ContextCompat.getColor(context, R.color.card_background));

        if (contrastRatio < 3 && isDarkBackground) {
            return ColorUtils.adjustBrightness(color, 2 - Math.sqrt(contrastRatio / 3));
        } else if (contrastRatio < 3) {
            return ColorUtils.adjustBrightness(color, Math.sqrt(contrastRatio / 3));
        } else {
            return color;
        }
    }

    public void setNightMode(Context context) {
        int mode;

        switch (WeatherPreferences.getInstance(context).getTheme()) {
            case DARK:
                mode = AppCompatDelegate.MODE_NIGHT_YES;
                break;
            case LIGHT:
                mode = AppCompatDelegate.MODE_NIGHT_NO;
                break;
            default:
                mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }

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
        switch (type) {
            case MIX:
                return COLOR_MIX;
            case SNOW:
                return COLOR_SNOW;
            default:
                return COLOR_RAIN;
        }
    }

    @ColorInt
    public int getTextColor(@ColorInt int backgroundColor) {
        return ColorUtils.isColorBright(backgroundColor) ? COLOR_TEXT_BLACK : COLOR_TEXT_WHITE;
    }

    public ColorStateList getNotificationTextColor(Context context) {
        final TypedArray a = context.obtainStyledAttributes(
                android.R.style.TextAppearance_Material_Notification_Title,
                new int[]{android.R.attr.textColor});
        final ColorStateList textColors = a.getColorStateList(0);
        a.recycle();

        return textColors;
    }
}
