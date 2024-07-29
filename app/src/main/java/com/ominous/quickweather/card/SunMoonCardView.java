/*
 *   Copyright 2019 - 2024 Tyler Williamson
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

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.ominous.quickweather.R;
import com.ominous.quickweather.data.WeatherModel;
import com.ominous.quickweather.util.GraphHelper;
import com.ominous.quickweather.util.WeatherUtils;
import com.ominous.tylerutils.async.Promise;
import com.ominous.tylerutils.util.LocaleUtils;
import com.ominous.tylerutils.util.ViewUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class SunMoonCardView extends BaseCardView {
    final float TEXT_SIZE;
    final float SUNMOON_SIZE;

    private final ImageView graphImageView;
    private final ImageView sunIconImageView;
    private final ImageView moonIconImageView;
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    private WeatherModel weatherModel;
    private boolean shouldGenerateGraph = false;

    private boolean isSunShown = true;
    private Bitmap sunGraphBitmap;
    private Bitmap moonGraphBitmap;

    private int dayIndex;

    public SunMoonCardView(Context context) {
        super(context);

        inflate(context, R.layout.card_sunmoon, this);

        graphImageView = findViewById(R.id.graph_image_view);
        sunIconImageView = findViewById(R.id.icon_sun);
        moonIconImageView = findViewById(R.id.icon_moon);

        TEXT_SIZE = context.getResources().getDimension(R.dimen.text_size_regular);
        SUNMOON_SIZE = context.getResources().getDimensionPixelSize(R.dimen.sunmoon_size);

        ViewUtils.setAccessibilityInfo(this, context.getString(R.string.label_sunmoon_action), null);
    }

    private Paint getTextPaint() {
        Paint paint = new Paint();
        paint.setTextSize(TEXT_SIZE);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.text_primary_emphasis));
        paint.setAntiAlias(true);

        return paint;
    }

    private Paint getTextOutlinePaint() {
        Paint paint = new Paint();
        paint.setTextSize(TEXT_SIZE);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10);
        paint.setAntiAlias(true);

        return paint;
    }

    private Paint getStrokePaint() {
        Paint paint = new Paint();

        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(8);

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

        sunGraphBitmap = null;
        moonGraphBitmap = null;

        shouldGenerateGraph = true;
        setState(true);

        if (weatherModel.date == null) {
            dayIndex = 0;
        } else {
            for (int i = 0, l = weatherModel.currentWeather.daily.length; i < l; i++) {
                if (weatherModel.currentWeather.daily[i].dt >= weatherModel.date.getTime()) {
                    dayIndex = i;
                    break;
                }
            }
        }

        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTimeZone(weatherModel.currentWeather.timezone);
        calendar.setTimeInMillis(weatherModel.currentWeather.daily[dayIndex].dt);

        String sunrise = LocaleUtils.formatTime(getContext(),
                Locale.getDefault(),
                new Date(weatherModel.currentWeather.daily[dayIndex].sunrise),
                weatherModel.currentWeather.timezone);
        String sunset = LocaleUtils.formatTime(getContext(),
                Locale.getDefault(),
                new Date(weatherModel.currentWeather.daily[dayIndex].sunset),
                weatherModel.currentWeather.timezone);
        String moonrise = LocaleUtils.formatTime(getContext(),
                Locale.getDefault(),
                new Date(weatherModel.currentWeather.daily[dayIndex].moonrise),
                weatherModel.currentWeather.timezone);
        String moonset = LocaleUtils.formatTime(getContext(),
                Locale.getDefault(),
                new Date(weatherModel.currentWeather.daily[dayIndex].moonset),
                weatherModel.currentWeather.timezone);
        String moonphase = WeatherUtils.getInstance(getContext())
                .getMoonPhaseString(weatherModel.currentWeather.daily[dayIndex].moonPhase);

        setContentDescription(getContext().getString(R.string.card_sunmoon_desc,
                dayIndex == 0 ? getContext().getString(R.string.text_today) : calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()),
                sunrise,
                sunset,
                moonrise,
                moonset,
                moonphase));
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        shouldGenerateGraph = true;
    }

    @Override
    public void onClick(View v) {
        if (sunGraphBitmap != null && moonGraphBitmap != null) {
            setState(!isSunShown);
        }
    }

    private void setState(boolean isSunShown) {
        this.isSunShown = isSunShown;

        graphImageView.setImageBitmap(isSunShown ? sunGraphBitmap : moonGraphBitmap);

        ColorStateList primaryColorStateList = ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.text_primary_emphasis));
        ColorStateList secondaryColorStateList = ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.sunmoon_normal_color));

        sunIconImageView.setImageTintList(isSunShown ? primaryColorStateList : secondaryColorStateList);
        moonIconImageView.setImageTintList(isSunShown ? secondaryColorStateList : primaryColorStateList);
    }

    private void generateGraph(WeatherModel weatherModel) {
        Promise
                .create(weatherModel)
                .then((m) -> {
                    sunGraphBitmap = generateCelestialGraph(true);

                    mainThreadHandler.post(() ->
                            graphImageView.setImageBitmap(sunGraphBitmap));//foreground
                }).then(a -> {
                    moonGraphBitmap = generateCelestialGraph(false);
                });
    }

    private Bitmap generateCelestialGraph(boolean isSun) {
        final double FRAC = 2 * Math.PI / 24;
        final int width = this.getMeasuredWidth()
                - 2 * getResources().getDimensionPixelSize(R.dimen.margin_half)
                - 4 * getResources().getDimensionPixelSize(R.dimen.margin_standard);
        final int height = this.getMeasuredHeight() - 2 * getResources().getDimensionPixelSize(R.dimen.margin_half);

        GraphHelper graphHelper = new GraphHelper(getResources(), width, height);

        GraphHelper.GraphBounds graphBounds = new GraphHelper.GraphBounds(
                0,
                24,
                0,
                2
        );

        int emphasisColor = ContextCompat.getColor(getContext(), R.color.text_primary_emphasis);
        int normalColor = ContextCompat.getColor(getContext(), R.color.sunmoon_normal_color);

        ArrayList<CelestialGraphPoint> celestialPoints = new ArrayList<>(24);
        ArrayList<CelestialGraphPoint> horizonPoints = new ArrayList<>(24);

        Calendar currentTime = Calendar.getInstance(weatherModel.currentWeather.timezone);
        currentTime.setTimeInMillis(weatherModel.currentWeather.timestamp);
        int currentMonth = currentTime.get(Calendar.MONTH);

        Calendar riseTime = Calendar.getInstance(weatherModel.currentWeather.timezone);
        riseTime.setTimeInMillis(isSun ?
                weatherModel.currentWeather.daily[dayIndex].sunrise :
                weatherModel.currentWeather.daily[dayIndex].moonrise);

        Calendar setTime = Calendar.getInstance(weatherModel.currentWeather.timezone);
        setTime.setTimeInMillis(isSun ?
                weatherModel.currentWeather.daily[dayIndex].sunset :
                weatherModel.currentWeather.daily[dayIndex].moonset);

        boolean is24HourSun = weatherModel.currentWeather.daily[0].sunrise == 0 &&
                ((weatherModel.weatherLocation.latitude < 0 && (currentMonth <= 3 || currentMonth >= 9)) ||
                        (weatherModel.weatherLocation.latitude > 0 && (currentMonth >= 3 && currentMonth <= 9)));

        boolean is24HourDarkness = weatherModel.currentWeather.daily[0].sunrise == 0 &&
                ((weatherModel.weatherLocation.latitude > 0 && (currentMonth <= 3 || currentMonth >= 9)) ||
                        (weatherModel.weatherLocation.latitude < 0 && (currentMonth >= 3 && currentMonth <= 9)));

        float riseXcoord = riseTime.get(Calendar.HOUR_OF_DAY) + riseTime.get(Calendar.MINUTE) / 60f;
        float setXcoord = setTime.get(Calendar.HOUR_OF_DAY) + setTime.get(Calendar.MINUTE) / 60f;

        float offset = is24HourSun ? 12 : is24HourDarkness ? 0 : 12 - (riseXcoord + setXcoord) / 2f;
        float riseYCoord = is24HourSun ? 0 : is24HourDarkness ? 2 : 1f - (float) Math.cos(FRAC * (riseXcoord + offset));

        if (riseTime.after(setTime)) {
            offset += 12;
        }

        for (int i = 0; i < 25; i++) {
            celestialPoints.add(new CelestialGraphPoint(
                    i,
                    1f - (float) Math.cos(FRAC * (i + offset)),
                    normalColor
            ));

            horizonPoints.add(new CelestialGraphPoint(
                    i,
                    riseYCoord,
                    emphasisColor
            ));
        }

        int segments = 5;
        ArrayList<CelestialGraphPoint> celestialCurve = getCelestialCurve(celestialPoints, segments);
        ArrayList<CelestialGraphPoint> horizonCurve = getCelestialCurve(horizonPoints, segments);

        RectF graphRegion = new RectF(SUNMOON_SIZE, SUNMOON_SIZE, width - SUNMOON_SIZE, height - SUNMOON_SIZE);

        Paint strokePaint = getStrokePaint();
        Paint textPaint = getTextPaint();
        Paint textOutlinePaint = getTextOutlinePaint();

        float sunX = currentTime.get(Calendar.HOUR_OF_DAY) + currentTime.get(Calendar.MINUTE) / 60f;
        float sunY = 1f - (float) Math.cos(FRAC * (sunX + offset));

        float sunXCoord = graphHelper.getXCoord(graphBounds, graphRegion, sunX);
        float sunYCoord = graphHelper.getYCoord(graphBounds, graphRegion, sunY);

        float labelY = graphHelper.getYCoord(graphBounds, graphRegion, riseYCoord);

        RectF drawRegion = new RectF(
                sunXCoord - SUNMOON_SIZE,
                sunYCoord - SUNMOON_SIZE,
                sunXCoord + SUNMOON_SIZE,
                sunYCoord + SUNMOON_SIZE);

        RectF eraseRegion = new RectF(
                sunXCoord - SUNMOON_SIZE - 10,
                sunYCoord - SUNMOON_SIZE - 10,
                sunXCoord + SUNMOON_SIZE + 10,
                sunYCoord + SUNMOON_SIZE + 10);

        graphHelper.plotLinesOnCanvas(graphRegion, strokePaint, graphBounds, celestialCurve);

        int sunMoonRes;
        int sunMoonColor = emphasisColor;

        graphHelper.plotLinesOnCanvas(graphRegion, strokePaint, graphBounds, horizonCurve);
        graphHelper.eraseCircle(eraseRegion);

        double moonPhase = weatherModel.currentWeather.daily[dayIndex].moonPhase;

        if (isSun) {
            sunMoonRes = R.drawable.sun;
        } else {
            if (moonPhase < 0.1) {
                sunMoonRes = R.drawable.moon_100;
                sunMoonColor = normalColor;
            } else if (moonPhase < 0.2) {
                sunMoonRes = R.drawable.moon_25;
            } else if (moonPhase < 0.3) {
                sunMoonRes = R.drawable.moon_50;
            } else if (moonPhase < 0.4) {
                sunMoonRes = R.drawable.moon_75;
            } else if (moonPhase < 0.6) {
                sunMoonRes = R.drawable.moon_100;
            } else if (moonPhase < 0.7) {
                sunMoonRes = R.drawable.moon_75;
            } else if (moonPhase < 0.8) {
                sunMoonRes = R.drawable.moon_50;
            } else if (moonPhase < 0.9) {
                sunMoonRes = R.drawable.moon_25;
            } else {
                sunMoonRes = R.drawable.moon_100;
                sunMoonColor = normalColor;
            }
        }

        Drawable sunMoonDrawable = ContextCompat.getDrawable(getContext(), sunMoonRes);

        if (sunMoonDrawable != null) {
            sunMoonDrawable.setColorFilter(sunMoonColor, PorterDuff.Mode.SRC_IN);
            graphHelper.drawDrawableOnCanvas(drawRegion, sunMoonDrawable, !isSun && moonPhase < 0.4);
        }

        String riseLabel = LocaleUtils.formatTime(getContext(),
                Locale.getDefault(),
                riseTime.after(setTime) ? setTime.getTime() : riseTime.getTime(),
                weatherModel.currentWeather.timezone);

        String setLabel = LocaleUtils.formatTime(getContext(),
                Locale.getDefault(),
                riseTime.after(setTime) ? riseTime.getTime() : setTime.getTime(),
                weatherModel.currentWeather.timezone);

        textPaint.setTextAlign(Paint.Align.LEFT);
        textOutlinePaint.setTextAlign(Paint.Align.LEFT);
        graphHelper.drawText(riseLabel, SUNMOON_SIZE + 4, labelY - 12, textOutlinePaint);
        graphHelper.drawText(riseLabel, SUNMOON_SIZE + 4, labelY - 12, textPaint);

        textPaint.setTextAlign(Paint.Align.RIGHT);
        textOutlinePaint.setTextAlign(Paint.Align.RIGHT);
        graphHelper.drawText(setLabel, width - SUNMOON_SIZE - 8, labelY - 12, textOutlinePaint);
        graphHelper.drawText(setLabel, width - SUNMOON_SIZE - 8, labelY - 12, textPaint);

        return graphHelper.getBitmap();
    }

    //Based on https://stackoverflow.com/a/15528789
    private ArrayList<CelestialGraphPoint> getCelestialCurve(
            ArrayList<CelestialGraphPoint> pts,
            int segments) {
        //noinspection UnnecessaryLocalVariable
        final float segmentsF = segments;
        final float tension = 0.5f;

        final ArrayList<CelestialGraphPoint> ptsCopy = new ArrayList<>(pts.size() + 2);
        final ArrayList<CelestialGraphPoint> ptsCurve = new ArrayList<>(pts.size() * segments);

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

                ptsCurve.add(new CelestialGraphPoint(

                        c1 * ptsCopy.get(i).getX() +
                                c2 * ptsCopy.get(i + 1).getX() +
                                c3 * (ptsCopy.get(i + 1).getX() - ptsCopy.get(i - 1).getX()) * tension +
                                c4 * (ptsCopy.get(i + 2).getX() - ptsCopy.get(i).getX()) * tension,
                        c1 * ptsCopy.get(i).getY() +
                                c2 * ptsCopy.get(i + 1).getY() +
                                c3 * (ptsCopy.get(i + 1).getY() - ptsCopy.get(i - 1).getY()) * tension +
                                c4 * (ptsCopy.get(i + 2).getY() - ptsCopy.get(i).getY()) * tension,
                        ptsCopy.get(i).color
                ));
            }
        }

        return ptsCurve;
    }

    public static class CelestialGraphPoint implements GraphHelper.IGraphPoint {
        public final float x;
        public final float y;
        public final int color;

        public CelestialGraphPoint(float x, float y, int color) {
            this.x = x;
            this.y = y;
            this.color = color;
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
}
