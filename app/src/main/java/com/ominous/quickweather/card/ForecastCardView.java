package com.ominous.quickweather.card;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.ominous.quickweather.R;
import com.ominous.quickweather.util.ColorUtils;
import com.ominous.quickweather.util.WeatherUtils;
import com.ominous.quickweather.weather.WeatherResponse;

import java.util.Calendar;
import java.util.Locale;

public class ForecastCardView extends BaseCardView {
    public ForecastCardView(Context context) {
        super(context);

        inflate(context, R.layout.card_forecast, this);
    }

    @Override
    public void update(WeatherResponse response, int position) {
        int day = position - (3 + (response.alerts == null ? 0 : response.alerts.length));

        WeatherResponse.DataPoint data = response.daily.data[day];

        TextView forecastTemperatureMin = findViewById(R.id.forecast_temperature_min);
        TextView forecastTemperatureMax = findViewById(R.id.forecast_temperature_max);
        TextView forecastTitle = findViewById(R.id.forecast_title);
        TextView forecastDescription = findViewById(R.id.forecast_desc);
        ImageView forecastIcon  = findViewById(R.id.forecast_icon);

        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.add(Calendar.DATE,day);

        forecastIcon            .setImageResource(WeatherUtils.getIconFromCode(data.icon));
        forecastIcon            .setContentDescription(data.summary);

        forecastTitle           .setText(day == 0 ? getContext().getString(R.string.text_today) : calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()));
        forecastTemperatureMax  .setText(WeatherUtils.getTemperatureString(data.temperatureMax,0));
        forecastTemperatureMax  .setTextColor(ColorUtils.getColorFromTemperature(data.temperatureMax,true));

        forecastTemperatureMin  .setText(WeatherUtils.getTemperatureString(data.temperatureMin,0));
        forecastTemperatureMin  .setTextColor(ColorUtils.getColorFromTemperature(data.temperatureMin,true));

        forecastIcon            .setImageResource(WeatherUtils.getIconFromCode(data.icon));
        forecastIcon            .setContentDescription(data.summary);
        forecastDescription     .setText(WeatherUtils.getCapitalizedWeather(data.summary));
    }

    @Override
    public void onClick(View v) {
        //Nothing
    }
}
