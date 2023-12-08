/*
 *   Copyright 2019 - 2023 Tyler Williamson
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

package com.ominous.quickweather.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.ominous.quickweather.R;
import com.ominous.quickweather.card.AlertCardView;
import com.ominous.quickweather.card.BaseCardView;
import com.ominous.quickweather.card.CurrentDetailCardView;
import com.ominous.quickweather.card.CurrentMainCardView;
import com.ominous.quickweather.card.ForecastDetailCardView;
import com.ominous.quickweather.card.ForecastMainCardView;
import com.ominous.quickweather.card.GraphCardView;
import com.ominous.quickweather.card.RadarCardView;
import com.ominous.quickweather.data.WeatherModel;
import com.ominous.quickweather.pref.RadarQuality;
import com.ominous.quickweather.pref.WeatherPreferences;
import com.ominous.tylerutils.util.LocaleUtils;

public class WeatherCardRecyclerView extends RecyclerView {
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

        int recyclerViewType = a.getInteger(R.styleable.WeatherCardRecyclerView_recyclerViewType, -1);
        a.recycle();

        if (recyclerViewType == -1) {
            throw new IllegalArgumentException("Unknown RecyclerView Type");
        }

        WeatherRecyclerViewType weatherRecyclerViewType = WeatherRecyclerViewType.values()[recyclerViewType];

        weatherCardAdapter = weatherRecyclerViewType == WeatherRecyclerViewType.CURRENT ?
                new CurrentWeatherCardAdapter(getContext()) :
                new ForecastWeatherCardAdapter(getContext());

        this.setAdapter(weatherCardAdapter);

        ItemAnimator itemAnimator = getItemAnimator();

        if (itemAnimator != null) {
            int duration = context.getResources().getInteger(R.integer.recyclerview_anim_duration);

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

    //TODO: Bug when opening Forecast, updating theme, then changing back to Main, no cards shown
    /*public void updateTheme() {
        setAdapter(weatherCardAdapter);

        setBackgroundColor(ContextCompat.getColor(getContext(),R.color.recyclerview_background));
    }*/

    public void setLayoutSpansByConfiguration(Configuration config) {
        staggeredGridLayoutManager.setSpanCount(config.orientation == Configuration.ORIENTATION_LANDSCAPE ? 2 : 1);
    }

    public void update(WeatherModel weatherModel) {
        weatherCardAdapter.update(weatherModel);
        scheduleLayoutAnimation();
    }

    public void setOnRadarWebViewCreatedListener(OnRadarCardViewCreatedListener onRadarCardViewCreatedListener) {
        weatherCardAdapter.setOnRadarCardViewCreatedListener(onRadarCardViewCreatedListener);
    }

    public interface OnRadarCardViewCreatedListener {
        void onRadarCardViewCreated(RadarCardView radarCardView);
    }

    private enum WeatherCardViewType {
        CURRENT_MAIN,
        CURRENT_FORECAST,
        RADAR,
        GRAPH,
        ALERT,
        FORECAST_DETAIL,
        FORECAST_MAIN
    }

    private static class WeatherCardViewHolder extends RecyclerView.ViewHolder {
        final BaseCardView card;

        WeatherCardViewHolder(@NonNull BaseCardView card) {
            super(card);

            this.card = card;
        }
    }

    private static class CurrentWeatherCardAdapter extends WeatherCardAdapter {
        private final boolean shouldShowRadar;

        @SuppressLint("InflateParams")
        CurrentWeatherCardAdapter(Context context) {
            super(context, (WeatherMapView) LayoutInflater.from(context).inflate(R.layout.card_radar, null, false));

            shouldShowRadar = WeatherPreferences.getInstance(context).getRadarQuality() != RadarQuality.DISABLED;
        }

        @Override
        public int getItemViewType(int position) {
            boolean isLandscape = resources.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

            int size = getItemCount();

            if (position == 0) {
                return WeatherCardViewType.CURRENT_MAIN.ordinal();
            } else if (position >= size - 7) {
                return WeatherCardViewType.CURRENT_FORECAST.ordinal();
            } else if (position == size - 8) {
                return isLandscape || !shouldShowRadar ? WeatherCardViewType.GRAPH.ordinal() : WeatherCardViewType.RADAR.ordinal();
            } else if (position == size - 9 && shouldShowRadar) {
                return isLandscape ? WeatherCardViewType.RADAR.ordinal() : WeatherCardViewType.GRAPH.ordinal();
            } else {
                return WeatherCardViewType.ALERT.ordinal();
            }
        }

        @Override
        public int getItemCount() {
            return weatherModel == null || weatherModel.currentWeather == null ? 0 :
                    9 + (weatherModel.currentWeather.alerts == null ? 0 : weatherModel.currentWeather.alerts.length)
                    + (shouldShowRadar ? 1 : 0);
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
        private boolean shouldCalculateItemCount = true;
        private int cachedItemCount;

        ForecastWeatherCardAdapter(Context context) {
            super(context, null);
        }

        @Override
        public int getItemViewType(int position) {
            //TODO Forecast landscape layout
            //boolean isLandscape = resources.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

            //int size = getItemCount();

            int alertCount = 0;

            if (weatherModel.currentWeather.alerts != null) {
                long thisDay = LocaleUtils.getStartOfDay(weatherModel.date, weatherModel.currentWeather.timezone) / 1000;

                for (int i = 0, l = weatherModel.currentWeather.alerts.length; i < l; i++) {
                    if (weatherModel.currentWeather.alerts[i].end >= thisDay) {
                        alertCount++;
                    }
                }
            }

            if (position == 0) {
                return WeatherCardViewType.FORECAST_MAIN.ordinal();
            } else if (alertCount > 0 && position <= alertCount) {
                return WeatherCardViewType.ALERT.ordinal();
            } else if (position == alertCount + 1) {
                return WeatherCardViewType.GRAPH.ordinal();
            } else {
                return WeatherCardViewType.FORECAST_DETAIL.ordinal();
            }
        }

        @Override
        public int getItemCount() {
            if (weatherModel == null || weatherModel.currentWeather.trihourly == null) {
                return 0;
            } else if (shouldCalculateItemCount) {
                int itemCount = 2;
                boolean hasHourlyData = false;

                long thisDay = LocaleUtils.getStartOfDay(weatherModel.date, weatherModel.currentWeather.timezone) / 1000;
                long nextDay = thisDay + 24 * 60 * 60;

                for (int i = 0, l = weatherModel.currentWeather.trihourly.length; i < l; i++) {
                    if (weatherModel.currentWeather.trihourly[i].dt >= thisDay &&
                            weatherModel.currentWeather.trihourly[i].dt < nextDay) {
                        itemCount++;
                        hasHourlyData = true;
                    }
                }

                if (weatherModel.currentWeather.alerts != null) {
                    for (int i = 0, l = weatherModel.currentWeather.alerts.length; i < l; i++) {
                        if (weatherModel.currentWeather.alerts[i].end >= thisDay) {
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
        protected final Resources resources;
        protected WeatherModel weatherModel;
        private OnRadarCardViewCreatedListener onRadarCardViewCreatedListener;
        private final WeatherMapView weatherMapView;

        WeatherCardAdapter(Context context, WeatherMapView weatherMapView) {
            this.resources = context.getResources();

            this.weatherMapView = weatherMapView;
        }

        protected void setOnRadarCardViewCreatedListener(OnRadarCardViewCreatedListener onRadarCardViewCreatedListener) {
            this.onRadarCardViewCreatedListener = onRadarCardViewCreatedListener;
        }

        abstract void update(WeatherModel weatherModel);

        @NonNull
        @Override
        public WeatherCardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            WeatherCardViewType weatherRecyclerViewType = WeatherCardViewType.values()[viewType];

            switch (weatherRecyclerViewType) {
                case GRAPH:
                    return new WeatherCardViewHolder(new GraphCardView(parent.getContext()));
                case ALERT:
                    return new WeatherCardViewHolder(new AlertCardView(parent.getContext()));
                case FORECAST_DETAIL:
                    return new WeatherCardViewHolder(new ForecastDetailCardView(parent.getContext()));
                case FORECAST_MAIN:
                    return new WeatherCardViewHolder(new ForecastMainCardView(parent.getContext()));
                case CURRENT_MAIN:
                    return new WeatherCardViewHolder(new CurrentMainCardView(parent.getContext()));
                case CURRENT_FORECAST:
                    return new WeatherCardViewHolder(new CurrentDetailCardView(parent.getContext()));
                case RADAR:
                    RadarCardView radarCardView = new RadarCardView(parent.getContext(), weatherMapView);

                    if (onRadarCardViewCreatedListener != null) {
                        onRadarCardViewCreatedListener.onRadarCardViewCreated(radarCardView);
                    }

                    return new WeatherCardViewHolder(radarCardView);
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

    private enum WeatherRecyclerViewType {
        CURRENT,
        FORECAST
    }
}
