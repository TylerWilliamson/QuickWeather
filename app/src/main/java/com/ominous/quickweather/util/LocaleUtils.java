package com.ominous.quickweather.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class LocaleUtils {
    private static SimpleDateFormat sdfHour24h = new SimpleDateFormat("H", Locale.US);
    private static SimpleDateFormat sdfHour12h = new SimpleDateFormat("ha", Locale.US);

    public static String formatHour(Locale locale, Date date, TimeZone timeZone) {
        if (is24HourFormat(locale)) {
            sdfHour24h.setTimeZone(timeZone);
            return sdfHour24h.format(date);
        } else {
            sdfHour12h.setTimeZone(timeZone);
            return sdfHour12h.format(date).replaceAll("[mM. ]", "");
        }
    }

    public static String formatDateTime(Locale locale, Date date, TimeZone timeZone) {
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG, locale);

        if (df instanceof SimpleDateFormat) {
            SimpleDateFormat sdf = new SimpleDateFormat(((SimpleDateFormat) df).toPattern().replaceAll(":ss", ""), locale);

            sdf.setTimeZone(timeZone);
            return sdf.format(date);
        } else {
            df.setTimeZone(timeZone);
            return df.format(date);
        }
    }

    private static boolean is24HourFormat(Locale locale) {
        DateFormat natural = DateFormat.getTimeInstance(DateFormat.LONG, locale);

        return !(natural instanceof SimpleDateFormat) || ((SimpleDateFormat) natural).toPattern().indexOf('H') >= 0;
    }

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
}
