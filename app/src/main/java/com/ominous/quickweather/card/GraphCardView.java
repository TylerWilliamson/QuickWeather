package com.ominous.quickweather.card;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

import com.ominous.quickweather.R;
import com.ominous.quickweather.util.ColorUtils;
import com.ominous.quickweather.util.GraphUtils;
import com.ominous.quickweather.util.LocaleUtils;
import com.ominous.quickweather.util.WeatherUtils;
import com.ominous.quickweather.weather.WeatherResponse;
import com.ominous.tylerutils.work.SimpleAsyncTask;

import java.lang.ref.WeakReference;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import androidx.core.content.ContextCompat;

public class GraphCardView extends BaseCardView {
    private final ImageView graphImageView;
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
        if (isDrawn()) {
            new GenerateGraphTask(graphImageView).execute(response);
        } else {
            this.response = response;
        }
    }

    private boolean isDrawn() {
        return graphImageView.getMeasuredWidth() != 0;
    }

    @Override
    public void onClick(View v) {
        //Nothing
    }

    private static class GenerateGraphTask extends SimpleAsyncTask<WeatherResponse, Bitmap> {
        private final WeakReference<ImageView> graphImageView;
        private final DrawListener drawListener;
        private final int width;
        private final int height;

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
            long now = System.currentTimeMillis() / 1000;

            int
                    LEFT_PADDING = getContext().getResources().getDimensionPixelSize(R.dimen.margin_double),
                    RIGHT_PADDING = getContext().getResources().getDimensionPixelSize(R.dimen.margin_half),
                    TOP_PADDING = getContext().getResources().getDimensionPixelSize(R.dimen.graph_point_size),
                    BOTTOM_PADDING = getContext().getResources().getDimensionPixelSize(R.dimen.text_size_regular);

            float
                    TEXT_SIZE = getContext().getResources().getDimension(R.dimen.text_size_regular),
                    POINT_SIZE = getContext().getResources().getDimensionPixelSize(R.dimen.graph_point_size);

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            RectF graphRegion = new RectF(LEFT_PADDING, TOP_PADDING, width - RIGHT_PADDING, height - BOTTOM_PADDING - TOP_PADDING);

            Drawable thermDrawable = ContextCompat.getDrawable(getContext(), R.drawable.thermometer_25);

            if (thermDrawable != null) {
                thermDrawable.setColorFilter(getContext().getResources().getColor(R.color.text_primary_emphasis), PorterDuff.Mode.SRC_IN);
            }

            ArrayList<PointF> temperaturePoints = new ArrayList<>(), precipPoints = new ArrayList<>(), precipTypes = new ArrayList<>();

            int segments = width / 32;

            int i=0;

            while (response.hourly.data[i + 1].time < now) {
                i++;
            }

            for (int l = i + 24; i < l; i++) {
                temperaturePoints.add(new PointF(response.hourly.data[i].time * 1000, (float) response.hourly.data[i].temperature));
                precipPoints.add(new PointF(response.hourly.data[i].time * 1000, Math.min((float) response.hourly.data[i].precipIntensity, 2f)));
                precipTypes.add(new PointF(response.hourly.data[i].time * 1000, response.hourly.data[i].precipType.equals(WeatherResponse.DataPoint.PRECIP_RAIN) ? 0f : response.hourly.data[i].precipType.equals(WeatherResponse.DataPoint.PRECIP_MIX) ? 1f : 2f));
            }

            GraphUtils.GraphBounds precipGraphBounds = new GraphUtils.GraphBounds(precipPoints.get(0).x, precipPoints.get(23).x, 0f, 2f);

            ArrayList<PointF> precipGraphPoints = GraphUtils.getCurvePoints(precipPoints, segments, 0f, 2f),
                temperatureGraphPoints = GraphUtils.getCurvePoints(temperaturePoints, segments);

            drawListener.setParams(DrawListener.PRECIP, Paint.Style.FILL, precipTypes);
            GraphUtils.plotAreaOnCanvas(canvas, graphRegion, precipGraphPoints, segments, precipGraphBounds, drawListener);

            drawListener.setParams(DrawListener.PRECIP, Paint.Style.STROKE, precipTypes);
            GraphUtils.plotLinesOnCanvas(canvas, graphRegion, precipGraphPoints, segments, precipGraphBounds, drawListener);

            drawListener.setParams(DrawListener.PRECIP, Paint.Style.FILL, precipTypes);
            GraphUtils.plotPointsOnCanvas(canvas, graphRegion, precipPoints, precipGraphBounds, drawListener);

            drawListener.setParams(DrawListener.TEMPERATURE, Paint.Style.STROKE);
            GraphUtils.plotLinesOnCanvas(canvas, graphRegion, temperatureGraphPoints, segments, null, drawListener);

            drawListener.setParams(DrawListener.TEMPERATURE, Paint.Style.FILL);
            GraphUtils.plotPointsOnCanvas(canvas, graphRegion, temperaturePoints, null, drawListener);

            drawListener.setParams(DrawListener.TEMPERATURE, Paint.Align.LEFT);
            GraphUtils.drawYAxisOnCanvas(canvas, new RectF(0f, 0f, LEFT_PADDING, height), temperaturePoints, (t) -> Integer.toString((int) WeatherUtils.getConvertedTemperature(t)), null, drawListener);

            drawListener.setParams(DrawListener.DEFAULT, Paint.Align.CENTER);
            GraphUtils.drawXAxisOnCanvas(canvas, new RectF(LEFT_PADDING, height - BOTTOM_PADDING + POINT_SIZE, width - RIGHT_PADDING, height), temperaturePoints, (f) -> LocaleUtils.formatHour(Locale.getDefault(), new Date((long) f), timeZone), null, drawListener);

            if (thermDrawable != null) {
                GraphUtils.drawDrawableOnCanvas(
                        canvas,
                        new RectF(0f, height / 2f - TEXT_SIZE / 2f, TEXT_SIZE, height / 2f + TEXT_SIZE / 2f),
                        thermDrawable);
            }

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
        private ArrayList<PointF> values;

        private final Context context;

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
            paint.setAntiAlias(style == Paint.Style.STROKE);

            if (type == DEFAULT) {
                paint.setColor(context.getResources().getColor(R.color.text_primary));
            }

            if (style != null) {
                paint.setStyle(style);
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setStrokeWidth(style == Paint.Style.STROKE ? 5 : 0);//TODO stroke size on smaller devices
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
