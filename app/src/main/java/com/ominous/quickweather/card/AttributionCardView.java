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

package com.ominous.quickweather.card;

import android.content.Context;
import android.net.Uri;
import android.view.View;

import com.ominous.quickweather.R;
import com.ominous.quickweather.weather.WeatherResponse;

public class AttributionCardView extends BaseCardView {
    private static final Uri poweredByDarkskyUri = Uri.parse("https://darksky.net/poweredby/");

    public AttributionCardView(Context context) {
        super(context);

        inflate(context,R.layout.card_powered_by_darksky,this);

        customTabs.addLikelyUris(poweredByDarkskyUri);
    }

    @Override
    public void update(WeatherResponse response, int position) {

    }

    @Override
    public void onClick(View v) {
        customTabs.launch(getContext(),poweredByDarkskyUri);
    }
}
