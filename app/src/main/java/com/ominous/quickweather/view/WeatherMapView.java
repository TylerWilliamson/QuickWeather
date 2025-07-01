/*
 *   Copyright 2019 - 2025 Tyler Williamson
 *
 *   This file is part of QuickWeather.
 *
 *   QuickWeather is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   QuickWeather is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with QuickWeather.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.ominous.quickweather.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.RangeSlider;
import org.maplibre.android.gestures.RotateGestureDetector;
import org.maplibre.android.MapLibre;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.Style;
import org.maplibre.android.maps.UiSettings;
import org.maplibre.android.module.http.HttpRequestUtil;
import org.maplibre.android.plugins.annotation.Symbol;
import org.maplibre.android.plugins.annotation.SymbolManager;
import org.maplibre.android.plugins.annotation.SymbolOptions;
import org.maplibre.android.style.layers.Layer;
import org.maplibre.android.style.layers.PropertyFactory;
import org.maplibre.android.style.layers.PropertyValue;
import org.maplibre.android.style.layers.RasterLayer;
import org.maplibre.android.style.sources.RasterSource;
import org.maplibre.android.style.sources.TileSet;
import com.ominous.quickweather.R;
import com.ominous.quickweather.activity.ILifecycleAwareActivity;
import com.ominous.quickweather.activity.LifecycleListener;
import com.ominous.quickweather.card.RadarCardView;
import com.ominous.quickweather.pref.RadarQuality;
import com.ominous.quickweather.pref.RadarTheme;
import com.ominous.quickweather.util.DialogHelper;
import com.ominous.quickweather.util.SnackbarHelper;
import com.ominous.tylerutils.async.Promise;
import com.ominous.tylerutils.http.HttpRequest;
import com.ominous.tylerutils.util.BitmapUtils;
import com.ominous.tylerutils.util.ColorUtils;
import com.ominous.tylerutils.util.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class WeatherMapView extends ConstraintLayout implements View.OnClickListener {
    private final static int ANIMATION_DURATION = 500;
    private final static int CONTROL_ANIMATION_DURATION = 250;
    private final static String RAINVIEWER_ATTRIBUTION = "&copy; <a href=\"https://rainviewer.com\">RainViewer</a>";
    private final static String MAPLIBRE_ATTRIBUTION = "<a href=\"https://maplibre.org/\">MapLibre</a>";
    private final static String OSM_ATTRIBUTION = "&copy; <a href=\"http://www.openstreetmap.org/about/\">OpenStreetMap</a> contributors";
    private final static String CARTO_ATTRIBUTION = "&copy; <a href=\"https://carto.com/about-carto/\">CARTO</a>";

    private final static String MAPPICKER_ICON_NAME = "MAPPICKER_ICON";
    private final static String LOCATION_ICON_NAME = "LOCATION_ICON";

    private final MapView mapView;
    private final ConstraintLayout buttonContainer;
    private final MaterialButton buttonExpand;
    private final MaterialButton buttonCompassCenter;
    private final MaterialButton buttonPlayPause;
    private final MaterialButton buttonZoomIn;
    private final MaterialButton buttonZoomOut;
    private final MaterialButton buttonCompassNorth;
    private final MaterialButton buttonAttribution;
    private final MaterialButton buttonLegend;
    private final ImageView buttonCompassNorthIcon;
    private final RangeSlider radarSlider;
    private final FrameLayout buttonCompassNorthFrame;
    private final CircularProgressIndicator radarLoadingIndicator;

    private SnackbarHelper snackbarHelper;
    private final DialogHelper dialogHelper;

    private final WeatherMapViewType weatherMapViewType;
    private RadarTheme currentTheme = null;
    private Boolean currentDarkModeActive = null;
    private RadarQuality currentRadarQuality = null;

    private double currentLatitude = 0;
    private double currentLongitude = 0;
    private int currentRainViewerFrame = 0;

    private final ArrayList<Pair<Long, String>> rainViewerTimestamps = new ArrayList<>();
    private boolean isPlaying = false;
    private boolean isFullscreen = false;

    private final Runnable nextFrameRunnable;
    private final Promise.PromiseCallable<Void, MapLibreMap> getMapAsync;
    private final Promise.PromiseCallable<MapLibreMap, Style> getStyleAsync;

    private WeatherMapAnimationListener weatherMapAnimationListener;
    private RadarCardView.OnFullscreenClickedListener onFullscreenClickedListener;
    private MapLibreMap.OnMapClickListener onMapClickListener;

    private SymbolManager symbolManager;
    private Symbol radarSymbol;

    public WeatherMapView(@NonNull Context context) {
        this(context, null, 0, 0);
    }

    public WeatherMapView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public WeatherMapView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    @SuppressLint("ClickableViewAccessibility")
    public WeatherMapView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        try (TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.WeatherMapView,
                0,
                0)) {
            int mapViewTypeIndex = a.getInteger(R.styleable.WeatherMapView_weatherMapViewType, -1);

            if (mapViewTypeIndex == -1) {
                throw new IllegalArgumentException("Invalid Map View Type");
            }

            weatherMapViewType = WeatherMapViewType.values()[mapViewTypeIndex];
        }

        MapLibre.getInstance(context);

        setHTTPOptions();
        inflate(context, R.layout.view_radar, this);

        mapView = findViewById(R.id.mapview);
        buttonContainer = findViewById(R.id.button_container);
        buttonExpand = findViewById(R.id.button_expand);
        buttonCompassNorthIcon = findViewById(R.id.button_compassnorth_icon);
        buttonCompassNorthFrame = findViewById(R.id.button_compassnorth_frame);
        buttonCompassCenter = findViewById(R.id.button_compasscenter);
        buttonPlayPause = findViewById(R.id.button_playpause);
        radarSlider = findViewById(R.id.radar_slider);
        buttonZoomIn = findViewById(R.id.button_zoomin);
        buttonZoomOut = findViewById(R.id.button_zoomout);
        buttonCompassNorth = findViewById(R.id.button_compassnorth);
        buttonAttribution = findViewById(R.id.button_attribution);
        buttonLegend = findViewById(R.id.button_legend);
        radarLoadingIndicator = findViewById(R.id.radar_loading_indicator);

        buttonZoomIn.setOnClickListener(this);
        buttonZoomOut.setOnClickListener(this);
        buttonCompassNorth.setOnClickListener(this);
        buttonCompassCenter.setOnClickListener(this);
        buttonPlayPause.setOnClickListener(this);
        buttonExpand.setOnClickListener(this);
        buttonAttribution.setOnClickListener(this);
        buttonLegend.setOnClickListener(this);

        nextFrameRunnable = () -> mapView.getMapAsync(
                mapLibreMap -> mapLibreMap.getStyle(
                        style -> showRainViewerFrame(style, true)));

        getMapAsync = v -> {
            AtomicReference<MapLibreMap> result = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);

            mapView.getMapAsync(mapLibreMap -> {
                result.set(mapLibreMap);
                latch.countDown();
            });

            try {
                if (latch.await(30, TimeUnit.SECONDS)) {
                    return result.get();
                } else {
                    return null;
                }
            } catch (InterruptedException e) {
                return null;
            }
        };

        getStyleAsync = mapLibreMap -> {
            AtomicReference<Style> result = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);

            mapLibreMap.getStyle(style -> {
                result.set(style);
                latch.countDown();
            });

            try {
                if (latch.await(30, TimeUnit.SECONDS)) {
                    return result.get();
                } else {
                    return null;
                }
            } catch (InterruptedException e) {
                return null;
            }
        };

        getViewTreeObserver().addOnScrollChangedListener(() -> {
            int behavior = radarSlider.getLabelBehavior();

            radarSlider.setLabelBehavior(LabelFormatter.LABEL_FLOATING);
            radarSlider.setLabelBehavior(behavior);
        });

        mapView.getMapAsync(mapLibreMap -> {
            UiSettings uiSettings = mapLibreMap.getUiSettings();

            uiSettings.setAttributionEnabled(false);
            uiSettings.setLogoEnabled(false);

            weatherMapAnimationListener = new WeatherMapAnimationListener(mapLibreMap);

            mapLibreMap.addOnRotateListener(weatherMapAnimationListener);

            if (weatherMapViewType == WeatherMapViewType.RADAR) {
                weatherMapAnimationListener.centerCamera();

                mapLibreMap.addOnMapClickListener(weatherMapAnimationListener);
                mapLibreMap.addOnCameraMoveStartedListener(weatherMapAnimationListener);
            }

            mapLibreMap.getStyle(style -> {
                symbolManager = new SymbolManager(mapView, mapLibreMap, style);

                symbolManager.setIconAllowOverlap(true);
                symbolManager.setTextAllowOverlap(true);

                if (weatherMapViewType == WeatherMapViewType.RADAR) {
                    radarSymbol = createRadarSymbol(symbolManager, style);

                    mapView.setOnTouchListener((v, event) -> {
                        if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                            v.performClick();
                        }

                        //Finds RecyclerView and stops it from stealing touch event
                        recursivelyFindRecyclerView(v.getParent());

                        return false;
                    });
                } else {
                    Symbol mappickerSymbol = createMappickerSymbol(symbolManager, style);

                    if (mappickerSymbol != null) {
                        mapLibreMap.addOnMapClickListener(point -> {
                            mappickerSymbol.setLatLng(point);

                            symbolManager.update(mappickerSymbol);

                            return onMapClickListener != null && onMapClickListener.onMapClick(point);
                        });
                    }
                }
            });
        });

        if (weatherMapViewType == WeatherMapViewType.RADAR) {
            radarSlider.setVisibility(View.VISIBLE);
            buttonPlayPause.setVisibility(View.VISIBLE);
            buttonExpand.setVisibility(View.VISIBLE);
            buttonLegend.setVisibility(View.VISIBLE);

            radarSlider.addOnChangeListener((slider, value, fromUser) -> {
                if (fromUser) {
                    currentRainViewerFrame = (int) value;
                    mapView.getMapAsync(
                            mapLibreMap -> mapLibreMap.getStyle(
                                    style -> showRainViewerFrame(style, false)));
                }
            });

            radarSlider.setLabelFormatter(new LabelFormatter() {
                final DateFormat simpleDateFormat = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT, Locale.getDefault());

                @NonNull
                @Override
                public String getFormattedValue(float value) {
                    return !rainViewerTimestamps.isEmpty() ? simpleDateFormat.format(
                            new Date(rainViewerTimestamps.get((int) value).first * 1000L)) : "";
                }
            });

            radarSlider.addOnSliderTouchListener(new RangeSlider.OnSliderTouchListener() {
                private boolean wasPlaying = false;

                @Override
                public void onStartTrackingTouch(@NonNull RangeSlider slider) {
                    wasPlaying = isPlaying;

                    if (isPlaying) {
                        playPause();
                    }
                }

                @Override
                public void onStopTrackingTouch(@NonNull RangeSlider slider) {
                    if (wasPlaying) {
                        playPause();
                    }
                }
            });
        } else {
            setTheme(null, null);
        }

        dialogHelper = new DialogHelper(context);
    }

    public void setTheme(RadarTheme radarTheme, RadarQuality radarQuality) {
        boolean darkModeActive = ColorUtils.isNightModeActive(getContext());

        Promise.VoidPromiseCallable<Style> addRainViewerLayersCallable = style ->
                addRainViewerLayers(style, radarTheme, radarQuality).then(v -> {
                    if (!rainViewerTimestamps.isEmpty()) {
                        currentRainViewerFrame = rainViewerTimestamps.size() - 1;
                        showRainViewerFrame(style, false);
                    }
                });

        if (currentDarkModeActive == null || currentDarkModeActive != darkModeActive) {
            currentDarkModeActive = darkModeActive;

            Promise<MapLibreMap, Void> loadStylePromise = loadStyle(currentDarkModeActive);

            if (weatherMapViewType == WeatherMapViewType.RADAR) {
                loadStylePromise.then(getMapAsync).then(getStyleAsync).then(addRainViewerLayersCallable);
            }
        } else {
            if (weatherMapViewType == WeatherMapViewType.RADAR) {
                Promise.create(getMapAsync).then(getStyleAsync).then(addRainViewerLayersCallable);
            }
        }
    }

    private Promise<MapLibreMap, Void> loadStyle(boolean isNightModeActive) {
        return Promise.create(getMapAsync).then(mapLibreMap -> {
            String styleJson;

            try (InputStream themeStream = getContext().getAssets().open(
                    isNightModeActive ? "dark_theme.json" : "light_theme.json")) {
                BufferedReader r = new BufferedReader(new InputStreamReader(themeStream));

                StringBuilder themeBuilder = new StringBuilder(themeStream.available());

                for (String line; (line = r.readLine()) != null; ) {
                    themeBuilder.append(line).append('\n');
                }

                styleJson = themeBuilder.toString();
            } catch (IOException e) {
                logError(getContext().getString(R.string.error_radar_map), e);

                return;
            }

            try {
                final Style.Builder styleBuilder = new Style.Builder().fromJson(
                        withStyledLocalizedText(styleJson, isNightModeActive));

                CountDownLatch latch = new CountDownLatch(1);

                post(() -> {
                    mapLibreMap.setStyle(styleBuilder);

                    latch.countDown();
                });

                latch.await(30, TimeUnit.SECONDS);
            } catch (JSONException e) {
                logError(getContext().getString(R.string.error_radar_map), e);
            }
        });
    }

    public void attachToActivity(ILifecycleAwareActivity activity) {
        activity.addLifecycleListener(new LifecycleListener() {
            @Override
            public void onStart() {
                mapView.onStart();
            }

            @Override
            public void onCreate(Bundle savedInstanceState) {
                mapView.onCreate(savedInstanceState);
            }

            @Override
            public void onResume() {
                mapView.onResume();

                // This fixes a bug where the Map in the Map Dialog would become invisible after
                // onPause/onStop
                mapView.post(() ->
                        mapView.getMapAsync(mapLibreMap -> {
                            CameraPosition c = mapLibreMap.getCameraPosition();

                            mapLibreMap.setCameraPosition(
                                    new CameraPosition(
                                            c.target,
                                            c.zoom + 0.01,
                                            c.tilt,
                                            c.bearing,
                                            c.padding
                                    ));
                        }));
            }

            @Override
            public void onPause() {
                if (isPlaying) {
                    playPause();
                }

                mapView.onPause();
            }

            @Override
            public void onStop() {
                mapView.onStop();
            }

            @Override
            public void onDestroy() {
                mapView.onDestroy();
            }

            @Override
            public void onSaveInstanceState(Bundle outState) {
                mapView.onSaveInstanceState(outState);
            }

            @Override
            public void onLowMemory() {
                mapView.onLowMemory();
            }
        });

        if (activity instanceof Activity) {
            View v = ((Activity) activity).findViewById(android.R.id.content);

            if (v != null) {
                snackbarHelper = new SnackbarHelper(v);
            }
        }
    }

    public void update(double latitude, double longitude) {
        currentLatitude = latitude;
        currentLongitude = longitude;

        setSliderLabelVisible(true);
        postDelayed(() -> setSliderLabelVisible(false), 2000);

        if (weatherMapViewType == WeatherMapViewType.RADAR &&
                symbolManager != null &&
                radarSymbol != null) {
            radarSymbol.setLatLng(new LatLng(latitude, longitude));

            symbolManager.update(radarSymbol);
        }

        if (currentTheme != null && currentRadarQuality != null) {
            setTheme(currentTheme, currentRadarQuality);
        }

        if (weatherMapAnimationListener != null) {
            weatherMapAnimationListener.centerCamera();
        }

        if (isPlaying) {
            playPause();
        }
    }

    public void setFullscreen(boolean state) {
        isFullscreen = state;

        buttonExpand.setIcon(ContextCompat.getDrawable(getContext(), state ? R.drawable.ic_fullscreen_exit_white_24dp : R.drawable.ic_fullscreen_white_24dp));

        WindowInsetsCompat wic = ViewCompat.getRootWindowInsets(this);

        if (wic != null) {
            Insets insets = wic.getInsets(
                    WindowInsetsCompat.Type.statusBars() |
                            WindowInsetsCompat.Type.navigationBars());

            MarginLayoutParams mlp = (MarginLayoutParams) buttonContainer.getLayoutParams();
            mlp.setMargins(
                    state ? insets.left : 0,
                    state ? insets.top : 0,
                    state ? insets.right : 0,
                    state ? insets.bottom : 0);
            buttonContainer.setLayoutParams(mlp);
        }
    }

    private void playPause() {
        isPlaying = !isPlaying;

        if (isPlaying) {
            post(nextFrameRunnable);
        } else {
            removeCallbacks(nextFrameRunnable);
        }

        buttonPlayPause.setIcon(ContextCompat.getDrawable(getContext(), isPlaying ? R.drawable.ic_pause_white_24dp : R.drawable.ic_play_arrow_white_24dp));
        setSliderLabelVisible(isPlaying);
    }

    private Symbol createMappickerSymbol(SymbolManager symbolManager, Style style) {
        Bitmap markerBitmap = BitmapUtils.drawableToBitmap(
                ContextCompat.getDrawable(getContext(), R.drawable.ic_add_location_white_48dp),
                ContextCompat.getColor(getContext(), R.color.color_accent_text));

        if (markerBitmap != null) {
            style.addImage(MAPPICKER_ICON_NAME, markerBitmap);

            SymbolOptions symbolOptions = new SymbolOptions()
                    .withLatLng(new LatLng(0, 0))
                    .withIconImage(MAPPICKER_ICON_NAME);

            return symbolManager.create(symbolOptions);
        } else {
            return null;
        }
    }

    private Symbol createRadarSymbol(SymbolManager symbolManager, Style style) {
        int iconSize = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                16,
                getResources().getDisplayMetrics()
        );

        int iconBorder = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                2,
                getResources().getDisplayMetrics()
        );

        Bitmap locationIndicatorBitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(locationIndicatorBitmap);

        Paint paint = new Paint();
        paint.setColor(ContextCompat.getColor(getContext(), ColorUtils.isNightModeActive(getContext()) ? R.color.color_white : R.color.color_black));
        canvas.drawArc(0, 0, iconSize, iconSize, 0, 360, true, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.color_accent_text));
        canvas.drawArc(iconBorder, iconBorder, iconSize - iconBorder, iconSize - iconBorder, 0, 360, true, paint);

        style.addImage(LOCATION_ICON_NAME, locationIndicatorBitmap);

        return symbolManager.create(new SymbolOptions()
                .withLatLng(new LatLng(
                        currentLatitude,
                        currentLongitude))
                .withIconImage(LOCATION_ICON_NAME));
    }

    private void showRainViewerFrame(Style style, boolean showNext) {
        post(() -> {
            if (!rainViewerTimestamps.isEmpty()) {
                currentRainViewerFrame = currentRainViewerFrame % rainViewerTimestamps.size();

                String layerId = getLayerName(rainViewerTimestamps.get(currentRainViewerFrame).first);

                if (style != null) {
                    for (Layer layer : style.getLayers()) {
                        if (layer.getId().startsWith("radar")) {
                            if (layer.getId().equals(layerId)) {
                                layer.setProperties(getRasterOpacity(true));
                            } else if (((RasterLayer) layer).getRasterOpacity().value > 0) {
                                layer.setProperties(getRasterOpacity(false));
                            }
                        }
                    }
                }


                if (radarSlider.getValueTo() - 1f > 0.01f) {
                    radarSlider.setValues((float) currentRainViewerFrame);
                }

                if (showNext) {
                    currentRainViewerFrame = (currentRainViewerFrame + 1) % rainViewerTimestamps.size();
                    postDelayed(nextFrameRunnable, ANIMATION_DURATION);
                }
            }
        });
    }

    private Promise<String, Void> addRainViewerLayers(Style style, RadarTheme radarTheme, RadarQuality radarQuality) {
        return new HttpRequest("https://api.rainviewer.com/public/weather-maps.json")
                .fetchAsync()
                .then(s -> {
                    JSONArray rainviewerData = new JSONObject(s)
                            .getJSONObject("radar")
                            .getJSONArray("past");

                    if (rainviewerData.length() <= 1) {
                        throw new RuntimeException("No timestamps from Rainviewer");
                    }

                    final ArrayList<Pair<Long, String>> timestamps = new ArrayList<>();

                    JSONObject timestampData;
                    for (int i = 0, l = rainviewerData.length(); i < l; i++) {
                        timestampData = rainviewerData.getJSONObject(i);

                        timestamps.add(new Pair<>(
                                timestampData.getLong("time"),
                                timestampData.getString("path")
                        ));
                    }

                    final String radarQualityString = radarQuality == RadarQuality.HIGH ? "512" : "256";
                    final String radarThemeString = radarTheme.getValue();

                    post(() -> {
                        if (currentTheme != radarTheme || currentRadarQuality != radarQuality) {
                            for (Layer l : style.getLayers()) {
                                if (l.getId().startsWith("radar")) {
                                    style.removeLayer(l);
                                    style.removeSource(l.getId());
                                }
                            }

                            rainViewerTimestamps.clear();

                            currentTheme = radarTheme;
                            currentRadarQuality = radarQuality;
                        }

                        String name;

                        Comparator<Pair<Long, String>> c = (a, b) -> Long.compare(a.first, b.first);

                        long firstNewTimestamp = !timestamps.isEmpty() ? Collections.min(timestamps, c).first : 0;
                        long lastNewTimestamp = !timestamps.isEmpty() ? Collections.max(timestamps, c).first : 0;

                        for (Pair<Long, String> oldRainViewerTimestamp : rainViewerTimestamps) {
                            name = getLayerName(oldRainViewerTimestamp.first);

                            if (firstNewTimestamp > oldRainViewerTimestamp.first) {
                                style.removeLayer(name);
                                style.removeSource(name);
                            } else {
                                Layer layer = style.getLayer(name);

                                if (layer != null) {
                                    layer.setProperties(getRasterOpacity(false));
                                }
                            }
                        }

                        for (Pair<Long, String> newRainViewerTimestamp : timestamps) {
                            name = getLayerName(newRainViewerTimestamp.first);

                            if (style.getSource(name) == null) {
                                TileSet tileSet = new TileSet(
                                        "",
                                        "https://tilecache.rainviewer.com" +
                                                newRainViewerTimestamp.second + "/" +
                                                radarQualityString +
                                                "/{z}/{x}/{y}/" +
                                                radarThemeString +
                                                "/1_1.png");
                                tileSet.setAttribution(RAINVIEWER_ATTRIBUTION);

                                style.addSource(new RasterSource(name, tileSet, 256));
                                style.addLayerAbove(
                                        new RasterLayer(name, name)
                                                .withProperties(
                                                        getRasterOpacity(newRainViewerTimestamp.first == lastNewTimestamp)),
                                        "highway_motorway_bridge_inner");
                            }
                        }

                        Layer lastLayer = style.getLayer(getLayerName(lastNewTimestamp));

                        if (lastLayer != null) {
                            lastLayer.setProperties(getRasterOpacity(true));
                        }

                        rainViewerTimestamps.clear();
                        rainViewerTimestamps.addAll(timestamps);

                        if (timestamps.size() > 1) {
                            radarSlider.setValueTo(timestamps.size() - 1f);
                            radarSlider.setValues(timestamps.size() - 1f);
                        }
                    });
                }, t -> logError(getContext().getString(R.string.error_radar_image), (Exception) t));
    }

    private float getTextScaling() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();

        float px_to_sp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10, metrics);
        float px_to_dp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, metrics);

        return 1.3f * px_to_sp / px_to_dp;
    }

    private static PropertyValue<Float> getRasterOpacity(boolean opaque) {
        return PropertyFactory.rasterOpacity(opaque ? 1f : 0f);
    }

    private String getLayerName(long timestamp) {
        return "radar" + (Long.MAX_VALUE - timestamp);
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
        if (v.getId() == R.id.button_zoomin) {
            weatherMapAnimationListener.zoomIn();
        } else if (v.getId() == R.id.button_zoomout) {
            weatherMapAnimationListener.zoomOut();
        } else if (v.getId() == R.id.button_compasscenter) {
            weatherMapAnimationListener.centerCamera();
        } else if (v.getId() == R.id.button_compassnorth) {
            weatherMapAnimationListener.resetCameraRotation();
        } else if (v.getId() == R.id.button_playpause) {
            playPause();
        } else if (v.getId() == R.id.button_attribution) {
            showAttributionDialog();
        } else if (v.getId() == R.id.button_legend) {
            dialogHelper.showLegendDialog(currentTheme.ordinal());
        } else if (v.getId() == R.id.button_expand) {
            if (onFullscreenClickedListener != null) {
                onFullscreenClickedListener.onFullscreenClicked(!isFullscreen);
            }
        }
    }

    public void setOnFullscreenClickedListener(RadarCardView.OnFullscreenClickedListener onFullscreenClickedListener) {
        this.onFullscreenClickedListener = onFullscreenClickedListener;
    }

    //based on https://github.com/klokantech/openmaptiles-language
    private String withStyledLocalizedText(String styleJson, boolean isNightModeActive) throws JSONException {
        JSONObject styleJsonObj = new JSONObject(styleJson);

        String language = Locale.getDefault().getLanguage();
        boolean isLatin = !Arrays.asList("ar", "hy", "be", "bg", "zh", "ka", "el", "he",
                "ja", "kn", "kk", "ko", "mk", "ru", "sr", "th", "uk").contains(language);

        String textField = "{name:" + language + "}\n{name:" + (isLatin ? "nonlatin" : "latin") + "}";
        JSONArray hasLanguageFilter = new JSONArray("[\"has\",\"name:" + language + "\"]");
        JSONArray notHasLanguageFilter = new JSONArray("[\"!\", [\"has\",\"name:" + language + "\"]]");

        String textColor = isNightModeActive ? "rgba(255,255,255,0.8)" : "rgba(0,0,0,0.8)";
        String textHaloColor = isNightModeActive ? "hsl(0, 0%, 20%)" : "rgb(242,243,240)";
        double textScaling = getTextScaling();

        styleJsonObj.put("sprite", null);

        JSONArray layersArray = styleJsonObj.getJSONArray("layers");

        for (int i = layersArray.length() - 1; i >= 0; i--) {
            JSONObject layer = layersArray.getJSONObject(i);

            if (layer.optString("type").equals("symbol")) {
                JSONObject layerPaint = layer.has("paint") ? layer.getJSONObject("paint") : new JSONObject();

                layerPaint.put("text-color", textColor);
                layerPaint.put("text-halo-color", textHaloColor);
                layerPaint.put("text-halo-width", 1);
                layerPaint.put("text-halo-blur", 1);

                layer.put("paint", layerPaint);

                JSONObject layerLayout = layer.getJSONObject("layout");

                if (layerLayout.has("text-size")) {
                    if (layerLayout.optJSONObject("text-size") == null) {
                        layerLayout.put("text-size", layerLayout.getDouble("text-size") * textScaling);
                    } else {
                        //It's a 2D array
                        JSONObject textSize = layerLayout.getJSONObject("text-size");
                        JSONArray textSizeStops = textSize.getJSONArray("stops");

                        for (int j = 0; j < textSizeStops.length(); j++) {
                            JSONArray textSizeStops2 = textSizeStops.getJSONArray(j);
                            for (int k = 0; k < textSizeStops2.length(); k++) {
                                textSizeStops2.put(k, textSizeStops2.getDouble(k) * textScaling);
                            }
                            textSizeStops.put(j, textSizeStops2);
                        }

                        textSize.put("stops", textSizeStops);
                        layerLayout.put("text-size", textSize);
                    }
                } else {
                    layerLayout.put("text-size", 10 * textScaling);
                }

                layer.put("layout", layerLayout);

                if (!layer.getJSONArray("filter").toString().startsWith("[\"all\",")) {
                    layer.put("filter", new JSONArray("[\"all\"," + layer.getJSONArray("filter") + "]"));
                }

                JSONObject duplicateLayer = new JSONObject(layer.toString());

                JSONArray layerFilter = layer.getJSONArray("filter");
                layerFilter.put(notHasLanguageFilter);
                layer.put("filter", layerFilter);

                duplicateLayer.put("id", duplicateLayer.getString("id") + "_" + language);

                JSONArray duplicateLayerFilter = duplicateLayer.getJSONArray("filter");
                duplicateLayerFilter.put(hasLanguageFilter);
                duplicateLayer.put("filter", duplicateLayerFilter);

                JSONObject duplicateLayerLayout = duplicateLayer.getJSONObject("layout");
                duplicateLayerLayout.put("text-field", textField);
                duplicateLayer.put("layout", duplicateLayerLayout);

                //insert duplicate layer after layer
                for (int j = layersArray.length() - 1; j >= i + 1; j--) {
                    layersArray.put(j + 1, layersArray.get(j));
                }

                layersArray.put(i + 1, duplicateLayer);

                layersArray.put(i, layer);
            }
        }

        styleJsonObj.put("layers", layersArray);

        return styleJsonObj.toString();
    }

    private void logError(String message, Exception e) {
        if (snackbarHelper == null) {
            e.printStackTrace();
        } else {
            snackbarHelper.notifyError(message, e);
        }
    }

    public void setOnMapClickListener(MapLibreMap.OnMapClickListener onMapClickListener) {
        this.onMapClickListener = onMapClickListener;
    }

    private void showAttributionDialog() {
        LinkedList<String> attributions = new LinkedList<>();

        attributions.add(MAPLIBRE_ATTRIBUTION);
        attributions.add(CARTO_ATTRIBUTION);
        attributions.add(OSM_ATTRIBUTION);

        if (weatherMapViewType == WeatherMapViewType.RADAR) {
            attributions.add(RAINVIEWER_ATTRIBUTION);
        }

        dialogHelper
                .showAttributionDialog(StringUtils.fromHtml(String.join("<br><br>", attributions)));
    }


    private void setSliderLabelVisible(boolean visible) {
        radarSlider.setLabelBehavior(visible | isPlaying ? LabelFormatter.LABEL_VISIBLE : LabelFormatter.LABEL_FLOATING);
    }

    // TODO Handle failed calls to RainViewer and retry them
    private void setHTTPOptions() {
        OkHttpClient client = new OkHttpClient.Builder()
                .addNetworkInterceptor(new CounterInterceptor())
                .build();

        HttpRequestUtil.setOkHttpClient(client);
        HttpRequestUtil.setLogEnabled(false);
    }

    private class CounterInterceptor implements Interceptor {
        private final AtomicInteger ACTIVE_REQUESTS = new AtomicInteger(0);
        private final AtomicBoolean IS_LOADING = new AtomicBoolean(false);

        private void updateLoadingIndicator(int requestCount) {
            boolean isLoading = IS_LOADING.get();
            if (isLoading && requestCount == 0) {
                IS_LOADING.set(false);
                post(() -> weatherMapAnimationListener.updateLoadingIndicator(false));
            } else if (!isLoading && requestCount > 0) {
                IS_LOADING.set(true);
                post(() -> weatherMapAnimationListener.updateLoadingIndicator(true));
            }
        }

        @NonNull
        @Override
        public Response intercept(@NonNull Chain chain) throws IOException {
            updateLoadingIndicator(ACTIVE_REQUESTS.incrementAndGet());

            try {
                return chain.proceed(chain.request());
            } finally {
                updateLoadingIndicator(ACTIVE_REQUESTS.decrementAndGet());
            }
        }
    }

    private enum WeatherMapViewType {
        RADAR,
        MAPPICKER
    }

    private class WeatherMapAnimationListener implements MapLibreMap.OnRotateListener,
            MapLibreMap.OnMapClickListener, MapLibreMap.OnCameraMoveStartedListener {
        private boolean areControlsVisible = true;
        private boolean isRotationControlVisible = false;
        private boolean isPanControlVisible = false;
        private final MapLibreMap mapLibreMap;

        public WeatherMapAnimationListener(MapLibreMap mapLibreMap) {
            this.mapLibreMap = mapLibreMap;
        }

        public boolean onMapClick(@NonNull LatLng point) {
            areControlsVisible = !areControlsVisible;

            doAnimation(buttonExpand, areControlsVisible);
            doAnimation(buttonPlayPause, areControlsVisible);
            doAnimation(radarSlider, areControlsVisible);
            doAnimation(buttonZoomIn, areControlsVisible);
            doAnimation(buttonZoomOut, areControlsVisible);
            doAnimation(buttonAttribution, areControlsVisible);
            doAnimation(buttonLegend, areControlsVisible);

            if (isRotationControlVisible) {
                doAnimation(buttonCompassNorthFrame, areControlsVisible);
                doAnimation(buttonCompassNorth, areControlsVisible);
            }

            if (isPanControlVisible) {
                doAnimation(buttonCompassCenter, areControlsVisible);
            }

            return true;
        }

        @Override
        public void onRotateBegin(@NonNull RotateGestureDetector detector) {
            if (buttonCompassNorthFrame.getVisibility() == View.GONE) {
                isRotationControlVisible = true;

                if (areControlsVisible) {
                    doAnimation(buttonCompassNorthFrame, true);
                }
            }
        }

        @Override
        public void onRotate(@NonNull RotateGestureDetector detector) {
            buttonCompassNorthIcon.setRotation((float) -mapLibreMap.getCameraPosition().bearing - 45);
        }

        @Override
        public void onRotateEnd(@NonNull RotateGestureDetector detector) {
        }

        @Override
        public void onCameraMoveStarted(int reason) {
            if (buttonCompassCenter.getVisibility() == View.GONE &&
                    reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                isPanControlVisible = true;

                if (areControlsVisible) {
                    doAnimation(buttonCompassCenter, true);
                }
            }
        }

        public void centerCamera() {
            isPanControlVisible = false;

            doAnimation(buttonCompassCenter, false);

            mapLibreMap.animateCamera(
                    CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(new LatLng(
                                            currentLatitude,
                                            currentLongitude))
                                    .zoom(7)
                                    .build()
                    ),
                    ANIMATION_DURATION);
        }

        public void resetCameraRotation() {
            isRotationControlVisible = false;

            mapLibreMap.cancelAllVelocityAnimations();
            mapLibreMap.animateCamera(CameraUpdateFactory.bearingTo(0), ANIMATION_DURATION);
            doAnimation(buttonCompassNorthFrame, false);
        }

        public void zoomIn() {
            isPanControlVisible = true;

            if (areControlsVisible) {
                doAnimation(buttonCompassCenter, true);
            }

            mapLibreMap.animateCamera(CameraUpdateFactory.zoomBy(1), ANIMATION_DURATION);
        }

        public void zoomOut() {
            isPanControlVisible = true;

            if (areControlsVisible) {
                doAnimation(buttonCompassCenter, true);
            }

            mapLibreMap.animateCamera(CameraUpdateFactory.zoomBy(-1), ANIMATION_DURATION);
        }

        public void updateLoadingIndicator(boolean toVisible) {
            doAnimation(radarLoadingIndicator, toVisible);
        }

        private void doAnimation(View v, boolean toVisible) {
            v.clearAnimation();

            v.animate()
                    .alpha(toVisible ? 1f : 0f)
                    .setDuration(CONTROL_ANIMATION_DURATION)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            v.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            v.setVisibility(toVisible ? View.VISIBLE : View.GONE);
                        }
                    });
        }
    }
}