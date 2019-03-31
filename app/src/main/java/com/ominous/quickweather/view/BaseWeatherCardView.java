package com.ominous.quickweather.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.support.design.card.MaterialCardView;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.ominous.quickweather.R;
import com.ominous.quickweather.util.CustomTabs;
import com.ominous.quickweather.util.Weather;

public abstract class BaseWeatherCardView extends MaterialCardView implements View.OnTouchListener {
    static CustomTabs customTabs;
    private RippleDrawable background;

    public BaseWeatherCardView(Context context) {
        this(context, null, 0);
    }

    public BaseWeatherCardView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BaseWeatherCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, R.style.weather_card);

        if (customTabs == null) {
            customTabs = CustomTabs.getInstance(context);
        }

        background = new RippleDrawable(ColorStateList.valueOf(getResources().getColor(R.color.card_background_pressed)), new ColorDrawable(getContext().getResources().getColor(R.color.card_background)), getBackground());

        setOnTouchListener(this);
        setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setBackground(background);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                this.setPressed(true);
                background.setHotspot(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_UP:
                Uri uri = getUri();
                if (uri != null) {
                    customTabs.launch(getContext(), uri);
                    //customTabs.launch(getContext(), getContext().getResources().getColor(R.color.color_primary), uri);
                }
            case MotionEvent.ACTION_CANCEL:
                this.setPressed(false);
            default:
                break;
        }

        return true;
    }

    abstract protected Uri getUri();

    abstract public void update(Weather.WeatherResponse response, int position);
}
