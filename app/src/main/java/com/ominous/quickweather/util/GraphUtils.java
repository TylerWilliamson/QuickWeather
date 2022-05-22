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

package com.ominous.quickweather.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import com.ominous.quickweather.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class GraphUtils {
    private static final Comparator<PointF> POINT_Y_COMPARATOR = (o1, o2) -> Float.compare(o1.y, o2.y);
    private static final Comparator<PointF> POINT_X_COMPARATOR = (o1, o2) -> Float.compare(o1.x, o2.x);
    private static float POINT_SIZE;

    public static void initialize(Context context) {
        //TODO: Dynamic based on screen size
        POINT_SIZE = context.getResources().getDimensionPixelSize(R.dimen.graph_point_size);
    }

    public static ArrayList<PointF> getCurvePoints(ArrayList<PointF> pts, int segments) {
        return getCurvePoints(pts, segments, Float.MIN_VALUE, Float.MAX_VALUE);
    }

    //Based on https://stackoverflow.com/a/15528789
    public static ArrayList<PointF> getCurvePoints(ArrayList<PointF> pts, int segments, float min, float max) {
        //noinspection UnnecessaryLocalVariable
        final float segmentsF = segments;
        final float tension = 0.5f;

        final ArrayList<PointF> ptsCopy = new ArrayList<>(pts.size() + 2);
        final ArrayList<PointF> ptsCurve = new ArrayList<>(pts.size() * segments);

        ptsCopy.add(pts.get(0));
        ptsCopy.addAll(pts);
        ptsCopy.add(pts.get(pts.size() - 1));

        for (int i = 1, l = ptsCopy.size() - 2; i < l; i++) {
            for (int t = 0; t < segments; t++) {

                float st = t / segmentsF,
                        st2 = st * st,
                        st3 = st2 * st,
                        c1 = 2 * st3 - 3 * st2 + 1,
                        c2 = -2 * st3 + 3 * st2,
                        c3 = st3 - 2 * st2 + st,
                        c4 = st3 - st2;

                ptsCurve.add(new PointF(
                        c1 * ptsCopy.get(i).x +
                                c2 * ptsCopy.get(i + 1).x +
                                c3 * (ptsCopy.get(i + 1).x - ptsCopy.get(i - 1).x) * tension +
                                c4 * (ptsCopy.get(i + 2).x - ptsCopy.get(i).x) * tension,
                        Math.min(Math.max(
                                c1 * ptsCopy.get(i).y +
                                        c2 * ptsCopy.get(i + 1).y +
                                        c3 * (ptsCopy.get(i + 1).y - ptsCopy.get(i - 1).y) * tension +
                                        c4 * (ptsCopy.get(i + 2).y - ptsCopy.get(i).y) * tension,
                                min), max)
                ));
            }
        }

        return ptsCurve;
    }

    public static void plotPointsOnCanvas(@NonNull Canvas canvas, @NonNull RectF region, @NonNull ArrayList<PointF> points, @Nullable GraphBounds graphBounds, @Nullable OnBeforeDrawListener onBeforeDrawListener) {
        RectF graphRegion = new RectF(region);
        graphRegion.left += POINT_SIZE;
        graphRegion.top += POINT_SIZE;
        graphRegion.bottom -= POINT_SIZE;
        graphRegion.right -= POINT_SIZE;

        if (graphBounds == null) {
            graphBounds = new GraphBounds(
                    Collections.min(points, POINT_X_COMPARATOR).x,
                    Collections.max(points, POINT_X_COMPARATOR).x,
                    Collections.min(points, POINT_Y_COMPARATOR).y,
                    Collections.max(points, POINT_Y_COMPARATOR).y
            );
        }

        Paint paint = new Paint();

        if (onBeforeDrawListener != null) {
            onBeforeDrawListener.onBeforeDraw(paint);
        }

        for (PointF point : points) {
            if (onBeforeDrawListener != null) {
                onBeforeDrawListener.onBeforeDrawPoint(point.x, point.y, paint);
            }

            float x = getXCoord(graphBounds, graphRegion, point.x),
                    y = getYCoord(graphBounds, graphRegion, point.y);

            canvas.drawArc(x - POINT_SIZE / 2, y - POINT_SIZE / 2, x + POINT_SIZE / 2, y + POINT_SIZE / 2, 0, 360, true, paint);
        }
    }

    public static void plotLinesOnCanvas(@NonNull Canvas canvas, @NonNull RectF region, @NonNull ArrayList<PointF> points, @Nullable GraphBounds graphBounds, @Nullable OnBeforeDrawListener onBeforeDrawListener) {
        RectF graphRegion = new RectF(region);
        graphRegion.left += POINT_SIZE;
        graphRegion.top += POINT_SIZE;
        graphRegion.bottom -= POINT_SIZE;
        graphRegion.right -= POINT_SIZE;

        if (graphBounds == null) {
            graphBounds = new GraphBounds(
                    Collections.min(points, POINT_X_COMPARATOR).x,
                    Collections.max(points, POINT_X_COMPARATOR).x,
                    Collections.min(points, POINT_Y_COMPARATOR).y,
                    Collections.max(points, POINT_Y_COMPARATOR).y
            );
        }

        Paint paint = new Paint();

        if (onBeforeDrawListener != null) {
            onBeforeDrawListener.onBeforeDraw(paint);
        }

        PointF prevPoint = null;
        for (PointF point : points) {
            if (onBeforeDrawListener != null) {
                onBeforeDrawListener.onBeforeDrawPoint(point.x, point.y, paint);
            }

            PointF coordPoint = new PointF(
                    getXCoord(graphBounds, graphRegion, point.x),
                    getYCoord(graphBounds, graphRegion, point.y));

            if (prevPoint != null) {
                canvas.drawLine(prevPoint.x, prevPoint.y, coordPoint.x, coordPoint.y, paint);
            }

            prevPoint = coordPoint;
        }
    }

    public static void plotAreaOnCanvas(@NonNull Canvas canvas, @NonNull RectF region, @NonNull ArrayList<PointF> points, @Nullable GraphBounds graphBounds, @Nullable OnBeforeDrawListener onBeforeDrawListener) {
        RectF graphRegion = new RectF(region);
        graphRegion.left += POINT_SIZE;
        graphRegion.top += POINT_SIZE;
        graphRegion.bottom -= POINT_SIZE;
        graphRegion.right -= POINT_SIZE;

        if (graphBounds == null) {
            graphBounds = new GraphBounds(
                    Collections.min(points, POINT_X_COMPARATOR).x,
                    Collections.max(points, POINT_X_COMPARATOR).x,
                    Collections.min(points, POINT_Y_COMPARATOR).y,
                    Collections.max(points, POINT_Y_COMPARATOR).y
            );
        }

        Paint paint = new Paint();

        if (onBeforeDrawListener != null) {
            onBeforeDrawListener.onBeforeDraw(paint);
        }

        float y0 = getYCoord(graphBounds, graphRegion, 0);

        PointF prevPoint = null;
        for (PointF point : points) {
            if (onBeforeDrawListener != null) {
                onBeforeDrawListener.onBeforeDrawPoint(point.x, point.y, paint);
            }

            PointF coordPoint = new PointF(
                    getXCoord(graphBounds, graphRegion, point.x),
                    getYCoord(graphBounds, graphRegion, point.y));

            if (prevPoint != null) {
                Path path = new Path();
                path.moveTo(prevPoint.x, prevPoint.y);
                path.lineTo(coordPoint.x, coordPoint.y);
                path.lineTo(coordPoint.x, y0);
                path.lineTo(prevPoint.x, y0);
                path.close();
                canvas.drawPath(path, paint);
            }

            prevPoint = coordPoint;
        }
    }

    public static void drawXAxisOnCanvas(@NonNull Canvas canvas, @NonNull RectF region, @NonNull ArrayList<PointF> points, @Nullable LabelFormatter formatter, @Nullable GraphBounds graphBounds, @Nullable OnBeforeDrawListener onBeforeDrawListener) {
        Paint paint = new Paint();

        RectF graphRegion = new RectF(region);
        graphRegion.left += POINT_SIZE;
        graphRegion.top += POINT_SIZE;
        graphRegion.bottom -= POINT_SIZE;
        graphRegion.right -= POINT_SIZE;

        if (onBeforeDrawListener != null) {
            onBeforeDrawListener.onBeforeDraw(paint);
        }

        GraphBounds bounds = graphBounds == null ? new GraphBounds() : graphBounds;

        bounds.MAX_X_VALUE = Math.max(bounds.MAX_X_VALUE, Collections.max(points, POINT_X_COMPARATOR).x);
        bounds.MIN_X_VALUE = Math.min(bounds.MIN_X_VALUE, Collections.min(points, POINT_X_COMPARATOR).x);

        float x;
        float prevX = -120;

        for (int i = 0, l = points.size(); i < l; i++) {
            if (onBeforeDrawListener != null) {
                onBeforeDrawListener.onBeforeDrawPoint(points.get(i).x, 0, paint);
            }

            x = getXCoord(bounds, graphRegion, points.get(i).x);

            if (x - prevX >= 120) {
                canvas.drawText(
                        formatter == null ?
                                Integer.toString((int) points.get(i).x) :
                                formatter.format(points.get(i).x),
                        prevX = x,
                        graphRegion.top,
                        paint);
            }
        }
    }

    public static void drawYAxisOnCanvas(@NonNull Canvas canvas, @NonNull RectF region, @NonNull ArrayList<PointF> points, @Nullable LabelFormatter formatter, @Nullable GraphBounds graphBounds, @Nullable OnBeforeDrawListener onBeforeDrawListener) {
        Paint paint = new Paint();

        if (onBeforeDrawListener != null) {
            onBeforeDrawListener.onBeforeDraw(paint);
        }

        GraphBounds bounds = graphBounds == null ? new GraphBounds() : graphBounds;

        bounds.MAX_Y_VALUE = Math.max(bounds.MAX_Y_VALUE, Collections.max(points, POINT_Y_COMPARATOR).y);
        bounds.MIN_Y_VALUE = Math.min(bounds.MIN_Y_VALUE, Collections.min(points, POINT_Y_COMPARATOR).y);

        if (onBeforeDrawListener != null) {
            onBeforeDrawListener.onBeforeDrawPoint(0, bounds.MIN_Y_VALUE, paint);
        }

        canvas.drawText(
                formatter == null ?
                        Integer.toString((int) bounds.MIN_Y_VALUE) :
                        formatter.format(bounds.MIN_Y_VALUE),
                region.left,
                region.bottom - paint.getTextSize(),
                paint);

        if (onBeforeDrawListener != null) {
            onBeforeDrawListener.onBeforeDrawPoint(0, bounds.MAX_Y_VALUE, paint);
        }

        canvas.drawText(
                formatter == null ?
                        Integer.toString((int) bounds.MAX_Y_VALUE) :
                        formatter.format(bounds.MAX_Y_VALUE),
                region.left,
                region.top + paint.getTextSize(),
                paint);
    }

    public static void drawDrawableOnCanvas(@NonNull Canvas canvas, @NonNull RectF region, @NonNull Drawable drawable) {
        canvas.save();

        canvas.translate(region.left, region.top);

        drawable.setBounds(0, 0, (int) region.width(), (int) region.height());
        drawable.draw(canvas);

        canvas.restore();
    }

    private static float getXCoord(GraphBounds graphBounds, RectF graphRect, float x) {
        return graphRect.left + graphRect.width() * (x - graphBounds.MIN_X_VALUE) / (graphBounds.MAX_X_VALUE - graphBounds.MIN_X_VALUE);
    }

    private static float getYCoord(GraphBounds graphBounds, RectF graphRect, float y) {
        return graphRect.top + graphRect.height() * (1 - (y - graphBounds.MIN_Y_VALUE) / (graphBounds.MAX_Y_VALUE - graphBounds.MIN_Y_VALUE));
    }

    //Used to update the Paint object
    public interface OnBeforeDrawListener {
        void onBeforeDraw(Paint paint);

        void onBeforeDrawPoint(float x, float y, Paint paint);
    }

    public interface LabelFormatter {
        String format(float f);
    }

    public static class GraphBounds {
        float MIN_X_VALUE = Float.MAX_VALUE;
        float MAX_X_VALUE = Float.MIN_VALUE;
        float MIN_Y_VALUE = Float.MAX_VALUE;
        float MAX_Y_VALUE = Float.MIN_VALUE;

        GraphBounds() {

        }

        public GraphBounds(float MIN_X_VALUE, float MAX_X_VALUE, float MIN_Y_VALUE, float MAX_Y_VALUE) {
            this.MIN_X_VALUE = MIN_X_VALUE;
            this.MAX_X_VALUE = MAX_X_VALUE;
            this.MIN_Y_VALUE = MIN_Y_VALUE;
            this.MAX_Y_VALUE = MAX_Y_VALUE;
        }
    }
}
