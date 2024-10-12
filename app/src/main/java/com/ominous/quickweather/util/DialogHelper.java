/*
 *   Copyright 2019 - 2024 Tyler Williamson
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
import com.ominous.quickweather.dialog.TranslatableAlertDialog;
import com.ominous.tylerutils.util.StringUtils;

public class DialogHelper {
    private final TextDialog textDialog;
    private final TranslatableAlertDialog alertDialog;
    private final Resources resources;

    public DialogHelper(Context context) {
        textDialog = new TextDialog(context);
        alertDialog = new TranslatableAlertDialog(context);
        resources = context.getResources();
    }

    public void showAlert(CurrentWeather.Alert alert) {
        alertDialog.show(alert);
    }

    public void showLocationDisclosure(Runnable onAcceptRunnable) {
        textDialog
                .setTitle(resources.getString(R.string.dialog_location_disclosure_title))
                .setContent(resources.getString(R.string.dialog_location_disclosure))
                .setButton(Dialog.BUTTON_POSITIVE, resources.getString(R.string.text_accept), onAcceptRunnable)
                .setButton(Dialog.BUTTON_NEGATIVE, resources.getString(R.string.text_decline), null)
                .show();
    }

    public void showLocationRationale() {
        textDialog
                .setTitle(resources.getString(R.string.dialog_location_denied_title))
                .setContent(resources.getString(R.string.dialog_location_denied))
                .setButton(Dialog.BUTTON_POSITIVE, null, null)
                .addCloseButton()
                .show();
    }

    public void showReleaseNotes(String version, String releaseNotes) {
        textDialog
                .setTitle(version)
                .setContent(releaseNotes)
                .setButton(Dialog.BUTTON_POSITIVE, null, null)
                .addCloseButton()
                .show();
    }

    public void showTranslation() {
        textDialog
                .setTitle(resources.getString(R.string.dialog_translation_title))
                .setContent(StringUtils.fromHtml(resources.getString(R.string.dialog_translation_text)))
                .setButton(Dialog.BUTTON_POSITIVE, null, null)
                .addCloseButton()
                .show();
    }
}
