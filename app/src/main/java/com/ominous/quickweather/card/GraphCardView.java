package com.ominous.quickweather.card;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import com.ominous.quickweather.R;
import com.ominous.quickweather.util.ColorUtils;
import com.ominous.quickweather.util.GraphUtils;
import com.ominous.quickweather.util.LocaleUtils;
import com.ominous.quickweather.weather.WeatherResponse;
import com.ominous.quickweather.util.WeatherUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class GraphCardView extends BaseCardView {
    private ImageView graphImageView;
    private WeatherResponse response;

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
    public void update(WeatherResponse response, int position) {
        if (graphImageView.getMeasuredWidth() == 0) {
            this.response = response;
        } else {
            new GenerateGraphTask(graphImageView).execute(response);
        }
    }

    private static class GenerateGraphTask extends AsyncTask<WeatherResponse, Void, Bitmap> {
        private WeakReference<ImageView> graphImageView;
        private DrawListener drawListener;
        private int width, height;

        GenerateGraphTask(ImageView graphImageView) {
            this.graphImageView = new WeakReference<>(graphImageView);

            this.drawListener = new DrawListener(getContext());

            width = graphImageView.getMeasuredWidth();
            height = graphImageView.getMeasuredHeight();

        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            graphImageView.get().setImageBitmap(bitmap);
        }

        @Override
        protected Bitmap doInBackground(WeatherResponse... responses) {
            WeatherResponse response = responses[0];
            TimeZone timeZone = TimeZone.getTimeZone(response.timezone);

            int
                    LEFT_PADDING = getContext().getResources().getDimensionPixelSize(R.dimen.margin_double),
                    RIGHT_PADDING = getContext().getResources().getDimensionPixelSize(R.dimen.margin_half),
                    TOP_PADDING = getContext().getResources().getDimensionPixelSize(R.dimen.graph_point_size),
                    BOTTOM_PADDING = getContext().getResources().getDimensionPixelSize(R.dimen.text_size_regular);

            float TEXT_SIZE = getContext().getResources().getDimension(R.dimen.text_size_regular);

            float POINT_SIZE = getContext().getResources().getDimensionPixelSize(R.dimen.graph_point_size);

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            RectF graphRegion = new RectF(LEFT_PADDING, TOP_PADDING, width - RIGHT_PADDING, height - BOTTOM_PADDING - TOP_PADDING);

            Drawable thermDrawable = ContextCompat.getDrawable(getContext(), R.drawable.thermometer_25);

            if (thermDrawable != null) {
                thermDrawable.setColorFilter(getContext().getResources().getColor(R.color.text_primary_emphasis), PorterDuff.Mode.SRC_IN);
            }

            ArrayList<PointF> temperaturePoints = new ArrayList<>(), precipPoints = new ArrayList<>(), precipTypes = new ArrayList<>();

            long startTime = (response.hourly.data[0].time / (60 * 60 * 24)) * (1000 * 60 * 60 * 24);

            for (int i = 0, l = 24; i < l; i++) {
                temperaturePoints.add(new PointF(response.hourly.data[i].time * 1000 - startTime, (float) response.hourly.data[i].temperature));
                precipPoints.add(new PointF(response.hourly.data[i].time * 1000 - startTime, Math.min((float) response.hourly.data[i].precipIntensity, 2f)));
                precipTypes.add(new PointF(response.hourly.data[i].time * 1000 - startTime, response.hourly.data[i].precipType.equals(WeatherResponse.DataPoint.PRECIP_RAIN) ? 0 : response.hourly.data[i].precipType.equals(WeatherResponse.DataPoint.PRECIP_MIX) ? 1 : 2));
            }

            GraphUtils.GraphBounds precipGraphBounds = new GraphUtils.GraphBounds(precipPoints.get(0).x, precipPoints.get(23).x, 0, 2);

            ArrayList<PointF> precipGraphPoints = GraphUtils.getCurvePoints(precipPoints, 0f, 2f);

            drawListener.setParams(DrawListener.PRECIP, Paint.Style.FILL, precipTypes);
            GraphUtils.plotAreaOnCanvas(canvas, graphRegion, precipGraphPoints, precipGraphBounds, drawListener);

            drawListener.setParams(DrawListener.PRECIP, Paint.Style.STROKE, precipTypes);
            GraphUtils.plotLinesOnCanvas(canvas, graphRegion, precipGraphPoints, precipGraphBounds, drawListener);

            drawListener.setParams(DrawListener.PRECIP, Paint.Style.FILL, precipTypes);
            GraphUtils.plotPointsOnCanvas(canvas, graphRegion, precipPoints, precipGraphBounds, drawListener);

            drawListener.setParams(DrawListener.TEMPERATURE, Paint.Style.STROKE);
            GraphUtils.plotLinesOnCanvas(canvas, graphRegion, GraphUtils.getCurvePoints(temperaturePoints), null, drawListener);

            drawListener.setParams(DrawListener.TEMPERATURE, Paint.Style.FILL);
            GraphUtils.plotPointsOnCanvas(canvas, graphRegion, temperaturePoints, null, drawListener);

            drawListener.setParams(DrawListener.TEMPERATURE, Paint.Align.LEFT);
            GraphUtils.drawYAxisOnCanvas(canvas, new RectF(0, 0, LEFT_PADDING, height), temperaturePoints, (t) -> Integer.toString((int) WeatherUtils.getConvertedTemperature(t)), null, drawListener);

            drawListener.setParams(DrawListener.DEFAULT, Paint.Align.CENTER);
            GraphUtils.drawXAxisOnCanvas(canvas, new RectF(LEFT_PADDING, height - BOTTOM_PADDING + POINT_SIZE, width - RIGHT_PADDING, height), temperaturePoints, (f) -> LocaleUtils.formatHour(Locale.getDefault(), new Date((long) f), timeZone), null, drawListener);

            if (thermDrawable != null) {
                GraphUtils.drawDrawableOnCanvas(
                        canvas,
                        new RectF(0, height / 2f - TEXT_SIZE / 2f, TEXT_SIZE, height / 2f + TEXT_SIZE / 2f),
                        thermDrawable);
            }

            return bitmap;
        }

        private Context getContext() {
            return graphImageView.get().getContext();
        }
    }

    @Override
    public void onClick(View v) {
        //Nothing
    }

    private static class DrawListener implements GraphUtils.OnBeforeDrawListener {
        static final int DEFAULT = 0, TEMPERATURE = 1, PRECIP = 2;

        private int type;
        private Paint.Style style;
        private Paint.Align align;
        private ArrayList<PointF> values;

        private Context context;

        DrawListener(Context context) {
            this.context = context;
        }

        void setParams(int type, Paint.Style style, ArrayList<PointF> values) {
            this.type = type;
            this.style = style;
            this.align = null;
            this.values = values;
        }

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
