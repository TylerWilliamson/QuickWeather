package com.ominous.quickweather.card;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.ominous.quickweather.R;
import com.ominous.quickweather.util.WeatherUtils;
import com.ominous.quickweather.view.IconTextView;
import com.ominous.quickweather.weather.WeatherResponse;

import java.util.Locale;

public class CurrentCardView extends BaseCardView {
    private TextView currentTemperature,currentDescription;
    private IconTextView currentWind, currentRain,  currentHumidity, currentUVIndex, currentPressure, currentDewPoint;
    private ImageView currentIcon, currentExpand;
    private TableLayout additionalConditions;
    private FrameLayout additionalConditionsViewport;
    private int cardHeight = 0;
    private int additionalConditionsHeight = 0;
    private boolean additionalConditionsShown = false;

    public CurrentCardView(Context context) {
        super(context);

        inflate(context, R.layout.card_current, this);

        additionalConditionsViewport = findViewById(R.id.current_additional_conditions_viewport);

        currentTemperature    = findViewById(R.id.current_temperature);
        currentIcon           = findViewById(R.id.current_icon);
        currentDescription    = findViewById(R.id.current_description);
        additionalConditions  = findViewById(R.id.current_additional_conditions);
        currentExpand         = findViewById(R.id.current_expand);
        currentWind           = findViewById(R.id.current_wind);
        currentRain           = findViewById(R.id.current_rain);
        currentHumidity       = findViewById(R.id.current_humidity);
        currentPressure       = findViewById(R.id.current_pressure);
        currentUVIndex        = findViewById(R.id.current_uvindex);
        currentDewPoint       = findViewById(R.id.current_dewpoint);

        currentWind         .getImageView().setImageResource(R.drawable.wind);
        currentRain         .getImageView().setImageResource(R.drawable.cloud_rain);
        currentHumidity     .getImageView().setImageResource(R.drawable.wet);
        currentPressure     .getImageView().setImageResource(R.drawable.meter);
        currentUVIndex      .getImageView().setImageResource(R.drawable.sun);
        currentDewPoint     .getImageView().setImageResource(R.drawable.thermometer_25);

        currentWind         .getImageView().setContentDescription(context.getString(R.string.current_wind_desc));
        currentRain         .getImageView().setContentDescription(context.getString(R.string.current_precip_desc));
        currentHumidity     .getImageView().setContentDescription(context.getString(R.string.current_humidity_desc));
        currentPressure     .getImageView().setContentDescription(context.getString(R.string.current_pressure_desc));
        currentUVIndex      .getImageView().setContentDescription(context.getString(R.string.current_uvindex_desc));
        currentDewPoint     .getImageView().setContentDescription(context.getString(R.string.current_dewpoint_desc));
    }

    @Override
    public void update(WeatherResponse response, int position) {
        currentIcon         .setImageResource(WeatherUtils.getIconFromCode(response.currently.icon));
        currentIcon         .setContentDescription(response.currently.summary);
        currentTemperature  .setText(WeatherUtils.getTemperatureString(response.currently.temperature,1));
        currentDescription  .setText(WeatherUtils.getCapitalizedWeather(WeatherUtils.getLongWeatherDesc(response.currently)));

        currentWind         .getTextView().setText(WeatherUtils.getWindSpeedString(response.currently.windSpeed,response.currently.windBearing));
        currentRain         .getTextView().setText(WeatherUtils.getPrecipitationString(response.currently.precipIntensity,response.currently.precipType));
        currentUVIndex      .getTextView().setText(getContext().getString(R.string.format_uvi,response.currently.uvIndex));
        currentDewPoint     .getTextView().setText(getContext().getString(R.string.format_dewpoint, WeatherUtils.getTemperatureString(response.currently.dewPoint, 1)));
        currentHumidity     .getTextView().setText(getContext().getString(R.string.format_humidity, WeatherUtils.getPercentageString(response.currently.humidity)));
        currentPressure     .getTextView().setText(String.format(Locale.getDefault(),"%.1f hPa",response.currently.pressure));

        this.post(() -> {
            if (cardHeight == 0) {
                cardHeight = getMeasuredHeight() - additionalConditions.getMeasuredHeight();
                additionalConditionsHeight = additionalConditions.getMeasuredHeight();

                ViewGroup.LayoutParams params = additionalConditions.getLayoutParams();
                params.height = additionalConditionsHeight;
                additionalConditions.setLayoutParams(params);
            }

            this.doTranslate(true,0);
        });
    }

    @Override
    public void onClick(View v) {
        this.doTranslate(additionalConditionsShown,400);
    }

    private void doTranslate(boolean toClose, int duration) {
        additionalConditionsShown = !toClose;

        final RecyclerView.LayoutParams thisParams = (RecyclerView.LayoutParams) this.getLayoutParams();
        final ConstraintLayout.LayoutParams viewPortParams = (ConstraintLayout.LayoutParams) additionalConditionsViewport.getLayoutParams();

        ValueAnimator anim = ValueAnimator.ofFloat(toClose ? additionalConditionsHeight : 0, toClose ? 0 : additionalConditionsHeight);
        anim.setDuration(duration);
        anim.addUpdateListener(valueAnimator -> {
            float translate = (Float) valueAnimator.getAnimatedValue();
            int translateInt = (int) translate;

            currentExpand.setRotation(180 * (toClose ? 1 - valueAnimator.getAnimatedFraction() : valueAnimator.getAnimatedFraction()));

            additionalConditions.setTranslationY(translate - additionalConditionsHeight);

            thisParams.height = cardHeight + translateInt;
            this.setLayoutParams(thisParams);

            viewPortParams.height = translateInt;
            additionalConditionsViewport.setLayoutParams(viewPortParams);
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                currentExpand.setRotation(toClose ? 0 : 180);
            }
        });
        anim.start();
    }
}