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

package com.ominous.quickweather.card;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.ominous.quickweather.R;
import com.ominous.quickweather.data.PrecipType;
import com.ominous.quickweather.data.WeatherModel;
import com.ominous.quickweather.data.CurrentWeather;
import com.ominous.quickweather.pref.TemperatureUnit;
import com.ominous.quickweather.pref.WeatherPreferences;
import com.ominous.quickweather.util.ColorHelper;
import com.ominous.quickweather.util.GraphHelper;
import com.ominous.quickweather.util.WeatherUtils;
import com.ominous.tylerutils.async.Promise;
import com.ominous.tylerutils.util.ColorUtils;
import com.ominous.tylerutils.util.LocaleUtils;
import com.ominous.tylerutils.util.ViewUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.TreeSet;

public class GraphCardView extends BaseCardView {
    private final static int ONE_HOUR = 60 * 60;
    private final static Comparator<GraphHelper.IGraphPoint> pointYComparator = (o1, o2) -> Float.compare(o1.getY(), o2.getY());
    private final static Comparator<GraphHelper.IGraphPoint> pointXComparator = (o1, o2) -> Float.compare(o1.getX(), o2.getX());
    final int LEFT_PADDING;
    final int RIGHT_PADDING;
    final int TOP_PADDING;
    final int BOTTOM_PADDING;
    final float TEXT_SIZE;
    final Drawable thermDrawable;
    private final ImageView graphImageView;
    private final HorizontalScrollView scrollView;
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    private WeatherModel weatherModel;
    private boolean shouldGenerateGraph = false;

    public GraphCardView(Context context) {
        super(context);

        inflate(context, R.layout.card_graph, this);

        graphImageView = findViewById(R.id.graph_image_view);
        scrollView = findViewById(R.id.scrollview);

        Resources resources = context.getResources();

        LEFT_PADDING = resources.getDimensionPixelSize(R.dimen.margin_double);
        RIGHT_PADDING = resources.getDimensionPixelSize(R.dimen.margin_half);
        TOP_PADDING = resources.getDimensionPixelSize(R.dimen.margin_quarter);
        BOTTOM_PADDING = resources.getDimensionPixelSize(R.dimen.text_size_regular);
        TEXT_SIZE = resources.getDimension(R.dimen.text_size_regular);

        thermDrawable = ContextCompat.getDrawable(context, R.drawable.thermometer_25);

        setContentDescription(resources.getString(R.string.card_graph_desc));

        ViewUtils.setAccessibilityInfo(this, null, null);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scrollView.onTouchEvent(event);

        return super.onTouchEvent(event);
    }

    private static Paint getFillPaint() {
        Paint paint = new Paint();

        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(false);
        paint.setStrokeWidth(0);

        return paint;
    }

    private static Paint getStrokePaint() {
        Paint paint = new Paint();

        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(5);

        return paint;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        if (shouldGenerateGraph) {
            generateGraph(weatherModel);

            shouldGenerateGraph = false;
        }
    }

    @Override
    public void update(WeatherModel weatherModel, int position) {
        this.weatherModel = weatherModel;

        graphImageView.setImageBitmap(null);

        shouldGenerateGraph = true;
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        shouldGenerateGraph = true;
    }

    @Override
    public void onClick(View v) {
        //Nothing
    }

    private void generateGraph(WeatherModel weatherModel) {
        Promise
                .create(weatherModel)
                .then((m) -> {
                    WeatherUtils weatherUtils = WeatherUtils.getInstance(getContext());
                    WeatherPreferences weatherPreferences = WeatherPreferences.getInstance(getContext());
                    ColorHelper colorHelper = ColorHelper.getInstance(getContext());
                    boolean isDarkModeActive = ColorUtils.isNightModeActive(getContext());

                    Bitmap graphBitmap = m.date == null ?
                            generateCurrentGraph(colorHelper, weatherUtils, weatherPreferences.getTemperatureUnit(), isDarkModeActive, m.currentWeather) :
                            generateForecastGraph(colorHelper, weatherUtils, weatherPreferences.getTemperatureUnit(), isDarkModeActive, m);//background

                    mainThreadHandler.post(() ->
                            graphImageView.setImageBitmap(graphBitmap));//foreground
                });
    }

    private Bitmap generateCurrentGraph(ColorHelper colorHelper,
                                        WeatherUtils weatherUtils,
                                        TemperatureUnit temperatureUnit,
                                        boolean isDarkModeActive,
                                        CurrentWeather response) {
        ArrayList<TemperatureGraphPoint> temperaturePoints = new ArrayList<>(48);
        ArrayList<PrecipitationGraphPoint> precipitationPoints = new ArrayList<>(48);

        //need to keep the longs short or the cast to float and back will break
        long start = response.hourly[0].dt / 1000L;

        for (int i = 0, l = 48; i < l; i++) {
            long x = response.hourly[i].dt / 1000L - start;

            temperaturePoints.add(new TemperatureGraphPoint(
                    colorHelper,
                    weatherUtils,
                    temperatureUnit,
                    isDarkModeActive,
                    x,
                    (float) response.hourly[i].temp));
            precipitationPoints.add(new PrecipitationGraphPoint(
                    colorHelper,
                    x,
                    Math.min((float) response.hourly[i].precipitationIntensity, 2f),
                    response.hourly[i].precipitationType
            ));
        }

        final ArrayList<XGraphLabel> xGraphLabels = new ArrayList<>();

        for (TemperatureGraphPoint point : temperaturePoints) {
            xGraphLabels.add(new XGraphLabel(
                    (int) point.x,
                    LocaleUtils.formatHour(
                            getContext(),
                            Locale.getDefault(),
                            new Date((((int) point.x) + start) * 1000),
                            weatherModel.currentWeather.timezone)));
        }

        final int width = (this.getMeasuredWidth() - 2 * getResources().getDimensionPixelSize(R.dimen.margin_half)) * 2;
        final int height = this.getMeasuredHeight() - 2 * getResources().getDimensionPixelSize(R.dimen.margin_half);

        return doGenerateGraph(width, height, colorHelper, isDarkModeActive, temperatureUnit, temperaturePoints, precipitationPoints, xGraphLabels);
    }

    private Bitmap generateForecastGraph(ColorHelper colorHelper,
                                         WeatherUtils weatherUtils,
                                         TemperatureUnit temperatureUnit,
                                         boolean isDarkModeActive,
                                         WeatherModel weatherModel) {
        final TreeSet<TemperatureGraphPoint> temperaturePointsSet = new TreeSet<>(pointXComparator);
        final TreeSet<PrecipitationGraphPoint> precipitationPointsSet = new TreeSet<>(pointXComparator);

        //need to keep the longs short or the cast to float and back will break
        long start = LocaleUtils.getStartOfDay(weatherModel.date, weatherModel.currentWeather.timezone) / 1000L;
        long end = start + 23 * ONE_HOUR;

        for (int i = 0, l = weatherModel.currentWeather.hourly.length; i < l; i++) {
            if (weatherModel.currentWeather.hourly[i].dt / 1000L >= start &&
                    weatherModel.currentWeather.hourly[i].dt / 1000L <= end) {
                long x = weatherModel.currentWeather.hourly[i].dt / 1000L - start;

                temperaturePointsSet.add(new TemperatureGraphPoint(
                        colorHelper,
                        weatherUtils,
                        temperatureUnit,
                        isDarkModeActive,
                        x,
                        (float) weatherModel.currentWeather.hourly[i].temp));
                precipitationPointsSet.add(new PrecipitationGraphPoint(
                        colorHelper,
                        x,
                        Math.min((float) weatherModel.currentWeather.hourly[i].precipitationIntensity, 2f),
                        weatherModel.currentWeather.hourly[i].precipitationType
                ));
            }
        }

        for (int i = 0, l = weatherModel.currentWeather.trihourly.length; i < l; i++) {
            if (weatherModel.currentWeather.trihourly[i].dt / 1000L >= start &&
                    weatherModel.currentWeather.trihourly[i].dt / 1000L <= end) {
                long x = weatherModel.currentWeather.trihourly[i].dt / 1000L - start;

                temperaturePointsSet.add(new TemperatureGraphPoint(
                        colorHelper,
                        weatherUtils,
                        temperatureUnit,
                        isDarkModeActive,
                        x,
                        (float) weatherModel.currentWeather.trihourly[i].temp));
                precipitationPointsSet.add(new PrecipitationGraphPoint(
                        colorHelper,
                        x,
                        Math.min((float) weatherModel.currentWeather.trihourly[i].precipitationIntensity / 3, 2f),
                        weatherModel.currentWeather.trihourly[i].precipitationType
                ));
            }
        }

        final ArrayList<TemperatureGraphPoint> temperaturePoints = new ArrayList<>(temperaturePointsSet);
        final ArrayList<PrecipitationGraphPoint> precipitationPoints = new ArrayList<>(precipitationPointsSet);

        final ArrayList<XGraphLabel> xGraphLabels = new ArrayList<>();

        for (TemperatureGraphPoint point : temperaturePoints) {
            xGraphLabels.add(
                    new XGraphLabel((int) point.x,
                            LocaleUtils.formatHour(
                                    getContext(),
                                    Locale.getDefault(),
                                    new Date((((int) point.x) + start) * 1000),
                                    weatherModel.currentWeather.timezone)));
        }

        final int width = this.getMeasuredWidth() - 2 * getResources().getDimensionPixelSize(R.dimen.margin_half);
        final int height = this.getMeasuredHeight() - 2 * getResources().getDimensionPixelSize(R.dimen.margin_half);

        return doGenerateGraph(width, height, colorHelper, isDarkModeActive, temperatureUnit, temperaturePoints, precipitationPoints, xGraphLabels);
    }

    private Bitmap doGenerateGraph(int width,
                                   int height,
                                   ColorHelper colorHelper,
                                   boolean isDarkModeActive,
                                   TemperatureUnit temperatureUnit,
                                   ArrayList<TemperatureGraphPoint> temperaturePoints,
                                   ArrayList<PrecipitationGraphPoint> precipitationPoints,
                                   ArrayList<XGraphLabel> xGraphLabels) {
        WeatherPreferences weatherPreferences = WeatherPreferences.getInstance(getContext());
        WeatherUtils weatherUtils = WeatherUtils.getInstance(getContext());

        float minTemp = Collections.min(temperaturePoints, pointYComparator).getTemperature();
        float maxTemp = Collections.max(temperaturePoints, pointYComparator).getTemperature();
        int yMin = (int) weatherUtils.getTemperature(weatherPreferences.getTemperatureUnit(), minTemp);
        int yMax = (int) weatherUtils.getTemperature(weatherPreferences.getTemperatureUnit(), maxTemp) + 1;
        int xMax = (Math.max(temperaturePoints.size(), 24) - 1) * ONE_HOUR;

        GraphHelper.GraphBounds precipitationGraphBounds = new GraphHelper.GraphBounds(
                0,
                xMax,
                0,
                2);

        GraphHelper.GraphBounds temperatureGraphBounds = new GraphHelper.GraphBounds(
                0,
                xMax,
                yMin,
                yMax
        );

        int segments = temperaturePoints.size() < 24 ? 15 : 5;

        if (thermDrawable != null) {
            thermDrawable.setColorFilter(ContextCompat.getColor(getContext(), R.color.text_primary_emphasis), PorterDuff.Mode.SRC_IN);
        }

        GraphHelper graphHelper = new GraphHelper(getContext().getResources(), width, height);
        RectF graphRegion = new RectF(LEFT_PADDING, TOP_PADDING, width - RIGHT_PADDING, height - BOTTOM_PADDING - TOP_PADDING);
        RectF yAxisRegion = new RectF(0f, 0f, LEFT_PADDING, height);
        RectF xAxisRegion = new RectF(LEFT_PADDING, height - BOTTOM_PADDING, width - RIGHT_PADDING, height);
        RectF iconRegion = new RectF(0f, height / 2f - TEXT_SIZE / 2f - BOTTOM_PADDING / 2f, TEXT_SIZE, height / 2f + TEXT_SIZE / 2f - BOTTOM_PADDING / 2f);

        ArrayList<PrecipitationCurveGraphPoint> precipitationCurvePoints = getPrecipitationCurve(colorHelper, precipitationPoints, segments);
        ArrayList<TemperatureCurveGraphPoint> temperatureCurvePoints = getTemperatureCurve(colorHelper, weatherUtils, temperatureUnit, isDarkModeActive, temperaturePoints, segments);
        ArrayList<YGraphLabel> yGraphLabels = new ArrayList<>(Arrays.asList(new YGraphLabel(yMin, colorHelper.getColorFromTemperature(minTemp, true, isDarkModeActive)),
                new YGraphLabel(yMax, colorHelper.getColorFromTemperature(maxTemp, true, isDarkModeActive))));

        Paint fillPaint = getFillPaint();
        Paint strokePaint = getStrokePaint();

        graphHelper.plotAreaOnCanvas(graphRegion, fillPaint, precipitationGraphBounds, precipitationCurvePoints);

        graphHelper.plotLinesOnCanvas(graphRegion, strokePaint, precipitationGraphBounds, precipitationCurvePoints);

        graphHelper.plotPointsOnCanvas(graphRegion, fillPaint, precipitationGraphBounds, precipitationPoints);

        graphHelper.plotLinesOnCanvas(graphRegion, strokePaint, temperatureGraphBounds, temperatureCurvePoints);

        graphHelper.plotPointsOnCanvas(graphRegion, fillPaint, temperatureGraphBounds, temperaturePoints);

        Paint textPaint = new Paint();
        textPaint.setTextSize(TEXT_SIZE);
        textPaint.setTextAlign(Paint.Align.LEFT);
        graphHelper.drawYAxisOnCanvas(yAxisRegion, textPaint, yGraphLabels);

        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_primary));
        textPaint.setTextAlign(Paint.Align.CENTER);
        graphHelper.drawXAxisOnCanvas(xAxisRegion, textPaint, temperatureGraphBounds, xGraphLabels);

        if (thermDrawable != null) {
            graphHelper.drawDrawableOnCanvas(iconRegion, thermDrawable);
        }

        return graphHelper.getBitmap();
    }

    //Based on https://stackoverflow.com/a/15528789
    private ArrayList<TemperatureCurveGraphPoint> getTemperatureCurve(
            ColorHelper colorHelper,
            WeatherUtils weatherUtils,
            TemperatureUnit temperatureUnit,
            boolean isDarkModeActive,
            ArrayList<TemperatureGraphPoint> pts,
            int segments) {
        //noinspection UnnecessaryLocalVariable
        final float segmentsF = segments;
        final float tension = 0.5f;

        final ArrayList<TemperatureGraphPoint> ptsCopy = new ArrayList<>(pts.size() + 2);
        final ArrayList<TemperatureCurveGraphPoint> ptsCurve = new ArrayList<>(pts.size() * segments);

        ptsCopy.add(pts.get(0));
        ptsCopy.addAll(pts);
        ptsCopy.add(pts.get(pts.size() - 1));

        for (int i = 1, l = ptsCopy.size() - 2; i < l; i++) {
            for (int t = 0; t < segments; t++) {

                float st = t / segmentsF;
                float st2 = st * st;
                float st3 = st2 * st;
                float c1 = 2 * st3 - 3 * st2 + 1;
                float c2 = -2 * st3 + 3 * st2;
                float c3 = st3 - 2 * st2 + st;
                float c4 = st3 - st2;

                ptsCurve.add(new TemperatureCurveGraphPoint(
                        colorHelper,
                        weatherUtils,
                        temperatureUnit,
                        isDarkModeActive,
                        c1 * ptsCopy.get(i).getX() +
                                c2 * ptsCopy.get(i + 1).getX() +
                                c3 * (ptsCopy.get(i + 1).getX() - ptsCopy.get(i - 1).getX()) * tension +
                                c4 * (ptsCopy.get(i + 2).getX() - ptsCopy.get(i).getX()) * tension,
                        c1 * ptsCopy.get(i).getTemperature() +
                                c2 * ptsCopy.get(i + 1).getTemperature() +
                                c3 * (ptsCopy.get(i + 1).getTemperature() - ptsCopy.get(i - 1).getTemperature()) * tension +
                                c4 * (ptsCopy.get(i + 2).getTemperature() - ptsCopy.get(i).getTemperature()) * tension
                ));
            }
        }

        return ptsCurve;
    }

    //Based on https://stackoverflow.com/a/15528789
    private ArrayList<PrecipitationCurveGraphPoint> getPrecipitationCurve(ColorHelper colorHelper, ArrayList<PrecipitationGraphPoint> pts, int segments) {
        //noinspection UnnecessaryLocalVariable
        final float segmentsF = segments;
        final float tension = 0.5f;

        final ArrayList<PrecipitationGraphPoint> ptsCopy = new ArrayList<>(pts.size() + 2);
        final ArrayList<PrecipitationCurveGraphPoint> ptsCurve = new ArrayList<>(pts.size() * segments);

        ptsCopy.add(pts.get(0));
        ptsCopy.addAll(pts);
        ptsCopy.add(pts.get(pts.size() - 1));

        for (int i = 1, l = ptsCopy.size() - 2; i < l; i++) {
            for (int t = 0; t < segments; t++) {

                float st = t / segmentsF;
                float st2 = st * st;
                float st3 = st2 * st;
                float c1 = 2 * st3 - 3 * st2 + 1;
                float c2 = -2 * st3 + 3 * st2;
                float c3 = st3 - 2 * st2 + st;
                float c4 = st3 - st2;

                ptsCurve.add(new PrecipitationCurveGraphPoint(
                        colorHelper,
                        c1 * ptsCopy.get(i).getX() +
                                c2 * ptsCopy.get(i + 1).getX() +
                                c3 * (ptsCopy.get(i + 1).getX() - ptsCopy.get(i - 1).getX()) * tension +
                                c4 * (ptsCopy.get(i + 2).getX() - ptsCopy.get(i).getX()) * tension,
                        Math.min(Math.max(
                                c1 * ptsCopy.get(i).getY() +
                                        c2 * ptsCopy.get(i + 1).getY() +
                                        c3 * (ptsCopy.get(i + 1).getY() - ptsCopy.get(i - 1).getY()) * tension +
                                        c4 * (ptsCopy.get(i + 2).getY() - ptsCopy.get(i).getY()) * tension,
                                0f), 2f),
                        ptsCopy.get(i),
                        ptsCopy.get(i + 1)));
            }
        }

        return ptsCurve;
    }

    private static class TemperatureGraphPoint implements GraphHelper.IGraphPoint {
        private final float x;
        private final float temperature;
        private final float y;
        private final int color;

        public TemperatureGraphPoint(ColorHelper colorHelper,
                                     WeatherUtils weatherUtils,
                                     TemperatureUnit temperatureUnit,
                                     boolean isDarkModeActive,
                                     float x,
                                     float temperature) {
            this.x = x;
            this.temperature = temperature;
            this.y = BigDecimal.valueOf(weatherUtils.getTemperature(temperatureUnit, temperature)).setScale(1, RoundingMode.HALF_UP).floatValue();
            this.color = colorHelper.getColorFromTemperature(temperature, true, isDarkModeActive);
        }

        @Override
        public float getX() {
            return x;
        }

        @Override
        public float getY() {
            return y;
        }

        public float getTemperature() {
            return temperature;
        }

        @Override
        public Paint getPaint(Paint paint) {
            paint.setColor(color);

            return paint;
        }
    }

    private static class TemperatureCurveGraphPoint implements GraphHelper.IGraphPoint {
        private final float x;
        private final float y;
        private final int color;

        public TemperatureCurveGraphPoint(ColorHelper colorHelper,
                                          WeatherUtils weatherUtils,
                                          TemperatureUnit temperatureUnit,
                                          boolean isDarkModeActive,
                                          float x,
                                          float temperature) {
            this.x = x;
            this.y = (float) weatherUtils.getTemperature(temperatureUnit, temperature);
            this.color = colorHelper.getColorFromTemperature(temperature, true, isDarkModeActive);
        }

        @Override
        public float getX() {
            return x;
        }

        @Override
        public float getY() {
            return y;
        }

        @Override
        public Paint getPaint(Paint paint) {
            paint.setColor(color);

            return paint;
        }
    }

    private static class PrecipitationGraphPoint implements GraphHelper.IGraphPoint {
        private final float x;
        private final float y;
        private final PrecipType type;
        private final int color;

        public PrecipitationGraphPoint(ColorHelper colorHelper, float x, float y, PrecipType type) {
            this.x = x;
            this.y = y;
            this.type = type;
            this.color = colorHelper.getPrecipColor(type);
        }

        @Override
        public float getX() {
            return x;
        }

        @Override
        public float getY() {
            return y;
        }

        @Override
        public Paint getPaint(Paint paint) {
            paint.setColor(color);

            return paint;
        }

        public PrecipType getType() {
            return type;
        }
    }

    private static class PrecipitationCurveGraphPoint implements GraphHelper.IGraphPoint {
        private final float x;
        private final float y;
        private final int color;

        public PrecipitationCurveGraphPoint(ColorHelper colorHelper,
                                            float x,
                                            float y,
                                            PrecipitationGraphPoint prevPoint,
                                            PrecipitationGraphPoint nextPoint) {
            this.x = x;
            this.y = y;

            this.color = prevPoint.getType() == nextPoint.getType() ?
                    colorHelper.getPrecipColor(prevPoint.getType()) :
                    ColorUtils.blendColors(colorHelper.getPrecipColor(nextPoint.getType()),
                            colorHelper.getPrecipColor(prevPoint.getType()),
                            (nextPoint.getX() - x) / (nextPoint.getX() - prevPoint.getX()) * 100.);
        }

        @Override
        public float getX() {
            return x;
        }

        @Override
        public float getY() {
            return y;
        }

        @Override
        public Paint getPaint(Paint paint) {
            paint.setColor(color);

            return paint;
        }
    }

    private static class XGraphLabel implements GraphHelper.IGraphLabel {
        private final int x;
        private final String label;

        public XGraphLabel(int x, String label) {
            this.x = x;
            this.label = label;
        }

        @Override
        public String getLabel() {
            return label;
        }

        @Override
        public float getX() {
            return x;
        }

        @Override
        public Paint getPaint(Paint paint) {
            return paint;
        }
    }

    private static class YGraphLabel implements GraphHelper.IGraphLabel {
        private final int y;
        private final int color;

        public YGraphLabel(int y, int color) {
            this.y = y;
            this.color = color;
        }

        @Override
        public String getLabel() {
            return Integer.toString(y);
        }

        @Override
        public float getX() {
            return 0;
        }

        @Override
        public Paint getPaint(Paint paint) {
            paint.setColor(color);

            return paint;
        }
    }
}
