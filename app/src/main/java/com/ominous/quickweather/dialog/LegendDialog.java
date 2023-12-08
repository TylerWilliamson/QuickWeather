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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ominous.quickweather.R;
import com.ominous.quickweather.pref.WeatherPreferences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class LegendDialog {
    private final AlertDialog legendDialog;

    private final static int[][] legendRainColors = new int[][]{
            new int[]{ //Original
                    0xFFDFDFDF,
                    0xFF9BEA8F,
                    0xFF47C278,
                    0xFF0C59FF,
                    0xFFFF93A3,
                    0xFFC20511
            },
            new int[]{ //Universal Blue
                    0xFF88DDEE,
                    0xFF0099CC,
                    0xFF005588,
                    0xFFFFAA00,
                    0xFFFF4400,
                    0xFF990000
            },
            new int[]{ //Titan
                    0xFF087FDB,
                    0xFF1C47E8,
                    0xFFC80F86,
                    0xFFD2883B,
                    0xFFFEFB02,
                    0xFFFE5F05
            },
            new int[]{ //The Weather Channel (TWC)
                    0xFF01B714,
                    0xFF088915,
                    0xFF064307,
                    0xFFF8BB08,
                    0xFFF07108,
                    0xFFDF370A
            },
            new int[]{ //Meteored
                    0xFF3FFEFC,
                    0xFF1790F9,
                    0xFF41FF50,
                    0xFF4B8339,
                    0xFFFDBB3C,
                    0xFFFD8788
            },
            new int[]{ //NEXRAD Level III
                    0xFF009CF7,
                    0xFF0000F7,
                    0xFF03B703,
                    0xFFFFFF00,
                    0xFFFE9300,
                    0xFFBD0000
            },
            new int[]{ //Rainbow @ Selex SI
                    0xFF009F9F,
                    0xFF008C4B,
                    0xFF21FD22,
                    0xFFFFD400,
                    0xFFFF6E00,
                    0xFFD00523
            },
            new int[]{ //Dark Sky
                    0x33005EB6,
                    0x55005EB6,
                    0xDD2458AF,
                    0xFFFC5370,
                    0xFFFFFD05,
                    0xFFFFFD05
            }
    };

    private final static int[][] legendSnowColors = new int[][]{
            new int[]{ //Original
                    0xFF8FFFFF,
                    0xFF5FCFFF,
                    0xFF0F6FFF
            },
            new int[]{ //Universal Blue
                    0xFF8FFFFF,//-5
                    0xFF5F9FFF,//25
                    0xFF0F4FFF //50
            },
            new int[]{ //Titan
                    0xFF8FFFFF,
                    0xFF5FCFFF,
                    0xFF0F6FFF
            },
            new int[]{ //The Weather Channel (TWC)
                    0xFFBFFFFF,
                    0xFF5FCFFF,
                    0xFF0F6FFF
            },
            new int[]{ //Meteored
                    0x00000000,
                    0xFF41FF50,
                    0xFFFD8788
            },
            new int[]{ //NEXRAD Level III
                    0xFFE5FEFE,//5-9
                    0xFF0F75FB,//25-29
                    0xFF0726A4//50-59
            },
            new int[]{ //Rainbow @ Selex SI
                    0xFFE5FEFE,
                    0xFF0F75FB,
                    0xFF0726A4
            },
            new int[]{ //Dark Sky
                    0xFFE5FEFE,
                    0xFF0F75FB,
                    0xFF0726A4
            }
    };

    public LegendDialog(Context context) {
        legendDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.dialog_legend_title)
                .setView(R.layout.dialog_legend)
                .create();

        legendDialog.setOnShowListener(d -> {
            int radarThemeOrdinal = WeatherPreferences.getInstance(context).getRadarTheme().ordinal();

            LegendAdapter rainAdapter = new LegendAdapter(
                    legendRainColors[radarThemeOrdinal],
                    new int[]{
                            R.string.openmeteo_overcast,
                            R.string.openmeteo_lightdrizzle,
                            R.string.openmeteo_lightrain,
                            R.string.openmeteo_moderaterain,
                            R.string.openmeteo_heavyrainshower,
                            R.string.openmeteo_thunderstormlighthail
                    });

            LegendAdapter snowAdapter = new LegendAdapter(
                    legendSnowColors[radarThemeOrdinal],
                    new int[]{
                            R.string.openmeteo_lightsnow,
                            R.string.openmeteo_moderatesnow,
                            R.string.openmeteo_heavysnow
                    });

            RecyclerView rainRecyclerView = legendDialog.findViewById(R.id.rain_recyclerview);
            RecyclerView snowRecyclerView = legendDialog.findViewById(R.id.snow_recyclerview);

            if (rainRecyclerView != null) {
                rainRecyclerView.setAdapter(rainAdapter);
                rainRecyclerView.setLayoutManager(new LinearLayoutManager(context));
            }

            if (snowRecyclerView != null) {
                snowRecyclerView.setAdapter(snowAdapter);
                snowRecyclerView.setLayoutManager(new LinearLayoutManager(context));
            }
        });
    }

    public void show() {
        legendDialog.show();
    }

    private static class LegendAdapter extends RecyclerView.Adapter<LegendViewHolder> {
        private final int[] weatherColors;
        private final int[] weatherNameRes;

        public LegendAdapter(int[] weatherColors, int[] weatherNameRes) {
            this.weatherColors = weatherColors;
            this.weatherNameRes = weatherNameRes;
        }

        @NonNull
        @Override
        public LegendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new LegendViewHolder(
                    LayoutInflater
                            .from(parent.getContext())
                            .inflate(R.layout.item_legend, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull LegendViewHolder holder, int position) {
            holder.weatherColor.setBackgroundColor(weatherColors[position]);
            holder.weatherText.setText(weatherNameRes[position]);
        }

        @Override
        public int getItemCount() {
            return weatherColors.length;
        }
    }

    private static class LegendViewHolder extends RecyclerView.ViewHolder {
        public final View weatherColor;
        public final TextView weatherText;

        public LegendViewHolder(@NonNull View itemView) {
            super(itemView);

            weatherColor = itemView.findViewById(R.id.weather_color);
            weatherText = itemView.findViewById(R.id.weather_text);
        }
    }
}