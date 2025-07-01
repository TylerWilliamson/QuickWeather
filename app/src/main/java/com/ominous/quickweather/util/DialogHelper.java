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

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.ominous.quickweather.R;
import com.ominous.quickweather.activity.ILifecycleAwareActivity;
import com.ominous.quickweather.api.LibreTranslate;
import com.ominous.quickweather.data.CurrentWeather;
import com.ominous.quickweather.data.WeatherDatabase;
import com.ominous.quickweather.dialog.ChoiceDialogView;
import com.ominous.quickweather.dialog.LayoutDialogView;
import com.ominous.quickweather.dialog.LegendDialogView;
import com.ominous.quickweather.dialog.LocationEditDialogView;
import com.ominous.quickweather.dialog.LocationMapDialogView;
import com.ominous.quickweather.dialog.LocationSearchDialogView;
import com.ominous.quickweather.dialog.OnItemChosenListener;
import com.ominous.quickweather.dialog.OnLayoutChangedListener;
import com.ominous.quickweather.dialog.OnLocationChosenListener;
import com.ominous.quickweather.dialog.OnTranslationApiKeyChangedListener;
import com.ominous.quickweather.dialog.TranslatableAlertDialogView;
import com.ominous.quickweather.dialog.TranslationApiKeyTester;
import com.ominous.quickweather.dialog.TranslationDialogView;
import com.ominous.quickweather.pref.RadarTheme;
import com.ominous.quickweather.pref.WeatherPreferences;
import com.ominous.tylerutils.async.Promise;
import com.ominous.tylerutils.browser.CustomTabs;
import com.ominous.tylerutils.http.HttpException;
import com.ominous.tylerutils.util.ApiUtils;
import com.ominous.tylerutils.util.LocaleUtils;
import com.ominous.tylerutils.util.StringUtils;
import com.ominous.tylerutils.view.LinkedTextView;

import org.json.JSONException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class DialogHelper {
    private final WeatherPreferences weatherPreferences;
    private final Context context;

    private AlertDialog legendDialog;
    private LegendDialogView legendDialogView;

    private AlertDialog radarThemeDialog;
    private ChoiceDialogView<RadarTheme> radarThemeDialogView;
    private OnItemChosenListener<RadarTheme> onRadarThemeChosenListener;

    private AlertDialog layoutDialog;
    private LayoutDialogView layoutDialogView;
    private OnLayoutChangedListener onLayoutChangedListener;

    private AlertDialog localeDialog;
    private ChoiceDialogView<Locale> localeDialogView;
    private OnItemChosenListener<Locale> onLocaleChosenListener;

    private AlertDialog translationDialog;
    private TranslationDialogView translationDialogView;
    private OnTranslationApiKeyChangedListener onTranslationApiKeyChangedListener;

    private AlertDialog locationMapDialog;
    private LocationMapDialogView locationMapDialogView;
    private AlertDialog locationSearchDialog;
    private LocationSearchDialogView locationSearchDialogView;
    private AlertDialog locationEditDialog;
    private LocationEditDialogView locationEditDialogView;
    private OnLocationChosenListener onLocationChosenListener;

    private AlertDialog translatableAlertDialog;
    private TranslatableAlertDialogView translatableAlertDialogView;
    private CurrentWeather.Alert alert;

    private AlertDialog textDialog;
    private LinkedTextView textDialogView;

    private AlertDialog newVersionDialog;
    private LinkedTextView newVersionDialogView;

    public DialogHelper(Context context) {
        weatherPreferences = WeatherPreferences.getInstance(context);

        this.context = context;
    }

    public void showAlert(CurrentWeather.Alert alert) {
        if (translatableAlertDialog == null) {
            translatableAlertDialogView = new TranslatableAlertDialogView(context);

            translatableAlertDialog = new MaterialAlertDialogBuilder(context, R.style.QuickWeather_Dialog)
                    .setView(translatableAlertDialogView)
                    .setCancelable(true)
                    .setNegativeButton(R.string.dialog_button_close, null)
                    .setNeutralButton(R.string.dialog_button_translate, null)
                    .create();

            translatableAlertDialog.setOnShowListener(d -> {
                translatableAlertDialog.getButton(Dialog.BUTTON_NEUTRAL).setOnClickListener(v ->
                        Promise.create(a -> {
                            LibreTranslate libreTranslate = LibreTranslate.getInstance();
                            Handler uiHandler = new Handler(Looper.getMainLooper());

                            String instance = weatherPreferences.getLTInstance();
                            String apiKey = weatherPreferences.getLTApiKey();

                            try {
                                uiHandler.post(() ->
                                        translatableAlertDialogView.setTranslationProgressVisibility(true));

                                String targetLanguage = LocaleUtils.getDefaultLocale(
                                                context
                                                        .getResources()
                                                        .getConfiguration())
                                        .getLanguage().split("-", 1)[0];

                                if (targetLanguage.length() == 2) {
                                    String detectedLanguage = libreTranslate.detect(instance, apiKey, DialogHelper.this.alert.description);
                                    String[] supportedLanguages = libreTranslate.getSupportedLanguages(instance, detectedLanguage);

                                    if (!targetLanguage.equals(detectedLanguage)) {
                                        if (Arrays.asList(supportedLanguages).contains(targetLanguage)) {
                                            String[] output = libreTranslate.translate(
                                                    instance,
                                                    apiKey,
                                                    targetLanguage,
                                                    DialogHelper.this.alert.event,
                                                    DialogHelper.this.alert.description);

                                            uiHandler.post(() -> {
                                                translatableAlertDialog.setTitle(output[0]);
                                                translatableAlertDialogView.setAlertText(DialogHelper.this.alert.senderName, output[1]);
                                            });
                                        } else {
                                            translatableAlertDialogView.setError(context.getString(R.string.error_translation_unsupported, detectedLanguage, targetLanguage));
                                        }
                                    }
                                } else {
                                    translatableAlertDialogView.setError(context.getString(R.string.error_translation_language_unsupported, targetLanguage));
                                }
                            } catch (HttpException e) {
                                translatableAlertDialogView.setError(context.getString(R.string.error_server_result, e.getMessage()));
                            } catch (IOException e) {
                                translatableAlertDialogView.setError(context.getString(R.string.error_server_connection, e.getMessage()));
                            } catch (JSONException e) {
                                translatableAlertDialogView.setError(context.getString(R.string.error_malformed_data, e.getMessage()));
                            } catch (Exception e) {
                                translatableAlertDialogView.setError(context.getString(R.string.error_unknown));
                            } finally {
                                uiHandler.post(() -> {
                                    translatableAlertDialogView.setTranslationProgressVisibility(false);
                                    translatableAlertDialog.getButton(Dialog.BUTTON_NEUTRAL).setOnClickListener(null);
                                });
                            }
                        }));

                translatableAlertDialog.getButton(Dialog.BUTTON_NEUTRAL).setVisibility(
                        weatherPreferences.canTranslateAlerts() ?
                                View.VISIBLE :
                                View.GONE);
            });
        }

        this.alert = alert;
        translatableAlertDialog.setTitle(alert.event);
        translatableAlertDialogView.setAlertText(alert.senderName, alert.description);

        translatableAlertDialog.show();
    }

    public void showLocationDisclosure(Runnable onAcceptRunnable) {
        makeTextDialog();

        textDialog.setTitle(context.getString(R.string.dialog_location_disclosure_title));
        textDialog.setButton(Dialog.BUTTON_POSITIVE, context.getString(R.string.text_accept), (d,w) -> onAcceptRunnable.run());
        textDialog.setButton(Dialog.BUTTON_NEGATIVE, context.getString(R.string.text_decline), (DialogInterface.OnClickListener) null);
        textDialogView.setText(context.getString(R.string.dialog_location_disclosure));

        textDialog.show();
    }

    public void showLocationRationale() {
        makeTextDialog();

        textDialog.setTitle(context.getString(R.string.dialog_location_denied_title));
        textDialog.setButton(Dialog.BUTTON_POSITIVE, null, (DialogInterface.OnClickListener) null);
        textDialog.setButton(Dialog.BUTTON_NEGATIVE, context.getString(R.string.dialog_button_close), (DialogInterface.OnClickListener) null);
        textDialogView.setText(context.getString(R.string.dialog_location_denied));

        textDialog.show();
    }

    public void showReleaseNotes(String version, String releaseNotes) {
        makeTextDialog();

        textDialog.setTitle(version);
        textDialog.setButton(Dialog.BUTTON_POSITIVE, null, (DialogInterface.OnClickListener) null);
        textDialog.setButton(Dialog.BUTTON_NEGATIVE, context.getString(R.string.dialog_button_close), (DialogInterface.OnClickListener) null);
        textDialogView.setText(releaseNotes);

        textDialog.show();
    }

    public void showWeblateTranslation() {
        makeTextDialog();

        textDialog.setTitle(context.getString(R.string.dialog_translation_title));
        textDialog.setButton(Dialog.BUTTON_POSITIVE, null, (DialogInterface.OnClickListener) null);
        textDialog.setButton(Dialog.BUTTON_NEGATIVE, context.getString(R.string.dialog_button_close), (DialogInterface.OnClickListener) null);
        textDialogView.setText(StringUtils.fromHtml(context.getString(R.string.dialog_translation_text)));

        textDialog.show();
    }

    public void showLegendDialog(int radarThemeOrdinal) {
        if (legendDialog == null) {
            legendDialogView = new LegendDialogView(context);

            legendDialog = new MaterialAlertDialogBuilder(context, R.style.QuickWeather_Dialog)
                    .setTitle(R.string.dialog_legend_title)
                    .setView(legendDialogView)
                    .setNegativeButton(context.getResources().getString(R.string.dialog_button_close),
                            (dialog, which) -> dialog.dismiss())
                    .create();
        }

        legendDialogView.setRadarThemeOrdinal(radarThemeOrdinal);

        legendDialog.show();
    }

    public void showRadarThemeDialog(RadarTheme radarTheme, OnItemChosenListener<RadarTheme> onRadarThemeChosenListener) {
        if (radarThemeDialog == null) {
            radarThemeDialogView = new ChoiceDialogView<>(context);

            radarThemeDialogView.setItems(
                    RadarTheme.values(),
                    context.getResources().getStringArray(R.array.text_radar_themes)
            );

            radarThemeDialog = new MaterialAlertDialogBuilder(context, R.style.QuickWeather_Dialog)
                    .setTitle(R.string.dialog_radar_theme_title)
                    .setView(radarThemeDialogView)
                    .setCancelable(true)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok,
                            (d, w) -> DialogHelper.this.onRadarThemeChosenListener.onItemChosen(
                                    radarThemeDialogView.getSelected()))
                    .create();
        }

        radarThemeDialogView.setSelected(radarTheme);
        this.onRadarThemeChosenListener = onRadarThemeChosenListener;

        radarThemeDialog.show();
    }

    public void showLayoutDialog(List<WeatherDatabase.WeatherCard> currentWeatherCards,
                                 List<WeatherDatabase.WeatherCard> forecastWeatherCards,
                                 OnLayoutChangedListener onLayoutChangedListener) {
        if (layoutDialog == null) {
            layoutDialogView = new LayoutDialogView(context);

            layoutDialog = new MaterialAlertDialogBuilder(context, R.style.QuickWeather_Dialog)
                    .setView(layoutDialogView)
                    .setCancelable(true)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, (d, w) ->
                            DialogHelper.this.onLayoutChangedListener.onLayoutChosen(
                                    layoutDialogView.getCurrentCards(),
                                    layoutDialogView.getForecastCards()))
                    .create();
        }

        layoutDialogView.setLayoutCards(currentWeatherCards, forecastWeatherCards);
        this.onLayoutChangedListener = onLayoutChangedListener;

        layoutDialog.show();
    }

    public void showLocaleDialog(Locale[] locales,
                                 Locale currentLocale,
                                 OnItemChosenListener<Locale> onLocaleChosenListener) {
        if (localeDialog == null) {
            localeDialogView = new ChoiceDialogView<>(context);

            Locale[] localesWithSystemDefault = new Locale[locales.length + 1];
            String[] localeNames = new String[locales.length + 1];

            localesWithSystemDefault[0] = null;
            localeNames[0] = ApiUtils.getStringResourceFromApplication(
                    context.getPackageManager(),
                    "com.android.settings",
                    "preference_of_system_locale_summary",
                    "System default").toString();

            Locale locale;
            for (int i = 0; i < locales.length; i++) {
                locale = locales[i];

                localeNames[i + 1] = locale.getDisplayName(locale);
                localesWithSystemDefault[i + 1] = locale;
            }

            LocaleChoiceAdapter adapter = new LocaleChoiceAdapter();
            adapter.setItems(localesWithSystemDefault, localeNames);
            localeDialogView.setAdapter(adapter);

            localeDialog = new MaterialAlertDialogBuilder(context, R.style.QuickWeather_Dialog)
                    .setTitle(R.string.dialog_locale_title)
                    .setView(localeDialogView)
                    .setCancelable(true)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, (d, w) ->
                            DialogHelper.this.onLocaleChosenListener.onItemChosen(
                                    localeDialogView.getSelected()))
                    .create();
        }

        localeDialogView.setSelected(currentLocale);
        this.onLocaleChosenListener = onLocaleChosenListener;

        localeDialog.show();
    }

    public void showTranslationDialog(String apiKey,
                                      String instance,
                                      TranslationApiKeyTester translationApiKeyTester,
                                      OnTranslationApiKeyChangedListener onTranslationApiKeyChangedListener) {
        if (translationDialog == null) {
            translationDialogView = new TranslationDialogView(context);

            translationDialog = new MaterialAlertDialogBuilder(context, R.style.QuickWeather_Dialog)
                    .setTitle(context.getString(R.string.advanced_translation))
                    .setView(translationDialogView)
                    .setCancelable(true)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, null)
                    .create();

            View.OnClickListener translateOnClickListener = v -> {
                switch (translationDialogView.getApiKeyState()) {
                    case PASS:
                        DialogHelper.this.onTranslationApiKeyChangedListener.onTranslationApiKeyChanged(
                                translationDialogView.getApiKey(),
                                translationDialogView.getInstance());

                        translationDialog.cancel();
                        break;
                    case NEUTRAL:
                        translationDialogView.setErrorMessage(R.string.error_api_test_required);
                        break;
                }
            };

            translationDialog.setOnShowListener(d ->
                    translationDialog
                            .getButton(AlertDialog.BUTTON_POSITIVE)
                            .setOnClickListener(translateOnClickListener));
        }

        this.onTranslationApiKeyChangedListener = onTranslationApiKeyChangedListener;
        translationDialogView.setParams(
                apiKey,
                instance,
                translationApiKeyTester
        );

        translationDialog.show();
    }

    public void showLocationMapDialog(ILifecycleAwareActivity lifecycleAwareActivity,
                                      OnLocationChosenListener onLocationChosenListener) {
        if (locationMapDialogView == null) {
            locationMapDialogView = new LocationMapDialogView(context);

            locationMapDialog = new MaterialAlertDialogBuilder(context, R.style.QuickWeather_Dialog)
                    .setTitle(R.string.dialog_map_title)
                    .setView(locationMapDialogView)
                    .setCancelable(true)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok,
                            (d, w) -> showLocationEditDialog(
                                    locationMapDialogView.getWeatherLocation(),
                                    DialogHelper.this.onLocationChosenListener))
                    .create();
        }

        this.onLocationChosenListener = onLocationChosenListener;
        locationMapDialogView.attachToActivity(lifecycleAwareActivity);
        locationMapDialog.show();
    }

    public void showLocationSearchDialog(OnLocationChosenListener onLocationChosenListener) {
        if (locationSearchDialog == null) {
            locationSearchDialogView = new LocationSearchDialogView(context);

            locationSearchDialogView.setOnAddressChosenListener(address -> {
                locationSearchDialog.dismiss();

                showLocationEditDialog(
                        new WeatherDatabase.WeatherLocation(
                                address.getLatitude(),
                                address.getLongitude(),
                                address.getAddressLine(0)),
                        DialogHelper.this.onLocationChosenListener);
            });

            locationSearchDialog = new MaterialAlertDialogBuilder(context, R.style.QuickWeather_Dialog)
                    .setTitle(R.string.dialog_search_location_title)
                    .setView(locationSearchDialogView)
                    .setCancelable(true)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setNeutralButton(R.string.dialog_button_manual, (d, w) ->
                            showLocationEditDialog(
                                    null,
                                    DialogHelper.this.onLocationChosenListener))
                    .create();

            locationSearchDialog.setOnShowListener(d -> {
                locationSearchDialogView.prepareSearchTextView();

                Window dialogWindow = locationSearchDialog.getWindow();

                if (dialogWindow != null) {
                    dialogWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            });
        }

        this.onLocationChosenListener = onLocationChosenListener;
        locationSearchDialog.show();
    }

    public void showLocationEditDialog(WeatherDatabase.WeatherLocation weatherLocation, OnLocationChosenListener onLocationChosenListener) {
        if (locationEditDialog == null) {
            locationEditDialogView = new LocationEditDialogView(context);

            locationEditDialog = new MaterialAlertDialogBuilder(context, R.style.QuickWeather_Dialog)
                    .setTitle(R.string.dialog_edit_location_title)
                    .setView(locationEditDialogView)
                    .setCancelable(true)
                    .setPositiveButton(android.R.string.ok, null)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setNeutralButton(android.R.string.search_go,
                            (d, w) -> showLocationSearchDialog(onLocationChosenListener))
                    .create();

            View.OnClickListener editDialogOnClickListener = v -> {
                String dialogNameString = locationEditDialogView.getName();

                String dialogLatString = locationEditDialogView.getLatitude();
                double dialogLat = BigDecimal.valueOf(
                                LocaleUtils.parseDouble(Locale.getDefault(), dialogLatString))
                        .setScale(3, RoundingMode.HALF_UP)
                        .doubleValue();

                String dialogLonString = locationEditDialogView.getLongitude();
                double dialogLon = BigDecimal.valueOf(
                                LocaleUtils.parseDouble(Locale.getDefault(), dialogLonString))
                        .setScale(3, RoundingMode.HALF_UP)
                        .doubleValue();

                final String errorRequired = context.getString(R.string.text_required);
                final String errorInvalidValue = context.getString(R.string.text_invalid_value);

                String dialogNameErrorMessage = dialogNameString.isEmpty() ? errorRequired : null;
                String dialogLatErrorMessage = dialogLatString.isEmpty() ? errorRequired :
                        Math.abs(dialogLat) > 90 ? errorInvalidValue : null;
                String dialogLonErrorMessage = dialogLonString.isEmpty() ? errorRequired :
                        Math.abs(dialogLon) > 180 ? errorInvalidValue : null;

                locationEditDialogView.setNameError(dialogNameErrorMessage);
                locationEditDialogView.setLatitudeError(dialogLatErrorMessage);
                locationEditDialogView.setLongitudeError(dialogLonErrorMessage);

                if (dialogNameErrorMessage == null
                        && dialogLatErrorMessage == null
                        && dialogLonErrorMessage == null
                        && onLocationChosenListener != null) {
                    onLocationChosenListener.onLocationChosen(
                            dialogNameString,
                            dialogLat,
                            dialogLon);
                    locationEditDialog.dismiss();
                }
            };

            locationEditDialog.setOnShowListener(d -> {
                locationEditDialog
                        .getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(editDialogOnClickListener);

                Window dialogWindow = locationEditDialog.getWindow();

                if (dialogWindow != null) {
                    dialogWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            });
        }

        locationEditDialogView.setWeatherLocation(weatherLocation);
        locationEditDialog.show();
    }

    public void showAttributionDialog(CharSequence attributions) {
        makeTextDialog();

        textDialog.setTitle(context.getString(R.string.dialog_attribution_title));
        textDialog.setButton(Dialog.BUTTON_POSITIVE, null, (DialogInterface.OnClickListener) null);
        textDialog.setButton(Dialog.BUTTON_NEGATIVE, context.getString(R.string.dialog_button_close), (DialogInterface.OnClickListener) null);
        textDialogView.setText(attributions);

        textDialog.show();
    }

    public void showBatteryOptimizationDialog(Runnable onAcceptRunnable) {
        makeTextDialog();

        textDialog.setTitle(R.string.dialog_batteryoptimization_title);
        textDialog.setButton(Dialog.BUTTON_POSITIVE, context.getString(android.R.string.ok), (d,w) -> onAcceptRunnable.run());
        textDialog.setButton(Dialog.BUTTON_NEGATIVE, context.getString(android.R.string.cancel), (DialogInterface.OnClickListener) null);
        textDialogView.setText(R.string.dialog_batteryoptimization_text);

        textDialog.show();
    }

    public void showNewVersionDialog(String content, String title) {
        if (newVersionDialog == null) {
            final Uri githubUri = Uri.parse("https://github.com/TylerWilliamson/QuickWeather/releases/latest");
            final Uri googlePlayUri = Uri.parse("https://play.google.com/web/store/apps/details?id=com.ominous.quickweather");
            final Uri fdroidUri = Uri.parse("https://f-droid.org/en/packages/com.ominous.quickweather/");

            final CustomTabs customTabs = CustomTabs.getInstance(
                    context,
                    githubUri,
                    googlePlayUri,
                    fdroidUri);

            newVersionDialogView = new LinkedTextView(new ContextThemeWrapper(context, R.style.QuickWeather_Text_Dialog));
            newVersionDialogView.setLinkTextColor(ContextCompat.getColor(context, R.color.color_accent_text));

            newVersionDialog = new MaterialAlertDialogBuilder(context, R.style.QuickWeather_Dialog)
                    .setView(newVersionDialogView)
                    .setPositiveButton(R.string.text_github, (d, w) -> customTabs.launch(context, githubUri))
                    .setNeutralButton(R.string.text_googleplay, (d, w) -> customTabs.launch(context, googlePlayUri))
                    .setNegativeButton(R.string.text_fdroid, (d, w) -> customTabs.launch(context, fdroidUri))
                    .create();
        }

        newVersionDialogView.setText(content);
        newVersionDialog.setTitle(title);

        newVersionDialog.show();
    }

    @RequiresApi(30)
    public void showBackgroundLocationInstructionsDialog() {
        PackageManager packageManager = context.getPackageManager();
        CharSequence backgroundLabel = packageManager.getBackgroundPermissionOptionLabel();
        CharSequence permissionsLabel = ApiUtils.getStringResourceFromApplication(
                packageManager,
                "com.android.settings",
                "permissions_label",
                "Permissions");
        CharSequence autoRevokeLabel = ApiUtils.getStringResourceFromApplication(
                packageManager,
                "com.google.android.permissioncontroller",
                "auto_revoke_label",
                "Remove permissions if app isn’t used");

        CharSequence locationLabel;

        try {
            locationLabel = packageManager.getPermissionGroupInfo(Manifest.permission_group.LOCATION, 0).loadLabel(packageManager);
        } catch (PackageManager.NameNotFoundException e) {
            locationLabel = "Location";
        }

        makeTextDialog();

        textDialog.setTitle(context.getString(R.string.dialog_background_location_title));
        textDialog.setButton(
                Dialog.BUTTON_POSITIVE,
                context.getString(R.string.text_settings),
                (d, w) -> context.startActivity(new Intent()
                        .setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .addCategory(Intent.CATEGORY_DEFAULT)
                        .setData(Uri.fromParts("package", context.getPackageName(), null))));
        textDialog.setButton(Dialog.BUTTON_NEGATIVE, context.getString(R.string.button_close), (DialogInterface.OnClickListener) null);
        textDialogView.setText(context.getString(
                R.string.dialog_background_location,
                permissionsLabel,
                locationLabel,
                backgroundLabel,
                autoRevokeLabel));

        textDialog.show();
    }

    private void makeTextDialog() {
        if (textDialog == null) {
            textDialogView = new LinkedTextView(new ContextThemeWrapper(context, R.style.QuickWeather_Text_Dialog));
            textDialogView.setLinkTextColor(ContextCompat.getColor(context, R.color.color_accent_text));

            textDialog = new MaterialAlertDialogBuilder(context, R.style.QuickWeather_Dialog)
                    .setView(textDialogView)
                    .create();
        }
    }

    private static class LocaleChoiceAdapter extends ChoiceDialogView.ChoiceAdapter<Locale> {
        @Override
        public int getItemViewType(int position) {
            Locale l = items[position];
            return TextUtils.getLayoutDirectionFromLocale(l == null ? Locale.getDefault() : l) == View.LAYOUT_DIRECTION_RTL ? 1 : 0;
        }

        @NonNull
        @Override
        public ChoiceDialogView.ChoiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ChoiceDialogView.ChoiceViewHolder viewHolder = super.onCreateViewHolder(parent, viewType);

            viewHolder.itemView
                    .setLayoutDirection(viewType == 1 ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);

            return viewHolder;
        }
    }
}
