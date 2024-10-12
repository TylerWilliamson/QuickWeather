package com.ominous.quickweather.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.ominous.quickweather.R;
import com.ominous.quickweather.api.LibreTranslate;
import com.ominous.quickweather.data.CurrentWeather;
import com.ominous.quickweather.pref.WeatherPreferences;
import com.ominous.tylerutils.async.Promise;
import com.ominous.tylerutils.http.HttpException;
import com.ominous.tylerutils.util.LocaleUtils;
import com.ominous.tylerutils.util.StringUtils;
import com.ominous.tylerutils.view.LinkedTextView;

import org.json.JSONException;

import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Pattern;

//TODO base class
//TODO use OnClickListener on buttons to not immediately close if there is an error
public class TranslatableAlertDialog {
    private final static Pattern httpPattern = Pattern.compile("(https?://)?(([\\w\\-])+\\.([a-zA-Z]{2,63})([/\\w-]*)*/?\\??([^ #\\n\\r<\"]*)?#?([^ \\n\\r\"<]*)[^.,;<\"])");
    private final static Pattern usTelPattern = Pattern.compile("(tel://)?((\\+?1[ \\-])?\\(?[0-9]{3}\\)?[-. ][0-9]{3}[-. ]?[0-9]{4})");

    private final AlertDialog alertDialog;

    private LinkedTextView alertTextView;
    private LinearProgressIndicator alertTranslationProgress;
    private TextView alertError;

    private final LibreTranslate libreTranslate = LibreTranslate.getInstance();
    private final WeatherPreferences weatherPreferences;

    public TranslatableAlertDialog(Context context) {
        weatherPreferences = WeatherPreferences.getInstance(context);

        alertDialog = new AlertDialog.Builder(context)
                .setView(R.layout.dialog_alert)
                .setCancelable(true)
                .setNegativeButton(R.string.dialog_button_close, null)
                .setNeutralButton(R.string.dialog_button_translate, null)
                .create();
    }

    public void show(CurrentWeather.Alert alert) {
        alertDialog.setOnShowListener(d -> {
            alertTextView = alertDialog.findViewById(R.id.alert_text);
            alertTranslationProgress = alertDialog.findViewById(R.id.alert_translation_progress);
            alertError = alertDialog.findViewById(R.id.alert_error);

            updateAlert(alert.senderName, alert.event, alert.description);

            if (weatherPreferences.canTranslateAlerts()) {
                alertDialog.getButton(Dialog.BUTTON_NEUTRAL).setVisibility(View.VISIBLE);
                alertDialog.getButton(Dialog.BUTTON_NEUTRAL).setOnClickListener(v ->
                        Promise.create(a -> {
                            Handler uiHandler = new Handler(Looper.getMainLooper());

                            String instance = weatherPreferences.getLTInstance();
                            String apiKey = weatherPreferences.getLTApiKey();

                            try {
                                uiHandler.post(() -> alertTranslationProgress.setVisibility(View.VISIBLE));

                                String targetLanguage = LocaleUtils.getDefaultLocale(
                                                alertTextView
                                                        .getResources()
                                                        .getConfiguration())
                                        .getLanguage().split("-", 1)[0];

                                if (targetLanguage.length() == 2) {
                                    String detectedLanguage = libreTranslate.detect(instance, apiKey, alert.description);
                                    String[] supportedLanguages = libreTranslate.getSupportedLanguages(instance, detectedLanguage);

                                    if (!targetLanguage.equals(detectedLanguage)) {
                                        if (Arrays.asList(supportedLanguages).contains(targetLanguage)) {
                                            String[] output = libreTranslate.translate(
                                                    instance,
                                                    apiKey,
                                                    targetLanguage,
                                                    alert.event,
                                                    alert.description);

                                            uiHandler.post(() -> updateAlert(alert.senderName, output[0], output[1]));
                                        } else {
                                            setError("Unsupported Translation: " + detectedLanguage + " to " + targetLanguage);
                                        }
                                    }
                                } else {
                                    setError("Unsupported Language: " + targetLanguage);
                                }
                            } catch (HttpException e) {
                                setError("Server returned error: " + e.getMessage());
                            } catch (IOException e) {
                                setError("Could not connect to server: " + e.getMessage());
                            } catch (JSONException e) {
                                setError("Server returned malformed data: " + e.getMessage());
                            } catch (Exception e) {
                                setError("Unknown Error");
                            } finally {
                                uiHandler.post(() -> {
                                    alertTranslationProgress.setVisibility(View.GONE);
                                    alertDialog.getButton(Dialog.BUTTON_NEUTRAL).setOnClickListener(null);
                                });
                            }
                        }));
            } else {
                alertDialog.getButton(Dialog.BUTTON_NEUTRAL).setVisibility(View.GONE);
            }

            int textColor = ContextCompat.getColor(alertTextView.getContext(), R.color.color_accent_text);

            for (Button button : new Button[]{
                    alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE),
                    alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL),
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            }) {
                button.setTextColor(textColor);
            }
        });
        alertDialog.setTitle(alert.event);
        alertDialog.show();
    }

    private void updateAlert(String senderName, String title, String content) {
        alertDialog.setTitle(title);
        alertTextView.setText(StringUtils.fromHtml(
                StringUtils.linkify(StringUtils.linkify(
                                content
                                        .replaceAll("\\n\\*", "<br>*")
                                        .replaceAll("\\n\\.", "<br>.")
                                        .replaceAll("\\n", " ") +
                                        (senderName != null && !senderName.isEmpty() ? "<br>Via " + senderName : ""),
                                httpPattern, "https"),
                        usTelPattern, "tel")));

    }

    //TODO resId + string error message
    private void setError(String error) {
        alertError.setText(error);
    }
}
