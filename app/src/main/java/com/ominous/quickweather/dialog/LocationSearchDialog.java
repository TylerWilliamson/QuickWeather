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
import android.content.res.Resources;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;
import androidx.core.content.ContextCompat;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputLayout;
import com.ominous.quickweather.R;
import com.ominous.quickweather.data.WeatherDatabase;
import com.ominous.tylerutils.async.Promise;
import com.ominous.tylerutils.util.ViewUtils;

import java.util.List;

public class LocationSearchDialog {

    private final static int AUTOCOMPLETE_DELAY = 300;
    private final static int THRESHOLD = 4;
    private final AlertDialog searchDialog;
    private final ArrayAdapter<String> autoCompleteAdapter;
    private final LocationDialogHandler messageHandler;
    private OnLocationChosenListener onLocationChosenListener;
    private List<Address> searchAddressResults;

    public LocationSearchDialog(Context context) {
        View searchDialogLayout = LayoutInflater.from(context).inflate(R.layout.dialog_searchlocation, null, false);

        messageHandler = new LocationDialogHandler(context, new OnGeocoderResult() {
            @Override
            public void onResult(List<Address> results) {
                searchAddressResults = results;

                autoCompleteAdapter.clear();

                for (Address a : results) {
                    autoCompleteAdapter.add(a.getAddressLine(0));
                }

                autoCompleteAdapter.notifyDataSetChanged();

                LinearProgressIndicator searchProgressIndicator = searchDialog.findViewById(R.id.dialog_loading_indicator);

                if (searchProgressIndicator != null) {
                    searchProgressIndicator.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onError(String error) {
                TextInputLayout searchInputLayout = searchDialog.findViewById(R.id.dialog_search_layout);

                if (searchInputLayout != null) {
                    searchInputLayout.setError(error);
                }

                LinearProgressIndicator searchProgressIndicator = searchDialog.findViewById(R.id.dialog_loading_indicator);

                if (searchProgressIndicator != null) {
                    searchProgressIndicator.setVisibility(View.INVISIBLE);
                }
            }
        });
        autoCompleteAdapter = new ArrayAdapterNoFilter(context, android.R.layout.simple_dropdown_item_1line);

        AppCompatAutoCompleteTextView searchDialogTextView = searchDialogLayout.findViewById(R.id.dialog_search);
        searchDialogTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                messageHandler.cancel();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                TextInputLayout searchInputLayout = searchDialog.findViewById(R.id.dialog_search_layout);
                LinearProgressIndicator searchProgressIndicator = searchDialog.findViewById(R.id.dialog_loading_indicator);

                if (searchInputLayout != null) {
                    searchInputLayout.setError(null);
                }

                if (s.length() >= THRESHOLD) {
                    messageHandler.sendMessageDelayed(Message.obtain(messageHandler, LocationDialogHandler.MESSAGE_TEXT_CHANGED, s.toString()), AUTOCOMPLETE_DELAY);

                    if (searchProgressIndicator != null) {
                        searchProgressIndicator.setVisibility(View.VISIBLE);
                    }
                } else {
                    autoCompleteAdapter.clear();

                    if (searchProgressIndicator != null) {
                        searchProgressIndicator.setVisibility(View.INVISIBLE);
                    }
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

                new LocationManualDialog(context).show(new WeatherDatabase.WeatherLocation(address.getLatitude(), address.getLongitude(), address.getAddressLine(0)), onLocationChosenListener);
            }
        });
        searchDialogTextView.setAdapter(autoCompleteAdapter);

        ViewUtils.setEditTextCursorColor(searchDialogTextView, ContextCompat.getColor(context, R.color.color_accent_text));

        searchDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.dialog_search_location_title)
                .setView(searchDialogLayout)
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.dialog_button_manual, (dialogInterface, w) -> new LocationManualDialog(context).show(null, onLocationChosenListener))
                .create();

        searchDialog.setOnShowListener(d -> {
            int textColor = ContextCompat.getColor(context, R.color.color_accent_text);

            searchDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(textColor);
            searchDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(textColor);
        });
    }

    public void show(OnLocationChosenListener onLocationChosenListener) {
        this.onLocationChosenListener = onLocationChosenListener;

        searchDialog.show();

        AppCompatAutoCompleteTextView searchDialogTextView = searchDialog.findViewById(R.id.dialog_search);

        if (searchDialogTextView != null) {
            searchDialogTextView.getText().clear();
            searchDialogTextView.requestFocus();
        }

        Window dialogWindow = searchDialog.getWindow();

        if (dialogWindow != null) {
            dialogWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
    }

    private interface OnGeocoderResult {
        void onResult(List<Address> results);

        void onError(String error);
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
        private final static int MESSAGE_TEXT_CHANGED = 0;
        private final Handler uiHandler = new Handler(Looper.getMainLooper());
        private final OnGeocoderResult onGeocoderResult;
        private final Geocoder geocoder;
        private final Resources resources;
        private Promise<List<Address>, Void> lookupPromise;

        LocationDialogHandler(Context context, OnGeocoderResult onGeocoderResult) {
            super(Looper.getMainLooper());

            this.geocoder = new Geocoder(context);
            this.onGeocoderResult = onGeocoderResult;
            this.resources = context.getResources();
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MESSAGE_TEXT_CHANGED) {
                doLookup((String) msg.obj);
            }
        }

        void cancel() {
            removeMessages(MESSAGE_TEXT_CHANGED);

            if (lookupPromise != null) {
                lookupPromise.cancel(true);
            }
        }

        private void doLookup(String name) {
            lookupPromise = Promise.create(name)
                    .then(input -> {
                        return geocoder.getFromLocationName(input, 5);
                    }, e -> uiHandler.post(() -> onGeocoderResult.onError(resources.getString(R.string.error_connecting_geocoder))))
                    .then(results -> {
                        uiHandler.post(() -> {
                            if (results == null || results.size() == 0) {
                                onGeocoderResult.onError(resources.getString(R.string.error_no_results));
                            } else {
                                onGeocoderResult.onResult(results);
                            }
                        });
                    });
        }
    }
}