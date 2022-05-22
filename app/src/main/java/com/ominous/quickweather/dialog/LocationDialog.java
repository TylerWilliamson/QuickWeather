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
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Filter;

import com.google.android.material.textfield.TextInputLayout;
import com.ominous.quickweather.R;
import com.ominous.quickweather.data.WeatherDatabase;
import com.ominous.quickweather.util.LocaleUtils;
import com.ominous.tylerutils.util.ViewUtils;
import com.ominous.tylerutils.work.SimpleAsyncTask;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

//TODO: Split into two classes, expose everything through interfaces, move logic to Activities
public class LocationDialog {
    private static final int MESSAGE_TEXT_CHANGED = 0, AUTOCOMPLETE_DELAY = 300, THRESHOLD = 4;

    private final OnLocationChosenListener onLocationChosenListener;
    private final AlertDialog searchDialog;
    private final AlertDialog editDialog;

    private final ArrayAdapter<String> autoCompleteAdapter;
    private final LocationDialogHandler messageHandler;
    private final String separator;
    private final AutoCompleteTextView searchDialogTextView;
    private final EditText editDialogLocationName;
    private final EditText editDialogLocationLatitude;
    private final EditText editDialogLocationLongitude;
    private final TextInputLayout editDialogLocationNameLayout;
    private final TextInputLayout editDialogLocationLatitudeLayout;
    private final TextInputLayout editDialogLocationLongitudeLayout;
    private List<Address> searchAddressResults;

    public LocationDialog(Context context, final OnLocationChosenListener onLocationChosenListener) {
        this.onLocationChosenListener = onLocationChosenListener;

        messageHandler = new LocationDialogHandler(context, this);
        autoCompleteAdapter = new ArrayAdapterNoFilter(context, android.R.layout.simple_dropdown_item_1line);
        autoCompleteAdapter.setNotifyOnChange(false);
        searchDialogTextView = new AutoCompleteTextView(context);
        searchDialogTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                messageHandler.cancel();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= THRESHOLD) {
                    messageHandler.sendMessageDelayed(Message.obtain(messageHandler, MESSAGE_TEXT_CHANGED, s.toString()), AUTOCOMPLETE_DELAY);
                } else {
                    autoCompleteAdapter.clear();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        searchDialogTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Address address = searchAddressResults.get(position);

                searchDialog.dismiss();

                onLocationChosenListener.onLocationChosen(addressToString(address), address.getLatitude(), address.getLongitude());
            }
        });
        searchDialogTextView.setThreshold(THRESHOLD);
        searchDialogTextView.setInputType(InputType.TYPE_CLASS_TEXT);
        searchDialogTextView.setAdapter(autoCompleteAdapter);

        ViewUtils.setEditTextCursorColor(searchDialogTextView, ContextCompat.getColor(context, R.color.color_accent_text));

        separator = context.getResources().getString(R.string.format_separator);

        if (Build.VERSION.SDK_INT > 26) {
            searchDialogTextView.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        }

        searchDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.dialog_search_location_title)
                .setView(searchDialogTextView)
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.dialog_button_manual, (dialogInterface, w) -> showEditDialog(null))
                .create();

        searchDialog.setOnShowListener(d -> {
            int textColor = ContextCompat.getColor(context, R.color.color_accent_text);

            searchDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(textColor);
            searchDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(textColor);
        });

        View editDialogLayout = LayoutInflater.from(context).inflate(R.layout.dialog_editlocation, null, false);

        editDialogLocationName = editDialogLayout.findViewById(R.id.editlocation_location);
        editDialogLocationLatitude = editDialogLayout.findViewById(R.id.editlocation_latitude);
        editDialogLocationLongitude = editDialogLayout.findViewById(R.id.editlocation_longitude);

        editDialogLocationNameLayout = editDialogLayout.findViewById(R.id.editlocation_location_layout);
        editDialogLocationLatitudeLayout = editDialogLayout.findViewById(R.id.editlocation_latitude_layout);
        editDialogLocationLongitudeLayout = editDialogLayout.findViewById(R.id.editlocation_longitude_layout);

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
                editDialogLocationNameLayout.setError(null);

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
                .setNeutralButton(android.R.string.search_go, (dialogInterface, which) -> showSearchDialog())
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

                if (emptyInputs == 0) {
                    onLocationChosenListener.onLocationChosen(
                            editDialogName,
                            BigDecimal.valueOf(LocaleUtils.parseDouble(Locale.getDefault(),editDialogLat)).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue(),
                            BigDecimal.valueOf(LocaleUtils.parseDouble(Locale.getDefault(),editDialogLon)).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue());
                    editDialog.dismiss();
                }
            });

            editDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(buttonTextColor);
            editDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(buttonTextColor);
            editDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(buttonTextColor);
        });
    }

    public void showSearchDialog() {
        Window dialogWindow = searchDialog.getWindow();

        if (dialogWindow != null) {
            dialogWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        searchDialogTextView.getText().clear();
        searchDialogTextView.requestFocus();

        searchDialog.show();
    }

    public void showEditDialog(WeatherDatabase.WeatherLocation weatherLocation) {
        if (weatherLocation != null) {
            NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());

            editDialogLocationLatitude.setText(numberFormat.format(BigDecimal.valueOf(weatherLocation.latitude).setScale(3, BigDecimal.ROUND_HALF_UP)));
            editDialogLocationLongitude.setText(numberFormat.format(BigDecimal.valueOf(weatherLocation.longitude).setScale(3, BigDecimal.ROUND_HALF_UP)));
            editDialogLocationName.setText(weatherLocation.name);
        }

        Window dialogWindow = editDialog.getWindow();

        if (dialogWindow != null) {
            dialogWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        editDialogLocationName.requestFocus();

        editDialog.show();
    }

    private void notifyResult(List<Address> addresses) {
        this.searchAddressResults = addresses;

        autoCompleteAdapter.clear();

        for (Address a : addresses) {
            autoCompleteAdapter.add(addressToString(a));
        }

        autoCompleteAdapter.notifyDataSetChanged();
    }

    private void notifyError(Exception error) {
        onLocationChosenListener.onGeoCoderError(error);
    }

    private String addressToString(Address address) {
        StringBuilder stringBuilder = new StringBuilder();

        if (address.getMaxAddressLineIndex() > 0) {
            stringBuilder.append(address.getAddressLine(1)).append(separator);
        }

        if (address.getLocality() != null) {
            stringBuilder.append(address.getLocality()).append(separator);
        }

        if (address.getAdminArea() != null) {
            stringBuilder.append(address.getAdminArea()).append(separator);
        }

        return stringBuilder.append(address.getCountryCode()).toString();
    }

    public interface OnLocationChosenListener {
        void onLocationChosen(String name, double latitude, double longitude);

        void onGeoCoderError(Throwable throwable);
    }

    private static class ArrayAdapterNoFilter extends ArrayAdapter<String> {

        private final NoFilter NO_FILTER = new NoFilter();

        @SuppressWarnings("SameParameterValue")
        ArrayAdapterNoFilter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        @NonNull
        @Override
        public Filter getFilter() {
            return NO_FILTER;
        }

        private static class NoFilter extends Filter {

            protected FilterResults performFiltering(CharSequence prefix) {
                return new FilterResults();
            }

            protected void publishResults(CharSequence constraint, FilterResults results) {
                // Do nothing
            }
        }

    }

    private static class LocationDialogHandler extends Handler {
        private final LocationDialog dialog;
        private final Geocoder geocoder;
        private GeocoderAsyncTask geocoderAsyncTask;

        LocationDialogHandler(Context context, LocationDialog dialog) {
            super(Looper.getMainLooper());

            this.geocoder = new Geocoder(context);
            this.dialog = dialog;
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MESSAGE_TEXT_CHANGED) {
                geocoderAsyncTask = new GeocoderAsyncTask(geocoder, dialog);
                geocoderAsyncTask.execute((String) msg.obj);
            }
        }

        void cancel() {
            removeMessages(MESSAGE_TEXT_CHANGED);

            if (geocoderAsyncTask != null) {
                geocoderAsyncTask.cancel(true);
            }
        }

        private static class GeocoderAsyncTask extends SimpleAsyncTask<String, List<Address>> {
            private final Geocoder geocoder;
            private final LocationDialog dialog;

            private IOException geocoderError;

            GeocoderAsyncTask(Geocoder geocoder, LocationDialog dialog) {
                this.geocoder = geocoder;
                this.dialog = dialog;
            }

            @Override
            protected List<Address> doInBackground(String... enteredText) {
                try {
                    List<Address> addresses = geocoder.getFromLocationName(enteredText[0], 10);

                    if (!isCancelled()) {
                        return addresses;
                    }
                } catch (IOException ex) {
                    geocoderError = ex;
                }
                return null;
            }

            @Override
            protected void onPostExecute(List<Address> addresses) {
                if (addresses != null) {
                    dialog.notifyResult(addresses);
                } else if (geocoderError != null) {
                    dialog.notifyError(geocoderError);
                }
            }
        }

    }
}