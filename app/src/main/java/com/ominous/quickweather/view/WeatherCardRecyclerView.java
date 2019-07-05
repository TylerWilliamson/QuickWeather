package com.ominous.quickweather.view;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ominous.quickweather.R;
import com.ominous.quickweather.card.AlertCardView;
import com.ominous.quickweather.card.BaseCardView;
import com.ominous.quickweather.card.CurrentCardView;
import com.ominous.quickweather.card.ForecastCardView;
import com.ominous.quickweather.card.GraphCardView;
import com.ominous.quickweather.card.PoweredByDarkskyCardView;
import com.ominous.quickweather.card.RadarCardView;
import com.ominous.quickweather.weather.Weather;

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

        setLayoutManager(new LinearLayoutManager(context, RecyclerView.VERTICAL, false));
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
                    return new WeatherCardViewHolder(new CurrentCardView(getContext()));
                case WeatherCardViewHolder.TYPE_FORECAST:
                    return new WeatherCardViewHolder(new ForecastCardView(getContext()));
                case WeatherCardViewHolder.TYPE_POWEREDBY:
                    return new WeatherCardViewHolder(new PoweredByDarkskyCardView(getContext()));
                case WeatherCardViewHolder.TYPE_GRAPH:
                    return new WeatherCardViewHolder(new GraphCardView(getContext()));
                case WeatherCardViewHolder.TYPE_RADAR:
                    return new WeatherCardViewHolder(new RadarCardView(getContext()));
                default:
                    return new WeatherCardViewHolder(new AlertCardView(getContext()));
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
                return WeatherCardViewHolder.TYPE_FORECAST;
            } else if (position == size - 3) {
                return WeatherCardViewHolder.TYPE_RADAR;
            } else if (position == size - 4) {
                return WeatherCardViewHolder.TYPE_GRAPH;
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
            return response == null ? 0 : 5 + (response.alerts == null ? 0 : response.alerts.length);
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
        final static int TYPE_CURRENT = -1, TYPE_FORECAST = -2, TYPE_RADAR = -3, TYPE_POWEREDBY = -4, TYPE_GRAPH = -5;

        BaseCardView card;

        WeatherCardViewHolder(@NonNull BaseCardView card) {
            super(card);

            this.card = card;
        }
    }
}
