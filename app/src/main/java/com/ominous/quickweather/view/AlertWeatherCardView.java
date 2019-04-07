package com.ominous.quickweather.view;

import android.content.Context;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;

import com.ominous.quickweather.R;
import com.ominous.quickweather.util.Weather;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AlertWeatherCardView extends BaseWeatherCardView {
    private TextView alertTextTitle, alertTextSubtitle;
    private Weather.WeatherResponse.AlertObj alert;
    private static final String TEXT_WATCH = "watch", TEXT_WARNING = "warning";
    private int COLOR_RED, COLOR_YELLOW, COLOR_BLUE;
    private SimpleDateFormat alertDateFormat = new SimpleDateFormat("EEE MMM d, h:mm a", Locale.getDefault());

    public AlertWeatherCardView(Context context) {
        super(context);

        inflate(context, R.layout.card_alert, this);

        alertTextTitle = findViewById(R.id.alert_text_title);
        alertTextSubtitle = findViewById(R.id.alert_text_subtitle);

        COLOR_RED = ContextCompat.getColor(context,R.color.color_red);
        COLOR_YELLOW = ContextCompat.getColor(context,R.color.color_yellow);
        COLOR_BLUE = ContextCompat.getColor(context,R.color.color_blue_light);
    }

    @Override
    protected Uri getUri() {
        return Uri.parse(alert.uri);
    }

    @Override
    public void update(Weather.WeatherResponse response, int position) {
        alert = response.alerts[position - 1];

        customTabs.addLikelyUris(getUri());

        alertTextTitle.setText(alert.title);
        alertTextTitle.setTextColor(alert.severity.equals(TEXT_WATCH) ? COLOR_YELLOW : alert.severity.equals(TEXT_WARNING) ? COLOR_RED : COLOR_BLUE);

        alertTextSubtitle.setText(getContext().getResources().getString(R.string.format_alert, alertDateFormat.format(new Date(alert.expires * 1000))));
    }
}
