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

package com.ominous.quickweather.card;

import android.content.Context;

import com.ominous.quickweather.R;
import com.ominous.quickweather.data.WeatherModel;
import com.ominous.quickweather.pref.DistanceUnit;
import com.ominous.quickweather.pref.SpeedUnit;
import com.ominous.quickweather.pref.TemperatureUnit;
import com.ominous.quickweather.pref.WeatherPreferences;
import com.ominous.quickweather.util.WeatherUtils;
import com.ominous.tylerutils.util.LocaleUtils;

import java.util.Locale;

public class CurrentMainCardView extends BaseMainCardView {

    public CurrentMainCardView(Context context) {
        super(context);
    }

    @Override
    public void update(WeatherModel weatherModel, int position) {
        super.update(weatherModel, position);

        WeatherUtils weatherUtils = WeatherUtils.getInstance(getContext());
        WeatherPreferences weatherPreferences = WeatherPreferences.getInstance(getContext());
        TemperatureUnit temperatureUnit = weatherPreferences.getTemperatureUnit();
        SpeedUnit speedUnit = weatherPreferences.getSpeedUnit();
        DistanceUnit distanceUnit = weatherPreferences.getDistanceUnit();

        String temperatureString = weatherUtils.getTemperatureString(temperatureUnit, weatherModel.currentWeather.current.temp, 1);
        String weatherString = weatherModel.currentWeather.current.weatherLongDescription;
        String dewPointString = weatherUtils.getTemperatureString(temperatureUnit, weatherModel.currentWeather.current.dewPoint, 1);
        String humidityString = LocaleUtils.getPercentageString(Locale.getDefault(), weatherModel.currentWeather.current.humidity / 100.0);
        String feelsLikeString = getContext().getString(R.string.format_feelslike, weatherUtils.getTemperatureString(temperatureUnit, weatherModel.currentWeather.current.feelsLike, 1));
        String pressureString = getContext().getString(R.string.format_pressure, weatherModel.currentWeather.current.pressure);
        String uvIndexString = getContext().getString(R.string.format_uvi, weatherModel.currentWeather.current.uvi);
        String visibilityString = getContext().getString(R.string.format_visibility, weatherModel.currentWeather.current.visibility / 1000.);

        mainIcon.setImageResource(weatherModel.currentWeather.current.weatherIconRes);
        mainTemperature.setText(temperatureString);
        mainDescription.setText(weatherString);

        windIconTextView.getTextView().setText(weatherUtils.getWindSpeedString(speedUnit, weatherModel.currentWeather.current.windSpeed, weatherModel.currentWeather.current.windDeg, false));
        rainIconTextView.getTextView().setText(weatherUtils.getPrecipitationString(distanceUnit, weatherModel.currentWeather.current.precipitationIntensity, weatherModel.currentWeather.current.precipitationType, false));
        uvIndexIconTextView.getTextView().setText(uvIndexString);
        dewPointIconTextView.getTextView().setText(getContext().getString(R.string.format_dewpoint, dewPointString));
        humidityIconTextView.getTextView().setText(getContext().getString(R.string.format_humidity, humidityString));
        pressureIconTextView.getTextView().setText(pressureString);
        feelsLikeIconTextView.getTextView().setText(feelsLikeString);
        visibilityIconTextView.getTextView().setText(visibilityString);

        setContentDescription(getContext().getString(R.string.format_current_desc,
                temperatureString,
                weatherString,
                feelsLikeString,
                weatherUtils.getPrecipitationString(distanceUnit, weatherModel.currentWeather.current.precipitationIntensity, weatherModel.currentWeather.current.precipitationType, true),
                weatherUtils.getWindSpeedString(speedUnit, weatherModel.currentWeather.current.windSpeed, weatherModel.currentWeather.current.windDeg, true),
                humidityString,
                pressureString,
                dewPointString,
                uvIndexString,
                visibilityString
        ));
    }
}