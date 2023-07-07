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

package com.ominous.quickweather.activity;

import android.app.Application;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.ominous.quickweather.R;
import com.ominous.quickweather.api.Gadgetbridge;
import com.ominous.quickweather.data.WeatherDataManager;
import com.ominous.quickweather.data.WeatherModel;
import com.ominous.quickweather.data.WeatherResponseOneCall;
import com.ominous.quickweather.util.ColorUtils;
import com.ominous.quickweather.util.NotificationUtils;
import com.ominous.quickweather.util.SnackbarHelper;
import com.ominous.quickweather.pref.WeatherPreferences;
import com.ominous.quickweather.view.WeatherCardRecyclerView;
import com.ominous.quickweather.work.WeatherWorkManager;
import com.ominous.tylerutils.browser.CustomTabs;
import com.ominous.tylerutils.util.LocaleUtils;
import com.ominous.tylerutils.util.WindowUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class ForecastActivity extends BaseActivity {
    public static final String EXTRA_DATE = "EXTRA_DATE";
    private WeatherCardRecyclerView weatherCardRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Toolbar toolbar;
    private ImageView toolbarMyLocation;
    private ForecastViewModel forecastViewModel;
    private final ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), r -> getWeather());
    private Date date = null;
    private SnackbarHelper snackbarHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        onReceiveIntent(getIntent());

        initViews();
        initViewModel();
    }

    protected void onReceiveIntent(Intent intent) {
        Bundle bundle;
        long timestamp;

        if ((bundle = intent.getExtras()) != null &&
                (timestamp = bundle.getLong(EXTRA_DATE)) != 0) {
            date = new Date(timestamp);
        } else {
            finish();
        }
    }

    @Override
    public void finish() {
        super.finish();
        this.overridePendingTransition(R.anim.slide_left_in, R.anim.slide_right_out);
    }

    private void getWeather() {
        if (WeatherPreferences.getInstance(this).isValidProvider()) {
            forecastViewModel.obtainWeatherAsync();

            WeatherWorkManager.enqueueNotificationWorker(true);

            if (!WeatherPreferences.getInstance(this).shouldShowPersistentNotification()) {
                NotificationUtils.cancelPersistentNotification(this);
            }
        } else {
            snackbarHelper.notifyInvalidProvider();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        getWeather();
    }

    private void initViewModel() {
        forecastViewModel = new ViewModelProvider(this)
                .get(ForecastActivity.ForecastViewModel.class);

        forecastViewModel.getWeatherModel().observe(this, weatherModel -> {
            swipeRefreshLayout.setRefreshing(
                    weatherModel.status == WeatherModel.WeatherStatus.UPDATING ||
                            weatherModel.status == WeatherModel.WeatherStatus.OBTAINING_LOCATION);

            snackbarHelper.dismiss();

            switch (weatherModel.status) {
                case SUCCESS:
                    weatherModel.date = date;
                    updateWeather(weatherModel);

                    if (WeatherPreferences.getInstance(this).shouldRunBackgroundJob()) {
                        WeatherWorkManager.enqueueNotificationWorker(true);

                        if (WeatherPreferences.getInstance(this).shouldShowPersistentNotification()) {
                            NotificationUtils.updatePersistentNotification(this, weatherModel.weatherLocation, weatherModel.responseOneCall);
                        }
                    }
                    break;
                case OBTAINING_LOCATION:
                    snackbarHelper.notifyObtainingLocation();
                    break;
                case ERROR_OTHER:
                    snackbarHelper.logError(weatherModel.errorMessage, null);
                    break;
                case ERROR_LOCATION_ACCESS_DISALLOWED:
                    snackbarHelper.notifyLocPermDenied(requestPermissionLauncher);
                    break;
                case ERROR_LOCATION_DISABLED:
                    snackbarHelper.notifyLocDisabled();
                    break;
            }
        });
    }

    private void updateWeather(WeatherModel weatherModel) {
        if (WeatherPreferences.getInstance(this).shouldDoGadgetbridgeBroadcast()) {
            Gadgetbridge.broadcastWeather(this, weatherModel.weatherLocation, weatherModel.responseOneCall);
        }

        long thisDate = LocaleUtils.getStartOfDay(date, TimeZone.getTimeZone(weatherModel.responseOneCall.timezone));

        boolean isToday = false;
        WeatherResponseOneCall.DailyData thisDailyData = null;
        for (int i = 0, l = weatherModel.responseOneCall.daily.length; i < l; i++) {
            WeatherResponseOneCall.DailyData dailyData = weatherModel.responseOneCall.daily[i];

            if (LocaleUtils.getStartOfDay(new Date(dailyData.dt * 1000),
                    TimeZone.getTimeZone(weatherModel.responseOneCall.timezone)) == thisDate) {
                thisDailyData = dailyData;

                isToday = i == 0;
                i = l;
            }
        }

        if (thisDailyData != null) {
            Calendar calendar = Calendar.getInstance(Locale.getDefault());
            calendar.setTimeInMillis(thisDailyData.dt * 1000);

            toolbar.setTitle(getString(R.string.format_forecast_title,
                    isToday ? getString(R.string.text_today) : calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()),
                    weatherModel.weatherLocation.isCurrentLocation ? getString(R.string.text_current_location) : weatherModel.weatherLocation.name));

            toolbar.setContentDescription(getString(R.string.format_forecast_title,
                    isToday ? getString(R.string.text_today) : calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()),
                    weatherModel.weatherLocation.isCurrentLocation ? getString(R.string.text_current_location) : weatherModel.weatherLocation.name));

            weatherCardRecyclerView.update(weatherModel);

            int color = ColorUtils.getColorFromTemperature((thisDailyData.temp.min + thisDailyData.temp.max) / 2, false);
            int darkColor = ColorUtils.getDarkenedColor(color);
            int textColor = ColorUtils.getTextColor(color);

            toolbar.setBackgroundColor(color);
            toolbar.setTitleTextColor(textColor);

            if (weatherModel.weatherLocation.isCurrentLocation) {
                toolbarMyLocation.setImageTintList(ColorStateList.valueOf(textColor));
                toolbarMyLocation.setVisibility(View.VISIBLE);
            } else {
                toolbarMyLocation.setVisibility(View.GONE);
            }

            Drawable navIcon = toolbar.getNavigationIcon();

            if (navIcon != null) {
                navIcon.setColorFilter(textColor, PorterDuff.Mode.SRC_IN);
                toolbar.setNavigationIcon(navIcon);
            }

            getWindow().setStatusBarColor(darkColor);
            getWindow().setNavigationBarColor(color);

            CustomTabs.getInstance(this).setColor(color);

            WindowUtils.setLightNavBar(getWindow(), textColor == ColorUtils.COLOR_TEXT_BLACK);
        }
    }

    private void initViews() {
        setContentView(R.layout.activity_forecast);

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        toolbar = findViewById(R.id.toolbar);
        toolbarMyLocation = findViewById(R.id.toolbar_mylocation_indicator);
        weatherCardRecyclerView = findViewById(R.id.weather_card_recycler_view);

        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener((a) -> onBackPressed());

        swipeRefreshLayout.setOnRefreshListener(() -> forecastViewModel.obtainWeatherAsync());

        snackbarHelper = new SnackbarHelper(findViewById(R.id.coordinator_layout));
    }

    public static class ForecastViewModel extends AndroidViewModel {
        private MutableLiveData<WeatherModel> weatherModelLiveData;

        public ForecastViewModel(@NonNull Application application) {
            super(application);
        }

        public MutableLiveData<WeatherModel> getWeatherModel() {
            if (weatherModelLiveData == null) {
                weatherModelLiveData = new MutableLiveData<>();
            }

            return weatherModelLiveData;
        }

        private void obtainWeatherAsync() {
            WeatherDataManager.getInstance().updateWeatherLiveData(getApplication().getApplicationContext(), weatherModelLiveData, true);
        }
    }
}