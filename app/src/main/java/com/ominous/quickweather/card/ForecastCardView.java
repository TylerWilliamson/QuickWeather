package com.ominous.quickweather.card;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.ominous.quickweather.R;
import com.ominous.quickweather.util.ViewUtils;
import com.ominous.quickweather.weather.Weather;
import com.ominous.quickweather.util.WeatherUtils;

import java.util.Calendar;
import java.util.Locale;

public class ForecastCardView extends BaseCardView {
    private static final String weatherUriFormat = "https://darksky.net/forecast/%1$f,%2$f/#week";
    private Uri weatherUri = null;
    private LinearLayout layoutContainer;

    public ForecastCardView(Context context) {
        super(context);

        layoutContainer = inflate(context, R.layout.card_forecast, this).findViewById(R.id.forecast_container);
    }

    @Override
    protected Uri getUri() {
        return weatherUri;
    }



    @Override
    public void update(Weather.WeatherResponse response, int position) {
        updateUri(response);

        customTabs.addLikelyUris(getUri());

        View container;
        ImageView forecastIcon;
        TextView forecastTemperatureMin, forecastTemperatureMax, forecastRain, forecastTitle;
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT,1);

        layoutContainer.removeAllViews();

        for (int i=0,l=response.daily.data.length;i<l && i<5;i++) {
            container               = inflate(getContext(), R.layout.item_forecast, null);

            forecastTemperatureMin  = container.findViewById(R.id.forecast_temperature_min);
            forecastTemperatureMax  = container.findViewById(R.id.forecast_temperature_max);
            forecastTitle           = container.findViewById(R.id.forecast_title);
            forecastRain            = container.findViewById(R.id.forecast_rain);
            forecastIcon            = container.findViewById(R.id.forecast_icon);

            forecastTitle           .setText(i == 0 ? getContext().getString(R.string.text_today) : calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()));
            forecastTemperatureMax  .setText(WeatherUtils.getTemperatureString(response.daily.data[i].temperatureMax,0));
            forecastTemperatureMin  .setText(WeatherUtils.getTemperatureString(response.daily.data[i].temperatureMin,0));
            forecastRain            .setText(WeatherUtils.getPercentageString(response.daily.data[i].precipProbability));
            forecastIcon            .setImageResource(WeatherUtils.getIconFromCode(response.daily.data[i].icon));
            forecastIcon            .setContentDescription(response.daily.data[i].summary);

            int textColor = ContextCompat.getColor(getContext(),R.color.text_primary_emphasis);

            ViewUtils.setDrawable(forecastTemperatureMin,R.drawable.thermometer_25,textColor,ViewUtils.FLAG_START);
            ViewUtils.setDrawable(forecastTemperatureMax,R.drawable.thermometer_100,textColor,ViewUtils.FLAG_START);
            ViewUtils.setDrawable(forecastRain,R.drawable.raindrop,textColor,ViewUtils.FLAG_START);

            layoutContainer.addView(container,layoutParams);

            calendar.add(Calendar.DATE,1);
        }
    }

    private void updateUri(Weather.WeatherResponse response) {
        weatherUri = Uri.parse(String.format(Locale.US,weatherUriFormat,response.latitude,response.longitude));
    }
}
