/*
 *   Copyright 2019 - 2023 Tyler Williamson
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
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.RangeSlider;
import com.mapbox.android.gestures.RotateGestureDetector;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.attribution.Attribution;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.AttributionDialogManager;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.module.http.HttpRequestUtil;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.PropertyValue;
import com.mapbox.mapboxsdk.style.layers.RasterLayer;
import com.mapbox.mapboxsdk.style.sources.RasterSource;
import com.mapbox.mapboxsdk.style.sources.Source;
import com.mapbox.mapboxsdk.style.sources.TileSet;
import com.ominous.quickweather.R;
import com.ominous.quickweather.activity.BaseActivity;
import com.ominous.quickweather.card.RadarCardView;
import com.ominous.quickweather.pref.RadarQuality;
import com.ominous.quickweather.pref.WeatherPreferences;
import com.ominous.quickweather.util.SnackbarHelper;
import com.ominous.quickweather.web.CachedWebServer;
import com.ominous.tylerutils.browser.CustomTabs;
import com.ominous.tylerutils.http.HttpRequest;
import com.ominous.tylerutils.util.ApiUtils;
import com.ominous.tylerutils.util.ColorUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

//todo start loading radar images faster
public class WeatherMapView extends ConstraintLayout implements View.OnClickListener {
    private final static int ANIMATION_DURATION = 500;
    private final static int CONTROL_ANIMATION_DURATION = 250;
    private final static String RAINVIEWER_ATTRIBUTION = "<a href=\"https://rainviewer.com\">RainViewer</a>";

    private final MapView mapView;
    private final MaterialButton buttonExpand;
    private final MaterialButton buttonCompassCenter;
    private final MaterialButton buttonPlayPause;
    private final MaterialButton buttonZoomIn;
    private final MaterialButton buttonZoomOut;
    private final MaterialButton buttonCompassNorth;
    private final ImageView buttonCompassNorthIcon;
    private final RangeSlider radarSlider;
    private final FrameLayout buttonCompassNorthFrame;
    private SnackbarHelper snackbarHelper;

    private final WeatherMapViewType weatherMapViewType;

    private RadarCardView.OnFullscreenClickedListener onFullscreenClickedListener;

    private double currentLatitude = 0;
    private double currentLongitude = 0;
    private int currentRainViewerFrame = 0;

    private final ArrayList<Pair<Long, String>> rainViewerTimestamps = new ArrayList<>();
    private boolean isPlaying = false;
    private boolean isFullscreen = false;

    private WeatherMapAnimationListener weatherMapAnimationListener;

    private final CachedWebServer cachedWebServer;

    private final Runnable nextFrameRunnable;

    private MapboxMap.OnMapClickListener onMapClickListener;

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

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.WeatherMapView,
                0,
                0);

        int mapViewTypeIndex = a.getInteger(R.styleable.WeatherMapView_weatherMapViewType, -1);
        a.recycle();

        if (mapViewTypeIndex == -1) {
            throw new IllegalArgumentException("Invalid Map View Type");
        }

        Mapbox.getInstance(context);
        HttpRequestUtil.setLogEnabled(false);
        inflate(context, R.layout.view_radar, this);

        setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        weatherMapViewType = WeatherMapViewType.values()[mapViewTypeIndex];

        mapView = findViewById(R.id.mapview);
        buttonExpand = findViewById(R.id.button_expand);
        buttonCompassNorthIcon = findViewById(R.id.button_compassnorth_icon);
        buttonCompassNorthFrame = findViewById(R.id.button_compassnorth_frame);
        buttonCompassCenter = findViewById(R.id.button_compasscenter);
        buttonPlayPause = findViewById(R.id.button_playpause);
        radarSlider = findViewById(R.id.radar_slider);
        buttonZoomIn = findViewById(R.id.button_zoomin);
        buttonZoomOut = findViewById(R.id.button_zoomout);
        buttonCompassNorth = findViewById(R.id.button_compassnorth);

        buttonZoomIn.setOnClickListener(this);
        buttonZoomOut.setOnClickListener(this);
        buttonCompassNorth.setOnClickListener(this);
        buttonCompassCenter.setOnClickListener(this);
        buttonPlayPause.setOnClickListener(this);
        buttonExpand.setOnClickListener(this);

        cachedWebServer = CachedWebServer.getInstance();

        nextFrameRunnable = () -> mapView.getMapAsync(mapboxMap -> showRainViewerFrame(mapboxMap, true));

        mapView.getMapAsync(mapboxMap -> {
            loadStyle(mapboxMap);

            weatherMapAnimationListener = new WeatherMapAnimationListener(mapboxMap);

            mapboxMap.addOnRotateListener(weatherMapAnimationListener);

            if (weatherMapViewType == WeatherMapViewType.RADAR) {
                mapboxMap.addOnMapClickListener(weatherMapAnimationListener);
                mapboxMap.addOnCameraMoveStartedListener(weatherMapAnimationListener);
            }
        });

        if (weatherMapViewType == WeatherMapViewType.RADAR) {
            addRainViewerLayers();

            mapView.setOnTouchListener((v, event) -> {
                if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                    v.performClick();
                }

                //Finds RecyclerView and stops it from stealing touch event
                recursivelyFindRecyclerView(v.getParent());

                return false;
            });

            radarSlider.addOnChangeListener((slider, value, fromUser) -> {
                if (fromUser) {
                    currentRainViewerFrame = (int) value;
                    mapView.getMapAsync(mapboxMap -> showRainViewerFrame(mapboxMap, false));
                }
            });

            radarSlider.setLabelFormatter(new LabelFormatter() {
                final DateFormat simpleDateFormat = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT, Locale.getDefault());

                @NonNull
                @Override
                public String getFormattedValue(float value) {
                    return rainViewerTimestamps.size() > 0 ? simpleDateFormat.format(new Date(rainViewerTimestamps.get((int) value).first * 1000L)) : "";
                }
            });

            radarSlider.addOnSliderTouchListener(new RangeSlider.OnSliderTouchListener() {
                private boolean wasPlaying = false;

                @Override
                public void onStartTrackingTouch(@NonNull RangeSlider slider) {
                    wasPlaying = isPlaying;

                    if (isPlaying) {
                        mapView.getMapAsync(m -> playPause());
                    }
                }

                @Override
                public void onStopTrackingTouch(@NonNull RangeSlider slider) {
                    if (wasPlaying) {
                        mapView.getMapAsync(m -> playPause());
                    }
                }
            });
        }
    }

    public void attachToActivity(BaseActivity activity) {
        activity.setLifecycleListener(new BaseActivity.LifecycleListener() {
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
            }

            @Override
            public void onPause() {
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

        snackbarHelper = new SnackbarHelper(activity.findViewById(R.id.coordinator_layout));

        cachedWebServer.setSnackbarHelper(snackbarHelper);
    }

    private void loadStyle(MapboxMap mapboxMap) {
        mapboxMap.getUiSettings()
                .setAttributionDialogManager(new CustomTabsAttributionDialogManager(getContext(), mapboxMap));

        new HttpRequest(ColorUtils.isNightModeActive(getContext()) ?
                cachedWebServer.getStadiaUrl() + "/styles/alidade_smooth_dark.json" :
                cachedWebServer.getStadiaUrl() + "/styles/alidade_smooth.json")
                .fetchAsync()
                .then(styleJson -> {
                    try {
                        final Style.Builder styleBuilder = new Style.Builder().fromJson(withStyledLocalizedText(new JSONObject(styleJson)).toString());

                        post(() -> mapboxMap.setStyle(styleBuilder,
                                style -> {
                                    if (weatherMapViewType == WeatherMapViewType.RADAR) {
                                        radarSlider.setVisibility(View.VISIBLE);
                                        buttonPlayPause.setVisibility(View.VISIBLE);
                                        buttonExpand.setVisibility(View.VISIBLE);
                                    } else {
                                        setupMappicker(mapboxMap);
                                    }
                                }));
                    } catch (JSONException e) {
                        logError(getContext().getString(R.string.error_radar_map), e);
                    }
                });
    }

    public void setCamera(double latitude, double longitude) {
        currentLatitude = latitude;
        currentLongitude = longitude;

        if (rainViewerTimestamps.size() > 0) {
            currentRainViewerFrame = rainViewerTimestamps.size() - 1;
            mapView.getMapAsync(mapboxMap -> showRainViewerFrame(mapboxMap, false));
        }

        addRainViewerLayers();

        mapView.getMapAsync(mapboxMap -> {
            weatherMapAnimationListener.centerCamera();

            if (isPlaying) {
                playPause();
            }
        });
    }

    public void setFullscreen(boolean state) {
        isFullscreen = state;

        buttonExpand.setIcon(ContextCompat.getDrawable(getContext(), state ? R.drawable.ic_fullscreen_exit_white_24dp : R.drawable.ic_fullscreen_white_24dp));
    }

    private void playPause() {
        isPlaying = !isPlaying;

        if (isPlaying) {
            post(nextFrameRunnable);
        } else {
            removeCallbacks(nextFrameRunnable);
        }

        buttonPlayPause.setIcon(ContextCompat.getDrawable(getContext(), isPlaying ? R.drawable.ic_pause_white_24dp : R.drawable.ic_play_arrow_white_24dp));
    }

    private Bitmap createMapPickerIcon() {
        Drawable drawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_add_location_white_24dp);

        if (drawable != null) {
            final int SIZE = drawable.getIntrinsicWidth() * 2;
            drawable.setColorFilter(ContextCompat.getColor(getContext(), R.color.color_accent_text), PorterDuff.Mode.SRC_IN);
            Bitmap bitmap = Bitmap.createBitmap(SIZE, SIZE * 2, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, SIZE, SIZE);
            drawable.draw(canvas);

            return bitmap;
        } else {
            return null;
        }
    }

    @SuppressWarnings({"deprecation", "ConstantConditions"})
    private void setupMappicker(MapboxMap mapboxMap) {
        final Marker marker = mapboxMap.addMarker(
                new MarkerOptions()
                        .setIcon(IconFactory
                                .getInstance(getContext())
                                .fromBitmap(createMapPickerIcon()))
                        .setPosition(new LatLng(0, 0)));

        mapboxMap.addOnMapClickListener(point -> {
            marker.setPosition(point);

            return onMapClickListener != null && onMapClickListener.onMapClick(point);
        });
    }

    private void showRainViewerFrame(MapboxMap mapboxMap, boolean showNext) {
        currentRainViewerFrame = currentRainViewerFrame % rainViewerTimestamps.size();

        String layerId = "radar" + rainViewerTimestamps.get(currentRainViewerFrame).first;

        Style style = mapboxMap.getStyle();

        if (style != null) {
            Layer shouldHideLayer = null;
            Layer shouldShowLayer = null;

            for (Layer layer : style.getLayers()) {
                if (layer.getId().startsWith("radar")) {
                    if (layer.getId().equals(layerId)) {
                        shouldShowLayer = layer;
                    } else if (((RasterLayer) layer).getRasterOpacity().value > 0) {
                        shouldHideLayer = layer;
                    }
                }
            }

            if (shouldShowLayer != null) {
                shouldShowLayer.setProperties(getRasterOpacity(true));
            }

            if (shouldHideLayer != null) {
                shouldHideLayer.setProperties(getRasterOpacity(false));
            }
        }

        radarSlider.setValues((float) currentRainViewerFrame);

        if (showNext) {
            currentRainViewerFrame = (currentRainViewerFrame + 1) % rainViewerTimestamps.size();
            postDelayed(nextFrameRunnable, ANIMATION_DURATION);
        }
    }

    private void addRainViewerLayers() {
        new HttpRequest(cachedWebServer.getRainviewerUrl() + "/public/weather-maps.json")
                .fetchAsync()
                .then(s -> {
                    JSONArray rainviewerData = new JSONObject(s)
                            .getJSONObject("radar")
                            .getJSONArray("past");

                    final ArrayList<Pair<Long, String>> timestamps = new ArrayList<>();

                    if (rainviewerData.length() == 0) {
                        throw new RuntimeException("No timestamps from Rainviewer");
                    }

                    JSONObject timestampData;
                    for (int i = 0, l = rainviewerData.length(); i < l; i++) {
                        timestampData = rainviewerData.getJSONObject(i);

                        timestamps.add(new Pair<>(
                                timestampData.getLong("time"),
                                timestampData.getString("path")
                        ));
                    }

                    post(() -> mapView.getMapAsync(mapboxMap -> mapboxMap.getStyle(style -> {
                        String name;
                        for (Pair<Long, String> oldRainViewerTimestamp : rainViewerTimestamps) {
                            name = "radar" + oldRainViewerTimestamp.first;

                            if (timestamps.size() > 0 &&
                                    timestamps.get(0).first > oldRainViewerTimestamp.first &&
                                    styleHasSource(style, name)) {
                                style.removeLayer(name);
                                style.removeSource(name);
                            } else {
                                Layer layer = style.getLayer(name);

                                if (layer != null) {
                                    layer.setProperties(getRasterOpacity(false));
                                }
                            }
                        }

                        String radarResolution = WeatherPreferences
                                .getInstance(getContext())
                                .getRadarQuality() == RadarQuality.HIGH ? "512" : "256";

                        for (Pair<Long, String> newRainViewerTimestamp : timestamps) {
                            name = "radar" + newRainViewerTimestamp.first;

                            if (!styleHasSource(style, name)) {
                                TileSet tileSet = new TileSet("",
                                        cachedWebServer.getRainviewerUrl() +
                                                newRainViewerTimestamp.second + "/" +
                                                radarResolution +
                                                "/{z}/{x}/{y}/2/1_1.png");
                                tileSet.setAttribution(RAINVIEWER_ATTRIBUTION);

                                style.addSource(new RasterSource(name, tileSet, 256));
                                style.addLayerAbove(new RasterLayer(name, name).withProperties(getRasterOpacity(false)), "highway_motorway_bridge_inner");
                            }
                        }

                        Layer lastLayer = style.getLayer("radar" + timestamps.get(timestamps.size() - 1).first);

                        if (lastLayer != null) {
                            lastLayer.setProperties(getRasterOpacity(true));
                        }

                        rainViewerTimestamps.clear();
                        rainViewerTimestamps.addAll(timestamps);

                        radarSlider.setValueTo(timestamps.size() - 1f);
                        radarSlider.setValues(timestamps.size() - 1f);
                    })));
                }, t -> logError(getContext().getString(R.string.error_radar_image), (Exception) t));
    }

    private float getTextScaling() {
        return 1.2f * getResources().getDisplayMetrics().scaledDensity / getResources().getDisplayMetrics().density;
    }

    private static PropertyValue<Float> getRasterOpacity(boolean opaque) {
        return PropertyFactory.rasterOpacity(opaque ? 1f : 0f);
    }

    private boolean styleHasSource(Style style, String sourceId) {
        for (Source source : style.getSources()) {
            if (source.getId().equals(sourceId)) {
                return true;
            }
        }
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
        if (v.getId() == R.id.button_zoomin) {
            weatherMapAnimationListener.zoomIn();
        } else if (v.getId() == R.id.button_zoomout) {
            weatherMapAnimationListener.zoomOut();
        } else if (v.getId() == R.id.button_compasscenter) {
            weatherMapAnimationListener.centerCamera();
        } else if (v.getId() == R.id.button_compassnorth) {
            weatherMapAnimationListener.resetCameraRotation();
        } else if (v.getId() == R.id.button_playpause) {
            mapView.getMapAsync(m -> playPause());
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
    private JSONObject withStyledLocalizedText(JSONObject styleJson) throws JSONException {
        String language = Locale.getDefault().getLanguage();
        boolean isLatin = !Arrays.asList("ar", "hy", "be", "bg", "zh", "ka", "el", "he",
                "ja", "kn", "kk", "ko", "mk", "ru", "sr", "th", "uk").contains(language);

        String textField = "{name:" + language + "}\n{name:" + (isLatin ? "nonlatin" : "latin") + "}";
        JSONArray hasLanguageFilter = new JSONArray("[\"has\",\"name:" + language + "\"]");
        JSONArray notHasLanguageFilter = new JSONArray("[\"!\", [\"has\",\"name:" + language + "\"]]");

        boolean isNightModeActive = ColorUtils.isNightModeActive(getContext());
        String textColor = isNightModeActive ? "rgba(255,255,255,0.8)" : "rgba(0,0,0,0.8)";
        String textHaloColor = isNightModeActive ? "hsl(0, 0%, 20%)" : "rgb(242,243,240)";
        double textScaling = getTextScaling();

        styleJson.put("sprite", null);

        JSONArray layersArray = styleJson.getJSONArray("layers");

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

        styleJson.put("layers", layersArray);

        return styleJson;
    }

    private void logError(String message, Exception e) {
        if (snackbarHelper == null) {
            e.printStackTrace();
        } else {
            snackbarHelper.notifyError(message, e);
        }
    }

    public void setOnMapClickListener(MapboxMap.OnMapClickListener onMapClickListener) {
        this.onMapClickListener = onMapClickListener;
    }

    private static class CustomTabsAttributionDialogManager extends AttributionDialogManager {
        private final Context context;

        public CustomTabsAttributionDialogManager(@NonNull Context context, @NonNull MapboxMap mapboxMap) {
            super(context, mapboxMap);

            this.context = context;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onClick(DialogInterface dialog, int which) {
            ArrayList<Attribution> uriList = new ArrayList<>((Set<Attribution>) ApiUtils.getPrivateField(AttributionDialogManager.class, this, "attributionSet"));
            CustomTabs.getInstance(context).launch(context, Uri.parse(uriList.get(which).getUrl()));
        }
    }

    private enum WeatherMapViewType {
        RADAR,
        MAPPICKER
    }

    private class WeatherMapAnimationListener implements MapboxMap.OnRotateListener,
            MapboxMap.OnMapClickListener, MapboxMap.OnCameraMoveStartedListener {
        private boolean areControlsVisible = true;
        private boolean isRotationControlVisible = false;
        private boolean isPanControlVisible = false;
        private final MapboxMap mapboxMap;

        public WeatherMapAnimationListener(MapboxMap mapboxMap) {
            this.mapboxMap = mapboxMap;
        }

        public boolean onMapClick(@NonNull LatLng point) {
            areControlsVisible = !areControlsVisible;

            doAnimation(buttonExpand, areControlsVisible);
            doAnimation(buttonPlayPause, areControlsVisible);
            doAnimation(radarSlider, areControlsVisible);
            doAnimation(buttonZoomIn, areControlsVisible);
            doAnimation(buttonZoomOut, areControlsVisible);

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
            buttonCompassNorthIcon.setRotation((float) -mapboxMap.getCameraPosition().bearing - 45);
        }

        @Override
        public void onRotateEnd(@NonNull RotateGestureDetector detector) {
        }

        @Override
        public void onCameraMoveStarted(int reason) {
            if (buttonCompassCenter.getVisibility() == View.GONE &&
                    reason == MapboxMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                isPanControlVisible = true;

                if (areControlsVisible) {
                    doAnimation(buttonCompassCenter, true);
                }
            }
        }

        public void centerCamera() {
            isPanControlVisible = false;

            doAnimation(buttonCompassCenter, false);

            mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                    .target(new LatLng(
                            currentLatitude,
                            currentLongitude))
                    .zoom(7)
                    .build()), ANIMATION_DURATION);
        }

        public void resetCameraRotation() {
            isRotationControlVisible = false;

            mapboxMap.cancelAllVelocityAnimations();
            mapboxMap.animateCamera(CameraUpdateFactory.bearingTo(0), ANIMATION_DURATION);
            doAnimation(buttonCompassNorthFrame, false);
        }

        public void zoomIn() {
            isPanControlVisible = true;

            if (areControlsVisible) {
                doAnimation(buttonCompassCenter, true);
            }

            mapboxMap.animateCamera(CameraUpdateFactory.zoomIn(), ANIMATION_DURATION);
        }

        public void zoomOut() {
            isPanControlVisible = true;

            if (areControlsVisible) {
                doAnimation(buttonCompassCenter, true);
            }

            mapboxMap.animateCamera(CameraUpdateFactory.zoomOut(), ANIMATION_DURATION);
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