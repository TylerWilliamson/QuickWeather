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

import android.app.ActivityManager;
import android.app.Application;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;

import com.ominous.quickweather.R;
import com.ominous.quickweather.data.WeatherDatabase;
import com.ominous.quickweather.data.WeatherLogic;
import com.ominous.quickweather.data.WeatherModel;
import com.ominous.quickweather.data.WeatherResponseOneCall;
import com.ominous.quickweather.location.WeatherLocationManager;
import com.ominous.quickweather.util.ColorUtils;
import com.ominous.quickweather.util.Logger;
import com.ominous.quickweather.util.SnackbarUtils;
import com.ominous.quickweather.util.WeatherPreferences;
import com.ominous.quickweather.view.WeatherCardRecyclerView;
import com.ominous.tylerutils.async.Promise;
import com.ominous.tylerutils.browser.CustomTabs;
import com.ominous.tylerutils.http.HttpException;
import com.ominous.tylerutils.util.LocaleUtils;
import com.ominous.tylerutils.util.WindowUtils;

import org.json.JSONException;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class ForecastActivity extends AppCompatActivity {
    public static final String EXTRA_DATE = "EXTRA_DATE";
    private static final String TAG = "ForecastActivity";
    private WeatherCardRecyclerView weatherCardRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Toolbar toolbar;
    private ImageView toolbarMyLocation;
    private ForecastViewModel forecastViewModel;
    private final ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), r -> forecastViewModel.obtainWeatherAsync());
    private Date date = null;
    private SnackbarUtils snackbarUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initActivity();

        if (WeatherPreferences.isInitialized()) {
            onReceiveIntent(getIntent());

            initViews();
            initViewModel();
        } else {
            ContextCompat.startActivity(this, new Intent(this, SettingsActivity.class), null);
            finish();
        }
    }

    private void initActivity() {
        ColorUtils.initialize(this);//Initializing after Activity created to get day/night properly

        setTaskDescription(
                Build.VERSION.SDK_INT >= 28 ?
                        new ActivityManager.TaskDescription(
                                getString(R.string.app_name),
                                R.mipmap.ic_launcher_round,
                                ContextCompat.getColor(this, R.color.color_app_accent)) :
                        new ActivityManager.TaskDescription(
                                getString(R.string.app_name),
                                BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_round),
                                ContextCompat.getColor(this, R.color.color_app_accent))
        );
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        onReceiveIntent(intent);
    }

    private void onReceiveIntent(Intent intent) {
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
    public void onBackPressed() {
        super.onBackPressed();
        this.overridePendingTransition(R.anim.slide_left_in, R.anim.slide_right_out);
    }

    @Override
    protected void onResume() {
        super.onResume();

        ColorUtils.setNightMode(this);

        forecastViewModel.obtainWeatherAsync();
    }

    private void initViewModel() {
        forecastViewModel = new ViewModelProvider(this)
                .get(ForecastActivity.ForecastViewModel.class);

        forecastViewModel.setDate(date);

        forecastViewModel.getWeatherModel().observe(this, weatherModel -> {
            swipeRefreshLayout.setRefreshing(
                    weatherModel.status == WeatherModel.WeatherStatus.UPDATING ||
                            weatherModel.status == WeatherModel.WeatherStatus.OBTAINING_LOCATION);

            snackbarUtils.dismiss();

            switch (weatherModel.status) {
                case SUCCESS:
                    Promise.create((a) -> {
                        WeatherDatabase.WeatherLocation weatherLocation = WeatherDatabase.getInstance(this).locationDao().getSelected();

                        new Handler(Looper.getMainLooper()).post(() -> updateWeather(weatherLocation, weatherModel));
                    });

                    break;
                case OBTAINING_LOCATION:
                    snackbarUtils.notifyObtainingLocation();
                    break;
                case ERROR_OTHER:
                    Logger.e(this, TAG, weatherModel.errorMessage, null);

                    swipeRefreshLayout.setRefreshing(false);
                    break;
                case ERROR_LOCATION_ACCESS_DISALLOWED:
                    snackbarUtils.notifyLocPermDenied(requestPermissionLauncher);
                    break;
                case ERROR_LOCATION_DISABLED:
                    snackbarUtils.notifyLocDisabled();
                    break;
            }
        });
    }

    private void updateWeather(WeatherDatabase.WeatherLocation weatherLocation, WeatherModel weatherModel) {
        long thisDate = LocaleUtils.getStartOfDay(weatherModel.date, TimeZone.getTimeZone(weatherModel.responseOneCall.timezone));

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
                    weatherLocation.isCurrentLocation ? getString(R.string.text_current_location) : weatherLocation.name));

            weatherCardRecyclerView.update(weatherModel);

            int color = ColorUtils.getColorFromTemperature((thisDailyData.temp.min + thisDailyData.temp.max) / 2, false);
            int darkColor = ColorUtils.getDarkenedColor(color);
            int textColor = ColorUtils.getTextColor(color);

            toolbar.setBackgroundColor(color);
            toolbar.setTitleTextColor(textColor);

            if (weatherLocation.isCurrentLocation) {
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

        toolbar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_white_24dp);
        toolbar.setNavigationOnClickListener((a) -> onBackPressed());

        swipeRefreshLayout.setOnRefreshListener(() -> forecastViewModel.obtainWeatherAsync());

        snackbarUtils = new SnackbarUtils(findViewById(R.id.coordinator_layout));
    }

    public static class ForecastViewModel extends AndroidViewModel {
        private MutableLiveData<WeatherModel> weatherModel;
        private Date date;

        public ForecastViewModel(@NonNull Application application) {
            super(application);
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public MutableLiveData<WeatherModel> getWeatherModel() {
            if (weatherModel == null) {
                weatherModel = new MutableLiveData<>();
            }

            return weatherModel;
        }

        private void obtainWeatherAsync() {
            Promise.create((a) -> {
                weatherModel.postValue(new WeatherModel(null, null, WeatherModel.WeatherStatus.UPDATING, null, date));

                WeatherModel.WeatherStatus weatherStatus = WeatherModel.WeatherStatus.ERROR_OTHER;
                String errorMessage = null;
                WeatherLogic.WeatherDataContainer weatherDataContainer = new WeatherLogic.WeatherDataContainer();

                try {
                    weatherDataContainer = WeatherLogic.getForecastWeather(getApplication(), false);

                    if (weatherDataContainer.location == null) {
                        weatherModel.postValue(new WeatherModel(null, null, WeatherModel.WeatherStatus.OBTAINING_LOCATION, null, date));

                        weatherDataContainer = WeatherLogic.getForecastWeather(getApplication(), true);
                    }

                    if (weatherDataContainer.location == null) {
                        errorMessage = getApplication().getString(R.string.error_null_location);
                    } else if (weatherDataContainer.weatherResponseOneCall == null || weatherDataContainer.weatherResponseOneCall.current == null ||
                            weatherDataContainer.weatherResponseForecast == null || weatherDataContainer.weatherResponseForecast.list == null) {
                        errorMessage = getApplication().getString(R.string.error_null_response);
                    } else {
                        weatherStatus = WeatherModel.WeatherStatus.SUCCESS;
                    }
                } catch (WeatherLocationManager.LocationPermissionNotAvailableException e) {
                    weatherStatus = WeatherModel.WeatherStatus.ERROR_LOCATION_ACCESS_DISALLOWED;
                    errorMessage = getApplication().getString(R.string.snackbar_background_location);
                } catch (WeatherLocationManager.LocationDisabledException e) {
                    weatherStatus = WeatherModel.WeatherStatus.ERROR_LOCATION_DISABLED;
                    errorMessage = getApplication().getString(R.string.error_gps_disabled);
                } catch (IOException e) {
                    errorMessage = getApplication().getString(R.string.error_connecting_api);
                } catch (JSONException e) {
                    errorMessage = getApplication().getString(R.string.error_unexpected_api_result);
                } catch (InstantiationException | IllegalAccessException e) {
                    errorMessage = getApplication().getString(R.string.error_creating_result);
                } catch (HttpException e) {
                    errorMessage = e.getMessage();
                }

                weatherModel.postValue(new WeatherModel(weatherDataContainer.weatherResponseOneCall, weatherDataContainer.weatherResponseForecast, weatherStatus, errorMessage, date));
            });
        }
    }
}