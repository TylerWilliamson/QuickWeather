package com.ominous.quickweather.view;

import android.content.Context;
import android.net.Uri;
import android.widget.ImageView;
import android.widget.TextView;

import com.ominous.quickweather.R;
import com.ominous.quickweather.util.ViewUtils;
import com.ominous.quickweather.util.Weather;
import com.ominous.quickweather.util.WeatherUtils;

import java.util.Locale;

public class CurrentWeatherCardView extends BaseWeatherCardView {
    private static final String weatherUriFormat = "https://darksky.net/forecast/%1$f,%2$f/#header";
    private TextView currentTemperature, currentRain, currentDescription, currentWind, currentHumidity;
    private ImageView currentIcon;
    private Uri weatherUri = null;

    public CurrentWeatherCardView(Context context) {
        super(context);

        inflate(context, R.layout.card_current, this);

        currentTemperature  = findViewById(R.id.current_temperature);
        currentRain         = findViewById(R.id.current_rain);
        currentIcon         = findViewById(R.id.current_icon);
        currentDescription  = findViewById(R.id.current_description);
        currentWind         = findViewById(R.id.current_wind);
        currentHumidity     = findViewById(R.id.current_humidity);

        int textColor = context.getResources().getColor(R.color.text_primary_emphasis);

        ViewUtils.setDrawable(currentRain,R.drawable.raindrop,textColor,ViewUtils.FLAG_START);
        ViewUtils.setDrawable(currentWind,R.drawable.wind,textColor,ViewUtils.FLAG_START);
        ViewUtils.setDrawable(currentHumidity,R.drawable.wet,textColor,ViewUtils.FLAG_START);
    }

    @Override
    public void update(Weather.WeatherResponse response, int position) {
        updateUri(response);

        customTabs.addLikelyUris(getUri());

        currentTemperature  .setText(WeatherUtils.getTemperatureString(response.currently.temperature,1));
        currentWind         .setText(WeatherUtils.getWindSpeedString(response.currently.windSpeed,response.currently.windBearing));
        currentRain         .setText(WeatherUtils.getPercentageString(response.currently.precipProbability));
        currentHumidity     .setText(WeatherUtils.getPercentageString(response.currently.humidity));
        currentDescription  .setText(response.currently.summary);
        currentIcon         .setImageResource(WeatherUtils.getIconFromCode(response.currently.icon));
        currentIcon         .setContentDescription(response.currently.summary);
    }

    @Override
    protected Uri getUri() {
        return weatherUri;
    }

    private void updateUri(Weather.WeatherResponse response) {
        weatherUri = Uri.parse(String.format(Locale.US, weatherUriFormat, response.latitude, response.longitude));
    }
}
