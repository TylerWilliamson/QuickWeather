package com.ominous.quickweather.util;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import java.util.Locale;

public class ViewUtils {
    public static final int FLAG_START = 1, FLAG_END = 2, FLAG_TOP = 4, FLAG_BOTTOM = 8;

    //Setting DrawableStart in XML does not work for these Vectors
    public static void setDrawable(TextView textview, int drawableRes, int color, int flagSide) {
        Drawable drawableStart,
                drawableEnd,
                drawableTop,
                drawableBottom,
                drawable = textview.getContext().getDrawable(drawableRes);

        if (drawable != null) {
            int size = (int) textview.getTextSize();
            drawable.setBounds(0, 0, size, size);

            drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);

            Drawable[] currentDrawables = textview.getCompoundDrawables();

            drawableStart = ((flagSide & FLAG_START) > 0) ? drawable : currentDrawables[0];
            drawableTop = ((flagSide & FLAG_TOP) > 0) ? drawable : currentDrawables[1];
            drawableEnd = ((flagSide & FLAG_END) > 0) ? drawable : currentDrawables[2];
            drawableBottom = ((flagSide & FLAG_BOTTOM) > 0) ? drawable : currentDrawables[3];

            if (TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == View.LAYOUT_DIRECTION_LTR) {
                textview.setCompoundDrawables(drawableStart,drawableTop,drawableEnd,drawableBottom);
            } else {
                textview.setCompoundDrawables(drawableEnd,drawableTop,drawableStart,drawableBottom);
            }
        }
    }

    public static void toggleKeyboardState(View v, boolean open) {
        InputMethodManager inputMethodManager = ContextCompat.getSystemService(v.getContext(),InputMethodManager.class);

        if (inputMethodManager != null) {
            if (open) {
                inputMethodManager.showSoftInput(v, InputMethodManager.SHOW_FORCED);
            } else {
                inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        }
    }
}
