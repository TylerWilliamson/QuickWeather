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
import android.text.Editable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.ominous.quickweather.R;
import com.ominous.quickweather.data.WeatherDatabase;
import com.ominous.quickweather.util.EditTextUtils;
import com.ominous.tylerutils.util.LocaleUtils;
import com.ominous.tylerutils.util.ViewUtils;

import java.util.Locale;

public class LocationEditDialogView extends FrameLayout {
    private final TextInputEditText nameEditText;
    private final TextInputLayout nameLayout;
    private final TextInputEditText latitudeEditText;
    private final TextInputLayout latitudeLayout;
    private final TextInputEditText longitudeEditText;
    private final TextInputLayout longitudeLayout;

    public LocationEditDialogView(@NonNull Context context) {
        this(context, null, 0, 0);
    }

    public LocationEditDialogView(@NonNull Context context, @Nullable @org.jetbrains.annotations.Nullable AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public LocationEditDialogView(@NonNull Context context, @Nullable @org.jetbrains.annotations.Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public LocationEditDialogView(@NonNull Context context, @Nullable @org.jetbrains.annotations.Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        LayoutInflater.from(context).inflate(R.layout.dialog_editlocation, this, true);

        nameEditText = findViewById(R.id.editlocation_location);
        nameLayout = findViewById(R.id.editlocation_location_layout);
        latitudeEditText = findViewById(R.id.editlocation_latitude);
        latitudeLayout = findViewById(R.id.editlocation_latitude_layout);
        longitudeEditText = findViewById(R.id.editlocation_longitude);
        longitudeLayout = findViewById(R.id.editlocation_longitude_layout);

        int cursorColor = ContextCompat.getColor(context, R.color.color_accent_text);
        ViewUtils.setEditTextCursorColor(nameEditText, cursorColor);
        ViewUtils.setEditTextCursorColor(latitudeEditText, cursorColor);
        ViewUtils.setEditTextCursorColor(longitudeEditText, cursorColor);

        nameEditText.addTextChangedListener(new EditTextUtils.SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                EditTextUtils.updateEditTextColors(
                        nameLayout,
                        nameEditText,
                        false,
                        null);
            }
        });

        latitudeEditText.addTextChangedListener(new EditTextUtils.SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                EditTextUtils.updateEditTextColors(
                        latitudeLayout,
                        latitudeEditText,
                        false,
                        null);
            }
        });

        longitudeEditText.addTextChangedListener(new EditTextUtils.SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                EditTextUtils.updateEditTextColors(
                        longitudeLayout,
                        longitudeEditText,
                        false,
                        null);
            }
        });
    }

    public void setWeatherLocation(WeatherDatabase.WeatherLocation weatherLocation) {
        latitudeEditText.setText(weatherLocation == null ? "" :
                LocaleUtils.getDecimalString(Locale.getDefault(), weatherLocation.latitude, 3));
        longitudeEditText.setText(weatherLocation == null ? "" :
                LocaleUtils.getDecimalString(Locale.getDefault(), weatherLocation.longitude, 3));
        nameEditText.setText(weatherLocation == null ? "" : weatherLocation.name);

        nameEditText.requestFocus();
    }

    public String getName() {
        return ViewUtils.editTextToString(nameEditText);
    }

    public String getLatitude() {
        return ViewUtils.editTextToString(latitudeEditText);
    }

    public String getLongitude() {
        return ViewUtils.editTextToString(longitudeEditText);
    }

    public void setNameError(String errorMessage) {
        EditTextUtils.updateEditTextColors(
                nameLayout,
                nameEditText,
                false,
                errorMessage
        );

        nameEditText.clearFocus();
    }

    public void setLatitudeError(String errorMessage) {
        EditTextUtils.updateEditTextColors(
                latitudeLayout,
                latitudeEditText,
                false,
                errorMessage
        );

        latitudeEditText.clearFocus();
    }

    public void setLongitudeError(String errorMessage) {
        EditTextUtils.updateEditTextColors(
                longitudeLayout,
                longitudeEditText,
                false,
                errorMessage
        );

        longitudeEditText.clearFocus();
    }
}