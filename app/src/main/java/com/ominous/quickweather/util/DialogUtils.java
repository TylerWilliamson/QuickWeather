package com.ominous.quickweather.util;

import android.content.Context;
import android.text.Html;
import android.text.SpannableString;

import com.ominous.quickweather.dialog.TextDialog;
import com.ominous.quickweather.weather.WeatherResponse;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DialogUtils {
    private static final Pattern httpPattern = Pattern.compile("(https?://)?(([\\w\\-])+\\.([a-zA-Z]{2,63})([/\\w-]*)*/?\\??([^ #\\n\\r]*)?#?([^ \\n\\r]*))");
    private static final Pattern usTelPattern = Pattern.compile("(tel://)?((\\+?1[ \\-])?\\(?[0-9]{3}\\)?[-. ][0-9]{3}[-. ]?[0-9]{4})");

    private static TextDialog textDialog;

    public static void showDialogForAlert(Context context, WeatherResponse.Alert alert) {
        if (textDialog == null) {
            textDialog = new TextDialog(context);
        }

        //TODO We default to https for all web links. Is this best practice? Is there a better way?
        SpannableString alertDescription = new SpannableString(Html.fromHtml(
                linkify(linkify(alert.description,
                        httpPattern,"https"),
                        usTelPattern,"tel")));

        textDialog.show(alert.title,alertDescription);
    }

    private static String linkify(String input, Pattern pattern, String defaultScheme) {
        Matcher matcher = pattern.matcher(input);
        StringBuffer sb = new StringBuffer(input.length());
        String scheme, url, link;

        while (matcher.find()) {
            scheme = matcher.group(1);
            url = matcher.group(2);
            link = scheme == null || scheme.isEmpty() ? defaultScheme + "://" + url : matcher.group();

            matcher.appendReplacement(sb,String.format("<a href='%1$s'>%2$s</a>",link,matcher.group()));
        }

        matcher.appendTail(sb);

        return sb.toString();
    }
}
