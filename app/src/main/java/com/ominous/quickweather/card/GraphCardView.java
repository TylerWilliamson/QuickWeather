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

package com.ominous.quickweather.card;

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
import android.view.View;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import com.ominous.quickweather.R;
import com.ominous.quickweather.api.OpenWeatherMap;
import com.ominous.quickweather.data.WeatherModel;
import com.ominous.quickweather.data.WeatherResponseOneCall;
import com.ominous.quickweather.util.ColorUtils;
import com.ominous.quickweather.util.GraphHelper;
import com.ominous.quickweather.pref.WeatherPreferences;
import com.ominous.quickweather.util.WeatherUtils;
import com.ominous.quickweather.pref.TemperatureUnit;
import com.ominous.tylerutils.async.Promise;
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
import java.util.TimeZone;
import java.util.TreeSet;

public class GraphCardView extends BaseCardView {
    private final static int TWENTY_THREE_HOURS = 23 * 60 * 60;
    private final static Comparator<GraphHelper.IGraphPoint> pointYComparator = (o1, o2) -> Float.compare(o1.getY(), o2.getY());
    private final static Comparator<GraphHelper.IGraphPoint> pointXComparator = (o1, o2) -> Float.compare(o1.getX(), o2.getX());
    final int LEFT_PADDING;
    final int RIGHT_PADDING;
    final int TOP_PADDING;
    final int BOTTOM_PADDING;
    final float TEXT_SIZE;
    final Drawable thermDrawable;
    private final ImageView graphImageView;
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    private WeatherModel weatherModel;
    private boolean shouldGenerateGraph = false;

    public GraphCardView(Context context) {
        super(context);

        inflate(context, R.layout.card_graph, this);

        graphImageView = findViewById(R.id.graph_image_view);

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
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (shouldGenerateGraph) {
            generateGraph(weatherModel);

            shouldGenerateGraph = false;
        }
    }

    @Override
    public void update(WeatherModel weatherModel, int position) {
        this.weatherModel = weatherModel;
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

                    Bitmap graphBitmap = m.responseForecast == null ?
                            generateCurrentGraph(weatherUtils, weatherPreferences.getTemperatureUnit(), m.responseOneCall) :
                            generateForecastGraph(weatherUtils, weatherPreferences.getTemperatureUnit(), m);//background

                    mainThreadHandler.post(() ->
                            graphImageView.setImageBitmap(graphBitmap));
                });//foreground
    }

    private Bitmap generateCurrentGraph(WeatherUtils weatherUtils, TemperatureUnit temperatureUnit, WeatherResponseOneCall response) {
        ArrayList<TemperatureGraphPoint> temperaturePoints = new ArrayList<>(24);
        ArrayList<PrecipitationGraphPoint> precipitationPoints = new ArrayList<>(24);

        //need to keep the longs short or the cast to float and back will break
        long start = response.hourly[0].dt;

        for (int i = 0, l = 24; i < l; i++) {
            temperaturePoints.add(new TemperatureGraphPoint(weatherUtils, temperatureUnit, response.hourly[i].dt - start, (float) response.hourly[i].temp));
            precipitationPoints.add(new PrecipitationGraphPoint(
                    response.hourly[i].dt - start,
                    Math.min((float) response.hourly[i].getPrecipitationIntensity(), 2f),
                    response.hourly[i].getPrecipitationType()
            ));
        }

        final ArrayList<XGraphLabel> xGraphLabels = new ArrayList<>();
        final TimeZone timeZone = TimeZone.getTimeZone(weatherModel.responseOneCall.timezone);

        for (TemperatureGraphPoint point : temperaturePoints) {
            xGraphLabels.add(new XGraphLabel((int) point.x, LocaleUtils.formatHour(getContext(), Locale.getDefault(), new Date((((int) point.x) + start) * 1000), timeZone)));
        }

        return doGenerateGraph(temperatureUnit, temperaturePoints, precipitationPoints, xGraphLabels);
    }

    private Bitmap generateForecastGraph(WeatherUtils weatherUtils, TemperatureUnit temperatureUnit, WeatherModel weatherModel) {
        final TreeSet<TemperatureGraphPoint> temperaturePointsSet = new TreeSet<>(pointXComparator);
        final TreeSet<PrecipitationGraphPoint> precipitationPointsSet = new TreeSet<>(pointXComparator);

        //need to keep the longs short or the cast to float and back will break
        long start = LocaleUtils.getStartOfDay(weatherModel.date, TimeZone.getTimeZone(weatherModel.responseOneCall.timezone)) / 1000;
        long end = start + TWENTY_THREE_HOURS;

        for (int i = 0, l = weatherModel.responseOneCall.hourly.length; i < l; i++) {
            if (weatherModel.responseOneCall.hourly[i].dt >= start &&
                    weatherModel.responseOneCall.hourly[i].dt <= end) {
                temperaturePointsSet.add(new TemperatureGraphPoint(weatherUtils, temperatureUnit, weatherModel.responseOneCall.hourly[i].dt - start, (float) weatherModel.responseOneCall.hourly[i].temp));
                precipitationPointsSet.add(new PrecipitationGraphPoint(
                        weatherModel.responseOneCall.hourly[i].dt - start,
                        Math.min((float) weatherModel.responseOneCall.hourly[i].getPrecipitationIntensity(), 2f),
                        weatherModel.responseOneCall.hourly[i].getPrecipitationType()
                ));
            }
        }

        for (int i = 0, l = weatherModel.responseForecast.list.length; i < l; i++) {
            if (weatherModel.responseForecast.list[i].dt >= start &&
                    weatherModel.responseForecast.list[i].dt <= end) {
                temperaturePointsSet.add(new TemperatureGraphPoint(weatherUtils, temperatureUnit, weatherModel.responseForecast.list[i].dt - start, (float) weatherModel.responseForecast.list[i].main.temp));
                precipitationPointsSet.add(new PrecipitationGraphPoint(
                        weatherModel.responseForecast.list[i].dt - start,
                        Math.min((float) weatherModel.responseForecast.list[i].getPrecipitationIntensity() / 3, 2f),
                        weatherModel.responseForecast.list[i].getPrecipitationType()
                ));
            }
        }

        final ArrayList<TemperatureGraphPoint> temperaturePoints = new ArrayList<>(temperaturePointsSet);
        final ArrayList<PrecipitationGraphPoint> precipitationPoints = new ArrayList<>(precipitationPointsSet);

        final ArrayList<XGraphLabel> xGraphLabels = new ArrayList<>();
        final TimeZone timeZone = TimeZone.getTimeZone(weatherModel.responseOneCall.timezone);

        for (TemperatureGraphPoint point : temperaturePoints) {
            xGraphLabels.add(new XGraphLabel((int) point.x, LocaleUtils.formatHour(getContext(), Locale.getDefault(), new Date((((int) point.x) + start) * 1000), timeZone)));
        }

        return doGenerateGraph(temperatureUnit, temperaturePoints, precipitationPoints, xGraphLabels);
    }

    private Bitmap doGenerateGraph(TemperatureUnit temperatureUnit, ArrayList<TemperatureGraphPoint> temperaturePoints, ArrayList<PrecipitationGraphPoint> precipitationPoints, ArrayList<XGraphLabel> xGraphLabels) {
        WeatherPreferences weatherPreferences = WeatherPreferences.getInstance(getContext());
        WeatherUtils weatherUtils = WeatherUtils.getInstance(getContext());

        float minTemp = Collections.min(temperaturePoints, pointYComparator).getTemperature();
        float maxTemp = Collections.max(temperaturePoints, pointYComparator).getTemperature();
        int yMin = (int) weatherUtils.getTemperature(weatherPreferences.getTemperatureUnit(), minTemp);
        int yMax = (int) weatherUtils.getTemperature(weatherPreferences.getTemperatureUnit(), maxTemp) + 1;

        GraphHelper.GraphBounds precipitationGraphBounds = new GraphHelper.GraphBounds(
                0,
                TWENTY_THREE_HOURS,
                0,
                2);

        GraphHelper.GraphBounds temperatureGraphBounds = new GraphHelper.GraphBounds(
                0,
                TWENTY_THREE_HOURS,
                yMin,
                yMax
        );

        int width = graphImageView.getMeasuredWidth();
        int height = graphImageView.getMeasuredHeight();
        int segments = width / 32;

        if (thermDrawable != null) {
            thermDrawable.setColorFilter(ContextCompat.getColor(getContext(), R.color.text_primary_emphasis), PorterDuff.Mode.SRC_IN);
        }

        GraphHelper graphHelper = new GraphHelper(getContext().getResources(), width, height);
        RectF graphRegion = new RectF(LEFT_PADDING, TOP_PADDING, width - RIGHT_PADDING, height - BOTTOM_PADDING - TOP_PADDING);
        RectF yAxisRegion = new RectF(0f, 0f, LEFT_PADDING, height);
        RectF xAxisRegion = new RectF(LEFT_PADDING, height - BOTTOM_PADDING, width - RIGHT_PADDING, height);
        RectF iconRegion = new RectF(0f, height / 2f - TEXT_SIZE / 2f, TEXT_SIZE, height / 2f + TEXT_SIZE / 2f);

        ArrayList<PrecipitationCurveGraphPoint> precipitationCurvePoints = getPrecipitationCurve(precipitationPoints, segments);
        ArrayList<TemperatureCurveGraphPoint> temperatureCurvePoints = getTemperatureCurve(weatherUtils, temperatureUnit, temperaturePoints, segments);
        ArrayList<YGraphLabel> yGraphLabels = new ArrayList<>(Arrays.asList(new YGraphLabel(yMin, ColorUtils.getColorFromTemperature(minTemp, true)), new YGraphLabel(yMax, ColorUtils.getColorFromTemperature(maxTemp, true))));

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
            WeatherUtils weatherUtils,
            TemperatureUnit temperatureUnit,
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

                ptsCurve.add(new TemperatureCurveGraphPoint(weatherUtils,
                        temperatureUnit,
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
    private ArrayList<PrecipitationCurveGraphPoint> getPrecipitationCurve(ArrayList<PrecipitationGraphPoint> pts, int segments) {
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

        public TemperatureGraphPoint(WeatherUtils weatherUtils, TemperatureUnit temperatureUnit, float x, float temperature) {
            this.x = x;
            this.temperature = temperature;
            this.y = BigDecimal.valueOf(weatherUtils.getTemperature(temperatureUnit, temperature)).setScale(1, RoundingMode.HALF_UP).floatValue();
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
            paint.setColor(ColorUtils.getColorFromTemperature(temperature, true));

            return paint;
        }
    }

    private static class TemperatureCurveGraphPoint implements GraphHelper.IGraphPoint {
        private final float x;
        private final float temperature;
        private final float y;

        public TemperatureCurveGraphPoint(WeatherUtils weatherUtils, TemperatureUnit temperatureUnit, float x, float temperature) {
            this.x = x;
            this.temperature = temperature;
            this.y = (float) weatherUtils.getTemperature(temperatureUnit, temperature);
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
            paint.setColor(ColorUtils.getColorFromTemperature(temperature, true));

            return paint;
        }
    }

    private static class PrecipitationGraphPoint implements GraphHelper.IGraphPoint {
        private final float x;
        private final float y;
        private final OpenWeatherMap.PrecipType type;

        public PrecipitationGraphPoint(float x, float y, OpenWeatherMap.PrecipType type) {
            this.x = x;
            this.y = y;
            this.type = type;
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
            paint.setColor(ColorUtils.getPrecipColor(type));

            return paint;
        }
    }

    private static class PrecipitationCurveGraphPoint implements GraphHelper.IGraphPoint {
        private final float x;
        private final float y;
        private final int color;

        public PrecipitationCurveGraphPoint(float x, float y, PrecipitationGraphPoint prevPoint, PrecipitationGraphPoint nextPoint) {
            this.x = x;
            this.y = y;

            this.color = prevPoint.type == nextPoint.type ?
                    ColorUtils.getPrecipColor(prevPoint.type) :
                    ColorUtils.blendColors(ColorUtils.getPrecipColor(nextPoint.type),
                            ColorUtils.getPrecipColor(prevPoint.type),
                            (nextPoint.x - x) / (nextPoint.x - prevPoint.x) * 100.);
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
