package com.ominous.quickweather.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ominous.quickweather.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class GraphUtils {
    private static float POINT_SIZE;
    private static final Comparator<PointF>
            pointYComparator = (o1, o2) -> Float.compare(o1.y, o2.y),
            pointXComparator = (o1, o2) -> Float.compare(o1.x, o2.x);

    public static void initialize(Context context) {
        POINT_SIZE = context.getResources().getDimensionPixelSize(R.dimen.graph_point_size);
    }

    //https://stackoverflow.com/a/15528789
    public static ArrayList<PointF> getCurvePoints(ArrayList<PointF> pts) {
        final float TENSION = 0.5f;
        final float SEGMENTS = 24;//TODO Dynamic segments based on width

        ArrayList<PointF> ptsCopy = new ArrayList<>(pts.size()), ptsCurve = new ArrayList<>();

        ptsCopy.add(pts.get(0));
        ptsCopy.addAll(pts);
        ptsCopy.add(pts.get(pts.size() - 1));

        for (int i = 1, l = ptsCopy.size() - 2; i < l; i++) {
            for (int t = 0; t < SEGMENTS; t++) {

                PointF t1 = new PointF(
                        (ptsCopy.get(i + 1).x - ptsCopy.get(i - 1).x) * TENSION,
                        (ptsCopy.get(i + 1).y - ptsCopy.get(i - 1).y) * TENSION
                );

                PointF t2 = new PointF(
                        (ptsCopy.get(i + 2).x - ptsCopy.get(i).x) * TENSION,
                        (ptsCopy.get(i + 2).y - ptsCopy.get(i).y) * TENSION
                );

                double st = t / SEGMENTS,
                        c1 = 2 * Math.pow(st, 3) - 3 * Math.pow(st, 2) + 1,
                        c2 = -(2 * Math.pow(st, 3)) + 3 * Math.pow(st, 2),
                        c3 = Math.pow(st, 3) - 2 * Math.pow(st, 2) + st,
                        c4 = Math.pow(st, 3) - Math.pow(st, 2);

                ptsCurve.add(new PointF(
                        (float) (c1 * ptsCopy.get(i).x + c2 * ptsCopy.get(i + 1).x + c3 * t1.x + c4 * t2.x),
                        (float) (c1 * ptsCopy.get(i).y + c2 * ptsCopy.get(i + 1).y + c3 * t1.y + c4 * t2.y)
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
                    Collections.min(points, pointXComparator).x,
                    Collections.max(points, pointXComparator).x,
                    Collections.min(points, pointYComparator).y,
                    Collections.max(points, pointYComparator).y
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

            float x = getXCoord(graphBounds,graphRegion,point.x),
                    y = getYCoord(graphBounds,graphRegion,point.y);

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
                    Collections.min(points, pointXComparator).x,
                    Collections.max(points, pointXComparator).x,
                    Collections.min(points, pointYComparator).y,
                    Collections.max(points, pointYComparator).y
            );
        }

        Paint paint = new Paint();

        if (onBeforeDrawListener != null) {
            onBeforeDrawListener.onBeforeDraw(paint);
        }

        PointF prevPoint = null;
        for (PointF point : GraphUtils.getCurvePoints(points)) {
            if (onBeforeDrawListener != null) {
                onBeforeDrawListener.onBeforeDrawPoint(point.x, point.y, paint);
            }

            PointF coordPoint = new PointF(
                    getXCoord(graphBounds,graphRegion,point.x),
                    getYCoord(graphBounds,graphRegion,point.y));

            if (prevPoint != null) {
                canvas.drawLine(prevPoint.x, prevPoint.y, coordPoint.x, coordPoint.y, paint);
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

        bounds.MAX_X_VALUE = Math.max(bounds.MAX_X_VALUE, Collections.max(points, pointXComparator).x);
        bounds.MIN_X_VALUE = Math.min(bounds.MIN_X_VALUE, Collections.min(points, pointXComparator).x);

        for (int i = 0, l = points.size(); i < l; i += 4) {
            if (onBeforeDrawListener != null) {
                onBeforeDrawListener.onBeforeDrawPoint(points.get(i).x,0, paint);
            }

            canvas.drawText(
                    formatter == null ?
                            Integer.toString((int) points.get(i).x) :
                            formatter.format(points.get(i).x),
                    getXCoord(bounds,graphRegion,points.get(i).x),
                    graphRegion.top,
                    paint);
        }
    }

    public static void drawYAxisOnCanvas(@NonNull Canvas canvas, @NonNull RectF region, @NonNull ArrayList<PointF> points, @Nullable LabelFormatter formatter, @Nullable GraphBounds graphBounds, @Nullable OnBeforeDrawListener onBeforeDrawListener) {
        Paint paint = new Paint();

        if (onBeforeDrawListener != null) {
            onBeforeDrawListener.onBeforeDraw(paint);
        }

        GraphBounds bounds = graphBounds == null ? new GraphBounds() : graphBounds;

        bounds.MAX_Y_VALUE = Math.max(bounds.MAX_Y_VALUE, Collections.max(points, pointYComparator).y);
        bounds.MIN_Y_VALUE = Math.min(bounds.MIN_Y_VALUE, Collections.min(points, pointYComparator).y);

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

    public static class GraphBounds {
        float MIN_X_VALUE = Float.MAX_VALUE,
                MAX_X_VALUE = Float.MIN_VALUE,
                MIN_Y_VALUE = Float.MAX_VALUE,
                MAX_Y_VALUE = Float.MIN_VALUE;

        GraphBounds() {

        }

        public GraphBounds(float MIN_X_VALUE, float MAX_X_VALUE, float MIN_Y_VALUE, float MAX_Y_VALUE) {
            this.MIN_X_VALUE = MIN_X_VALUE;
            this.MAX_X_VALUE = MAX_X_VALUE;
            this.MIN_Y_VALUE = MIN_Y_VALUE;
            this.MAX_Y_VALUE = MAX_Y_VALUE;
        }
    }

    //Used to update the Paint object
    public interface OnBeforeDrawListener {
        void onBeforeDraw(Paint paint);

        void onBeforeDrawPoint(float x, float y, Paint paint);
    }

    public interface LabelFormatter {
        String format(float f);
    }
}
