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

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;

import com.ominous.quickweather.R;
import com.ominous.quickweather.data.CurrentWeather;
import com.ominous.quickweather.dialog.TextDialog;
import com.ominous.tylerutils.util.StringUtils;

import java.util.regex.Pattern;

public class DialogHelper {
    private final static Pattern httpPattern = Pattern.compile("(https?://)?(([\\w\\-])+\\.([a-zA-Z]{2,63})([/\\w-]*)*/?\\??([^ #\\n\\r<]*)?#?([^ \\n\\r<]*)[^.,;<])");
    private final static Pattern usTelPattern = Pattern.compile("(tel://)?((\\+?1[ \\-])?\\(?[0-9]{3}\\)?[-. ][0-9]{3}[-. ]?[0-9]{4})");

    private final TextDialog alertDialog;
    private final Resources resources;

    public DialogHelper(Context context) {
        alertDialog = new TextDialog(context);
        resources = context.getResources();
    }

    public void showAlert(CurrentWeather.Alert alert) {
        alertDialog
                .setTitle(alert.event)
                .setContent(StringUtils.fromHtml(
                        StringUtils.linkify(StringUtils.linkify(alert.getHTMLFormattedDescription(),
                                        httpPattern, "https"),
                                usTelPattern, "tel")))
                .setButton(Dialog.BUTTON_POSITIVE, null, null)
                .addCloseButton()
                .show();
    }

    public void showLocationDisclosure(Runnable onAcceptRunnable) {
        alertDialog
                .setTitle(resources.getString(R.string.dialog_location_disclosure_title))
                .setContent(resources.getString(R.string.dialog_location_disclosure))
                .setButton(Dialog.BUTTON_POSITIVE, resources.getString(R.string.text_accept), onAcceptRunnable)
                .setButton(Dialog.BUTTON_NEGATIVE, resources.getString(R.string.text_decline), null)
                .show();
    }

    public void showLocationRationale() {
        alertDialog
                .setTitle(resources.getString(R.string.dialog_location_denied_title))
                .setContent(resources.getString(R.string.dialog_location_denied))
                .setButton(Dialog.BUTTON_POSITIVE, null, null)
                .addCloseButton()
                .show();
    }

    public void showReleaseNotes(String version, String releaseNotes) {
        alertDialog
                .setTitle(version)
                .setContent(releaseNotes)
                .setButton(Dialog.BUTTON_POSITIVE, null, null)
                .addCloseButton()
                .show();
    }

    public void showTranslation() {
        alertDialog
                .setTitle(resources.getString(R.string.dialog_translation_title))
                .setContent(StringUtils.fromHtml(resources.getString(R.string.dialog_translation_text)))
                .setButton(Dialog.BUTTON_POSITIVE, null, null)
                .addCloseButton()
                .show();
    }


}
