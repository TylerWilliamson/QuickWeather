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

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.ominous.quickweather.R;
import com.ominous.quickweather.data.WeatherDatabase;
import com.woxthebox.draglistview.DragItemAdapter;
import com.woxthebox.draglistview.DragListView;

import java.util.ArrayList;
import java.util.List;

public class LayoutDragListView extends DragListView {
    private final LayoutDragAdapter adapter;

    public LayoutDragListView(Context context) {
        this(context, null, 0);
    }

    public LayoutDragListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LayoutDragListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        adapter = new LayoutDragAdapter(context);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();

        this.setAdapter(adapter, true);
        this.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    public void setItemList(List<WeatherDatabase.WeatherCard> itemList) {
        adapter.setItemList(itemList);
    }

    private static class LayoutDragAdapter extends DragItemAdapter<WeatherDatabase.WeatherCard, LayoutViewHolder> {
        private final Context context;

        LayoutDragAdapter(Context context) {
            super();

            this.context = context;

            setItemList(new ArrayList<>());
        }

        @Override
        public long getUniqueItemId(int position) {
            return mItemList.get(position).id;
        }

        @NonNull
        @Override
        public LayoutViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
            LayoutViewHolder viewHolder = new LayoutViewHolder(
                    LayoutInflater.from(context).inflate(R.layout.item_card_dialog, viewGroup, false));

            viewHolder.enabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int position = viewHolder.getBindingAdapterPosition();

                WeatherDatabase.WeatherCard weatherCard = mItemList.get(position);

                weatherCard.enabled = isChecked;

                mItemList.set(position, weatherCard);
            });

            return viewHolder;
        }

        @Override
        public void onBindViewHolder(final @NonNull LayoutViewHolder viewHolder, int position) {
            super.onBindViewHolder(viewHolder, position);

            WeatherDatabase.WeatherCard weatherCard = mItemList.get(position);

            viewHolder.enabledSwitch.setChecked(weatherCard.enabled);
            viewHolder.enabledSwitch.setText(weatherCard.weatherCardType.getDescriptionRes());
        }
    }

    private static class LayoutViewHolder extends DragItemAdapter.ViewHolder {
        final SwitchMaterial enabledSwitch;

        LayoutViewHolder(View itemView) {
            super(itemView, R.id.button_drag, false);

            enabledSwitch = itemView.findViewById(R.id.switch_enabled);
        }
    }
}
