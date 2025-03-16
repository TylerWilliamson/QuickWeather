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
import com.ominous.tylerutils.util.ApiUtils;

import java.util.ArrayList;
import java.util.Locale;

public class LocaleDialogView extends FrameLayout {
    private final LocaleAdapter localeAdapter;

    public LocaleDialogView(@NonNull Context context) {
        this(context, null, 0, 0);
    }

    public LocaleDialogView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public LocaleDialogView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public LocaleDialogView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        LayoutInflater.from(context).inflate(R.layout.dialog_choice, this, true);

        RecyclerView localeRecyclerView = findViewById(R.id.choice_recyclerview);

        localeAdapter = new LocaleAdapter();
        localeRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        localeRecyclerView.setAdapter(localeAdapter);
    }

    public void updateData(Locale[] locales, Locale selectedLocale) {
        localeAdapter.setLocales(locales, selectedLocale);
    }

    public Locale getSelectedLocale() {
        return localeAdapter.getSelectedLocale();
    }

    private static class LocaleAdapter extends RecyclerView.Adapter<LocaleViewHolder> {
        private Locale[] locales = new Locale[]{};
        private int selectedPosition = RecyclerView.NO_POSITION;

        public LocaleAdapter() {
        }

        @NonNull
        @Override
        public LocaleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LocaleViewHolder viewHolder = new LocaleViewHolder(
                    LayoutInflater
                            .from(parent.getContext())
                            .inflate(R.layout.item_choice_dialog, parent, false));

            boolean isRTL = viewType == Character.DIRECTIONALITY_RIGHT_TO_LEFT ||
                    viewType == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC;

            viewHolder.itemView
                    .setLayoutDirection(isRTL ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);

            viewHolder.itemView.setOnClickListener(v -> {
                setSelected(viewHolder.getLayoutPosition());
                v.setSelected(true);
            });

            return viewHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull LocaleViewHolder holder, int position) {
            Locale locale = locales[position];

            TextView localeTextView = holder.itemView.findViewById(R.id.textview_locale);

            localeTextView.setText(locale == null ?
                    ApiUtils.getStringResourceFromApplication(
                            localeTextView.getContext().getPackageManager(),
                            "com.android.settings",
                            "preference_of_system_locale_summary",
                            "System default") :
                    locale.getDisplayName(locale));

            holder.itemView.setSelected(selectedPosition == position);
        }

        @Override
        public int getItemCount() {
            return locales.length;
        }

        @Override
        public int getItemViewType(int position) {
            Locale l = locales[position];

            return l == null ?
                    Character.DIRECTIONALITY_LEFT_TO_RIGHT :
                    Character.getDirectionality(l.getDisplayName(l).charAt(0));
        }

        private void setSelected(int position) {
            int prevPosition = selectedPosition;
            selectedPosition = position;

            if (prevPosition != selectedPosition) {
                notifyItemChanged(prevPosition);
            }
        }

        public Locale getSelectedLocale() {
            return locales[selectedPosition];
        }

        public void setLocales(Locale[] locales, Locale selectedLocale) {
            ArrayList<Locale> localeList = new ArrayList<>(locales.length);

            localeList.add(null);

            for (Locale l : locales) {
                if (!l.getCountry().isEmpty()) {
                    localeList.add(l);

                    if (l.equals(selectedLocale)) {
                        selectedPosition = localeList.size() - 1;
                    }
                }
            }

            if (selectedLocale == null) {
                selectedPosition = 0;
            }

            this.locales = localeList.toArray(new Locale[0]);

            //TODO smarter adapter dataset updates
            notifyDataSetChanged();
        }
    }

    private static class LocaleViewHolder extends RecyclerView.ViewHolder {
        public LocaleViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

}