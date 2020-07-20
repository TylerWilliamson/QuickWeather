package com.ominous.quickweather.work;

import android.content.Context;
import android.location.Location;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import com.ominous.quickweather.util.WeatherPreferences;
import com.ominous.quickweather.weather.WeatherLocationManager;
import com.ominous.tylerutils.work.BaseWorker;

public class WeatherWorker extends BaseWorker<GenericWeatherWorker> {
    public WeatherWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    //TODO: notify user if errors by extending doWork?

    @Override
    public GenericWeatherWorker getWorker(Context context) {
        try {
            Location location = WeatherLocationManager.getLocation(context);

            return new GenericWeatherWorker(context,
                    WeatherPreferences.getProvider(),
                    WeatherPreferences.getApiKey(),
                    new Pair<>(
                            location.getLatitude(),
                            location.getLongitude()),
                    true);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
