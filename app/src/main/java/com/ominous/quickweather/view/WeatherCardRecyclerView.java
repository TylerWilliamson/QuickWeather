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
import com.ominous.quickweather.card.AttributionCardView;
import com.ominous.quickweather.card.RadarCardView;
import com.ominous.quickweather.util.WeatherPreferences;
import com.ominous.quickweather.weather.WeatherResponse;

public class WeatherCardRecyclerView extends RecyclerView {
    private final WeatherCardAdapter weatherCardAdapter;

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
            private final int margin = (int) getContext().getResources().getDimension(R.dimen.margin_half);

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

    public void update(WeatherResponse response) {
        weatherCardAdapter.update(response);
    }

    private class WeatherCardAdapter extends RecyclerView.Adapter<WeatherCardViewHolder> {
        private WeatherResponse response;

        @NonNull
        @Override
        public WeatherCardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            switch (viewType) {
                case WeatherCardViewHolder.TYPE_CURRENT:
                    return new WeatherCardViewHolder(new CurrentCardView(getContext()));
                case WeatherCardViewHolder.TYPE_FORECAST:
                    return new WeatherCardViewHolder(new ForecastCardView(getContext()));
                case WeatherCardViewHolder.TYPE_ATTRIBUTION:
                    return new WeatherCardViewHolder(new AttributionCardView(getContext()));
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
            //TODO: remove after DS API shuts down
            boolean isProviderDS = WeatherPreferences.getProvider().equals(WeatherPreferences.PROVIDER_DS);
            int dsOffset = isProviderDS ? 0 : 1;

            int size = getItemCount();

            if (position == 0) {
                return WeatherCardViewHolder.TYPE_CURRENT;
            } else if (isProviderDS && position == size - 1) {
                return WeatherCardViewHolder.TYPE_ATTRIBUTION;
            } else if (position >= size - 8 + dsOffset) {
                return WeatherCardViewHolder.TYPE_FORECAST;
            } else if (position == size - 9 + dsOffset) {
                return WeatherCardViewHolder.TYPE_RADAR;
            } else if (position == size - 10 + dsOffset) {
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
            //TODO: remove after DS API shuts down
            boolean isProviderDS = WeatherPreferences.getProvider().equals(WeatherPreferences.PROVIDER_DS);

            return response == null ? 0 : 10 + (response.alerts == null ? 0 : response.alerts.length) + (isProviderDS ? 1 : 0);
        }

        void update(WeatherResponse response) {
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

    private static class WeatherCardViewHolder extends RecyclerView.ViewHolder {
        final static int TYPE_CURRENT = -1, TYPE_FORECAST = -2, TYPE_RADAR = -3, TYPE_ATTRIBUTION = -4, TYPE_GRAPH = -5;

        final BaseCardView card;

        WeatherCardViewHolder(@NonNull BaseCardView card) {
            super(card);

            this.card = card;
        }
    }
}
