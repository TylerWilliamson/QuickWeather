package com.ominous.quickweather.work;

import androidx.work.Data;

import com.ominous.quickweather.weather.WeatherResponse;
import com.ominous.tylerutils.work.GenericResults;

public class WeatherResults extends GenericResults<WeatherResponse> {
    public WeatherResults(Data data, WeatherResponse results) {
        super(data, results);
    }
}
