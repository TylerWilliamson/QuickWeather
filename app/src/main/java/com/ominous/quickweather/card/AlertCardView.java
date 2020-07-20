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
    private TextView alertTextTitle, alertTextSubtitle;
    private WeatherResponse.Alert alert;

    private int COLOR_RED, COLOR_YELLOW, COLOR_BLUE;

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
