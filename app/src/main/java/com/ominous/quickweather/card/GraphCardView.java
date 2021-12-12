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

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;

import com.ominous.quickweather.R;
import com.ominous.quickweather.data.WeatherModel;
import com.ominous.quickweather.data.WeatherResponseOneCall;
import com.ominous.quickweather.util.ColorUtils;
import com.ominous.quickweather.util.GraphUtils;
import com.ominous.quickweather.util.LocaleUtils;
import com.ominous.quickweather.util.WeatherUtils;
import com.ominous.quickweather.weather.Weather;
import com.ominous.tylerutils.async.Promise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.TreeSet;

import androidx.core.content.ContextCompat;

public class GraphCardView extends BaseCardView {
    private final static int TWENTY_THREE_HOURS = 23 * 60 * 60;
    private final static Comparator<PointF> pointYComparator = (o1, o2) -> Float.compare(o1.y, o2.y);
    private final static Comparator<PointF> pointXComparator = (o1, o2) -> Float.compare(o1.x, o2.x);
    final int LEFT_PADDING;
    final int RIGHT_PADDING;
    final int TOP_PADDING;
    final int BOTTOM_PADDING;
    final float POINT_SIZE;
    final float TEXT_SIZE;
    final Drawable thermDrawable;
    final DrawListener drawListener;
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
        TOP_PADDING = resources.getDimensionPixelSize(R.dimen.graph_point_size);
        BOTTOM_PADDING = resources.getDimensionPixelSize(R.dimen.text_size_regular);
        POINT_SIZE = resources.getDimensionPixelSize(R.dimen.graph_point_size);
        TEXT_SIZE = resources.getDimension(R.dimen.text_size_regular);

        thermDrawable = ContextCompat.getDrawable(context, R.drawable.thermometer_25);
        drawListener = new DrawListener(context);
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
                .then((m) -> m.responseForecast == null ?
                        generateCurrentGraph(m.responseOneCall) :
                        generateForecastGraph(m))//background
                .then((bitmap) -> {
                    mainThreadHandler.post(() ->
                            graphImageView.setImageBitmap(bitmap));
                });//foreground
    }

    private Bitmap generateCurrentGraph(WeatherResponseOneCall response) {
        TimeZone timeZone = TimeZone.getTimeZone(response.timezone);

        ArrayList<PointF> temperaturePoints = new ArrayList<>(24);
        ArrayList<PointF> precipPoints = new ArrayList<>(24);
        ArrayList<PointF> precipTypes = new ArrayList<>(24);

        //need to keep the longs short or the cast to float and back will break
        long start = response.hourly[0].dt;

        for (int i = 0, l = 24; i < l; i++) {
            temperaturePoints.add(new PointF(response.hourly[i].dt - start, (float) response.hourly[i].temp));
            precipPoints.add(new PointF(response.hourly[i].dt - start, Math.min((float) response.hourly[i].getPrecipitationIntensity(), 2f)));
            precipTypes.add(new PointF(response.hourly[i].dt - start, response.hourly[i].getPrecipitationType() == Weather.PrecipType.RAIN ? 0f : response.hourly[i].getPrecipitationType() == Weather.PrecipType.MIX ? 1f : 2f));
        }

        GraphUtils.GraphBounds precipGraphBounds = new GraphUtils.GraphBounds(
                0,
                TWENTY_THREE_HOURS,
                0f,
                2f);

        GraphUtils.GraphBounds temperatureGraphBounds = new GraphUtils.GraphBounds(
                0,
                TWENTY_THREE_HOURS,
                Collections.min(temperaturePoints, pointYComparator).y,
                Collections.max(temperaturePoints, pointYComparator).y
        );

        return doGenerateGraph(temperaturePoints, precipPoints, precipTypes, temperatureGraphBounds, precipGraphBounds, (f) -> LocaleUtils.formatHour(getContext(), Locale.getDefault(), new Date((((long) f) + start) * 1000), timeZone));
    }

    private Bitmap generateForecastGraph(WeatherModel weatherModel) {
        TimeZone timeZone = TimeZone.getTimeZone(weatherModel.responseOneCall.timezone);

        final TreeSet<PointF> temperaturePointsSet = new TreeSet<>(pointXComparator);
        final TreeSet<PointF> precipPointsSet = new TreeSet<>(pointXComparator);
        final TreeSet<PointF> precipTypesSet = new TreeSet<>(pointXComparator);

        //need to keep the longs short or the cast to float and back will break
        long start = LocaleUtils.getStartOfDay(weatherModel.date, TimeZone.getTimeZone(weatherModel.responseOneCall.timezone)) / 1000;
        long end = start + TWENTY_THREE_HOURS;
        double dailyLowTemp = 0;
        double dailyHighTemp = 0;

        for (int i = 0, l = weatherModel.responseOneCall.daily.length; i < l; i++) {
            if (LocaleUtils.getStartOfDay(new Date(weatherModel.responseOneCall.daily[i].dt * 1000), TimeZone.getTimeZone(weatherModel.responseOneCall.timezone)) == start * 1000) {
                dailyLowTemp = weatherModel.responseOneCall.daily[i].temp.min;
                dailyHighTemp = weatherModel.responseOneCall.daily[i].temp.max;
            }
        }

        for (int i = 0, l = weatherModel.responseOneCall.hourly.length; i < l; i++) {
            if (weatherModel.responseOneCall.hourly[i].dt >= start &&
                    weatherModel.responseOneCall.hourly[i].dt <= end) {
                temperaturePointsSet.add(new PointF(weatherModel.responseOneCall.hourly[i].dt - start, (float) weatherModel.responseOneCall.hourly[i].temp));
                precipPointsSet.add(new PointF(weatherModel.responseOneCall.hourly[i].dt - start, Math.min((float) weatherModel.responseOneCall.hourly[i].getPrecipitationIntensity(), 2f)));
                precipTypesSet.add(new PointF(weatherModel.responseOneCall.hourly[i].dt - start, weatherModel.responseOneCall.hourly[i].getPrecipitationType() == Weather.PrecipType.RAIN ? 0f : weatherModel.responseOneCall.hourly[i].getPrecipitationType() == Weather.PrecipType.MIX ? 1f : 2f));
            }
        }

        for (int i = 0, l = weatherModel.responseForecast.list.length; i < l; i++) {
            if (weatherModel.responseForecast.list[i].dt >= start &&
                    weatherModel.responseForecast.list[i].dt <= end) {
                temperaturePointsSet.add(new PointF(weatherModel.responseForecast.list[i].dt - start, (float) weatherModel.responseForecast.list[i].main.temp));
                precipPointsSet.add(new PointF(weatherModel.responseForecast.list[i].dt - start, Math.min((float) weatherModel.responseForecast.list[i].getPrecipitationIntensity() / 3, 2f)));
                precipTypesSet.add(new PointF(weatherModel.responseForecast.list[i].dt - start, weatherModel.responseForecast.list[i].getPrecipitationType() == Weather.PrecipType.RAIN ? 0f : weatherModel.responseForecast.list[i].getPrecipitationType() == Weather.PrecipType.MIX ? 1f : 2f));
            }
        }

        final ArrayList<PointF> temperaturePoints = new ArrayList<>(temperaturePointsSet);
        final ArrayList<PointF> precipPoints = new ArrayList<>(precipPointsSet);
        final ArrayList<PointF> precipTypes = new ArrayList<>(precipTypesSet);

        GraphUtils.GraphBounds precipGraphBounds = new GraphUtils.GraphBounds(
                0,
                TWENTY_THREE_HOURS,
                0f,
                2f);

        GraphUtils.GraphBounds temperatureGraphBounds = new GraphUtils.GraphBounds(
                0,
                TWENTY_THREE_HOURS,
                Collections.min(temperaturePoints, pointYComparator).y,
                Collections.max(temperaturePoints, pointYComparator).y
        );

        return doGenerateGraph(temperaturePoints, precipPoints, precipTypes, temperatureGraphBounds, precipGraphBounds, (f) -> LocaleUtils.formatHour(getContext(), Locale.getDefault(), new Date((((long) f) + start) * 1000), timeZone));
    }

    private Bitmap doGenerateGraph(ArrayList<PointF> temperaturePoints, ArrayList<PointF> precipPoints, ArrayList<PointF> precipTypes, GraphUtils.GraphBounds temperatureGraphBounds, GraphUtils.GraphBounds precipGraphBounds, GraphUtils.LabelFormatter dateFormatter) {
        int width = graphImageView.getMeasuredWidth();
        int height = graphImageView.getMeasuredHeight();
        int segments = width / 32;

        if (thermDrawable != null) {
            thermDrawable.setColorFilter(ContextCompat.getColor(getContext(), R.color.text_primary_emphasis), PorterDuff.Mode.SRC_IN);
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        RectF graphRegion = new RectF(LEFT_PADDING, TOP_PADDING, width - RIGHT_PADDING, height - BOTTOM_PADDING - TOP_PADDING);

        ArrayList<PointF> precipGraphPoints = GraphUtils.getCurvePoints(precipPoints, segments, 0f, 2f);
        ArrayList<PointF> temperatureGraphPoints = GraphUtils.getCurvePoints(temperaturePoints, segments);

        drawListener.setParams(DrawListener.PRECIP, Paint.Style.FILL, precipTypes);
        GraphUtils.plotAreaOnCanvas(canvas, graphRegion, precipGraphPoints, precipGraphBounds, drawListener);

        drawListener.setParams(DrawListener.PRECIP, Paint.Style.STROKE, precipTypes);
        GraphUtils.plotLinesOnCanvas(canvas, graphRegion, precipGraphPoints, precipGraphBounds, drawListener);

        drawListener.setParams(DrawListener.PRECIP, Paint.Style.FILL, precipTypes);
        GraphUtils.plotPointsOnCanvas(canvas, graphRegion, precipPoints, precipGraphBounds, drawListener);

        drawListener.setParams(DrawListener.TEMPERATURE, Paint.Style.STROKE);
        GraphUtils.plotLinesOnCanvas(canvas, graphRegion, temperatureGraphPoints, temperatureGraphBounds, drawListener);

        drawListener.setParams(DrawListener.TEMPERATURE, Paint.Style.FILL);
        GraphUtils.plotPointsOnCanvas(canvas, graphRegion, temperaturePoints, temperatureGraphBounds, drawListener);

        drawListener.setParams(DrawListener.TEMPERATURE, Paint.Align.LEFT);
        GraphUtils.drawYAxisOnCanvas(canvas, new RectF(0f, 0f, LEFT_PADDING, height), temperaturePoints,
                (t) -> Integer.toString((int) WeatherUtils.getConvertedTemperature(t)), temperatureGraphBounds, drawListener);

        drawListener.setParams(DrawListener.DEFAULT, Paint.Align.CENTER);
        GraphUtils.drawXAxisOnCanvas(canvas, new RectF(LEFT_PADDING, height - BOTTOM_PADDING + POINT_SIZE, width - RIGHT_PADDING, height), temperaturePoints,
                dateFormatter, temperatureGraphBounds, drawListener);

        if (thermDrawable != null) {
            GraphUtils.drawDrawableOnCanvas(
                    canvas,
                    new RectF(0f, height / 2f - TEXT_SIZE / 2f, TEXT_SIZE, height / 2f + TEXT_SIZE / 2f),
                    thermDrawable);
        }

        return bitmap;
    }

    private static class DrawListener implements GraphUtils.OnBeforeDrawListener {
        static final int DEFAULT = 0, TEMPERATURE = 1, PRECIP = 2;
        private final Resources resources;
        private int type;
        private Paint.Style style;
        private Paint.Align align;
        private ArrayList<PointF> values;

        DrawListener(Context context) {
            this.resources = context.getResources();
        }

        void setParams(int type, Paint.Style style, ArrayList<PointF> values) {
            this.type = type;
            this.style = style;
            this.align = null;
            this.values = values;
        }

        @SuppressWarnings("SameParameterValue")
        void setParams(int type, Paint.Style style) {
            this.setParams(type, style, null);
        }

        void setParams(int type, Paint.Align textAlign) {
            this.type = type;
            this.style = null;
            this.align = textAlign;
        }

        @Override
        public void onBeforeDraw(Paint paint) {
            paint.setAntiAlias(style == Paint.Style.STROKE);

            if (type == DEFAULT) {
                paint.setColor(resources.getColor(R.color.text_primary));
            }

            if (style != null) {
                paint.setStyle(style);
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setStrokeWidth(style == Paint.Style.STROKE ? 5 : 0);
            } else if (align != null) {
                paint.setTextSize(resources.getDimension(R.dimen.text_size_regular));
                paint.setTextAlign(align);
            }
        }

        @Override
        public void onBeforeDrawPoint(float x, float y, Paint paint) {
            float percent = 0;
            int precipType1 = 0, precipType2 = 0;

            switch (type) {
                case TEMPERATURE:
                    paint.setColor(ColorUtils.getColorFromTemperature(y, true));
                    break;
                case PRECIP:
                    for (int i = 1, l = values.size(); i < l; i++) {
                        if (values.get(i).x >= x) {
                            precipType1 = (int) values.get(i).y;
                            precipType2 = (int) values.get(i - 1).y;
                            percent = (values.get(i).x - x) / (values.get(i).x - values.get(i - 1).x) * 100;
                            break;
                        }
                    }

                    paint.setColor(precipType1 == precipType2 ?
                            ColorUtils.getPrecipColor(precipType1) :
                            ColorUtils.blendColors(ColorUtils.getPrecipColor(precipType1), ColorUtils.getPrecipColor(precipType2), percent));

                    break;
            }
        }
    }
}
