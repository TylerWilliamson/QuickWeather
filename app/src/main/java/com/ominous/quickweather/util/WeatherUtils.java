package com.ominous.quickweather.util;

import android.content.Context;

import com.ominous.quickweather.R;

import java.lang.ref.WeakReference;

public class WeatherUtils {
    private static WeakReference<Context> context;

    public static void initialize(Context c) {
        context = new WeakReference<>(c);
    }

    public static int getIconFromCode(String code) {

        if (code == null) {
            return R.drawable.thermometer_25;
        }

        switch (code) {
            case "clear-day":
                return R.drawable.sun;
            case "clear-night":
                return R.drawable.moon_25;
            case "rain":
                return R.drawable.cloud_rain;
            case "snow":
                return R.drawable.cloud_snow;
            case "sleet":
                return R.drawable.cloud_hail;
            case "wind":
                return R.drawable.cloud_wind;
            case "fog":
                return R.drawable.cloud_fog;
            case "cloudy":
                return R.drawable.cloud;
            case "partly-cloudy-day":
                return R.drawable.cloud_sun;
            case "partly-cloudy-night":
                return R.drawable.cloud_moon;
            default:
                return R.drawable.thermometer_25;
        }
    }

    public static String getTemperatureString(double temperature, int decimals) {
        String units = WeatherPreferences.getTemperatureUnit();

        return context.get().getString(
                R.string.format_temperature,
                round(units.equals(WeatherPreferences.TEMPERATURE_CELSIUS) ? (temperature - 32) / 9 * 5 : temperature, decimals),
                units.equals(WeatherPreferences.TEMPERATURE_CELSIUS) ? 'C' : 'F');
    }

    public static String getWindSpeedString(double windSpeed, int degrees) {
        String units = WeatherPreferences.getSpeedUnit();

        return context.get().getString(
                R.string.format_wind,
                round(units.equals(WeatherPreferences.SPEED_KMH) ? windSpeed * 1.60934 : units.equals(WeatherPreferences.SPEED_MS) ? windSpeed * 0.44704 : windSpeed,1),
                units,
                getWindDirection(degrees + 180)); //Wind bearing is the direction FROM WHICH the wind is blowing
    }

    private static String getWindDirection(int degrees) {
        //N, NNE, NE, ENE, E, ESE, SE, SSE, S, SSW, SW, WSW, W, WNW, NW, NNW

        while (degrees < 0) {
            degrees += 360;
        }

        final char cardinals[] = {'N','E','S','W'};

        final int bearing = (int) (((degrees % 360) + 11.24) / 22.5);

        StringBuilder directionBuilder = new StringBuilder(3);

        if (bearing % 2 == 1) {
            directionBuilder.append(cardinals[((bearing + 1) % 16) / 4]);
        }

        if (bearing % 8 != 4) {
            directionBuilder.append(bearing > 12 || bearing < 4 ? cardinals[0] : cardinals[2]);
        }

        if (bearing % 8 != 0) {
            directionBuilder.append(bearing < 8 ? cardinals[1] : cardinals[3]);
        }

        return directionBuilder.toString();
    }

    public static String getPercentageString(double percentage) {
        return context.get().getString(
                R.string.format_percent,
                round(percentage * 100,0));
    }

    private static String round(double d, int i) {
        int factor = (int) Math.pow(10,i);
        int t = (int) (d * factor);

        return i > 0 ? context.get().getResources().getString(R.string.format_decimal,t / factor,Math.abs(t % factor)) : Integer.toString(t);
    }
}
