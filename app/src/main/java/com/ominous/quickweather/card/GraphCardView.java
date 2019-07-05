package com.ominous.quickweather.card;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.ImageView;

import com.ominous.quickweather.R;
import com.ominous.quickweather.util.ColorUtils;
import com.ominous.quickweather.util.GraphUtils;
import com.ominous.quickweather.weather.Weather;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class GraphCardView extends BaseCardView {
    private ImageView graphImageView;
    private Weather.WeatherResponse response;

    public GraphCardView(Context context) {
        super(context);

        inflate(context, R.layout.card_graph, this);

        graphImageView = findViewById(R.id.graph_image_view);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (response != null) {
            new GenerateGraphTask(graphImageView).execute(response);
            response = null;
        }
    }

    @Override
    protected Uri getUri() {
        return null;
    }

    @Override
    public void update(Weather.WeatherResponse response, int position) {
        if (graphImageView.getMeasuredWidth() == 0) {
            this.response = response;
        } else {
            new GenerateGraphTask(graphImageView).execute(response);
        }
    }

    private static class GenerateGraphTask extends AsyncTask<Weather.WeatherResponse, Void, Bitmap> {
        private WeakReference<ImageView> graphImageView;
        private DrawListener drawListener;

        GenerateGraphTask(ImageView graphImageView) {
            this.graphImageView = new WeakReference<>(graphImageView);

            this.drawListener = new DrawListener(getContext());
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            graphImageView.get().setImageBitmap(bitmap);
        }

        @Override
        protected Bitmap doInBackground(Weather.WeatherResponse... responses) {
            Weather.WeatherResponse response = responses[0];

            int
                    WIDTH = graphImageView.get().getMeasuredWidth(),
                    HEIGHT = graphImageView.get().getMeasuredHeight(),
                    LEFT_PADDING = getContext().getResources().getDimensionPixelSize(R.dimen.margin_double),
                    RIGHT_PADDING = getContext().getResources().getDimensionPixelSize(R.dimen.margin_triple),
                    TOP_PADDING = getContext().getResources().getDimensionPixelSize(R.dimen.graph_point_size),
                    BOTTOM_PADDING = getContext().getResources().getDimensionPixelSize(R.dimen.text_size_regular);

            float TEXT_SIZE = getContext().getResources().getDimension(R.dimen.text_size_regular);

            float POINT_SIZE = getContext().getResources().getDimensionPixelSize(R.dimen.graph_point_size);

            Bitmap bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            RectF graphRegion = new RectF(LEFT_PADDING, TOP_PADDING, WIDTH - RIGHT_PADDING, HEIGHT - BOTTOM_PADDING - TOP_PADDING);

            Drawable thermDrawable = getContext().getResources().getDrawable(R.drawable.thermometer_25),
                    precipDrawable = getContext().getResources().getDrawable(R.drawable.raindrop);

            thermDrawable.setColorFilter(getContext().getResources().getColor(R.color.text_primary_emphasis), PorterDuff.Mode.SRC_IN);
            precipDrawable.setColorFilter(getContext().getResources().getColor(R.color.text_primary_emphasis), PorterDuff.Mode.SRC_IN);

            ArrayList<PointF> temperaturePoints = new ArrayList<>(), precipPoints = new ArrayList<>();

            long startTime = (response.hourly.data[0].time / (60 * 60 * 24)) * (1000 * 60 * 60 * 24);

            for (int i = 0, l = 24; i < l; i++) {
                temperaturePoints.add(new PointF(response.hourly.data[i].time * 1000 - startTime, (float) response.hourly.data[i].temperature));
                precipPoints.add(new PointF(response.hourly.data[i].time * 1000 - startTime, (float) response.hourly.data[i].precipProbability * 100));
            }

            GraphUtils.GraphBounds precipGraphBounds = new GraphUtils.GraphBounds(precipPoints.get(0).x, precipPoints.get(23).x, 0, 100);

            drawListener.setParams(DrawListener.PRECIP, Paint.Style.STROKE);
            GraphUtils.plotLinesOnCanvas(canvas, graphRegion, GraphUtils.getCurvePoints(precipPoints), precipGraphBounds, drawListener);

            drawListener.setParams(DrawListener.PRECIP, Paint.Style.FILL);
            GraphUtils.plotPointsOnCanvas(canvas, graphRegion, precipPoints, precipGraphBounds, drawListener);

            drawListener.setParams(DrawListener.TEMPERATURE, Paint.Style.STROKE);
            GraphUtils.plotLinesOnCanvas(canvas, graphRegion, GraphUtils.getCurvePoints(temperaturePoints), null, drawListener);

            drawListener.setParams(DrawListener.TEMPERATURE, Paint.Style.FILL);
            GraphUtils.plotPointsOnCanvas(canvas, graphRegion, temperaturePoints, null, drawListener);

            drawListener.setParams(DrawListener.TEMPERATURE, Paint.Align.LEFT);
            GraphUtils.drawYAxisOnCanvas(canvas, new RectF(0, 0, LEFT_PADDING, HEIGHT), temperaturePoints, null, null, drawListener);

            drawListener.setParams(DrawListener.PRECIP, Paint.Align.RIGHT);
            GraphUtils.drawYAxisOnCanvas(canvas, new RectF(WIDTH, 0, WIDTH + RIGHT_PADDING, HEIGHT), precipPoints, (f) -> String.format(Locale.getDefault(), "%1$d%%", (int) f), precipGraphBounds, drawListener);

            drawListener.setParams(DrawListener.DEFAULT, Paint.Align.CENTER);
            GraphUtils.drawXAxisOnCanvas(canvas, new RectF(LEFT_PADDING, HEIGHT - BOTTOM_PADDING + POINT_SIZE, WIDTH - RIGHT_PADDING, HEIGHT), temperaturePoints, (f) -> String.format(Locale.getDefault(), "%1$tl%1$tp", new Date((long) f)).toUpperCase().replace("M",""), null, drawListener);

            GraphUtils.drawDrawableOnCanvas(
                    canvas,
                    new RectF(0, HEIGHT / 2f - TEXT_SIZE / 2f, TEXT_SIZE, HEIGHT / 2f + TEXT_SIZE / 2f),
                    thermDrawable);
            GraphUtils.drawDrawableOnCanvas(
                    canvas,
                    new RectF(WIDTH - TEXT_SIZE, HEIGHT / 2f - TEXT_SIZE / 2f, WIDTH, HEIGHT / 2f + TEXT_SIZE / 2f),
                    precipDrawable);

            return bitmap;
        }

        private Context getContext() {
            return graphImageView.get().getContext();
        }
    }

    private static class DrawListener implements GraphUtils.OnBeforeDrawListener {
        static final int DEFAULT = 0, TEMPERATURE = 1, PRECIP = 2;

        private int type;
        private Paint.Style style;
        private Paint.Align align;

        private Context context;

        DrawListener(Context context) {
            this.context = context;
        }

        void setParams(int type, Paint.Style style) {
            this.type = type;
            this.style = style;
            this.align = null;
        }

        void setParams(int type, Paint.Align textAlign) {
            this.type = type;
            this.style = null;
            this.align = textAlign;
        }

        @Override
        public void onBeforeDraw(Paint paint) {
            paint.setAntiAlias(true);

            if (type == DEFAULT) {
                paint.setColor(context.getResources().getColor(R.color.text_primary));
            }

            if (style != null) {
                paint.setStyle(style);
                paint.setStrokeWidth(style == Paint.Style.STROKE ? 5 : 0);
            } else if (align != null) {
                paint.setTextSize(context.getResources().getDimension(R.dimen.text_size_regular));
                paint.setTextAlign(align);
            }
        }

        @Override
        public void onBeforeDrawPoint(float x, float y, Paint paint) {
            switch (type) {
                case TEMPERATURE:
                    paint.setColor(ColorUtils.getColorFromTemperature(y));
                    break;
                case PRECIP:
                    paint.setColor(ColorUtils.getColorFromPrecipChance(y));
                    break;
            }
        }
    }
}
