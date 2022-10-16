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

package com.ominous.quickweather.api;

import android.content.Context;
import android.content.Intent;

import com.ominous.quickweather.data.WeatherDatabase;
import com.ominous.quickweather.data.WeatherResponseOneCall;
import com.ominous.quickweather.util.WeatherUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

public class Gadgetbridge {
    private final static String ACTION_GENERIC_WEATHER = "nodomain.freeyourgadget.gadgetbridge.ACTION_GENERIC_WEATHER";
    private final static String EXTRA_WEATHER_JSON = "WeatherJson";

    public static void broadcastWeather(Context context, WeatherDatabase.WeatherLocation weatherLocation, WeatherResponseOneCall weatherResponseOneCall) {
        try {
            JSONObject weatherJson = new JSONObject();

            weatherJson.put("timestamp", (int) (Calendar.getInstance().getTimeInMillis() / 1000));
            weatherJson.put("location", weatherLocation.name);
            weatherJson.put("currentTemp", convertTemperatureToKelvin(weatherResponseOneCall.current.temp));
            weatherJson.put("todayMinTemp", convertTemperatureToKelvin(weatherResponseOneCall.daily[0].temp.min));
            weatherJson.put("todayMaxTemp", convertTemperatureToKelvin(weatherResponseOneCall.daily[0].temp.max));
            weatherJson.put("currentCondition", WeatherUtils.getCurrentLongWeatherDesc(weatherResponseOneCall));
            weatherJson.put("currentConditionCode", weatherResponseOneCall.current.weather[0].id);
            weatherJson.put("currentHumidity", weatherResponseOneCall.current.humidity);
            weatherJson.put("windSpeed", weatherResponseOneCall.current.wind_speed);
            weatherJson.put("windDirection", weatherResponseOneCall.current.wind_deg);

            JSONArray weatherForecasts = new JSONArray();

            for (int i = 1; i < weatherResponseOneCall.daily.length; i++) {
                JSONObject dailyJsonData = new JSONObject();

                dailyJsonData.put("conditionCode", weatherResponseOneCall.daily[i].weather[0].id);
                dailyJsonData.put("humidity", weatherResponseOneCall.daily[i].humidity);
                dailyJsonData.put("maxTemp", convertTemperatureToKelvin(weatherResponseOneCall.daily[i].temp.max));
                dailyJsonData.put("minTemp", convertTemperatureToKelvin(weatherResponseOneCall.daily[i].temp.min));

                weatherForecasts.put(dailyJsonData);
            }

            weatherJson.put("forecasts", weatherForecasts);

            context.sendBroadcast(
                    new Intent(ACTION_GENERIC_WEATHER)
                            .putExtra(EXTRA_WEATHER_JSON, weatherJson.toString())
                            .setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES));
        } catch (JSONException e) {
            //
        }
    }

    private static int convertTemperatureToKelvin(double temperature) {
        return (int) ((temperature - 32.) * 5. / 9. + 273.15);
    }
}
