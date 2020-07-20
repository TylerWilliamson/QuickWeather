package com.ominous.quickweather.view;

import android.content.Context;
import android.net.Uri;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.view.View;

import androidx.appcompat.widget.AppCompatTextView;

import com.ominous.tylerutils.browser.CustomTabs;

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

        if (customTabs == null) {
            customTabs = CustomTabs.getInstance(getContext());
        }

        setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        SpannedString currentText = new SpannedString(text);
        SpannableString newText = new SpannableString(currentText.toString());

        for (URLSpan span : currentText.getSpans(0,currentText.length(),URLSpan.class)) {
            if (customTabs == null) {
                customTabs = CustomTabs.getInstance(getContext());
            }

            newText.setSpan(new CustomTabsURLSpan(customTabs, span.getURL()),currentText.getSpanStart(span),currentText.getSpanEnd(span), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        super.setText(newText, type);
    }

    private class CustomTabsURLSpan extends URLSpan {
        private CustomTabs customTabs;
        private Uri uri;

        CustomTabsURLSpan(CustomTabs customTabs, String url) {
            super(url);

            uri = Uri.parse(url);

            this.customTabs = customTabs;

            customTabs.addLikelyUris(uri);
        }

        @Override
        public void onClick(View widget) {
            customTabs.launch(getContext(), uri);
        }
    }
}