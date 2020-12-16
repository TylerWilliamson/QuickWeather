package com.ominous.quickweather.dialog;

import android.content.Context;
import android.text.SpannableString;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.ContextCompat;

import com.ominous.quickweather.R;
import com.ominous.tylerutils.view.LinkedTextView;

import java.lang.ref.WeakReference;

public class TextDialog {
    private final WeakReference<Context> context;
    private final AlertDialog dialog;
    private final LinkedTextView textView;

    public TextDialog(Context context) {
        this.context = new WeakReference<>(context);

        textView = new LinkedTextView(new ContextThemeWrapper(context, R.style.textdialog_textview));

        int padding = context.getResources().getDimensionPixelSize(R.dimen.margin_standard);
        textView.setPadding(padding, padding, padding, padding);

        dialog = new AlertDialog.Builder(context)
                .setView(textView)
                .create();

        dialog.setOnShowListener(d -> {
            for (Button button : new Button[]{
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE),
                    dialog.getButton(AlertDialog.BUTTON_NEUTRAL),
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            }) {
                button.setTextColor(ContextCompat.getColor(context, R.color.color_accent_emphasis));
            }
        });
    }

    public TextDialog setButton(int which, String text, @Nullable Runnable onAcceptRunnable) {
        dialog.setButton(which, text, null, (d, w) -> {
            if (onAcceptRunnable != null) {
                onAcceptRunnable.run();
            }

            d.dismiss();
        });

        return this;
    }

    public TextDialog addCloseButton() {
        return setButton(AlertDialog.BUTTON_NEGATIVE, context.get().getString(R.string.text_dialog_close), null);
    }

    public TextDialog setContent(CharSequence content) {
        textView.setText(new SpannableString(content));

        return this;
    }

    public TextDialog setTitle(CharSequence title) {
        dialog.setTitle(title);

        return this;
    }

    public void show() {
        dialog.show();
    }
}
