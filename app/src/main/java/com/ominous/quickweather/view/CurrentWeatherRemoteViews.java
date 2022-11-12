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

package com.ominous.quickweather.view;

import android.content.Context;
import android.widget.RemoteViews;

import androidx.core.content.ContextCompat;

import com.ominous.quickweather.R;
import com.ominous.quickweather.data.WeatherDatabase;
import com.ominous.quickweather.data.WeatherResponseOneCall;
import com.ominous.quickweather.util.ColorUtils;
import com.ominous.quickweather.util.WeatherUtils;
import com.ominous.tylerutils.util.BitmapUtils;
import com.ominous.tylerutils.util.StringUtils;

public class CurrentWeatherRemoteViews extends RemoteViews {
    public final Context context;

    public CurrentWeatherRemoteViews(Context context) {
        super(context.getPackageName(), R.layout.remoteviews_current);

        this.context = context;
    }

    public void update(WeatherDatabase.WeatherLocation weatherLocation, WeatherResponseOneCall responseOneCall) {
        setTextViewText(R.id.main_location, weatherLocation.isCurrentLocation ? context.getString(R.string.text_current_location) : weatherLocation.name);
        setTextViewText(R.id.current_main_temperature, WeatherUtils.getTemperatureString(responseOneCall.current.temp, 1));
        setTextViewText(R.id.main_description, StringUtils.capitalizeEachWord(WeatherUtils.getCurrentShortWeatherDesc(responseOneCall)));
        setImageViewBitmap(R.id.current_main_icon,
                BitmapUtils.drawableToBitmap(
                        ContextCompat.getDrawable(context, WeatherUtils.getIconFromCode(responseOneCall.current.weather[0].icon, responseOneCall.current.weather[0].id)),
                        ColorUtils.getNotificationTextColor(context).getDefaultColor() | 0xFF000000));
    }
}
