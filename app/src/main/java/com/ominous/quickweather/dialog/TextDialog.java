package com.ominous.quickweather.dialog;

import android.content.Context;
import android.text.SpannableString;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.ContextCompat;

import com.ominous.quickweather.R;
import com.ominous.quickweather.view.LinkedTextView;

public class TextDialog {
    private AlertDialog dialog;
    private LinkedTextView textView;

    public TextDialog(Context context) {
        textView = new LinkedTextView(new ContextThemeWrapper(context,R.style.textdialog_textview));

        int padding = context.getResources().getDimensionPixelSize(R.dimen.margin_standard);
        textView.setPadding(padding,padding,padding,padding);

        dialog = new AlertDialog.Builder(context)
                .setCancelable(true)
                .setNegativeButton(R.string.text_dialog_close, (dialog, which) -> dialog.dismiss())
                .setView(textView)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(context, R.color.color_accent_emphasis)));
    }

    public void show(String title, SpannableString content) {
        textView.setText(content);

        dialog.setTitle(title);

        dialog.show();
    }
}
