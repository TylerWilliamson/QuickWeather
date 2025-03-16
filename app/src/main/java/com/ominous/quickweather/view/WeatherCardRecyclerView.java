/*
 *   Copyright 2019 - 2025 Tyler Williamson
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
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;

import com.ominous.quickweather.R;
import com.ominous.quickweather.card.AlertCardView;
import com.ominous.quickweather.card.BaseCardView;
import com.ominous.quickweather.card.CurrentDetailCardView;
import com.ominous.quickweather.card.CurrentMainCardView;
import com.ominous.quickweather.card.ForecastDetailCardView;
import com.ominous.quickweather.card.ForecastMainCardView;
import com.ominous.quickweather.card.GraphCardView;
import com.ominous.quickweather.card.RadarCardView;
import com.ominous.quickweather.card.SunMoonCardView;
import com.ominous.quickweather.data.CurrentWeather;
import com.ominous.quickweather.data.WeatherCardType;
import com.ominous.quickweather.data.WeatherModel;
import com.ominous.tylerutils.util.LocaleUtils;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

public class WeatherCardRecyclerView extends RecyclerView {
    private final WeatherCardAdapter weatherCardAdapter;
    private final StaggeredGridLayoutManager staggeredGridLayoutManager;
    private boolean isLandscape;

    public WeatherCardRecyclerView(@NonNull Context context) {
        this(context, null, 0);
    }

    public WeatherCardRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @SuppressLint("InflateParams")
    public WeatherCardRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        weatherCardAdapter = new WeatherCardAdapter();

        this.setAdapter(weatherCardAdapter);

        LayoutAnimationController animationController = new LayoutAnimationController(AnimationUtils.loadAnimation(context, R.anim.item_fade));
        animationController.setDelay(0.3f);

        setLayoutAnimation(animationController);

        isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        setLayoutManager(staggeredGridLayoutManager = new StaggeredGridLayoutManager(isLandscape ? 2 : 1, RecyclerView.VERTICAL));

        addItemDecoration(new WeatherCardItemDecoration(context, staggeredGridLayoutManager));
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE;

        staggeredGridLayoutManager.setSpanCount(isLandscape ? 2 : 1);
    }

    public void setCardSections(WeatherCardType[] cardTypeList) {
        weatherCardAdapter.setCardSectionTypeList(cardTypeList);
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

    private static class WeatherCardViewHolder extends RecyclerView.ViewHolder {
        final BaseCardView card;

        WeatherCardViewHolder(@NonNull BaseCardView card) {
            super(card);

            this.card = card;
        }
    }

    private class WeatherCardAdapter extends RecyclerView.Adapter<WeatherCardViewHolder> {
        protected WeatherModel weatherModel;
        private OnRadarCardViewCreatedListener onRadarCardViewCreatedListener;
        private WeatherMapView weatherMapView;
        private WeatherCardType[] weatherCardViewTypes = new WeatherCardType[]{};
        private WeatherCardType[] cardSectionTypeList = new WeatherCardType[]{};

        WeatherCardAdapter() {
        }

        @SuppressLint("NotifyDataSetChanged")
        protected void setCardSectionTypeList(WeatherCardType[] cardSectionTypeList) {
            this.cardSectionTypeList = cardSectionTypeList;
            weatherCardViewTypes = getWeatherCardViewTypes();

            notifyDataSetChanged();
        }

        protected void setOnRadarCardViewCreatedListener(OnRadarCardViewCreatedListener onRadarCardViewCreatedListener) {
            this.onRadarCardViewCreatedListener = onRadarCardViewCreatedListener;
        }

        void update(WeatherModel weatherModel) {
            WeatherCardType[] prevCardViewTypes = weatherCardViewTypes;

            this.weatherModel = weatherModel;
            weatherCardViewTypes = getWeatherCardViewTypes();

            int pos = 0;
            for (WeatherCardType sectionType : cardSectionTypeList) {
                int prevCount = countMatchesInList(prevCardViewTypes, sectionType);
                int newCount = countMatchesInList(weatherCardViewTypes, sectionType);

                notifyItemRangeSizeChanged(pos,
                        prevCount,
                        newCount);

                pos += Math.min(prevCount, newCount);
            }
        }

        private <T> int countMatchesInList(T[] list, T val) {
            int count = 0;

            for (T item : list) {
                if (item.equals(val)) {
                    count++;
                }
            }

            return count;
        }

        private void notifyItemRangeSizeChanged(int positionStart, int previousSize, int newSize) {
            if (newSize > previousSize) {
                notifyItemRangeChanged(positionStart, previousSize);
                notifyItemRangeInserted(positionStart + previousSize, newSize - previousSize);
            } else if (previousSize > newSize) {
                notifyItemRangeChanged(positionStart, newSize);
                notifyItemRangeRemoved(positionStart + newSize, previousSize - newSize);
            } else {
                notifyItemRangeChanged(positionStart, newSize);
            }
        }

        @SuppressLint("InflateParams") //handled by the card itself
        @NonNull
        @Override
        public WeatherCardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            WeatherCardType weatherRecyclerViewType = WeatherCardType.values()[viewType];

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
                    if (weatherMapView == null) {
                        weatherMapView = (WeatherMapView) LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.card_radar, null, false);
                    }

                    RadarCardView radarCardView = new RadarCardView(parent.getContext(), weatherMapView);

                    if (onRadarCardViewCreatedListener != null) {
                        onRadarCardViewCreatedListener.onRadarCardViewCreated(radarCardView);
                    }

                    return new WeatherCardViewHolder(radarCardView);
                case SUNMOON:
                    return new WeatherCardViewHolder(new SunMoonCardView(parent.getContext()));
                default:
                    throw new IllegalArgumentException("Unknown Card Type");
            }
        }

        @Override
        public void onBindViewHolder(@NonNull WeatherCardViewHolder weatherCardViewHolder, int position) {
            weatherCardViewHolder.card.update(weatherModel, getPositionInSection(position));
        }

        @Override
        public int getItemCount() {
            return weatherCardViewTypes.length;
        }

        @Override
        public int getItemViewType(int position) {
            WeatherCardType cardViewType = weatherCardViewTypes[position];

            return weatherModel.date == null && isLandscape ?
                    (cardViewType == WeatherCardType.RADAR ? WeatherCardType.GRAPH.ordinal() :
                            cardViewType == WeatherCardType.GRAPH ? WeatherCardType.RADAR.ordinal() : cardViewType.ordinal()) : cardViewType.ordinal();
        }

        private int getPositionInSection(int position) {
            int itemViewType = getItemViewType(position);

            for (int i=0;i<position;i++) {
                if (getItemViewType(i) == itemViewType) {
                    return position - i;
                }
            }

            return 0;
        }

        private WeatherCardType[] getWeatherCardViewTypes() {
            if (weatherModel != null && weatherModel.currentWeather != null) {
                ArrayList<WeatherCardType> cardList = new ArrayList<>();

                long thisDay = weatherModel.date == null ? 0 : LocaleUtils.getStartOfDay(weatherModel.date, weatherModel.currentWeather.timezone);
                long nextDay = thisDay + 24 * 60 * 60 * 1000;

                for (WeatherCardType sectionType : cardSectionTypeList) {
                    switch (sectionType) {
                        case CURRENT_MAIN:
                            cardList.add(WeatherCardType.CURRENT_MAIN);
                            break;
                        case ALERT:
                            if (weatherModel.currentWeather.alerts != null) {
                                for (CurrentWeather.Alert alert : weatherModel.currentWeather.alerts) {
                                    if (weatherModel.date == null) {
                                        cardList.add(WeatherCardType.ALERT);
                                    } else {
                                        //Only show the alert if it occurs on that day
                                        if (alert.end >= (thisDay / 1000)) {
                                            cardList.add(WeatherCardType.ALERT);
                                        }
                                    }
                                }
                            }
                            break;
                        case GRAPH:
                            if (weatherModel.date == null) {
                                cardList.add(WeatherCardType.GRAPH);
                            } else {
                                for (CurrentWeather.DataPoint dataPoint : weatherModel.currentWeather.trihourly) {
                                    //Only show the graph if there are data points
                                    if (dataPoint.dt >= thisDay && dataPoint.dt < nextDay) {
                                        cardList.add(WeatherCardType.GRAPH);
                                        break;
                                    }
                                }
                            }
                            break;
                        case RADAR:
                            cardList.add(WeatherCardType.RADAR);
                            break;
                        case CURRENT_FORECAST:
                            for (CurrentWeather.DataPoint ignored : weatherModel.currentWeather.daily) {
                                cardList.add(WeatherCardType.CURRENT_FORECAST);
                            }
                            break;
                        case FORECAST_MAIN:
                            cardList.add(WeatherCardType.FORECAST_MAIN);
                            break;
                        case FORECAST_DETAIL:
                            for (int i = 0, l = weatherModel.currentWeather.trihourly.length; i < l; i++) {
                                if (weatherModel.currentWeather.trihourly[i].dt >= thisDay &&
                                        weatherModel.currentWeather.trihourly[i].dt < nextDay) {
                                    cardList.add(WeatherCardType.FORECAST_DETAIL);
                                }
                            }
                            break;
                        case SUNMOON:
                            cardList.add(WeatherCardType.SUNMOON);
                            break;
                    }
                }

                return cardList.toArray(new WeatherCardType[]{});
            } else {
                return new WeatherCardType[]{};
            }
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
