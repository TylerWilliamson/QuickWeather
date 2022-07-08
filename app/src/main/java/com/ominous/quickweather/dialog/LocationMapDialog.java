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

package com.ominous.quickweather.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.ominous.quickweather.R;
import com.ominous.quickweather.data.WeatherDatabase;
import com.ominous.quickweather.util.ColorUtils;
import com.ominous.tylerutils.browser.CustomTabs;
import com.ominous.tylerutils.util.LocaleUtils;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

public class LocationMapDialog {
    private static final String mapUriFormat = "http://localhost:4234/radar/mappicker.html#lat=%1$f&lon=%2$f&theme=%3$s&ts=%4$f&mc=%5$s";
    private final AlertDialog mapDialog;
    private final WebView webView;
    private OnLocationChosenListener onLocationChosenListener;
    private WeatherDatabase.WeatherLocation weatherLocation = new WeatherDatabase.WeatherLocation(0, -30, null);
    private CustomTabs customTabs;

    public LocationMapDialog(Context context) {
        View locationMapDialogView = LayoutInflater.from(context).inflate(R.layout.dialog_map, null, false);

        webView = locationMapDialogView.findViewById(R.id.map);

        mapDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.dialog_map_title)
                .setView(locationMapDialogView)
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok,
                        (d, w) -> new LocationManualDialog(context).show(getWeatherLocation(), onLocationChosenListener))
                .create();

        mapDialog.setOnShowListener(d -> {
            int textColor = ContextCompat.getColor(context, R.color.color_accent_text);

            mapDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(textColor);
            mapDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(textColor);
        });

        if (customTabs == null) {
            customTabs = CustomTabs.getInstance(context);
        }
    }

    @SuppressLint({"SetJavaScriptEnabled"})
    public void show(OnLocationChosenListener onLocationChosenListener) {
        mapDialog.show();
        this.onLocationChosenListener = onLocationChosenListener;

        if (webView != null) {
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    customTabs.launch(webView.getContext(), request.getUrl());
                    return true;
                }
            });

            webView.getSettings().setJavaScriptEnabled(true);
            webView.addJavascriptInterface(this, "Android");

            webView.loadUrl(String.format(Locale.US,
                    mapUriFormat,
                    0.,
                    -30.,
                    ColorUtils.isNightModeActive(webView.getContext()) ? "dark" : "light",
                    getTextScaling(webView.getContext()),
                    Integer.toHexString(ContextCompat.getColor(webView.getContext(), R.color.color_accent) & 0xFFFFFF)
            ));
        }
    }

    private WeatherDatabase.WeatherLocation getWeatherLocation() {
        return weatherLocation;
    }

    @JavascriptInterface
    public void setWeatherLocation(String latLonStr) {
        Matcher matcher = Pattern.compile("([0-9.\\-]+),([0-9.\\-]+)").matcher(latLonStr);

        double lat = 0;
        double lon = 0;

        if (matcher.matches()) {
            lat = LocaleUtils.parseDouble(Locale.getDefault(), matcher.group(1));
            lon = LocaleUtils.parseDouble(Locale.getDefault(), matcher.group(2));
        }

        weatherLocation = new WeatherDatabase.WeatherLocation(lat, lon, null);
    }

    private float getTextScaling(Context context) {
        return context.getResources().getDisplayMetrics().scaledDensity / context.getResources().getDisplayMetrics().density;
    }
}
