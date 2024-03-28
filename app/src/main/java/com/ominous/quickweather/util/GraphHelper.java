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

package com.ominous.quickweather.util;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import com.ominous.quickweather.R;

import java.util.ArrayList;

public class GraphHelper {
    private final float POINT_SIZE;
    private final Canvas canvas;
    private final Bitmap bitmap;

    public GraphHelper(Resources resources, int width, int height) {
        //TODO: Dynamic based on screen size
        POINT_SIZE = resources.getDimensionPixelSize(R.dimen.graph_point_size);

        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
    }

    public void plotPointsOnCanvas(@NonNull RectF region,
                                   @NonNull Paint paint,
                                   @NonNull GraphBounds graphBounds,
                                   @NonNull ArrayList<? extends IGraphPoint> points) {
        RectF graphRegion = getGraphRect(region);

        for (IGraphPoint point : points) {
            float x = getXCoord(graphBounds, graphRegion, point.getX());
            float y = getYCoord(graphBounds, graphRegion, point.getY());

            canvas.drawArc(x - POINT_SIZE / 2, y - POINT_SIZE / 2, x + POINT_SIZE / 2, y + POINT_SIZE / 2, 0, 360, true, point.getPaint(paint));
        }
    }

    public void plotLinesOnCanvas(@NonNull RectF region,
                                  @NonNull Paint paint,
                                  @NonNull GraphBounds graphBounds,
                                  @NonNull ArrayList<? extends IGraphPoint> points) {
        RectF graphRegion = getGraphRect(region);

        float prevX = -1;
        float prevY = -1;

        for (IGraphPoint point : points) {
            float x = getXCoord(graphBounds, graphRegion, point.getX());
            float y = getYCoord(graphBounds, graphRegion, point.getY());

            if (prevX != -1 || prevY != -1) {
                canvas.drawLine(prevX, prevY, x, y, point.getPaint(paint));
            }

            prevX = x;
            prevY = y;
        }
    }

    public void plotAreaOnCanvas(@NonNull RectF region,
                                 @NonNull Paint paint,
                                 @NonNull GraphBounds graphBounds,
                                 @NonNull ArrayList<? extends IGraphPoint> points) {
        RectF graphRegion = getGraphRect(region);

        float y0 = getYCoord(graphBounds, graphRegion, 0);
        float prevX = -1;
        float prevY = -1;

        for (IGraphPoint point : points) {
            float x = getXCoord(graphBounds, graphRegion, point.getX());
            float y = getYCoord(graphBounds, graphRegion, point.getY());

            if (prevX != -1 || prevY != -1) {
                Path path = new Path();
                path.moveTo(prevX, prevY);
                path.lineTo(x, y);
                path.lineTo(x, y0);
                path.lineTo(prevX, y0);
                path.close();
                canvas.drawPath(path, point.getPaint(paint));
            }

            prevX = x;
            prevY = y;
        }
    }

    public void drawXAxisOnCanvas(@NonNull RectF region,
                                  @NonNull Paint paint,
                                  @NonNull GraphBounds graphBounds,
                                  @NonNull ArrayList<? extends IGraphLabel> points) {
        RectF graphRegion = getGraphRect(region);
        graphRegion.top += POINT_SIZE;

        float prevX = -120;

        for (IGraphLabel point : points) {
            float x = getXCoord(graphBounds, graphRegion, point.getX());

            if (x - prevX >= 120) {
                canvas.drawText(
                        point.getLabel(),
                        x,
                        graphRegion.top,
                        paint);

                prevX = x;
            }
        }
    }

    public void drawYAxisOnCanvas(@NonNull RectF region,
                                  @NonNull Paint paint,
                                  @NonNull ArrayList<? extends IGraphLabel> points) {
        canvas.drawText(
                points.get(0).getLabel(),
                region.left,
                region.bottom - paint.getTextSize(),
                points.get(0).getPaint(paint));

        canvas.drawText(
                points.get(1).getLabel(),
                region.left,
                region.top + paint.getTextSize(),
                points.get(1).getPaint(paint));
    }

    public void eraseCircle(@NonNull RectF region) {
        Paint clearPaint = new Paint();
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawArc(region.left, region.top, region.right, region.bottom, 0, 360, true, clearPaint);
    }

    public void drawText(String text, float x, float y, Paint paint) {
        canvas.drawText(text, x, y, paint);
    }

    public void drawDrawableOnCanvas(@NonNull RectF region, @NonNull Drawable drawable) {
        drawDrawableOnCanvas(region, drawable, false);
    }

    public void drawDrawableOnCanvas(@NonNull RectF region, @NonNull Drawable drawable, boolean flip) {
        canvas.save();

        if (!flip) {
            canvas.translate(region.left, region.top);
        } else  {
            canvas.rotate(180, canvas.getWidth() / 2f, canvas.getHeight() / 2f);
            canvas.translate(canvas.getWidth() - region.left - region.width(), canvas.getHeight() - region.top - region.height());
        }

        drawable.setBounds(0, 0, (int) region.width(), (int) region.height());

        drawable.draw(canvas);

        canvas.restore();
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public float getXCoord(GraphBounds graphBounds, RectF graphRect, float x) {
        return graphRect.left + graphRect.width() * (x - graphBounds.MIN_X_VALUE) / (graphBounds.MAX_X_VALUE - graphBounds.MIN_X_VALUE);
    }

    public float getYCoord(GraphBounds graphBounds, RectF graphRect, float y) {
        return graphRect.top + graphRect.height() * (1 - (y - graphBounds.MIN_Y_VALUE) / (graphBounds.MAX_Y_VALUE - graphBounds.MIN_Y_VALUE));
    }

    private RectF getGraphRect(RectF region) {
        RectF graphRect = new RectF(region);
        graphRect.left += POINT_SIZE;
        graphRect.top += POINT_SIZE;
        graphRect.bottom -= POINT_SIZE;
        graphRect.right -= POINT_SIZE;

        return graphRect;
    }

    public interface IGraphPoint {
        float getX();

        float getY();

        Paint getPaint(Paint paint);
    }

    public interface IGraphLabel {
        String getLabel();

        float getX();

        Paint getPaint(Paint paint);
    }

    public static class GraphBounds {
        private final int MIN_X_VALUE;
        private final int MAX_X_VALUE;
        private final int MIN_Y_VALUE;
        private final int MAX_Y_VALUE;

        public GraphBounds(int MIN_X_VALUE, int MAX_X_VALUE, int MIN_Y_VALUE, int MAX_Y_VALUE) {
            this.MIN_X_VALUE = MIN_X_VALUE;
            this.MAX_X_VALUE = MAX_X_VALUE;
            this.MIN_Y_VALUE = MIN_Y_VALUE;
            this.MAX_Y_VALUE = MAX_Y_VALUE;
        }
    }
}
