/*
 *     Copyright 2019 - 2021 Tyler Williamson
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

package com.ominous.quickweather.view;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;

import com.ominous.quickweather.R;
import com.ominous.quickweather.card.AlertCardView;
import com.ominous.quickweather.card.BaseCardView;
import com.ominous.quickweather.card.CurrentForecastCardView;
import com.ominous.quickweather.card.CurrentMainCardView;
import com.ominous.quickweather.card.ForecastDetailCardView;
import com.ominous.quickweather.card.ForecastMainCardView;
import com.ominous.quickweather.card.GraphCardView;
import com.ominous.quickweather.card.RadarCardView;
import com.ominous.quickweather.data.WeatherModel;
import com.ominous.quickweather.util.LocaleUtils;

import java.util.TimeZone;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

//TODO addOnItemTouchListener?
public class WeatherCardRecyclerView extends RecyclerView {
    private final static int TYPE_CURRENT = 1, TYPE_FORECAST = 2;
    private final WeatherCardAdapter weatherCardAdapter;
    private final StaggeredGridLayoutManager staggeredGridLayoutManager;

    public WeatherCardRecyclerView(@NonNull Context context) {
        this(context, null, 0);
    }

    public WeatherCardRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WeatherCardRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.WeatherCardRecyclerView,
                0, 0);

        try {
            int cardRecyclerViewType = a.getInteger(R.styleable.WeatherCardRecyclerView_recyclerViewType, TYPE_CURRENT);

            switch (cardRecyclerViewType) {
                case TYPE_CURRENT:
                    weatherCardAdapter = new CurrentWeatherCardAdapter(context);
                    break;
                case TYPE_FORECAST:
                    weatherCardAdapter = new ForecastWeatherCardAdapter(context);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown RecyclerView Type");
            }

            this.setAdapter(weatherCardAdapter);
        } finally {
            a.recycle();
        }

        ItemAnimator itemAnimator;
        int duration = context.getResources().getInteger(R.integer.recyclerview_anim_duration);

        if ((itemAnimator = getItemAnimator()) != null) {
            itemAnimator.setChangeDuration(duration);
            itemAnimator.setRemoveDuration(duration);
            itemAnimator.setAddDuration(duration);
        }

        LayoutAnimationController animationController = new LayoutAnimationController(AnimationUtils.loadAnimation(context, R.anim.item_fade));
        animationController.setDelay(0.1f);

        setLayoutAnimation(animationController);
        setLayoutManager(staggeredGridLayoutManager = new StaggeredGridLayoutManager(1, RecyclerView.VERTICAL));
        setLayoutSpansByConfiguration(context.getResources().getConfiguration());

        addItemDecoration(new WeatherCardItemDecoration(context, staggeredGridLayoutManager));
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        setLayoutSpansByConfiguration(newConfig);
    }

    public void setLayoutSpansByConfiguration(Configuration config) {
        staggeredGridLayoutManager.setSpanCount(config.orientation == Configuration.ORIENTATION_LANDSCAPE ? 2 : 1);
    }

    public void update(WeatherModel weatherModel) {
        weatherCardAdapter.update(weatherModel);
        scheduleLayoutAnimation();
    }

    private static class WeatherCardViewHolder extends RecyclerView.ViewHolder {
        final static int
                TYPE_CURRENT_MAIN = 1,
                TYPE_CURRENT_FORECAST = 2,
                TYPE_RADAR = 3,
                TYPE_GRAPH = 4,
                TYPE_ALERT = 5,
                TYPE_FORECAST_DETAIL = 6,
                TYPE_FORECAST_MAIN = 7;

        final BaseCardView card;

        WeatherCardViewHolder(@NonNull BaseCardView card) {
            super(card);

            this.card = card;
        }
    }

    private static class CurrentWeatherCardAdapter extends WeatherCardAdapter {
        private final Resources resources;

        protected CurrentWeatherCardAdapter(Context context) {
            resources = context.getResources();
        }

        @Override
        public int getItemViewType(int position) {
            boolean isLandscape = resources.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

            int size = getItemCount();

            if (position == 0) {
                return WeatherCardViewHolder.TYPE_CURRENT_MAIN;
            } else if (position >= size - 7) {
                return WeatherCardViewHolder.TYPE_CURRENT_FORECAST;
            } else if (position == size - 8) {
                return isLandscape ? WeatherCardViewHolder.TYPE_GRAPH : WeatherCardViewHolder.TYPE_RADAR;
            } else if (position == size - 9) {
                return isLandscape ? WeatherCardViewHolder.TYPE_RADAR : WeatherCardViewHolder.TYPE_GRAPH;
            } else {
                return WeatherCardViewHolder.TYPE_ALERT;
            }
        }

        @Override
        public int getItemCount() {
            return weatherModel == null || weatherModel.responseOneCall == null ? 0 : 10 + (weatherModel.responseOneCall.alerts == null ? 0 : weatherModel.responseOneCall.alerts.length);
        }

        @Override
        void update(WeatherModel weatherModel) {
            int prevLen = getItemCount();
            this.weatherModel = weatherModel;
            int newLen = getItemCount();

            notifyItemChanged(0);

            if (newLen > prevLen) {
                notifyItemRangeInserted(prevLen - 9, newLen - prevLen);

                if (prevLen > 10) {
                    notifyItemRangeChanged(1, prevLen - 9);
                }
            } else if (newLen < prevLen) {
                notifyItemRangeRemoved(newLen - 9, prevLen - newLen);

                if (newLen > 10) {
                    notifyItemRangeChanged(1, newLen - 9);
                }
            } else {
                if (newLen > 10) {
                    notifyItemRangeChanged(1, newLen - 10);
                }
            }

            notifyItemRangeChanged(newLen - 9, 9);
        }
    }

    private static class ForecastWeatherCardAdapter extends WeatherCardAdapter {
        private final Resources resources;
        private boolean shouldCalculateItemCount = true;
        private int cachedItemCount;

        protected ForecastWeatherCardAdapter(Context context) {
            resources = context.getResources();
        }

        @Override
        public int getItemViewType(int position) {
            //TODO Forecast landscape layout
            //boolean isLandscape = resources.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

            //int size = getItemCount();

            int alertCount = 0;

            if (weatherModel.responseOneCall.alerts != null) {
                long thisDay = LocaleUtils.getStartOfDay(weatherModel.date, TimeZone.getTimeZone(weatherModel.responseOneCall.timezone)) / 1000;

                for (int i = 0, l = weatherModel.responseOneCall.alerts.length; i < l; i++) {
                    if (weatherModel.responseOneCall.alerts[i].end >= thisDay) {
                        alertCount++;
                    }
                }
            }

            if (position == 0) {
                return WeatherCardViewHolder.TYPE_FORECAST_MAIN;
            } else if (alertCount > 0 && position <= alertCount) {
                return WeatherCardViewHolder.TYPE_ALERT;
            } else if (position == alertCount + 1) {
                return WeatherCardViewHolder.TYPE_GRAPH;
            } else {
                return WeatherCardViewHolder.TYPE_FORECAST_DETAIL;
            }
        }

        @Override
        public int getItemCount() {
            if (weatherModel == null || weatherModel.responseForecast == null) {
                return 0;
            } else if (shouldCalculateItemCount) {
                int itemCount = 2;
                boolean hasHourlyData = false;

                long thisDay = LocaleUtils.getStartOfDay(weatherModel.date, TimeZone.getTimeZone(weatherModel.responseOneCall.timezone)) / 1000;
                long nextDay = thisDay + 24 * 60 * 60;

                for (int i = 0, l = weatherModel.responseForecast.list.length; i < l; i++) {
                    if (weatherModel.responseForecast.list[i].dt >= thisDay &&
                            weatherModel.responseForecast.list[i].dt < nextDay) {
                        itemCount++;
                        hasHourlyData = true;
                    }
                }

                if (weatherModel.responseOneCall.alerts != null) {
                    for (int i = 0, l = weatherModel.responseOneCall.alerts.length; i < l; i++) {
                        if (weatherModel.responseOneCall.alerts[i].end >= thisDay) {
                            itemCount++;
                        }
                    }
                }

                //if there is no hourly, dont show the graph
                cachedItemCount = hasHourlyData ? itemCount : 1;
                shouldCalculateItemCount = false;

                return cachedItemCount;
            } else {
                return cachedItemCount;
            }
        }

        @Override
        void update(WeatherModel weatherModel) {

            int prevLen = cachedItemCount;
            shouldCalculateItemCount = true;
            this.weatherModel = weatherModel;
            int newLen = getItemCount();

            //TODO fix the animations like currentadapter
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

    private abstract static class WeatherCardAdapter extends RecyclerView.Adapter<WeatherCardViewHolder> {
        protected WeatherModel weatherModel;

        abstract void update(WeatherModel weatherModel);

        @NonNull
        @Override
        public WeatherCardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            switch (viewType) {
                case WeatherCardViewHolder.TYPE_GRAPH:
                    return new WeatherCardViewHolder(new GraphCardView(parent.getContext()));
                case WeatherCardViewHolder.TYPE_ALERT:
                    return new WeatherCardViewHolder(new AlertCardView(parent.getContext()));
                case WeatherCardViewHolder.TYPE_FORECAST_DETAIL:
                    return new WeatherCardViewHolder(new ForecastDetailCardView(parent.getContext()));
                case WeatherCardViewHolder.TYPE_FORECAST_MAIN:
                    return new WeatherCardViewHolder(new ForecastMainCardView(parent.getContext()));
                case WeatherCardViewHolder.TYPE_CURRENT_MAIN:
                    return new WeatherCardViewHolder(new CurrentMainCardView(parent.getContext()));
                case WeatherCardViewHolder.TYPE_CURRENT_FORECAST:
                    return new WeatherCardViewHolder(new CurrentForecastCardView(parent.getContext()));
                case WeatherCardViewHolder.TYPE_RADAR:
                    return new WeatherCardViewHolder(new RadarCardView(parent.getContext()));
                default:
                    throw new IllegalArgumentException("Unknown Card Type");
            }
        }

        @Override
        public void onBindViewHolder(@NonNull WeatherCardViewHolder weatherCardViewHolder, int position) {
            weatherCardViewHolder.card.update(weatherModel, position);
        }
    }

    private static class WeatherCardItemDecoration extends RecyclerView.ItemDecoration {
        private final int margin;
        private final StaggeredGridLayoutManager staggeredGridLayoutManager;

        public WeatherCardItemDecoration(Context context, StaggeredGridLayoutManager staggeredGridLayoutManager) {
            margin = (int) context.getResources().getDimension(R.dimen.margin_half);
            this.staggeredGridLayoutManager = staggeredGridLayoutManager;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull State state) {
            outRect.top = parent.getChildAdapterPosition(view) < staggeredGridLayoutManager.getSpanCount() ? margin : 0;
            outRect.left = margin;
            outRect.right = margin;
            outRect.bottom = margin;
        }
    }
}
