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

package com.ominous.quickweather.dialog;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.ominous.quickweather.R;
import com.ominous.quickweather.data.WeatherDatabase;
import com.ominous.quickweather.view.LayoutDragListView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

public class LayoutDialog {
    private final AlertDialog layoutDialog;
    private OnLayoutChangedListener onLayoutChangedListener;
    private ViewPager2 viewPager2;
    private List<WeatherDatabase.WeatherCard> currentWeatherCards = null;
    private List<WeatherDatabase.WeatherCard> forecastWeatherCards = null;

    public LayoutDialog(Context context) {
        layoutDialog = new AlertDialog.Builder(context)
                .setView(R.layout.dialog_layout)
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (d, w) ->
                        onLayoutChangedListener.onLayoutChosen(currentWeatherCards, forecastWeatherCards))
                .create();

        layoutDialog.setOnShowListener(d -> {
            viewPager2 = layoutDialog.findViewById(R.id.pager);
            TabLayout tabLayout = layoutDialog.findViewById(R.id.tab_layout);

            if (viewPager2 != null) {
                viewPager2.setAdapter(new LayoutViewPagerAdapter());

                if (tabLayout != null) {
                    new TabLayoutMediator(tabLayout, viewPager2,
                            (tab, position) -> tab.setText(position == 0 ?
                                    context.getString(R.string.dialog_layout_tab_current) :
                                    context.getString(R.string.dialog_layout_tab_forecast))
                    ).attach();
                }
            }

            int textColor = ContextCompat.getColor(context, R.color.color_accent_text);

            layoutDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(textColor);
            layoutDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(textColor);
        });
    }

    public void show(List<WeatherDatabase.WeatherCard> currentWeatherCards,
                     List<WeatherDatabase.WeatherCard> forecastWeatherCards,
                     OnLayoutChangedListener onLayoutChangedListener) {
        this.currentWeatherCards = currentWeatherCards;
        this.forecastWeatherCards = forecastWeatherCards;

        this.onLayoutChangedListener = onLayoutChangedListener;
        layoutDialog.show();
    }

    public interface OnLayoutChangedListener {
        void onLayoutChosen(List<WeatherDatabase.WeatherCard> currentWeatherCards,
                            List<WeatherDatabase.WeatherCard> forecastWeatherCards);
    }


    private class LayoutViewPagerAdapter extends RecyclerView.Adapter<SimpleViewHolder> {
        public LayoutViewPagerAdapter() {
            super();
        }

        @NonNull
        @Override
        public SimpleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutDragListView layoutDragListView = new LayoutDragListView(parent.getContext());

            layoutDragListView.setLayoutParams(
                    new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT));

            return new SimpleViewHolder(layoutDragListView);
        }

        @Override
        public void onBindViewHolder(@NonNull SimpleViewHolder holder, int position) {
            LayoutDragListView layoutDragListView = ((LayoutDragListView) holder.itemView);

            layoutDragListView.setItemList(
                    position == 0 ? currentWeatherCards : forecastWeatherCards);

            //have to call this manually
            layoutDragListView.onFinishInflate();
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }

    private static class SimpleViewHolder extends RecyclerView.ViewHolder {
        public SimpleViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

}