package com.ominous.quickweather.view;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.ominous.quickweather.R;
import com.ominous.quickweather.util.Weather;

public class WeatherCardRecyclerView extends RecyclerView {
    private WeatherCardAdapter weatherCardAdapter;

    public WeatherCardRecyclerView(@NonNull Context context) {
        this(context, null, 0);
    }

    public WeatherCardRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WeatherCardRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        weatherCardAdapter = new WeatherCardAdapter();
        setAdapter(weatherCardAdapter);

        addItemDecoration(new RecyclerView.ItemDecoration() {
            private int margin = (int) getContext().getResources().getDimension(R.dimen.margin_half);

            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull State state) {
                if (parent.getChildAdapterPosition(view) == 0) {
                    outRect.top = margin;
                }
                outRect.left = margin;
                outRect.right = margin;
                outRect.bottom = margin;
            }
        });

        setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
    }

    public void update(Weather.WeatherResponse response) {
        weatherCardAdapter.update(response);
    }

    private class WeatherCardAdapter extends RecyclerView.Adapter<WeatherCardViewHolder> {
        private Weather.WeatherResponse response;

        @NonNull
        @Override
        public WeatherCardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            switch (viewType) {
                case WeatherCardViewHolder.TYPE_CURRENT:
                    return new WeatherCardViewHolder(new CurrentWeatherCardView(getContext()));
                case WeatherCardViewHolder.TYPE_FORECAST:
                    return new WeatherCardViewHolder(new ForecastWeatherCardView(getContext()));
                case WeatherCardViewHolder.TYPE_POWEREDBY:
                    return new WeatherCardViewHolder(new PoweredByDarkskyCardView(getContext()));
                case WeatherCardViewHolder.TYPE_RADAR:
                    return new WeatherCardViewHolder(new RadarCardView(getContext()));
                default:
                    return new WeatherCardViewHolder(new AlertWeatherCardView(getContext()));
            }
        }

        @Override
        public int getItemViewType(int position) {
            int size = getItemCount();

            if (position == 0) {
                return WeatherCardViewHolder.TYPE_CURRENT;
            } else if (position == size - 1) {
                return WeatherCardViewHolder.TYPE_POWEREDBY;
            } else if (position == size - 2) {
                return WeatherCardViewHolder.TYPE_RADAR;
            } else if (position == size - 3) {
                return WeatherCardViewHolder.TYPE_FORECAST;
            } else {
                return position - 1;
            }
        }

        @Override
        public void onBindViewHolder(@NonNull WeatherCardViewHolder weatherCardViewHolder, int position) {
            weatherCardViewHolder.card.update(response, position);
        }

        @Override
        public int getItemCount() {
            return response == null ? 0 : 4 + (response.alerts == null ? 0 : response.alerts.data.length);
        }

        void update(Weather.WeatherResponse response) {
            int prevLen = getItemCount();
            this.response = response;
            int newLen = getItemCount();

            for (int i = 0; i < newLen; i++) {
                if (newLen > prevLen && i >= prevLen - 3 && i < newLen - 3) {
                    notifyItemInserted(i);
                } else if (newLen < prevLen && i < prevLen - 3 && i >= newLen - 3) {
                    notifyItemRemoved(i--);
                    prevLen--;
                } else {
                    notifyItemChanged(i);
                }
            }
        }
    }

    private class WeatherCardViewHolder extends RecyclerView.ViewHolder {
        final static int TYPE_CURRENT = -1, TYPE_FORECAST = -2, TYPE_RADAR = -3, TYPE_POWEREDBY = -4;

        BaseWeatherCardView card;

        WeatherCardViewHolder(@NonNull BaseWeatherCardView card) {
            super(card);

            this.card = card;
        }
    }
}
