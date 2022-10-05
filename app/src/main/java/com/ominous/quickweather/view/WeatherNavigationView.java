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
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.navigation.NavigationView;
import com.ominous.quickweather.R;
import com.ominous.tylerutils.util.ViewUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatCheckedTextView;
import androidx.recyclerview.widget.RecyclerView;

public class WeatherNavigationView extends NavigationView {
    public WeatherNavigationView(@NonNull Context context) {
        this(context, null, 0);
    }

    public WeatherNavigationView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WeatherNavigationView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        addAccessibilityLabelToMenuItems();
    }

    //TODO move to MainActivity, delete entire class
    private void addAccessibilityLabelToMenuItems() {
        ((RecyclerView) this.getChildAt(0)).addItemDecoration(new RecyclerView.ItemDecoration() {
            final String SETTINGS = getContext().getString(R.string.text_settings);
            final String CHECK_FOR_UPDATES = getContext().getString(R.string.text_check_for_updates);
            final String WHATS_NEW = getContext().getString(R.string.text_whats_new);
            final String REPORT_A_BUG = getContext().getString(R.string.text_report_a_bug);

            //Hackermans
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                super.getItemOffsets(outRect, view, parent, state);

                if (view instanceof ViewGroup) {
                    View childView = ((ViewGroup) view).getChildAt(0);

                    if (childView instanceof AppCompatCheckedTextView) {
                        String textViewText = ((AppCompatCheckedTextView) childView).getText().toString();
                        String clickLabel;

                        if (SETTINGS.equals(textViewText)) {
                            clickLabel = getContext().getString(R.string.format_label_open, SETTINGS);
                        } else if (CHECK_FOR_UPDATES.equals(textViewText)) {
                            clickLabel = CHECK_FOR_UPDATES;
                        } else if (WHATS_NEW.equals(textViewText)) {
                            clickLabel = getContext().getString(R.string.format_label_open, WHATS_NEW);
                        } else if (REPORT_A_BUG.equals(textViewText)) {
                            clickLabel = REPORT_A_BUG;
                        } else {
                            clickLabel = getContext().getString(R.string.label_location_choose_action);
                        }

                        ViewUtils.setAccessibilityInfo(view, clickLabel, null);
                    }
                }
            }
        });
    }
}
