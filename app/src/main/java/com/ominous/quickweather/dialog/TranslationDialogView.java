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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.ominous.quickweather.R;
import com.ominous.quickweather.api.ApiKeyState;
import com.ominous.quickweather.util.EditTextUtils;
import com.ominous.tylerutils.async.Promise;
import com.ominous.tylerutils.util.ViewUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

public class TranslationDialogView extends FrameLayout {
    private final TextInputEditText apiKeyEditText;
    private final TextInputLayout apiKeyEditTextLayout;
    private final TextInputEditText instanceEditText;
    private final TextInputLayout instanceEditTextLayout;
    private final LinearProgressIndicator testApiProgressIndicator;
    private final TextView errorTextView;

    private TranslationApiKeyTester translationApiKeyTester;

    private ApiKeyState apiKeyState = ApiKeyState.NEUTRAL;

    public TranslationDialogView(@NonNull Context context) {
        this(context, null, 0, 0);
    }

    public TranslationDialogView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public TranslationDialogView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TranslationDialogView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        LayoutInflater.from(context).inflate(R.layout.dialog_translation, this, true);

        apiKeyEditText = findViewById(R.id.onboarding_libretranslate_apikey_edittext);
        apiKeyEditTextLayout = findViewById(R.id.onboarding_libretranslate_apikey_layout);
        instanceEditText = findViewById(R.id.onboarding_libretranslate_instance_edittext);
        instanceEditTextLayout = findViewById(R.id.onboarding_libretranslate_instance_layout);

        MaterialButton testApiKeyButton = findViewById(R.id.onboarding_test_libretranslate_apikey);
        testApiProgressIndicator = findViewById(R.id.onboarding_test_libretranslate_apikey_progress);

        errorTextView = findViewById(R.id.alert_error);

        int cursorColor = ContextCompat.getColor(context, R.color.color_accent_text);
        ViewUtils.setEditTextCursorColor(apiKeyEditText, cursorColor);
        ViewUtils.setEditTextCursorColor(instanceEditText, cursorColor);

        testApiKeyButton.setOnClickListener(v1 -> {
            if (apiKeyState != ApiKeyState.PASS) {
                testApiProgressIndicator.show();

                apiKeyEditText.setEnabled(false);
                apiKeyEditText.clearFocus();

                instanceEditText.setEnabled(false);
                instanceEditText.clearFocus();

                Promise.create((a) -> {
                    testApiProgressIndicator.post(testApiProgressIndicator::show);

                    ApiKeyState translationResult = translationApiKeyTester.testApiKey(
                            ViewUtils.editTextToString(apiKeyEditText),
                            ViewUtils.editTextToString(instanceEditText));

                    testApiProgressIndicator.post(() -> {
                        testApiProgressIndicator.hide();
                        apiKeyEditText.setEnabled(true);
                        instanceEditText.setEnabled(true);
                        setApiKeyState(translationResult);
                    });

                    return translationResult;
                });
            }
        });

        TextWatcher textWatcher = new EditTextUtils.SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                setApiKeyState(ApiKeyState.NEUTRAL);
            }
        };

        apiKeyEditText.addTextChangedListener(textWatcher);
        instanceEditText.addTextChangedListener(textWatcher);
    }

    public void setParams(String apiKey,
                          String instance,
                          TranslationApiKeyTester translationApiKeyTester) {
        apiKeyEditText.setText(apiKey);
        instanceEditText.setText(instance);
        setApiKeyState(!(apiKey == null || instance == null) && (!apiKey.isEmpty() || !instance.isEmpty()) ?
                ApiKeyState.PASS : ApiKeyState.NEUTRAL);

        this.translationApiKeyTester = translationApiKeyTester;
    }

    private void setApiKeyState(ApiKeyState apiKeyState) {
        this.apiKeyState = apiKeyState;

        String badApiKeyErrorMessage = apiKeyEditTextLayout.getContext().getString(R.string.text_invalid_api_key_or_instance);
        String networkErrorErrorMessage = "Network Error"; //TODO error message

        switch (apiKeyState) {
            case PASS:
                errorTextView.setText(null);

                EditTextUtils.updateEditTextColors(apiKeyEditTextLayout, apiKeyEditText, true, null);
                EditTextUtils.updateEditTextColors(instanceEditTextLayout, instanceEditText, true, null);
                break;
            case NEUTRAL:
                errorTextView.setText(null);

                EditTextUtils.updateEditTextColors(apiKeyEditTextLayout, apiKeyEditText, false, null);
                EditTextUtils.updateEditTextColors(instanceEditTextLayout, instanceEditText, false, null);
                break;
            case BAD_API_KEY:
                errorTextView.setText(null);

                EditTextUtils.updateEditTextColors(apiKeyEditTextLayout, apiKeyEditText, false, badApiKeyErrorMessage);
                EditTextUtils.updateEditTextColors(instanceEditTextLayout, instanceEditText, false, badApiKeyErrorMessage);
                break;
            case NETWORK_ERROR:
                errorTextView.setText(networkErrorErrorMessage);

                EditTextUtils.updateEditTextColors(apiKeyEditTextLayout, apiKeyEditText, false, null);
                EditTextUtils.updateEditTextColors(instanceEditTextLayout, instanceEditText, false, null);
                break;
        }
    }

    public ApiKeyState getApiKeyState() {
        return apiKeyState;
    }

    public String getApiKey() {
        return ViewUtils.editTextToString(apiKeyEditText);
    }

    public String getInstance() {
        return ViewUtils.editTextToString(instanceEditText);
    }

    //TODO resId
    public void setErrorMessage(String errorMessage) {
        errorTextView.setText(errorMessage);
    }

}
