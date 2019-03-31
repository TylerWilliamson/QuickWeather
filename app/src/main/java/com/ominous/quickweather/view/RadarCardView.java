package com.ominous.quickweather.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.ominous.quickweather.R;
import com.ominous.quickweather.util.Weather;

import java.util.Locale;

public class RadarCardView extends BaseWeatherCardView implements View.OnTouchListener {
    private static final String weatherUriFormat = "https://maps.darksky.net/@radar,%1$f,%2$f,9?embed=true&timeControl=false&fieldControl=false&defaultField=radar";
    private WebView radarWebView;

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    public RadarCardView(Context context) {
        super(context);

        inflate(context, R.layout.card_radar, this);

        radarWebView = findViewById(R.id.radar_web_view);

        radarWebView.setOnTouchListener(this);

        WebSettings webSettings = radarWebView.getSettings();

        webSettings.setJavaScriptEnabled(true);
        webSettings.setAppCacheEnabled(false);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
    }

    @Override
    protected Uri getUri() {
        return null;
    }

    @Override
    public void update(Weather.WeatherResponse response, int position) {
        radarWebView.clearCache(true);
        radarWebView.loadUrl(String.format(Locale.US, weatherUriFormat, response.latitude, response.longitude));
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
            v.performClick();
        }

        getParent().requestDisallowInterceptTouchEvent(true);

        return false;
    }
}
