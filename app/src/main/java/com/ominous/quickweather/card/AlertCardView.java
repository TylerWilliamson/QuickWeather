/*
 *     Copyright 2019 - 2022 Tyler Williamson
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

package com.ominous.quickweather.card;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.ominous.quickweather.R;
import com.ominous.quickweather.api.OpenWeatherMap;
import com.ominous.quickweather.data.WeatherModel;
import com.ominous.quickweather.data.WeatherResponseOneCall;
import com.ominous.quickweather.util.DialogHelper;
import com.ominous.tylerutils.util.LocaleUtils;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import androidx.core.content.ContextCompat;

public class AlertCardView extends BaseCardView {
    private final TextView alertTextTitle;
    private final TextView alertTextSubtitle;
    private final int COLOR_RED;
    private final int COLOR_YELLOW;
    private final int COLOR_BLUE;
    private final DialogHelper dialogHelper;
    private WeatherResponseOneCall.Alert alert;

    public AlertCardView(Context context) {
        super(context);

        inflate(context, R.layout.card_alert, this);

        alertTextTitle = findViewById(R.id.alert_text_title);
        alertTextSubtitle = findViewById(R.id.alert_text_subtitle);

        COLOR_RED = ContextCompat.getColor(context, R.color.color_red);
        COLOR_YELLOW = ContextCompat.getColor(context, R.color.color_yellow);
        COLOR_BLUE = ContextCompat.getColor(context, R.color.color_blue_light);

        dialogHelper = new DialogHelper(context);
    }

    @Override
    public void update(WeatherModel weatherModel, int position) {
        alert = weatherModel.responseOneCall.alerts[position - 1];
        OpenWeatherMap.AlertSeverity severity = alert.getSeverity();

        alertTextTitle.setText(alert.event);
        alertTextTitle.setTextColor(severity == OpenWeatherMap.AlertSeverity.WATCH ? COLOR_YELLOW : severity == OpenWeatherMap.AlertSeverity.WARNING ? COLOR_RED : COLOR_BLUE);

        alertTextSubtitle.setText(getContext().getResources().getString(R.string.format_alert, alert.end == 0 ? getContext().getString(R.string.text_unknown) : LocaleUtils.formatDateTime(getContext(), Locale.getDefault(), new Date(alert.end * 1000), TimeZone.getTimeZone(weatherModel.responseOneCall.timezone))));
    }

    @Override
    public void onClick(View v) {
        dialogHelper.showAlert(alert);
    }
}
