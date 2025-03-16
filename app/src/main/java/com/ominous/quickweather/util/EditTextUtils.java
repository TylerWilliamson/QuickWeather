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

package com.ominous.quickweather.util;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.text.TextWatcher;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.ominous.quickweather.R;
import com.ominous.tylerutils.util.ViewUtils;

//TODO move to TylerUtils ViewUtils
public class EditTextUtils {
    public static void updateEditTextColors(TextInputLayout layout,
                                      TextInputEditText editText,
                                      boolean isPass,
                                      String errorMessage) {
        Resources r = editText.getContext().getResources();
        int greenTextColor = r.getColor(R.color.color_green);
        int primaryTextColor = r.getColor(R.color.text_primary_emphasis);

        ColorStateList greenColorStateList = new ColorStateList(
                new int[][]{
                        new int[]{-android.R.attr.state_focused},
                        new int[]{android.R.attr.state_focused}
                },
                new int[]{
                        greenTextColor,
                        greenTextColor
                }
        );

        ColorStateList defaultColorStateList = new ColorStateList(
                new int[][]{
                        new int[]{-android.R.attr.state_focused},
                        new int[]{android.R.attr.state_focused}
                },
                new int[]{
                        primaryTextColor,
                        primaryTextColor
                }
        );

        layout.setError(errorMessage);

        if (isPass) {
            ViewUtils.setDrawable(editText, R.drawable.ic_done_white_24dp, greenTextColor, ViewUtils.FLAG_END);

            layout.setBoxStrokeColorStateList(greenColorStateList);
            layout.setHintTextColor(greenColorStateList);
            layout.setDefaultHintTextColor(greenColorStateList);
        } else {
            editText.setCompoundDrawables(null, null, null, null);

            layout.setBoxStrokeColorStateList(defaultColorStateList);
            layout.setHintTextColor(defaultColorStateList);
            layout.setDefaultHintTextColor(defaultColorStateList);
        }
    }

    public abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }
}
