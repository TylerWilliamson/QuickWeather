package com.ominous.quickweather.dialog;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;

import com.ominous.quickweather.R;

import java.io.IOException;
import java.util.List;

public class LocationDialog implements TextWatcher, AdapterView.OnItemClickListener {
    private static final int MESSAGE_TEXT_CHANGED = 0;
    private static final int AUTOCOMPLETE_DELAY = 300;
    private static final int THRESHOLD = 4;
    private ArrayAdapter<String> autoCompleteAdapter;
    private LocationDialogHandler messageHandler;
    private OnLocationChosenListener onLocationChosenListener;
    private List<Address> addresses;
    private AutoCompleteTextView textView;
    private AlertDialog dialog;

    private String separator;

    public LocationDialog(Context context, final OnLocationChosenListener onLocationChosenListener) {
        this.onLocationChosenListener = onLocationChosenListener;

        messageHandler = new LocationDialogHandler(context, this);
        autoCompleteAdapter = new ArrayAdapterNoFilter(context,android.R.layout.simple_dropdown_item_1line);
        autoCompleteAdapter.setNotifyOnChange(false);
        textView = new AutoCompleteTextView(context);
        textView.addTextChangedListener(this);
        textView.setOnItemClickListener(this);
        textView.setThreshold(THRESHOLD);
        textView.setInputType(InputType.TYPE_CLASS_TEXT);
        textView.setAdapter(autoCompleteAdapter);

        separator = context.getResources().getString(R.string.format_address_separator);

        if (Build.VERSION.SDK_INT > 26) {
            textView.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        }

        dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.text_choose_location)
                .setView(textView)
                .setCancelable(true)
                .setNegativeButton(R.string.text_cancel,null)
                .create();
    }

    public void show() {
        textView.getText().clear();

        Window dialogWindow = dialog.getWindow();

        if (dialogWindow != null) {
            dialogWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        textView.requestFocus();

        dialog.show();
    }

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

    private void notifyResult(List<Address> addresses) {
        this.addresses = addresses;

        autoCompleteAdapter.clear();

        for (Address a : addresses) {
            autoCompleteAdapter.add(addressToString(a));
        }

        autoCompleteAdapter.notifyDataSetChanged();
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

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Address address = addresses.get(position);

        dialog.dismiss();

        onLocationChosenListener.onLocationChosen(addressToString(address),address.getLatitude(),address.getLongitude());
    }

    private class ArrayAdapterNoFilter extends ArrayAdapter<String> {
        private final NoFilter NO_FILTER = new NoFilter();

        ArrayAdapterNoFilter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        @NonNull
        @Override
        public Filter getFilter() {
            return NO_FILTER;
        }

        private class NoFilter extends Filter {
            protected FilterResults performFiltering(CharSequence prefix) {
                return new FilterResults();
            }

            protected void publishResults(CharSequence constraint, FilterResults results) {
                // Do nothing
            }
        }
    }

    private static class LocationDialogHandler extends Handler {
        private GeocoderAsyncTask geocoderAsyncTask;
        private LocationDialog dialog;
        private Geocoder geocoder;

        LocationDialogHandler(Context context, LocationDialog dialog) {
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

        private static class GeocoderAsyncTask extends AsyncTask<String, Void, List<Address>> {
            private Geocoder geocoder;
            private LocationDialog dialog;
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

    private void notifyError(Exception error) {
        onLocationChosenListener.onGeoCoderError(error.getLocalizedMessage());
    }

    public interface OnLocationChosenListener {
        void onLocationChosen(String location, double latitude, double longitude);
        void onGeoCoderError(String error);
    }
}