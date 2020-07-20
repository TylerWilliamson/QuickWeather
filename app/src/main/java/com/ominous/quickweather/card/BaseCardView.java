package com.ominous.quickweather.card;

import android.content.Context;
import android.util.AttributeSet;

import com.ominous.quickweather.weather.WeatherResponse;
import com.ominous.tylerutils.browser.CustomTabs;

public abstract class BaseCardView extends com.ominous.tylerutils.card.BaseCardView {
    static CustomTabs customTabs;

    public BaseCardView(Context context) {
        this(context, null, 0);
    }

    public BaseCardView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BaseCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (customTabs == null) {
            customTabs = CustomTabs.getInstance(context);
        }
    }

    abstract public void update(WeatherResponse response, int position);
}