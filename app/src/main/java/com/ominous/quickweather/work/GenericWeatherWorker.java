package com.ominous.quickweather.work;

import android.content.Context;
import android.util.Pair;

import androidx.work.Data;

import com.ominous.quickweather.util.NotificationUtils;
import com.ominous.quickweather.util.WeatherPreferences;
import com.ominous.quickweather.weather.Weather;
import com.ominous.quickweather.weather.WeatherResponse;
import com.ominous.tylerutils.work.GenericWorker;

public class GenericWeatherWorker extends GenericWorker<WeatherResults> {
    private String apiKey;
    private Pair<Double,Double> locationKey;
    private boolean updateAlerts;
    private String provider;

    public GenericWeatherWorker(Context context, String provider,  String apiKey, Pair<Double,Double> locationKey, boolean updateAlerts) {
        super(context);

        this.apiKey = apiKey;
        this.locationKey = locationKey;
        this.updateAlerts = updateAlerts;
        this.provider = provider;
    }

    @Override
    public WeatherResults doWork(WorkerInterface workerInterface) throws Throwable {
        WeatherResponse weatherResponse = Weather.getWeather(provider,apiKey,locationKey);

        if (weatherResponse != null) {
            if (updateAlerts && weatherResponse.alerts != null && WeatherPreferences.getShowAlertNotification().equals(WeatherPreferences.ENABLED)) {
                for (WeatherResponse.Alert alert : weatherResponse.alerts) {
                    NotificationUtils.makeAlert(getContext(), alert);
                }
            }

            if (WeatherPreferences.getShowPersistentNotification().equals(WeatherPreferences.ENABLED)) {
                NotificationUtils.updatePersistentNotification(getContext(), weatherResponse.currently);
            }
        }

        return new WeatherResults(
                Data.EMPTY,//TODO data?
                weatherResponse);
    }
}
