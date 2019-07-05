package com.ominous.quickweather.view;

import android.content.Context;
import android.net.Uri;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.view.View;

import androidx.appcompat.widget.AppCompatTextView;

import com.ominous.quickweather.util.CustomTabs;

public class LinkedTextView extends AppCompatTextView {
    private CustomTabs customTabs;

    public LinkedTextView(Context context) {
        this(context, null, 0);
    }

    public LinkedTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LinkedTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        customTabs = CustomTabs.getInstance(getContext());

        setMovementMethod(LinkMovementMethod.getInstance());

        Spanned currentText = (Spanned) getText();
        SpannableString text = new SpannableString(currentText.toString());

        for (URLSpan span : currentText.getSpans(0,currentText.length(),URLSpan.class)) {
            text.setSpan(new CustomTabsURLSpan(span.getURL()),currentText.getSpanStart(span),currentText.getSpanEnd(span), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        setText(text);
    }

    private class CustomTabsURLSpan extends URLSpan {
        CustomTabsURLSpan(String url) {
            super(url);
        }

        @Override
        public void onClick(View widget) {
            customTabs.launch(getContext(), Uri.parse(getURL()));
        }
    }
}