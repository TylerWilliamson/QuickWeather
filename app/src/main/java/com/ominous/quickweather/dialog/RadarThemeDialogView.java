/*
 *   Copyright 2019 - 2024 Tyler Williamson
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
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ominous.quickweather.R;
import com.ominous.quickweather.pref.RadarTheme;

public class RadarThemeDialogView extends FrameLayout {
    private final RadarThemeAdapter radarThemeAdapter;

    public RadarThemeDialogView(@NonNull Context context) {
        this(context, null, 0, 0);
    }

    public RadarThemeDialogView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public RadarThemeDialogView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public RadarThemeDialogView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        LayoutInflater.from(context).inflate(R.layout.dialog_choice, this, true);

        RecyclerView recyclerView = findViewById(R.id.choice_recyclerview);
        radarThemeAdapter = new RadarThemeAdapter(context);

        if (recyclerView != null) {
            recyclerView.setAdapter(radarThemeAdapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
        }
    }

    public void setSelectedRadarTheme(RadarTheme radarTheme) {
        radarThemeAdapter.setSelectedRadarTheme(radarTheme);
    }

    public RadarTheme getSelectedRadarTheme() {
        return radarThemeAdapter.getSelectedRadarTheme();
    }

    //TODO set list of radar themes and names via method, reuse adapter
    private static class RadarThemeAdapter extends RecyclerView.Adapter<RadarThemeViewHolder> {
        private int selectedPosition = RecyclerView.NO_POSITION;
        private final String[] radarThemeNames;
        private final RadarTheme[] radarThemes = RadarTheme.values();

        public RadarThemeAdapter(Context context) {
            radarThemeNames = context.getResources().getStringArray(R.array.text_radar_themes);
        }

        @NonNull
        @Override
        public RadarThemeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            RadarThemeViewHolder viewHolder = new RadarThemeViewHolder(
                    LayoutInflater
                            .from(parent.getContext())
                            .inflate(R.layout.item_choice_dialog, parent, false));

            viewHolder.itemView.setOnClickListener(v -> {
                setSelected(viewHolder.getLayoutPosition());
                v.setSelected(true);
            });

            return viewHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull RadarThemeViewHolder holder, int position) {
            TextView localeTextView = holder.itemView.findViewById(R.id.textview_locale);

            localeTextView.setText(radarThemeNames[position]);

            holder.itemView.setSelected(selectedPosition == position);
        }

        @Override
        public int getItemCount() {
            return radarThemes.length;
        }

        private void setSelected(int position) {
            int prevPosition = selectedPosition;
            selectedPosition = position;

            if (prevPosition != selectedPosition) {
                notifyItemChanged(prevPosition);
            }
        }

        public RadarTheme getSelectedRadarTheme() {
            return radarThemes[selectedPosition];
        }

        public void setSelectedRadarTheme(RadarTheme radarTheme) {
            for (int i = 0, l = radarThemes.length; i < l; i++) {
                if (radarThemes[i] == radarTheme) {
                    selectedPosition = i;
                }
            }
        }
    }

    private static class RadarThemeViewHolder extends RecyclerView.ViewHolder {
        public RadarThemeViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}