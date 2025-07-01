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
import android.content.res.Resources;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;
import androidx.core.content.ContextCompat;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputLayout;
import com.ominous.quickweather.R;
import com.ominous.quickweather.api.openmeteo.OpenMeteo;
import com.ominous.tylerutils.async.Promise;
import com.ominous.tylerutils.http.HttpException;
import com.ominous.tylerutils.util.ViewUtils;

import org.json.JSONException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

public class LocationSearchDialogView extends FrameLayout {
    private final static int AUTOCOMPLETE_DELAY = 300;
    private final static int THRESHOLD = 4;

    private final AppCompatAutoCompleteTextView searchDialogTextView;
    private final LinearProgressIndicator searchProgressIndicator;
    private final TextInputLayout searchInputLayout;
    private final ArrayAdapter<String> autoCompleteAdapter;
    private final LocationDialogHandler messageHandler;

    private OnItemChosenListener<Address> onAddressChosenListener;
    private List<Address> searchAddressResults;

    public LocationSearchDialogView(@NonNull Context context) {
        this(context, null, 0, 0);
    }

    public LocationSearchDialogView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public LocationSearchDialogView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public LocationSearchDialogView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        LayoutInflater.from(context).inflate(R.layout.dialog_searchlocation, this, true);

        messageHandler = new LocationDialogHandler(context, new OnGeocoderResult() {
            @Override
            public void onResult(List<Address> results) {
                searchAddressResults = results;

                autoCompleteAdapter.clear();

                for (Address a : results) {
                    autoCompleteAdapter.add(a.getAddressLine(0));
                }

                autoCompleteAdapter.notifyDataSetChanged();

                searchProgressIndicator.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onError(String error) {
                searchInputLayout.setError(error);

                searchProgressIndicator.setVisibility(View.INVISIBLE);
            }
        });
        autoCompleteAdapter = new ArrayAdapterNoFilter(context, android.R.layout.simple_dropdown_item_1line);

        searchProgressIndicator = findViewById(R.id.dialog_loading_indicator);
        searchInputLayout = findViewById(R.id.dialog_search_layout);
        searchDialogTextView = findViewById(R.id.dialog_search);

        searchDialogTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                messageHandler.cancel();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchInputLayout.setError(null);

                if (s.length() >= THRESHOLD) {
                    messageHandler.sendMessageDelayed(
                            Message.obtain(
                                    messageHandler,
                                    LocationDialogHandler.MESSAGE_TEXT_CHANGED,
                                    s.toString()),
                            AUTOCOMPLETE_DELAY);

                    searchProgressIndicator.setVisibility(View.VISIBLE);
                } else {
                    autoCompleteAdapter.clear();

                    searchProgressIndicator.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        searchDialogTextView.setOnItemClickListener(
                (parent, view, position, id) ->
                        onAddressChosenListener.onItemChosen(
                                searchAddressResults.get(position)));

        searchDialogTextView.setAdapter(autoCompleteAdapter);

        ViewUtils.setEditTextCursorColor(searchDialogTextView, ContextCompat.getColor(context, R.color.color_accent_text));
    }

    public void setOnAddressChosenListener(OnItemChosenListener<Address> onAddressChosenListener) {
        this.onAddressChosenListener = onAddressChosenListener;
    }

    public void prepareSearchTextView() {
        searchDialogTextView.getText().clear();
        searchDialogTextView.requestFocus();
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

            if (Geocoder.isPresent()) {
                this.geocoder = new Geocoder(context);
            } else {
                this.geocoder = null;
            }

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
            if (geocoder != null) {
                lookupPromise = Promise.create(name)
                        .then(input -> {
                            return geocoder.getFromLocationName(input, 5);
                        }, e -> uiHandler.post(() -> onGeocoderResult.onError(resources.getString(R.string.error_connecting_geocoder))))
                        .then(results -> {
                            uiHandler.post(() -> {
                                if (results == null || results.isEmpty()) {
                                    onGeocoderResult.onError(resources.getString(R.string.error_no_results));
                                } else {
                                    onGeocoderResult.onResult(results);
                                }
                            });
                        });
            } else {
                lookupPromise = Promise.create(name)
                        .then(
                                input -> {
                                    return OpenMeteo.getInstance().getGeocoderResult(resources, input);
                                },
                                e -> {
                                    if (e instanceof UnsupportedEncodingException) {
                                        uiHandler.post(() -> onGeocoderResult.onError(resources.getString(R.string.error_url_encoding)));
                                    } else if (e instanceof HttpException) {
                                        uiHandler.post(() -> onGeocoderResult.onError(resources.getString(R.string.error_server_result, e.getMessage())));
                                    } else if (e instanceof IOException) {
                                        uiHandler.post(() -> onGeocoderResult.onError(resources.getString(R.string.error_server_connection, e.getMessage())));
                                    } else if (e instanceof JSONException) {
                                        uiHandler.post(() -> onGeocoderResult.onError(resources.getString(R.string.error_malformed_data, e.getMessage())));
                                    } else {
                                        uiHandler.post(() -> onGeocoderResult.onError(resources.getString(R.string.error_unknown)));
                                    }
                        })
                        .then(results -> {
                            uiHandler.post(() -> {
                                if (results == null || results.isEmpty()) {
                                    onGeocoderResult.onError(resources.getString(R.string.error_no_results));
                                } else {
                                    onGeocoderResult.onResult(results);
                                }
                            });
                        });
            }
        }
    }
}