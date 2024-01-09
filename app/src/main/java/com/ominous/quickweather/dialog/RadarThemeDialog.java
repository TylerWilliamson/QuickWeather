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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ominous.quickweather.R;
import com.ominous.quickweather.pref.RadarTheme;
import com.ominous.quickweather.pref.WeatherPreferences;

//TODO create base dialog, combine with localedialog
public class RadarThemeDialog {
    private final AlertDialog radarThemeDialog;
    private OnRadarThemeChosenListener onRadarThemeChosenListener;

    public RadarThemeDialog(Context context) {
        RadarThemeAdapter radarThemeAdapter = new RadarThemeAdapter(context);

        radarThemeDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.dialog_locale_title)
                .setView(R.layout.dialog_choice)
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (d, w) ->
                        onRadarThemeChosenListener.onRadarThemeChosen(radarThemeAdapter.getSelectedRadarTheme()))
                .create();

        radarThemeDialog.setOnShowListener(d -> {
            RecyclerView recyclerView = radarThemeDialog.findViewById(R.id.choice_recyclerview);

            if (recyclerView != null) {
                recyclerView.setAdapter(radarThemeAdapter);
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            }

            Window window = radarThemeDialog.getWindow();

            if (window != null) {
                window.setLayout(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        context.getResources().getDimensionPixelSize(R.dimen.mappicker_height));
            }

            int textColor = ContextCompat.getColor(context, R.color.color_accent_text);

            radarThemeDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(textColor);
            radarThemeDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(textColor);
        });
    }

    public void show(OnRadarThemeChosenListener onRadarThemeChosenListener) {
        this.onRadarThemeChosenListener = onRadarThemeChosenListener;
        radarThemeDialog.show();
    }

    private static class RadarThemeAdapter extends RecyclerView.Adapter<RadarThemeViewHolder> {
        private int selectedPosition = RecyclerView.NO_POSITION;
        private final String[] radarThemeNames;
        private final RadarTheme[] radarThemes = RadarTheme.values();

        public RadarThemeAdapter(Context context) {
            RadarTheme currentTheme = WeatherPreferences.getInstance(context).getRadarTheme();

            for (int i = 0, l = radarThemes.length; i < l; i++) {
                if (radarThemes[i] == currentTheme) {
                    selectedPosition = i;
                }
            }

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
    }

    private static class RadarThemeViewHolder extends RecyclerView.ViewHolder {
        public RadarThemeViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public interface OnRadarThemeChosenListener {
        void onRadarThemeChosen(RadarTheme radarTheme);
    }
}