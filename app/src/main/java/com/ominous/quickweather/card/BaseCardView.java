package com.ominous.quickweather.card;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.ominous.quickweather.R;
import com.ominous.quickweather.util.CustomTabs;
import com.ominous.quickweather.weather.Weather;

public abstract class BaseCardView extends MaterialCardView implements View.OnTouchListener {
    static CustomTabs customTabs;
    private RippleDrawable rippleDrawable;
    private final static int[] STATE_PRESSED = new int[]{android.R.attr.state_pressed, android.R.attr.state_enabled},
            STATE_DEFAULT = new int[]{};

    public BaseCardView(Context context) {
        this(context, null, 0);
    }

    public BaseCardView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BaseCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, R.style.weather_card);

        if (customTabs == null) {
            customTabs = CustomTabs.getInstance(context);
        }

        this.setOnTouchListener(this);
        this.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        this.setCardElevation(getContext().getResources().getDimension(R.dimen.margin_half));
        this.setRadius(getContext().getResources().getDimension(R.dimen.margin_half));

        //TODO MaterialCardView: Setting a custom background is not supported
        //Find a way to do this without Java reflection or setBackground
        setBackground(rippleDrawable = new RippleDrawable(ColorStateList.valueOf(getResources().getColor(R.color.card_background_pressed)), getBackground(), getBackground()));
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                rippleDrawable.setHotspot(event.getX(), event.getY());
                rippleDrawable.setState(STATE_PRESSED);
                break;
            case MotionEvent.ACTION_UP:
                Uri uri = getUri();
                if (uri != null) {
                    customTabs.launch(getContext(), uri);
                }
            case MotionEvent.ACTION_CANCEL:
                rippleDrawable.setState(STATE_DEFAULT);
            default:
                break;
        }

        return true;
    }

    abstract protected Uri getUri();

    abstract public void update(Weather.WeatherResponse response, int position);
}
