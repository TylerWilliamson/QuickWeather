package com.ominous.quickweather.util;

import android.content.Context;
import android.graphics.Color;
import android.util.SparseIntArray;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.ominous.quickweather.R;

public class ColorUtils {
    private static SparseIntArray temperatureColors, precipColors;
    public static int COLOR_TEXT_BLACK, COLOR_TEXT_WHITE;

    public static void initialize(Context context) {
        temperatureColors = new SparseIntArray();
        temperatureColors.put(40, ContextCompat.getColor(context, R.color.color_blue_light));
        temperatureColors.put(50, ContextCompat.getColor(context, R.color.color_blue));
        temperatureColors.put(60, ContextCompat.getColor(context, R.color.color_green));
        temperatureColors.put(70, ContextCompat.getColor(context, R.color.color_yellow));
        temperatureColors.put(80, ContextCompat.getColor(context, R.color.color_red));
        temperatureColors.put(90, ContextCompat.getColor(context, R.color.color_red_dark));

        precipColors = new SparseIntArray();
        precipColors.put(0, ContextCompat.getColor(context, R.color.color_blue));
        precipColors.put(1, ContextCompat.getColor(context, R.color.color_blue_light));

        COLOR_TEXT_BLACK = ContextCompat.getColor(context, R.color.color_black);
        COLOR_TEXT_WHITE = ContextCompat.getColor(context, R.color.color_white);
    }

    public static void setNightMode(Context context) {
        //using a dummy WebView to avoid an Android bug regarding Dark Mode
        new WebView(context);

        int mode;

        switch (WeatherPreferences.getTheme()) {
            case WeatherPreferences.THEME_DARK:
                mode = AppCompatDelegate.MODE_NIGHT_YES;
                break;
            case WeatherPreferences.THEME_LIGHT:
                mode = AppCompatDelegate.MODE_NIGHT_NO;
                break;
            default:
                mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }

        AppCompatDelegate.setDefaultNightMode(mode);
    }

    public static int getColorFromTemperature(double temperature) {
        temperature = temperature < 90 ? temperature > 40 ? temperature : 40 : 90;

        int low = (int) (temperature / 10) * 10;
        int high = low == 90 ? 90 : low + 10;

        return blendColors(temperatureColors.get(low), temperatureColors.get(high), (temperature % 10) * 10);
    }

    public static int getColorFromPrecipChance(double precipChance) {
        precipChance = precipChance < 100 ? precipChance > 0 ? precipChance : 0 : 100;

        return blendColors(precipColors.get(0), precipColors.get(1), precipChance);
    }

    private static int blendColors(int low, int high, double percent) {
        return Color.argb(
                255,
                (int) ((Color.red(low) * (100 - percent) / 100) + (Color.red(high) * percent / 100)),
                (int) ((Color.green(low) * (100 - percent) / 100) + (Color.green(high) * percent / 100)),
                (int) ((Color.blue(low) * (100 - percent) / 100) + (Color.blue(high) * percent / 100)));
    }

    public static int getDarkenedColor(int color) {
        return Color.argb(255, (int) (Color.red(color) * 0.75), (int) (Color.green(color) * 0.75), (int) (Color.blue(color) * 0.75));
    }

    public static int getTextColor(int backgroundColor) {
        //Luminosity method via https://stackoverflow.com/a/41335343
        boolean isBackgroundBright = (0.299 * Color.red(backgroundColor) + 0.587 * Color.green(backgroundColor) + 0.114 * Color.blue(backgroundColor)) / 255 > 0.5;

        return isBackgroundBright ? COLOR_TEXT_BLACK : COLOR_TEXT_WHITE;
    }
}
