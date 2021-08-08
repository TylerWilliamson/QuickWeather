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

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import com.ominous.quickweather.R;
import com.ominous.quickweather.activity.MainActivity;
import com.ominous.quickweather.activity.QuickWeather;
import com.ominous.quickweather.util.ColorUtils;
import com.ominous.quickweather.weather.WeatherResponse;

import java.lang.ref.WeakReference;
import java.util.Locale;

import androidx.recyclerview.widget.RecyclerView;

public class RadarCardView extends BaseCardView implements View.OnTouchListener {
    private static final String weatherUriFormat = "http://localhost:4234/radar/radar.html#lat=%1$f&lon=%2$f&theme=%3$s&ts=%4$f&tz=%5$s";
    //Single static WebView to reduce map reloading
    private static WeakReference<WebView> radarWebView;
    private final FrameLayout radarFrame;

    public RadarCardView(Context context) {
        super(context);

        inflate(context, R.layout.card_radar, this);

        radarFrame = findViewById(R.id.radar_framelayout);
    }

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    public WebView getWebView() {
        if (radarWebView == null || radarWebView.get() == null) {
            WebView webView = new WebView(getContext());
            radarWebView = new WeakReference<>(webView);
            webView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            webView.addJavascriptInterface(this, "Android");
            webView.setOnTouchListener(this);

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    customTabs.launch(getContext(), request.getUrl());
                    return false;
                }
            });

            webView.getSettings().setJavaScriptEnabled(true);

            QuickWeather.getMainActivity().registerRadarCardView(this);

            return webView;
        } else {
            return radarWebView.get();
        }
    }

    @Override
    public void update(WeatherResponse response, int position) {
        WebView webView = getWebView();

        if (webView.getParent() != null) {
            ((ViewGroup) webView.getParent()).removeView(webView);
        }

        radarFrame.getLayoutParams().height = getResources().getDimensionPixelSize(R.dimen.radar_height);
        radarFrame.addView(webView);

        webView.loadUrl(String.format(Locale.US, weatherUriFormat, response.latitude, response.longitude, ColorUtils.isNightModeActive(getContext()) ? "dark" : "light", getTextScaling(), response.timezone));
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
            v.performClick();
        }

        //Finds RecyclerView and stops it from stealing touch event
        recursivelyFindRecyclerView(v.getParent());

        return false;
    }

    private void recursivelyFindRecyclerView(ViewParent v) {
        if (v != null) {
            if (v instanceof RecyclerView) {
                v.requestDisallowInterceptTouchEvent(true);
            } else {
                recursivelyFindRecyclerView(v.getParent());
            }
        }
    }

    @Override
    public void onClick(View v) {
        //Nothing
    }

    private float getTextScaling() {
        return getResources().getDisplayMetrics().scaledDensity / getResources().getDisplayMetrics().density;
    }

    @JavascriptInterface
    public void fullscreenRadar(boolean expand) {
        QuickWeather.getViewModelProvider()
                .get(MainActivity.MainViewModel.class)
                .getFullscreenModel()
                .postValue(expand ?
                        MainActivity.FullscreenModel.OPENING :
                        MainActivity.FullscreenModel.CLOSING);
    }

    public void setRadarState(boolean expand) {
        getWebView().evaluateJavascript(
                String.format(
                        "var event = document.createEvent('Event'); " +
                                "event.initEvent('%1$s', true, true); " +
                                "window.dispatchEvent(event);",
                        expand ? "setFullscreen" : "unsetFullscreen"), null);
    }
}