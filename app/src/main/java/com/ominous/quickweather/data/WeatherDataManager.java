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

package com.ominous.quickweather.data;

import android.content.Context;
import android.location.Location;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;

import com.ominous.quickweather.R;
import com.ominous.quickweather.api.openmeteo.OpenMeteo;
import com.ominous.quickweather.api.openweather.OpenWeatherMap;
import com.ominous.quickweather.location.LocationDisabledException;
import com.ominous.quickweather.location.LocationPermissionNotAvailableException;
import com.ominous.quickweather.location.LocationUnavailableException;
import com.ominous.quickweather.location.WeatherLocationManager;
import com.ominous.quickweather.pref.OwmApiVersion;
import com.ominous.quickweather.pref.WeatherPreferences;
import com.ominous.quickweather.pref.WeatherProvider;
import com.ominous.tylerutils.async.Promise;
import com.ominous.tylerutils.http.HttpException;

import org.json.JSONException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WeatherDataManager {
    private final Map<Pair<Double, Double>, CurrentWeather> currentWeatherCache = new HashMap<>();
    private final int CACHE_EXPIRATION = 60 * 1000; //1 minute
    private final int MAX_ATTEMPTS = 3;
    private final int ATTEMPT_SLEEP_DURATION = 5000;
    private final WeatherLocationManager weatherLocationManager = WeatherLocationManager.getInstance();

    private WeatherProvider currentProvider = null;

    private static WeatherDataManager instance;

    private WeatherDataManager() {

    }

    public static WeatherDataManager getInstance() {
        if (instance == null) {
            instance = new WeatherDataManager();
        }

        return instance;
    }

    public Promise<Void, WeatherModel> getWeatherAsync(Context context,
                                                       @Nullable MutableLiveData<WeatherModel> weatherLiveData,
                                                       boolean isBackground) {
        return getWeatherAsync(context, weatherLiveData, isBackground, null);
    }

    public Promise<Void, WeatherModel> getWeatherAsync(Context context,
                                                       @Nullable MutableLiveData<WeatherModel> weatherLiveData,
                                                       boolean isBackground,
                                                       Date date) {
        return Promise.create(a -> {
            if (weatherLiveData != null) {
                weatherLiveData.postValue(new WeatherModel(WeatherModel.WeatherStatus.UPDATING, null, null));
            }

            try {
                WeatherDatabase.WeatherLocation weatherLocation = WeatherDatabase.getInstance(context).locationDao().getSelected();
                Pair<Double, Double> locationKey;

                if (weatherLocation.isCurrentLocation) {
                    Location location = weatherLocationManager.getLastKnownLocation(context, isBackground);

                    if (location == null) {
                        if (weatherLiveData != null) {
                            weatherLiveData.postValue(new WeatherModel(WeatherModel.WeatherStatus.OBTAINING_LOCATION, null, null));
                        }

                        location = weatherLocationManager.obtainCurrentLocation(context, isBackground);

                        if (location == null) {
                            WeatherModel result = new WeatherModel(
                                    WeatherModel.WeatherStatus.ERROR_LOCATION_UNAVAILABLE,
                                    context.getString(R.string.error_null_location),
                                    new LocationUnavailableException());

                            if (weatherLiveData != null) {
                                weatherLiveData.postValue(result);
                            }

                            return result;
                        }
                    }

                    locationKey = new Pair<>(
                            BigDecimal.valueOf(location.getLatitude()).setScale(3, RoundingMode.HALF_UP).doubleValue(),
                            BigDecimal.valueOf(location.getLongitude()).setScale(3, RoundingMode.HALF_UP).doubleValue()
                    );
                } else {
                    locationKey = new Pair<>(
                            weatherLocation.latitude,
                            weatherLocation.longitude
                    );
                }

                WeatherPreferences weatherPreferences = WeatherPreferences.getInstance(context);
                WeatherProvider weatherProvider = weatherPreferences.getWeatherProvider();
                OwmApiVersion owmApiVersion = weatherPreferences.getOwmApiVersion();

                if (weatherProvider == WeatherProvider.DEFAULT) {
                    weatherPreferences.setWeatherProvider(WeatherProvider.OPENWEATHERMAP);
                    weatherProvider = WeatherProvider.OPENWEATHERMAP;
                }

                if (weatherProvider == WeatherProvider.OPENWEATHERMAP &&
                        owmApiVersion == OwmApiVersion.DEFAULT) {
                    weatherPreferences.setOwmApiVersion(OwmApiVersion.ONECALL_2_5);
                    owmApiVersion = OwmApiVersion.ONECALL_2_5;
                }

                if (currentProvider == null) {
                    currentProvider = weatherProvider;
                } else if (currentProvider != weatherProvider) {
                    currentProvider = weatherProvider;
                    clearCache(context);
                }

                String apiKey = weatherProvider == WeatherProvider.OPENMETEO ?
                        weatherPreferences.getOpenMeteoAPIKey() :
                        weatherPreferences.getOWMAPIKey();

                String weatherProviderInstance = weatherProvider == WeatherProvider.OPENMETEO ?
                        weatherPreferences.getOpenMeteoInstance() :
                        null;

                if (currentWeatherCache.containsKey(locationKey)) {
                    CurrentWeather previousWeather = currentWeatherCache.get(locationKey);

                    if (previousWeather != null) {
                        long now = Calendar.getInstance(previousWeather.timezone).getTimeInMillis();

                        if (now - previousWeather.timestamp < CACHE_EXPIRATION) {
                            return updateLiveDataAndReturn(weatherLiveData,
                                    new WeatherModel(
                                            previousWeather,
                                            weatherLocation,
                                            locationKey,
                                            WeatherModel.WeatherStatus.SUCCESS,
                                            date,
                                            false));
                        }
                    }
                }

                CurrentWeather cachedCurrentWeather = getCurrentWeatherFromFileCache(context, locationKey);

                if (cachedCurrentWeather != null) {
                    WeatherModel cachedWeatherModel = new WeatherModel(
                            cachedCurrentWeather,
                            weatherLocation,
                            locationKey,
                            WeatherModel.WeatherStatus.SUCCESS,
                            date,
                            true);

                    if (weatherLiveData != null) {
                        weatherLiveData.postValue(cachedWeatherModel);
                    }

                    long now = Calendar.getInstance(cachedCurrentWeather.timezone).getTimeInMillis();

                    if (now - cachedCurrentWeather.timestamp < CACHE_EXPIRATION) {
                        return cachedWeatherModel;
                    }
                }

                CurrentWeather currentWeather = getCurrentWeather(
                        context,
                        weatherProvider,
                        apiKey,
                        weatherProviderInstance,
                        owmApiVersion,
                        locationKey);

                if (currentWeather == null || currentWeather.current == null ||
                        currentWeather.trihourly == null) {
                    return updateLiveDataAndReturn(weatherLiveData,
                            new WeatherModel(
                                    WeatherModel.WeatherStatus.ERROR_OTHER,
                                    context.getString(R.string.error_null_response),
                                    new WeatherDataUnavailableException()));
                } else {
                    currentWeatherCache.put(locationKey, currentWeather);

                    Promise.create(b -> {
                        writeCurrentWeatherToFileCache(context, locationKey, currentWeather);
                    });

                    return updateLiveDataAndReturn(weatherLiveData,
                            new WeatherModel(
                                    currentWeather,
                                    weatherLocation,
                                    locationKey,
                                    WeatherModel.WeatherStatus.SUCCESS,
                                    date,
                                    false));
                }
            } catch (LocationPermissionNotAvailableException e) {
                return updateLiveDataAndReturn(weatherLiveData,
                        new WeatherModel(WeatherModel.WeatherStatus.ERROR_LOCATION_ACCESS_DISALLOWED, context.getString(R.string.snackbar_background_location_notifications), e));
            } catch (LocationDisabledException e) {
                return updateLiveDataAndReturn(weatherLiveData,
                        new WeatherModel(WeatherModel.WeatherStatus.ERROR_LOCATION_DISABLED, context.getString(R.string.error_gps_disabled), e));
            } catch (IOException e) {
                return updateLiveDataAndReturn(weatherLiveData,
                        new WeatherModel(WeatherModel.WeatherStatus.ERROR_OTHER, context.getString(R.string.error_connecting_api), e));
            } catch (JSONException e) {
                return updateLiveDataAndReturn(weatherLiveData,
                        new WeatherModel(WeatherModel.WeatherStatus.ERROR_OTHER, context.getString(R.string.error_unexpected_api_result), e));
            } catch (InstantiationException | IllegalAccessException | NullPointerException e) {
                return updateLiveDataAndReturn(weatherLiveData,
                        new WeatherModel(WeatherModel.WeatherStatus.ERROR_OTHER, context.getString(R.string.error_creating_result), e));
            } catch (HttpException e) {
                return updateLiveDataAndReturn(weatherLiveData,
                        new WeatherModel(WeatherModel.WeatherStatus.ERROR_OTHER, e.getMessage(), e));
            }
        });
    }

    private WeatherModel updateLiveDataAndReturn
            (@Nullable MutableLiveData<WeatherModel> liveData,
             WeatherModel weatherModel) {
        if (liveData != null) {
            liveData.postValue(weatherModel);
        }

        return weatherModel;
    }

    private CurrentWeather getCurrentWeather(Context context,
                                             @NonNull WeatherProvider weatherProvider,
                                             String apiKey,
                                             String weatherProviderInstance,
                                             OwmApiVersion owmApiVersion,
                                             Pair<Double, Double> locationKey) throws
            JSONException, HttpException, IOException, InstantiationException, IllegalAccessException {
        CurrentWeather newWeather = null;

        int attempt = 0;
        HttpException lastException = null;

        do {
            try {
                if (weatherProvider == WeatherProvider.OPENWEATHERMAP) {
                    if (owmApiVersion != null && owmApiVersion != OwmApiVersion.DEFAULT) {
                        newWeather = OpenWeatherMap.getInstance().getCurrentWeather(
                                context,
                                owmApiVersion,
                                locationKey.first,
                                locationKey.second,
                                apiKey);
                    } else {
                        throw new IllegalArgumentException("Illegal OwmApiVersion provided");
                    }
                } else if (weatherProvider == WeatherProvider.OPENMETEO) {
                    newWeather = OpenMeteo.getInstance()
                            .getCurrentWeather(context,
                                    locationKey.first,
                                    locationKey.second,
                                    apiKey,
                                    weatherProviderInstance);
                } else {
                    throw new IllegalArgumentException("Illegal WeatherProvider provided");
                }
            } catch (HttpException e) {
                lastException = e;
                try {
                    Thread.sleep(ATTEMPT_SLEEP_DURATION);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            } catch (IOException | JSONException | InstantiationException |
                     IllegalAccessException e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException("Uncaught Exception occurred", e);
            }
        } while (newWeather == null && attempt++ < MAX_ATTEMPTS);

        if (newWeather == null && lastException != null) {
            throw lastException;
        }

        return newWeather;
    }

    public void clearCache(Context context) {
        File[] files = context.getCacheDir().listFiles((dir, name) -> name.endsWith(".ser"));

        if (files != null) {
            for (File file : files) {
                try {
                    file.delete();
                } catch (SecurityException e) {
                    //
                }
            }
        }

        currentWeatherCache.clear();
    }

    public void removeUnneededFileCache(Context context,
                                        List<WeatherDatabase.WeatherLocation> weatherLocations) {
        final List<String> possibleFileNames = new ArrayList<>(weatherLocations.size());

        for (WeatherDatabase.WeatherLocation weatherLocation : weatherLocations) {
            possibleFileNames.add(getFileName(new Pair<>(weatherLocation.latitude, weatherLocation.longitude)));
        }

        File[] files = context.getCacheDir()
                .listFiles((dir, name) -> name.endsWith(".ser") && !possibleFileNames.contains(name));

        if (files != null) {
            for (File file : files) {
                try {
                    file.delete();
                } catch (SecurityException e) {
                    //
                }
            }
        }
    }

    public CurrentWeather getCurrentWeatherFromFileCache(Context context,
                                                         Pair<Double, Double> locationKey) {
        File file = new File(context.getCacheDir(), getFileName(locationKey));

        if (file.exists()) {
            try (
                    FileInputStream fileInputStream = new FileInputStream(file);
                    BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
                    ObjectInputStream objectInputStream = new ObjectInputStream(bufferedInputStream)) {
                return (CurrentWeather) objectInputStream.readObject();
            } catch (IOException | ClassNotFoundException e) {
                //either the file is corrupt, or the version is incorrect
                try {
                    file.delete();
                } catch (SecurityException e2) {
                    //
                }

                return null;
            }
        } else {
            return null;
        }
    }

    public void writeCurrentWeatherToFileCache(Context context,
                                               Pair<Double, Double> locationKey,
                                               CurrentWeather currentWeather) {
        File file = new File(context.getCacheDir(), getFileName(locationKey));
        try (
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(bufferedOutputStream)) {
            objectOutputStream.writeObject(currentWeather);
        } catch (Exception e) {
            //
        }
    }

    private String getFileName(Pair<Double, Double> locationKey) {
        return String.format(Locale.US,
                "%1$.3f_%2$.3f",
                locationKey.first,
                locationKey.second).replaceAll("\\.", "_") + ".ser";
    }
}
