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
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.ominous.quickweather.R;
import com.ominous.quickweather.api.LibreTranslate;
import com.ominous.quickweather.pref.WeatherPreferences;
import com.ominous.tylerutils.async.Promise;
import com.ominous.tylerutils.util.ViewUtils;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

public class TranslationDialog {
    private final WeatherPreferences weatherPreferences;

    private final TextInputEditText apiKeyEditText;
    private final TextInputLayout apiKeyEditTextLayout;
    private final TextInputEditText instanceEditText;
    private final TextInputLayout instanceEditTextLayout;

    private final LinearProgressIndicator testApiProgressIndicator;

    private final TextView errorTextView;

    private String apiKey = null;
    private String instance = null;
    private ApiKeyState apiKeyState = ApiKeyState.NEUTRAL;

    private final AlertDialog dialog;

    public TranslationDialog(Context context) {
        View dialogLayout = LayoutInflater.from(context).inflate(R.layout.dialog_translation, null, false);

        weatherPreferences = WeatherPreferences.getInstance(context);

        apiKeyEditText = dialogLayout.findViewById(R.id.onboarding_libretranslate_apikey_edittext);
        apiKeyEditTextLayout = dialogLayout.findViewById(R.id.onboarding_libretranslate_apikey_layout);
        instanceEditText = dialogLayout.findViewById(R.id.onboarding_libretranslate_instance_edittext);
        instanceEditTextLayout = dialogLayout.findViewById(R.id.onboarding_libretranslate_instance_layout);

        MaterialButton testApiKeyButton = dialogLayout.findViewById(R.id.onboarding_test_libretranslate_apikey);
        testApiProgressIndicator = dialogLayout.findViewById(R.id.onboarding_test_libretranslate_apikey_progress);

        errorTextView = dialogLayout.findViewById(R.id.alert_error);

        testApiKeyButton.setOnClickListener(v1 -> {
            if (apiKeyState != ApiKeyState.PASS) {
                testApiProgressIndicator.show();

                apiKeyEditText.setEnabled(false);
                apiKeyEditText.clearFocus();

                instanceEditText.setEnabled(false);
                instanceEditText.clearFocus();

                Promise.create((a) -> {
                    testApiProgressIndicator.post(testApiProgressIndicator::show);

                    apiKey = ViewUtils.editTextToString(apiKeyEditText);
                    instance = ViewUtils.editTextToString(instanceEditText);

                    String translatedText = LibreTranslate.getInstance().translate(
                            instance,
                            apiKey,
                            "es",
                            "library")[0];

                    if (translatedText.equals("biblioteca")) {
                        testApiProgressIndicator.post(() -> {
                            testApiProgressIndicator.hide();
                            apiKeyEditText.setEnabled(true);
                            instanceEditText.setEnabled(true);
                            setApiKeyState(ApiKeyState.PASS);
                        });
                    } else {
                        testApiProgressIndicator.post(() -> {
                            testApiProgressIndicator.hide();
                            apiKeyEditText.setEnabled(true);
                            instanceEditText.setEnabled(true);
                            setApiKeyState(ApiKeyState.BAD_API_KEY);
                        });
                    }
                }, e -> testApiProgressIndicator.post(() -> {
                    testApiProgressIndicator.hide();
                    apiKeyEditText.setEnabled(true);
                    instanceEditText.setEnabled(true);
                    setApiKeyState(ApiKeyState.BAD_API_KEY);
                }));
            }
        });

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                setApiKeyState(ApiKeyState.NEUTRAL);
            }
        };

        apiKeyEditText.addTextChangedListener(textWatcher);
        instanceEditText.addTextChangedListener(textWatcher);

        dialog = new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.advanced_translation))
                .setView(dialogLayout)
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, null)
                .create();

        dialog.setOnShowListener(d -> {
            boolean shouldSetApiKeyStatePass = false;

            if (apiKey == null || instance == null) {
                apiKey = weatherPreferences.getLTApiKey();
                instance = weatherPreferences.getLTInstance();

                if (!apiKey.isEmpty() || !instance.isEmpty()) {
                    //TODO Test API Key, do not assume
                    shouldSetApiKeyStatePass = true;
                }
            }

            apiKeyEditText.setText(apiKey);
            instanceEditText.setText(instance);
            setApiKeyState(shouldSetApiKeyStatePass ? ApiKeyState.PASS : ApiKeyState.NEUTRAL);

            int textColor = ContextCompat.getColor(context, R.color.color_accent_text);

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(textColor);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(textColor);

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                if (apiKeyState == ApiKeyState.PASS) {
                    weatherPreferences.setLTApiKey(apiKey);
                    weatherPreferences.setLTInstance(instance);

                    dialog.cancel();
                } else {
                    errorTextView.setText(R.string.error_invalid_apikey);
                }
            });
        });
    }

    public void show() {
        dialog.show();
    }

    private void setApiKeyState(ApiKeyState apiKeyState) {
        this.apiKeyState = apiKeyState;

        String errorMessage = apiKeyState == ApiKeyState.BAD_API_KEY ?
                apiKeyEditTextLayout.getContext()
                        .getString(R.string.text_invalid_api_key_or_instance) : null;

        apiKeyEditTextLayout.setError(errorMessage);
        instanceEditTextLayout.setError(errorMessage);
        errorTextView.setText(null);

        updateEditTextColors(apiKeyState, apiKeyEditTextLayout, apiKeyEditText);
        updateEditTextColors(apiKeyState, instanceEditTextLayout, instanceEditText);
    }

    private enum ApiKeyState {
        NULL,
        NEUTRAL,
        PASS,
        BAD_API_KEY,
    }

    //TODO use same method in settings and other dialogs
    private void updateEditTextColors(ApiKeyState apiKeyState, TextInputLayout layout, TextInputEditText editText) {
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

        if (apiKeyState == ApiKeyState.PASS) {
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
}
