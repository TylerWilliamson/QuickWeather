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

import android.app.Dialog;
import android.content.Context;

import com.ominous.quickweather.R;
import com.ominous.quickweather.data.WeatherResponseOneCall;
import com.ominous.quickweather.dialog.TextDialog;
import com.ominous.tylerutils.util.StringUtils;

import java.util.regex.Pattern;

public class DialogUtils {
    private static final Pattern httpPattern = Pattern.compile("(https?://)?(([\\w\\-])+\\.([a-zA-Z]{2,63})([/\\w-]*)*/?\\??([^ #\\n\\r<]*)?#?([^ \\n\\r<]*)[^.,;<])");
    private static final Pattern usTelPattern = Pattern.compile("(tel://)?((\\+?1[ \\-])?\\(?[0-9]{3}\\)?[-. ][0-9]{3}[-. ]?[0-9]{4})");

    private static TextDialog alertDialog;

    public static void showDialogForAlert(Context context, WeatherResponseOneCall.Alert alert) {
        if (alertDialog == null) {
            alertDialog = new TextDialog(context);
        }

        alertDialog
                .setTitle(alert.event)
                .setContent(StringUtils.fromHtml(
                        StringUtils.linkify(StringUtils.linkify(alert.getHTMLFormattedDescription(),
                                httpPattern, "https"),
                                usTelPattern, "tel")))
                .addCloseButton()
                .show();
    }

    public static void showLocationDisclosure(Context context, Runnable onAcceptRunnable) {
        new TextDialog(context)
                .setTitle(context.getResources().getString(R.string.dialog_location_disclosure_title))
                .setContent(context.getResources().getString(R.string.dialog_location_disclosure))
                .setButton(Dialog.BUTTON_POSITIVE, context.getString(R.string.text_accept), onAcceptRunnable)
                .setButton(Dialog.BUTTON_NEGATIVE, context.getString(R.string.text_decline), null)
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
