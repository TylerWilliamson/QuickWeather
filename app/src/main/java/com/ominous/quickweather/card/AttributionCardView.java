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
