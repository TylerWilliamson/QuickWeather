package com.ominous.quickweather.card;

import android.content.Context;
import android.net.Uri;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.ominous.quickweather.R;
import com.ominous.quickweather.weather.Weather;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AlertCardView extends BaseCardView {
    private TextView alertTextTitle, alertTextSubtitle;
    private Weather.WeatherResponse.Alert alert;

    private int COLOR_RED, COLOR_YELLOW, COLOR_BLUE;
    private SimpleDateFormat alertDateFormat = new SimpleDateFormat("EEE MMM d, h:mm a", Locale.getDefault());

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
    protected Uri getUri() {
        return Uri.parse(alert.uri);
    }

    @Override
    public void update(Weather.WeatherResponse response, int position) {
        alert = response.alerts[position - 1];

        customTabs.addLikelyUris(getUri());

        alertTextTitle.setText(alert.title);
        alertTextTitle.setTextColor(alert.severity.equals(Weather.WeatherResponse.Alert.TEXT_WATCH) ? COLOR_YELLOW : alert.severity.equals(Weather.WeatherResponse.Alert.TEXT_WARNING) ? COLOR_RED : COLOR_BLUE);

        alertTextSubtitle.setText(getContext().getResources().getString(R.string.format_alert, alertDateFormat.format(new Date(alert.expires * 1000))));
    }
}
