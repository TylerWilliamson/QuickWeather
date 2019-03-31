package com.ominous.quickweather.view;

import android.content.Context;
import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.widget.TextView;

import com.ominous.quickweather.R;
import com.ominous.quickweather.util.Weather;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AlertWeatherCardView extends BaseWeatherCardView {
    private TextView alertTextView;
    private Weather.WeatherResponse.AlertObj alert;
    private static final String TEXT_WATCH = "watch", TEXT_WARNING = "warning";
    private int COLOR_RED, COLOR_YELLOW, COLOR_BLUE;

    public AlertWeatherCardView(Context context) {
        super(context);

        inflate(context, R.layout.card_alert, this);

        alertTextView = findViewById(R.id.alert_text_view);

        COLOR_RED = context.getResources().getColor(R.color.color_red);
        COLOR_YELLOW = context.getResources().getColor(R.color.color_yellow);
        COLOR_BLUE = context.getResources().getColor(R.color.color_blue_light);
    }

    @Override
    protected Uri getUri() {
        return Uri.parse(alert.uri);
    }

    @Override
    public void update(Weather.WeatherResponse response, int position) {
        alert = response.alerts.data[position - 1];

        customTabs.addLikelyUris(getUri());

        SpannableStringBuilder stringBuilder = new SpannableStringBuilder(getResources().getString(R.string.format_alert,alert.title,new SimpleDateFormat("EEE MMM d h:mm a", Locale.getDefault()).format(new Date(alert.expires * 1000))));

        stringBuilder.setSpan(
                new ForegroundColorSpan(alert.severity.equals(TEXT_WATCH) ? COLOR_YELLOW : alert.severity.equals(TEXT_WARNING) ? COLOR_RED : COLOR_BLUE),
                0,
                alert.title.length(),
                0);

        alertTextView.setText(stringBuilder);
        alertTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX,getResources().getDimensionPixelSize(R.dimen.text_size_regular));
    }
}
