package com.ominous.quickweather.util;

import android.content.Context;

import com.ominous.quickweather.R;
import com.ominous.quickweather.weather.WeatherResponse;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Locale;

public class WeatherUtils {
    private static WeakReference<Context> context;

    private static HashMap<String, String> owmCodeToDSCode;
    private static HashMap<String, Integer> codeToIcon;

    public static void initialize(Context context) {
        WeatherUtils.context = new WeakReference<>(context);

        owmCodeToDSCode = new HashMap<>();
        owmCodeToDSCode.put("01d", "clear-day");
        owmCodeToDSCode.put("02d", "partly-cloudy-day");
        owmCodeToDSCode.put("03d", "partly-cloudy-day");
        owmCodeToDSCode.put("04d", "partly-cloudy-day");
        owmCodeToDSCode.put("09d", "rain");
        owmCodeToDSCode.put("10d", "rain");
        owmCodeToDSCode.put("11d", "rain");
        owmCodeToDSCode.put("13d", "snow");
        owmCodeToDSCode.put("50d", "fog");
        owmCodeToDSCode.put("01n", "clear-night");
        owmCodeToDSCode.put("02n", "partly-cloudy-night");
        owmCodeToDSCode.put("03n", "partly-cloudy-night");
        owmCodeToDSCode.put("04n", "partly-cloudy-night");
        owmCodeToDSCode.put("09n", "rain");
        owmCodeToDSCode.put("10n", "rain");
        owmCodeToDSCode.put("11n", "rain");
        owmCodeToDSCode.put("13n", "snow");
        owmCodeToDSCode.put("50n", "fog");

        codeToIcon = new HashMap<>();
        codeToIcon.put("clear-day", R.drawable.sun);
        codeToIcon.put("clear-night", R.drawable.moon_25);
        codeToIcon.put("rain", R.drawable.cloud_rain);
        codeToIcon.put("snow", R.drawable.cloud_snow);
        codeToIcon.put("sleet", R.drawable.cloud_hail);
        codeToIcon.put("wind", R.drawable.cloud_wind);
        codeToIcon.put("fog", R.drawable.cloud_fog);
        codeToIcon.put("cloudy", R.drawable.cloud);
        codeToIcon.put("partly-cloudy-day", R.drawable.cloud_sun);
        codeToIcon.put("partly-cloudy-night", R.drawable.cloud_moon);
    }

    public static String getCodeFromOWMCode(String code) {
        return owmCodeToDSCode.get(code);
    }

    public static int getIconFromCode(String code) {
        Integer icon = codeToIcon.get(code);

        return icon == null ? R.drawable.thermometer_25 : icon;
    }

    public static String getLongWeatherDesc(WeatherResponse.DataPoint data) {
        StringBuilder result = new StringBuilder(data.summary);

        if (data.dewPoint >= 60) {
            result.append(context.get().getString(R.string.weather_desc_humid));
        } else if (data.dewPoint <= 35) {
            result.append(context.get().getString(R.string.weather_desc_dry));
        }

        if (data.windSpeed > 25.3) {
            result.append(context.get().getString(R.string.weather_desc_strongwinds));
        } else if (data.windSpeed > 8.05) {
            result.append(context.get().getString(R.string.weather_desc_breezy));
        }

        return result.toString();
    }

    public static String getCapitalizedWeather(String weather) {
        String[] words = weather.split(" ");

        StringBuilder result = new StringBuilder();

        for (int i = 0, l = words.length; i < l; i++) {
            if (i > 0) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(words[i].charAt(0))).append(words[i].substring(1).toLowerCase());
        }

        return result.toString();
    }

    public static double getConvertedTemperature(double tempFahrenheit) {
        return WeatherPreferences.getTemperatureUnit().equals(WeatherPreferences.TEMPERATURE_CELSIUS) ? (tempFahrenheit - 32) / 1.8 : tempFahrenheit;
    }

    public static String getPrecipitationString(double precipIntensity, String type) {
        boolean isImperial = WeatherPreferences.getSpeedUnit().equals(WeatherPreferences.SPEED_MPH);

        if (type == null) {
            type = WeatherResponse.DataPoint.PRECIP_RAIN;
        }

        return getDecimalString(isImperial ? precipIntensity / 25.4 : precipIntensity, 1) +
                (isImperial ? " in " : " mm ") +
                (type.equals(WeatherResponse.DataPoint.PRECIP_RAIN) ? context.get().getString(R.string.weather_precip_rain) :
                        type.equals(WeatherResponse.DataPoint.PRECIP_SNOW) ? context.get().getString(R.string.weather_precip_snow) :
                                context.get().getString(R.string.weather_precip_mix));
    }

    public static String getTemperatureString(double temperature, int decimals) {
        return getDecimalString(getConvertedTemperature(temperature), decimals) + "\u00B0" + (WeatherPreferences.getTemperatureUnit().equals(WeatherPreferences.TEMPERATURE_CELSIUS) ? 'C' : 'F');
    }

    public static String getWindSpeedString(double windSpeed, int degrees) {
        String units = WeatherPreferences.getSpeedUnit();

        return getDecimalString(units.equals(WeatherPreferences.SPEED_KMH) ? windSpeed * 1.60934 : units.equals(WeatherPreferences.SPEED_MS) ? windSpeed * 0.44704 : windSpeed, 1) + " " +
                units + " " +
                getWindDirection(degrees + 180); //Wind bearing is the direction FROM WHICH the wind is blowing
    }

    private static String getWindDirection(int degrees) {
        //N, NNE, NE, ENE, E, ESE, SE, SSE, S, SSW, SW, WSW, W, WNW, NW, NNW

        while (degrees < 0) {
            degrees += 360;
        }

        final char[] cardinals = {'N', 'E', 'S', 'W'};

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
        switch (Locale.getDefault().getLanguage()) {
            case "cs":
            case "sk":
            case "fi":
            case "fr":
            case "es":
            case "sv":
            case "de":
                return getDecimalString(percentage * 100, 0) + " %";
            case "he":
            case "tr":
                return "%" + getDecimalString(percentage * 100, 0);
            default:
                return getDecimalString(percentage * 100, 0) + "%";
        }
    }

    private static String getDecimalString(double d, int i) {
        return i == 0 ? Integer.toString((int) d) : String.format(Locale.getDefault(), "%." + i + "f", d);
    }
}
