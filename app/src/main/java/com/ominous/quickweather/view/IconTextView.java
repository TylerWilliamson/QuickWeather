package com.ominous.quickweather.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.ominous.quickweather.R;

import androidx.annotation.Nullable;

public class IconTextView extends FrameLayout {
    private TextView textView;
    private ImageView imageView;

    public IconTextView(Context context) {
        this(context, null, 0,0);
    }

    public IconTextView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0,0);
    }

    public IconTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr,0);
    }

    public IconTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        LayoutInflater.from(context).inflate(R.layout.view_icontextview,this,true);

        textView = findViewById(R.id.text);
        imageView = findViewById(R.id.icon);
    }

    public TextView getTextView() {
        return textView;
    }

    public ImageView getImageView() {
        return imageView;
    }
}
