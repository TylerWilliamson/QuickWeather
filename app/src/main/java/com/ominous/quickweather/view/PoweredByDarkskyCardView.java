package com.ominous.quickweather.view;

import android.content.Context;
import android.net.Uri;

import com.ominous.quickweather.R;
import com.ominous.quickweather.util.Weather;

public class PoweredByDarkskyCardView extends BaseWeatherCardView {
    private static final Uri poweredByDarkskyUri = Uri.parse("https://darksky.net/poweredby/");

    public PoweredByDarkskyCardView(Context context) {
        super(context);

        inflate(context,R.layout.card_powered_by_darksky,this);

        customTabs.addLikelyUris(poweredByDarkskyUri);
    }

    @Override
    protected Uri getUri() {
        return poweredByDarkskyUri;
    }

    @Override
    public void update(Weather.WeatherResponse response, int position) {

    }
}
