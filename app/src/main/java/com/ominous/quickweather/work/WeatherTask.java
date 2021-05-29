/*
 *     Copyright 2019 - 2021 Tyler Williamson
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

package com.ominous.quickweather.work;

import android.content.Context;
import android.content.res.Resources;
import android.util.Pair;

import androidx.annotation.MainThread;

import com.ominous.quickweather.R;
import com.ominous.quickweather.weather.Weather;
import com.ominous.quickweather.weather.WeatherResponse;
import com.ominous.tylerutils.work.BaseAsyncTask;
import com.ominous.tylerutils.work.GenericResults;

import org.json.JSONException;

import java.io.IOException;

public class WeatherTask extends BaseAsyncTask<GenericWeatherWorker> {
    private final Weather.WeatherListener weatherListener;
    private final Pair<Double, Double> locationKey;
    private final String provider;
    private final String apiKey;
    private String errorMessage = "";
    private final Resources resources;

    private Throwable error;

    public WeatherTask(Context context, Weather.WeatherListener weatherListener, String provider, String apiKey, Pair<Double, Double> locationKey) {
        super(context);
        this.weatherListener = weatherListener;
        this.locationKey = locationKey;
        this.apiKey = apiKey;
        this.resources = context.getResources();
        this.provider = provider;

        setWorker(getWorker(context));
    }

    @Override
    protected WeatherResults doInBackground(Void... voids) {
        try {
            return (WeatherResults) doWork();
        } catch (IOException e) {
            errorMessage = resources.getString(R.string.error_connecting_api);
            error = e;
        } catch (JSONException e) {
            errorMessage = resources.getString(R.string.error_unexpected_api_result);
            error = e;
        } catch (InstantiationException | IllegalAccessException e) {
            errorMessage = resources.getString(R.string.error_creating_result);
            error = e;
        } catch (Throwable other) { //Includes HttpException
            errorMessage = other.getMessage();
            error = other;
        }
        return null;
    }

    @Override
    @MainThread
    protected void onPostExecute(GenericResults result) {
        if (error == null) {
            WeatherResponse response = result == null ? null : (WeatherResponse) result.getResults();
            
            if (response == null || response.currently == null) {
                String errorMessage = resources.getString(R.string.error_null_response);
                weatherListener.onWeatherError(errorMessage, new RuntimeException(errorMessage));
            } else {
                weatherListener.onWeatherRetrieved(response);
            }
        } else {
            weatherListener.onWeatherError(errorMessage, error);
        }
    }

    @Override
    public GenericWeatherWorker getWorker(Context context) {
        if (apiKey == null || locationKey == null) {
            return null;
        } else {
            return new GenericWeatherWorker(context, provider, apiKey, locationKey, false);
        }
    }
}
