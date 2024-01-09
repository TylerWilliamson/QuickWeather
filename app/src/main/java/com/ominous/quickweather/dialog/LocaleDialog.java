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
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ominous.quickweather.R;
import com.ominous.tylerutils.util.ApiUtils;

import java.util.ArrayList;
import java.util.Locale;

public class LocaleDialog {
    private final AlertDialog languageDialog;
    private OnLocaleChosenListener onLocaleChosenListener;

    public LocaleDialog(Context context, Locale currentLocale) {
        LocaleAdapter localeAdapter = new LocaleAdapter(currentLocale);

        languageDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.dialog_locale_title)
                .setView(R.layout.dialog_choice)
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (d, w) ->
                        onLocaleChosenListener.onLocaleChosen(localeAdapter.getSelectedLocale()))
                .create();

        languageDialog.setOnShowListener(d -> {
            RecyclerView localeRecyclerView = languageDialog.findViewById(R.id.choice_recyclerview);

            if (localeRecyclerView != null) {
                localeRecyclerView.setAdapter(localeAdapter);
                localeRecyclerView.setLayoutManager(new LinearLayoutManager(context));
            }

            Window window = languageDialog.getWindow();

            if (window != null) {
                window.setLayout(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        context.getResources().getDimensionPixelSize(R.dimen.mappicker_height));
            }

            int textColor = ContextCompat.getColor(context, R.color.color_accent_text);

            languageDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(textColor);
            languageDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(textColor);
        });
    }

    public void show(OnLocaleChosenListener onLocaleChosenListener) {
        this.onLocaleChosenListener = onLocaleChosenListener;
        languageDialog.show();
    }

    private static class LocaleAdapter extends RecyclerView.Adapter<LocaleViewHolder> {
        private final Locale[] locales;
        private int selectedPosition;

        public LocaleAdapter(Locale selectedLocale) {
            selectedPosition = selectedLocale == null ? 0 : RecyclerView.NO_POSITION;

            Locale[] initialLocales = Locale.getAvailableLocales();
            ArrayList<Locale> localeList = new ArrayList<>(initialLocales.length);

            localeList.add(null);

            for (Locale l : initialLocales) {
                if (!l.getCountry().equals("")) {
                    localeList.add(l);

                    if (l.equals(selectedLocale)) {
                        selectedPosition = localeList.size() - 1;
                    }
                }
            }

            this.locales = localeList.toArray(new Locale[0]);
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
    }

    private static class LocaleViewHolder extends RecyclerView.ViewHolder {
        public LocaleViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public interface OnLocaleChosenListener {
        void onLocaleChosen(Locale locale);
    }
}