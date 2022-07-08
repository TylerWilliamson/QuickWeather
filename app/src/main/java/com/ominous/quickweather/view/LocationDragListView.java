/*
 *     Copyright 2019 - 2022 Tyler Williamson
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
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ominous.quickweather.R;
import com.ominous.quickweather.data.WeatherDatabase;
import com.ominous.quickweather.dialog.LocationManualDialog;
import com.ominous.quickweather.dialog.OnLocationChosenListener;
import com.woxthebox.draglistview.DragItemAdapter;
import com.woxthebox.draglistview.DragListView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class LocationDragListView extends DragListView {
    private final TextView noLocationsChosenTextView;
    private final LocationDragAdapter adapter;

    public LocationDragListView(Context context) {
        this(context, null, 0);
    }

    public LocationDragListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LocationDragListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        noLocationsChosenTextView = new TextView(context, null, 0, R.style.QuickWeather_Text);
        noLocationsChosenTextView.setText(context.getString(R.string.text_no_location));

        DragListView.LayoutParams layoutParams = new DragListView.LayoutParams(DragListView.LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;

        this.addView(noLocationsChosenTextView, layoutParams);

        adapter = new LocationDragAdapter();
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                setNoLocationTextVisibility();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                setNoLocationTextVisibility();
            }
        });
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        this.setAdapter(adapter, false);
        this.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    public List<WeatherDatabase.WeatherLocation> getLocationList() {
        return adapter.getItemList();
    }

    public void setLocationList(List<WeatherDatabase.WeatherLocation> locationList) {
        adapter.setItemList(locationList);
        setNoLocationTextVisibility();
    }

    public void addLocation(WeatherDatabase.WeatherLocation location) {
        int position = adapter.getItemCount();

        adapter.addItem(position, location);
        adapter.notifyItemInserted(position);
    }

    private void setNoLocationTextVisibility() {
        noLocationsChosenTextView.setVisibility((adapter.getItemCount() == 0) ? View.VISIBLE : View.INVISIBLE);
    }

    private class LocationDragAdapter extends DragItemAdapter<WeatherDatabase.WeatherLocation, LocationViewHolder> {
        LocationDragAdapter() {
            super();

            setItemList(new ArrayList<>());
        }

        @Override
        public long getUniqueItemId(int position) {
            return mItemList.get(position).hashCode();
        }

        @NonNull
        @Override
        public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
            return new LocationViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.item_location, viewGroup, false));
        }

        @Override
        public void onBindViewHolder(final @NonNull LocationViewHolder viewHolder, int position) {
            super.onBindViewHolder(viewHolder, position);

            WeatherDatabase.WeatherLocation weatherLocation = mItemList.get(position);

            if (weatherLocation.isCurrentLocation) {
                viewHolder.locationTextView.setText(R.string.text_current_location);
                viewHolder.buttonEdit.setImageResource(R.drawable.ic_near_me_white_24dp);
            } else {
                viewHolder.locationTextView.setText(weatherLocation.name);
                viewHolder.buttonEdit.setImageResource(R.drawable.ic_edit_white_24dp);
            }
        }
    }

    private class LocationViewHolder extends DragItemAdapter.ViewHolder implements View.OnClickListener {
        final TextView locationTextView;
        final ImageView buttonClear;
        final ImageView buttonEdit;
        final LocationManualDialog locationDialog = new LocationManualDialog(getContext());
        final OnLocationChosenListener onLocationChosenListener = (location, latitude, name) -> {
            int position = getAdapterPosition();
            WeatherDatabase.WeatherLocation weatherLocation = adapter.getItemList().get(position);

            adapter.getItemList().set(position,
                    new WeatherDatabase.WeatherLocation(
                            weatherLocation.id,
                            latitude,
                            name,
                            location,
                            weatherLocation.isSelected,
                            weatherLocation.isCurrentLocation,
                            weatherLocation.order));
            adapter.notifyItemChanged(position);
        };

        LocationViewHolder(View itemView) {
            super(itemView, R.id.cardview, false);

            locationTextView = itemView.findViewById(R.id.textview_location);
            buttonClear = itemView.findViewById(R.id.button_clear);
            buttonEdit = itemView.findViewById(R.id.button_edit);

            buttonClear.setOnClickListener(this);
            buttonEdit.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            LocationDragAdapter adapter = (LocationDragAdapter) getAdapter();

            if (v.getId() == R.id.button_clear) {
                adapter.removeItem(position);
                adapter.notifyItemRemoved(position);
                adapter.notifyItemRangeChanged(position, adapter.getItemCount());//Android bug: need to explicitly tell the adapter
            } else if (!adapter.getItemList().get(position).isCurrentLocation) {
                locationDialog.show(adapter.getItemList().get(position), onLocationChosenListener);
            }
        }
    }
}
