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

package com.ominous.quickweather.util;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.util.SparseIntArray;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.ominous.quickweather.R;
import com.ominous.quickweather.api.OpenWeatherMap;
import com.ominous.quickweather.pref.WeatherPreferences;
import com.ominous.tylerutils.util.ColorUtils;

//TODO move methods to TylerUtils
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
    private final SparseIntArray temperatureColors;

    private static ColorHelper instance;

    private ColorHelper(Context context) {
        setNightMode(context);

        adjustedTemperatureColors = new SparseIntArray();
        adjustedTemperatureColors.put(20, getAdjustedColor(context, R.color.color_pink));
        adjustedTemperatureColors.put(30, getAdjustedColor(context, R.color.color_purple_light));
        adjustedTemperatureColors.put(40, getAdjustedColor(context, R.color.color_blue_light));
        adjustedTemperatureColors.put(50, getAdjustedColor(context, R.color.color_blue));
        adjustedTemperatureColors.put(60, getAdjustedColor(context, R.color.color_green));
        adjustedTemperatureColors.put(70, getAdjustedColor(context, R.color.color_yellow));
        adjustedTemperatureColors.put(80, getAdjustedColor(context, R.color.color_orange));
        adjustedTemperatureColors.put(90, getAdjustedColor(context, R.color.color_red));
        adjustedTemperatureColors.put(100, getAdjustedColor(context, R.color.color_pink));
        adjustedTemperatureColors.put(110, getAdjustedColor(context, R.color.color_purple_light));

        temperatureColors = new SparseIntArray();
        temperatureColors.put(20, ContextCompat.getColor(context, R.color.color_pink));
        temperatureColors.put(30, ContextCompat.getColor(context, R.color.color_purple_light));
        temperatureColors.put(40, ContextCompat.getColor(context, R.color.color_blue_light));
        temperatureColors.put(50, ContextCompat.getColor(context, R.color.color_blue));
        temperatureColors.put(60, ContextCompat.getColor(context, R.color.color_green));
        temperatureColors.put(70, ContextCompat.getColor(context, R.color.color_yellow));
        temperatureColors.put(80, ContextCompat.getColor(context, R.color.color_orange));
        temperatureColors.put(90, ContextCompat.getColor(context, R.color.color_red));
        temperatureColors.put(100, ContextCompat.getColor(context, R.color.color_pink));
        temperatureColors.put(110, ContextCompat.getColor(context, R.color.color_purple_light));

        COLOR_RAIN = getAdjustedColor(context, R.color.color_blue_light);
        COLOR_MIX = getAdjustedColor(context, R.color.color_pink);
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
    public int getAdjustedColor(Context context, @ColorRes int colorResId) {
        int color = ContextCompat.getColor(context, colorResId);
        int backgroundColor = ContextCompat.getColor(context, R.color.card_background);
        double colorLum = getRelativeLuminance(color);
        double backgroundColorLum = getRelativeLuminance(backgroundColor);
        double contrastRatio = (Math.max(colorLum, backgroundColorLum) + 0.05) / (Math.min(colorLum, backgroundColorLum) + 0.05);

        if (contrastRatio < 3 && ColorUtils.isNightModeActive(context)) {
            return ColorUtils.adjustBrightness(color, 2 - Math.sqrt(contrastRatio / 3));
        } else if (contrastRatio < 3) {
            return ColorUtils.adjustBrightness(color, Math.sqrt(contrastRatio / 3));
        } else {
            return color;
        }
    }

    public double getRelativeLuminance(@ColorInt int color) {
        double Rs = Color.red(color) / 255.;
        double Gs = Color.green(color) / 255.;
        double Bs = Color.blue(color) / 255.;
        double R = Rs <= 0.03928 ? Rs / 12.92 : Math.pow((Rs + 0.055) / 1.055, 2.4);
        double G = Gs <= 0.03928 ? Gs / 12.92 : Math.pow((Gs + 0.055) / 1.055, 2.4);
        double B = Bs <= 0.03928 ? Bs / 12.92 : Math.pow((Bs + 0.055) / 1.055, 2.4);

        return 0.2126 * R + 0.7152 * G + 0.0722 * B;
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
    public int getColorFromTemperature(double temperature, boolean adjusted) {
        int minTemp = temperatureColors.keyAt(0);
        int maxTemp = temperatureColors.keyAt(temperatureColors.size() - 1);

        temperature = temperature < maxTemp ? temperature > minTemp ? temperature : minTemp : maxTemp;

        int low = (int) (temperature / 10) * 10;
        int high = low == maxTemp ? maxTemp : low + 10;
        int lowColor = adjusted ? adjustedTemperatureColors.get(low) : temperatureColors.get(low);
        int highColor = adjusted ? adjustedTemperatureColors.get(high) : temperatureColors.get(high);

        return ColorUtils.blendColors(lowColor, highColor, (temperature % 10) * 10);
    }

    @ColorInt
    public int getPrecipColor(OpenWeatherMap.PrecipType type) {
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
        final TypedArray a = context.obtainStyledAttributes(android.R.style.TextAppearance_Material_Notification_Title, new int[]{android.R.attr.textColor});
        final ColorStateList textColors = a.getColorStateList(0);
        a.recycle();

        return textColors;
    }
}
