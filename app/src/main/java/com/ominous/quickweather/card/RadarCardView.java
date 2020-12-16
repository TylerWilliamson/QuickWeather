package com.ominous.quickweather.card;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import com.ominous.quickweather.R;
import com.ominous.quickweather.util.ColorUtils;
import com.ominous.quickweather.weather.WeatherResponse;

import java.lang.ref.WeakReference;
import java.util.Locale;

public class RadarCardView extends BaseCardView implements View.OnTouchListener {
    private static final String weatherUriFormat = "http://localhost:4234/radar/radar.html#lat=%1$f&lon=%2$f&theme=%3$s&ts=%4$f&tz=%5$s";

    private final FrameLayout radarFrame;

    //Single static WebView to reduce map reloading
    private static WeakReference<WebView> radarWebView;

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    public RadarCardView(Context context) {
        super(context);

        inflate(context, R.layout.card_radar, this);

        radarFrame = findViewById(R.id.radar_framelayout);

        if (radarWebView == null) {
            WebView webView = new WebView(context);
            radarWebView = new WeakReference<>(webView);
            webView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            webView.setOnTouchListener(this);

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    customTabs.launch(getContext(), request.getUrl());
                    return false;
                }
            });

            WebSettings webSettings = webView.getSettings();

            webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
            webSettings.setJavaScriptEnabled(true);
        }
    }

    @Override
    public void update(WeatherResponse response, int position) {
        WebView webView = radarWebView.get();

        if (webView.getParent() != null) {
            ((ViewGroup) webView.getParent()).removeView(webView);
        }

        radarFrame.addView(webView);

        webView.loadUrl(String.format(Locale.US, weatherUriFormat, response.latitude, response.longitude, ColorUtils.isNightModeActive(getContext()) ? "dark" : "light",getTextScaling(),response.timezone));
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
            v.performClick();
        }

        //WV/FL/CV/RV
        v.getParent().getParent().getParent().requestDisallowInterceptTouchEvent(true);

        return false;
    }

    @Override
    public void onClick(View v) {
        //Nothing
    }

    private float getTextScaling() {
        return getResources().getDisplayMetrics().scaledDensity / getResources().getDisplayMetrics().density;
    }
}