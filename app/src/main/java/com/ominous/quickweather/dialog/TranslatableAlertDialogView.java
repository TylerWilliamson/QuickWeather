/*
 *   Copyright 2019 - 2025 Tyler Williamson
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

package com.ominous.quickweather.dialog;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.ominous.quickweather.R;
import com.ominous.tylerutils.util.StringUtils;
import com.ominous.tylerutils.view.LinkedTextView;

import java.util.regex.Pattern;

public class TranslatableAlertDialogView extends FrameLayout {
    private final static Pattern httpPattern = Pattern.compile("(https?://)?(([\\w\\-])+\\.([a-zA-Z]{2,63})([/\\w-]*)*/?\\??([^ #\\n\\r<\"]*)?#?([^ \\n\\r\"<]*)[^.,;<\"])");
    private final static Pattern usTelPattern = Pattern.compile("(tel://)?((\\+?1[ \\-])?\\(?[0-9]{3}\\)?[-. ][0-9]{3}[-. ]?[0-9]{4})");

    private final LinkedTextView alertTextView;
    private final LinearProgressIndicator alertTranslationProgress;
    private final TextView alertError;

    public TranslatableAlertDialogView(@NonNull Context context) {
        this(context, null, 0, 0);
    }

    public TranslatableAlertDialogView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public TranslatableAlertDialogView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TranslatableAlertDialogView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        LayoutInflater.from(context).inflate(R.layout.dialog_alert, this, true);

        alertTextView = findViewById(R.id.alert_text);
        alertTranslationProgress = findViewById(R.id.alert_translation_progress);
        alertError = findViewById(R.id.alert_error);
    }

    public void setAlertText(String senderName, String content) {
        alertTextView.setText(StringUtils.fromHtml(
                StringUtils.linkify(StringUtils.linkify(
                                content
                                        .replaceAll("\\n\\*", "<br>*")
                                        .replaceAll("\\n\\.", "<br>.")
                                        .replaceAll("\\n", " ") +
                                        (senderName != null && !senderName.isEmpty() ? "<br><br>\u2022 " + senderName: ""),
                                httpPattern, "https"),
                        usTelPattern, "tel")));
    }

    public void setTranslationProgressVisibility(boolean visible) {
        alertTranslationProgress.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public void setError(String error) {
        alertError.setText(error);
    }
}
