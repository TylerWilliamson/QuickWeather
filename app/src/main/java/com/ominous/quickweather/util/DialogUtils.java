package com.ominous.quickweather.util;

import android.app.Dialog;
import android.content.Context;

import com.ominous.quickweather.R;
import com.ominous.quickweather.dialog.TextDialog;
import com.ominous.quickweather.weather.WeatherResponse;
import com.ominous.tylerutils.util.StringUtils;

import java.util.regex.Pattern;

public class DialogUtils {
    private static final Pattern httpPattern = Pattern.compile("(https?://)?(([\\w\\-])+\\.([a-zA-Z]{2,63})([/\\w-]*)*/?\\??([^ #\\n\\r]*)?#?([^ \\n\\r]*))");
    private static final Pattern usTelPattern = Pattern.compile("(tel://)?((\\+?1[ \\-])?\\(?[0-9]{3}\\)?[-. ][0-9]{3}[-. ]?[0-9]{4})");

    private static TextDialog alertDialog;

    public static void showDialogForAlert(Context context, WeatherResponse.Alert alert) {
        if (alertDialog == null) {
            alertDialog = new TextDialog(context);
        }

        alertDialog
                .setTitle(alert.title)
                .setContent(StringUtils.fromHtml(
                        StringUtils.linkify(StringUtils.linkify(alert.description,
                                httpPattern,"https"),
                                usTelPattern,"tel")))
                .addCloseButton()
                .show();
    }

    public static void showLocationDisclosure(Context context, Runnable onAcceptRunnable) {
        new TextDialog(context)
                .setTitle(context.getResources().getString(R.string.dialog_location_disclosure_title))
                .setContent(context.getResources().getString(R.string.dialog_location_disclosure))
                .setButton(Dialog.BUTTON_POSITIVE,"Accept", onAcceptRunnable)
                .setButton(Dialog.BUTTON_NEGATIVE,"Decline", null)
                .show();
    }

    public static void showLocationRationale(Context context) {
        new TextDialog(context)
                .setTitle(context.getString(R.string.dialog_location_denied_title))
                .setContent(context.getString(R.string.dialog_location_denied))
                .addCloseButton()
                .show();
    }
}
