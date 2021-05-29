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

package com.ominous.quickweather.card;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.ominous.quickweather.R;
import com.ominous.quickweather.util.DialogUtils;
import com.ominous.quickweather.util.LocaleUtils;
import com.ominous.quickweather.weather.WeatherResponse;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import androidx.core.content.ContextCompat;

public class AlertCardView extends BaseCardView {
    private final TextView alertTextTitle;
    private final TextView alertTextSubtitle;
    private WeatherResponse.Alert alert;

    private final int COLOR_RED;
    private final int COLOR_YELLOW;
    private final int COLOR_BLUE;

    public AlertCardView(Context context) {
        super(context);

        inflate(context, R.layout.card_alert, this);

        alertTextTitle = findViewById(R.id.alert_text_title);
        alertTextSubtitle = findViewById(R.id.alert_text_subtitle);

        COLOR_RED = ContextCompat.getColor(context,R.color.color_red);
        COLOR_YELLOW = ContextCompat.getColor(context,R.color.color_yellow);
        COLOR_BLUE = ContextCompat.getColor(context,R.color.color_blue_light);
    }

    @Override
    public void update(WeatherResponse response, int position) {
        alert = response.alerts[position - 1];

        alertTextTitle.setText(alert.title);
        alertTextTitle.setTextColor(alert.severity.equals(WeatherResponse.Alert.TEXT_WATCH) ? COLOR_YELLOW : alert.severity.equals(WeatherResponse.Alert.TEXT_WARNING) ? COLOR_RED : COLOR_BLUE);

        alertTextSubtitle.setText(getContext().getResources().getString(R.string.format_alert,alert.expires == 0 ? "Unknown" : LocaleUtils.formatDateTime(Locale.getDefault(),new Date(alert.expires * 1000),TimeZone.getTimeZone(response.timezone))));
    }

    @Override
    public void onClick(View v) {
        DialogUtils.showDialogForAlert(getContext(),alert);
    }
}
