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

package com.ominous.quickweather.dialog;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

import com.google.android.material.textfield.TextInputLayout;
import com.ominous.quickweather.R;
import com.ominous.quickweather.data.WeatherDatabase;
import com.ominous.tylerutils.util.LocaleUtils;
import com.ominous.tylerutils.util.ViewUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

public class LocationManualDialog {

    private final AlertDialog editDialog;
    private OnLocationChosenListener onLocationChosenListener;

    public LocationManualDialog(Context context) {
        View editDialogLayout = LayoutInflater.from(context).inflate(R.layout.dialog_editlocation, null, false);

        final EditText editDialogLocationName = editDialogLayout.findViewById(R.id.editlocation_location);
        final EditText editDialogLocationLatitude = editDialogLayout.findViewById(R.id.editlocation_latitude);
        final EditText editDialogLocationLongitude = editDialogLayout.findViewById(R.id.editlocation_longitude);

        final TextInputLayout editDialogLocationNameLayout = editDialogLayout.findViewById(R.id.editlocation_location_layout);
        final TextInputLayout editDialogLocationLatitudeLayout = editDialogLayout.findViewById(R.id.editlocation_latitude_layout);
        final TextInputLayout editDialogLocationLongitudeLayout = editDialogLayout.findViewById(R.id.editlocation_longitude_layout);

        ViewUtils.setEditTextCursorColor(editDialogLocationName, ContextCompat.getColor(context, R.color.color_accent_text));
        ViewUtils.setEditTextCursorColor(editDialogLocationLatitude, ContextCompat.getColor(context, R.color.color_accent_text));
        ViewUtils.setEditTextCursorColor(editDialogLocationLongitude, ContextCompat.getColor(context, R.color.color_accent_text));

        TextWatcher editDialogTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                for (TextInputLayout inputLayout : new TextInputLayout[]{editDialogLocationNameLayout, editDialogLocationLatitudeLayout, editDialogLocationLongitudeLayout}) {
                    int len = inputLayout.getEditText() == null || inputLayout.getEditText().getText() == null ? 0 : inputLayout.getEditText().getText().length();

                    if (len > 0 && inputLayout.getError() != null) {
                        inputLayout.setError(null);
                    }
                }
            }
        };

        editDialogLocationName.addTextChangedListener(editDialogTextWatcher);
        editDialogLocationLatitude.addTextChangedListener(editDialogTextWatcher);
        editDialogLocationLongitude.addTextChangedListener(editDialogTextWatcher);

        editDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.dialog_edit_location_title)
                .setView(editDialogLayout)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, null)
                .setNeutralButton(android.R.string.search_go, (dialogInterface, which) -> new LocationSearchDialog(context).show(onLocationChosenListener))
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        editDialog.setOnShowListener(d -> {
            int buttonTextColor = ContextCompat.getColor(context, R.color.color_accent_text);

            editDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener((v) -> {
                int emptyInputs = 0;

                String editDialogName = ViewUtils.editTextToString(editDialogLocationName);
                if (editDialogName.equals("")) {
                    editDialogLocationNameLayout.setError(context.getString(R.string.text_required));
                    emptyInputs++;
                }

                String editDialogLat = ViewUtils.editTextToString(editDialogLocationLatitude);
                if (editDialogLat.equals("")) {
                    editDialogLocationLatitudeLayout.setError(context.getString(R.string.text_required));
                    emptyInputs++;
                }

                String editDialogLon = ViewUtils.editTextToString(editDialogLocationLongitude);
                if (editDialogLon.equals("")) {
                    editDialogLocationLongitudeLayout.setError(context.getString(R.string.text_required));
                    emptyInputs++;
                }

                editDialogLocationName.clearFocus();
                editDialogLocationLatitude.clearFocus();
                editDialogLocationLongitude.clearFocus();

                if (emptyInputs == 0 && onLocationChosenListener != null) {
                    onLocationChosenListener.onLocationChosen(
                            editDialogName,
                            BigDecimal.valueOf(LocaleUtils.parseDouble(Locale.getDefault(), editDialogLat)).setScale(3, RoundingMode.HALF_UP).doubleValue(),
                            BigDecimal.valueOf(LocaleUtils.parseDouble(Locale.getDefault(), editDialogLon)).setScale(3, RoundingMode.HALF_UP).doubleValue());
                    editDialog.dismiss();
                }
            });

            editDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(buttonTextColor);
            editDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(buttonTextColor);
            editDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(buttonTextColor);

            editDialogLocationName.requestFocus();
        });
    }

    public void show(WeatherDatabase.WeatherLocation weatherLocation, OnLocationChosenListener onLocationChosenListener) {
        this.onLocationChosenListener = onLocationChosenListener;

        editDialog.show();

        EditText editDialogLocationName = editDialog.findViewById(R.id.editlocation_location);
        EditText editDialogLocationLatitude = editDialog.findViewById(R.id.editlocation_latitude);
        EditText editDialogLocationLongitude = editDialog.findViewById(R.id.editlocation_longitude);

        if (editDialogLocationName != null && editDialogLocationLatitude != null && editDialogLocationLongitude != null) {
            editDialogLocationLatitude.setText(weatherLocation == null ? "" : LocaleUtils.getDecimalString(Locale.getDefault(), weatherLocation.latitude, 3));
            editDialogLocationLongitude.setText(weatherLocation == null ? "" : LocaleUtils.getDecimalString(Locale.getDefault(), weatherLocation.longitude, 3));
            editDialogLocationName.setText(weatherLocation == null ? "" : weatherLocation.name);
        }

        Window dialogWindow = editDialog.getWindow();

        if (dialogWindow != null) {
            dialogWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
    }
}