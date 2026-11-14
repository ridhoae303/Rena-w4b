package com.rena.w4b;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class RenaDialog extends Dialog {
    public static final int BUTTON_POSITIVE = DialogInterface.BUTTON_POSITIVE;
    public static final int BUTTON_NEGATIVE = DialogInterface.BUTTON_NEGATIVE;
    public static final int BUTTON_NEUTRAL = DialogInterface.BUTTON_NEUTRAL;

    private final Context ctx;
    private CharSequence title;
    private CharSequence message;
    private View customView;
    private View customTitle;
    private Drawable icon;
    private CharSequence positiveText;
    private CharSequence negativeText;
    private CharSequence neutralText;
    private OnClickListener positiveListener;
    private OnClickListener negativeListener;
    private OnClickListener neutralListener;
    private CharSequence[] items;
    private int checkedItem = -1;
    private boolean singleChoice;
    private OnClickListener itemListener;
    private Button positiveButton;
    private Button negativeButton;
    private Button neutralButton;
    private float dim = 0.5f;
    private boolean built;

    public interface OnClickListener extends DialogInterface.OnClickListener {
    }

    protected RenaDialog(Context context) {
        super(context);
        this.ctx = context;
    }

    public static class Builder {
        private final RenaDialog dialog;

        public Builder(Context context) {
            dialog = new RenaDialog(context);
        }

        public Builder setTitle(CharSequence value) {
            dialog.title = value;
            return this;
        }

        public Builder setTitle(int resId) {
            dialog.title = dialog.ctx.getString(resId);
            return this;
        }

        public Builder setMessage(CharSequence value) {
            dialog.message = value;
            return this;
        }

        public Builder setMessage(int resId) {
            dialog.message = dialog.ctx.getString(resId);
            return this;
        }

        public Builder setView(View view) {
            dialog.customView = view;
            return this;
        }

        public Builder setCustomTitle(View view) {
            dialog.customTitle = view;
            return this;
        }

        public Builder setIcon(Drawable drawable) {
            dialog.icon = drawable;
            return this;
        }

        public Builder setIcon(int resId) {
            try {
                dialog.icon = dialog.ctx.getResources().getDrawable(resId);
            } catch (Throwable ignored) {
            }
            return this;
        }

        public Builder setDim(float amount) {
            dialog.dim = amount;
            return this;
        }

        public Builder setPositiveButton(CharSequence text, DialogInterface.OnClickListener listener) {
            dialog.positiveText = text;
            dialog.positiveListener = wrap(listener);
            return this;
        }

        public Builder setPositiveButton(int resId, DialogInterface.OnClickListener listener) {
            return setPositiveButton(dialog.ctx.getString(resId), listener);
        }

        public Builder setNegativeButton(CharSequence text, DialogInterface.OnClickListener listener) {
            dialog.negativeText = text;
            dialog.negativeListener = wrap(listener);
            return this;
        }

        public Builder setNegativeButton(int resId, DialogInterface.OnClickListener listener) {
            return setNegativeButton(dialog.ctx.getString(resId), listener);
        }

        public Builder setNeutralButton(CharSequence text, DialogInterface.OnClickListener listener) {
            dialog.neutralText = text;
            dialog.neutralListener = wrap(listener);
            return this;
        }

        public Builder setNeutralButton(int resId, DialogInterface.OnClickListener listener) {
            return setNeutralButton(dialog.ctx.getString(resId), listener);
        }

        public Builder setItems(CharSequence[] items, DialogInterface.OnClickListener listener) {
            dialog.items = items;
            dialog.singleChoice = false;
            dialog.itemListener = wrap(listener);
            return this;
        }

        public Builder setSingleChoiceItems(CharSequence[] items, int checked, DialogInterface.OnClickListener listener) {
            dialog.items = items;
            dialog.checkedItem = checked;
            dialog.singleChoice = true;
            dialog.itemListener = wrap(listener);
            return this;
        }

        public Builder setCancelable(boolean cancelable) {
            dialog.setCancelable(cancelable);
            return this;
        }

        public Builder setOnCancelListener(DialogInterface.OnCancelListener listener) {
            dialog.setOnCancelListener(listener);
            return this;
        }

        public Builder setOnDismissListener(DialogInterface.OnDismissListener listener) {
            dialog.setOnDismissListener(listener);
            return this;
        }

        public RenaDialog create() {
            dialog.build();
            return dialog;
        }

        public RenaDialog show() {
            RenaDialog created = create();
            try {
                created.show();
            } catch (Throwable ignored) {
            }
            return created;
        }

        private static OnClickListener wrap(final DialogInterface.OnClickListener listener) {
            if (listener == null) {
                return null;
            }
            if (listener instanceof OnClickListener) {
                return (OnClickListener) listener;
            }
            return new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    listener.onClick(dialog, which);
                }
            };
        }
    }

    public Button getButton(int which) {
        if (!built) {
            build();
        }
        switch (which) {
            case BUTTON_POSITIVE:
                return positiveButton;
            case BUTTON_NEGATIVE:
                return negativeButton;
            default:
                return neutralButton;
        }
    }

    public void setMessage(CharSequence value) {
        message = value;
    }

    private int dp(float value) {
        return Ui.dp(ctx, value);
    }

    private Button makeButton(CharSequence text, boolean primary, final int which, final OnClickListener listener) {
        Button button = new Button(ctx);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(14.5f);
        button.setTypeface(Ui.font(ctx, primary ? Typeface.BOLD : Typeface.NORMAL));
        button.setTextColor(primary ? Color.WHITE : RenaTheme.muted());
        button.setMinWidth(dp(72));
        button.setMinimumWidth(dp(72));
        button.setMinHeight(dp(42));
        button.setMinimumHeight(dp(42));
        button.setPadding(dp(18), 0, dp(18), 0);
        button.setStateListAnimator(null);
        if (primary) {
            button.setBackground(Ui.ripple(ctx, Ui.round(RenaTheme.accent(), dp(21)), 21));
        } else {
            button.setBackground(Ui.ripple(ctx, Ui.round(Color.TRANSPARENT, dp(21)), 21));
        }
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (listener != null) {
                        listener.onClick(RenaDialog.this, which);
                    }
                } finally {
                    dismiss();
                }
            }
        });
        return button;
    }

    private void build() {
        if (built) {
            return;
        }
        built = true;
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        final LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(Ui.card(ctx, RenaTheme.sheet(), 26));
        card.setPadding(dp(22), dp(22), dp(22), dp(14));
        if (Build.VERSION.SDK_INT >= 21) {
            card.setElevation(dp(10));
        }

        if (customTitle != null) {
            card.addView(customTitle, new LinearLayout.LayoutParams(-1, -2));
        } else if (!TextUtils.isEmpty(title) || icon != null) {
            LinearLayout header = new LinearLayout(ctx);
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);
            if (icon != null) {
                ImageView iconView = new ImageView(ctx);
                iconView.setImageDrawable(icon);
                header.addView(iconView, new LinearLayout.LayoutParams(dp(30), dp(30)));
                ((LinearLayout.LayoutParams) iconView.getLayoutParams()).rightMargin = dp(12);
            }
            if (!TextUtils.isEmpty(title)) {
                TextView titleView = Ui.text(ctx, title, 19f, RenaTheme.text(), Typeface.BOLD);
                titleView.setLineSpacing(0f, 1.1f);
                header.addView(titleView, new LinearLayout.LayoutParams(0, -2, 1f));
            }
            card.addView(header, new LinearLayout.LayoutParams(-1, -2));
        }

        final LinearLayout body = new LinearLayout(ctx);
        body.setOrientation(LinearLayout.VERTICAL);
        if (!TextUtils.isEmpty(message)) {
            TextView messageView = Ui.text(ctx, message, 14.5f, RenaTheme.muted(), Typeface.NORMAL);
            messageView.setLineSpacing(0f, 1.22f);
            LinearLayout.LayoutParams messageLp = new LinearLayout.LayoutParams(-1, -2);
            messageLp.topMargin = dp(12);
            body.addView(messageView, messageLp);
        }
        if (customView != null) {
            ViewGroup parent = customView.getParent() instanceof ViewGroup ? (ViewGroup) customView.getParent() : null;
            if (parent != null) {
                parent.removeView(customView);
            }
            LinearLayout.LayoutParams viewLp = new LinearLayout.LayoutParams(-1, -2);
            viewLp.topMargin = dp(12);
            body.addView(customView, viewLp);
        }
        if (items != null) {
            LinearLayout list = new LinearLayout(ctx);
            list.setOrientation(LinearLayout.VERTICAL);
            for (int i = 0; i < items.length; i++) {
                final int index = i;
                LinearLayout row = new LinearLayout(ctx);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(12), 0, dp(12), 0);
                boolean selected = singleChoice && i == checkedItem;
                row.setBackground(Ui.ripple(ctx, Ui.round(selected ? RenaTheme.accentSoft() : Color.TRANSPARENT, dp(14)), 14));
                TextView label = Ui.text(ctx, items[i], 15f,
                        selected ? RenaTheme.accentText() : RenaTheme.text(),
                        selected ? Typeface.BOLD : Typeface.NORMAL);
                row.addView(label, new LinearLayout.LayoutParams(0, -2, 1f));
                if (singleChoice) {
                    TextView mark = Ui.text(ctx, selected ? "\u25CF" : "\u25CB", 16f,
                            selected ? RenaTheme.accentText() : RenaTheme.faint(), Typeface.NORMAL);
                    row.addView(mark, new LinearLayout.LayoutParams(-2, -2));
                }
                row.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            if (itemListener != null) {
                                itemListener.onClick(RenaDialog.this, index);
                            }
                        } finally {
                            dismiss();
                        }
                    }
                });
                LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, dp(50));
                rowLp.topMargin = dp(2);
                list.addView(row, rowLp);
            }
            LinearLayout.LayoutParams listLp = new LinearLayout.LayoutParams(-1, -2);
            listLp.topMargin = dp(10);
            body.addView(list, listLp);
        }

        if (body.getChildCount() > 0) {
            LimitedScroll scroll = new LimitedScroll(ctx);
            scroll.setVerticalScrollBarEnabled(false);
            scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
            scroll.addView(body, new FrameLayout.LayoutParams(-1, -2));
            card.addView(scroll, new LinearLayout.LayoutParams(-1, -2));
        }

        boolean hasButtons = !TextUtils.isEmpty(positiveText) || !TextUtils.isEmpty(negativeText) || !TextUtils.isEmpty(neutralText);
        if (hasButtons) {
            LinearLayout buttons = new LinearLayout(ctx);
            buttons.setOrientation(LinearLayout.HORIZONTAL);
            buttons.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
            if (!TextUtils.isEmpty(neutralText)) {
                neutralButton = makeButton(neutralText, false, BUTTON_NEUTRAL, neutralListener);
                buttons.addView(neutralButton, new LinearLayout.LayoutParams(-2, dp(42)));
                View filler = new View(ctx);
                buttons.addView(filler, new LinearLayout.LayoutParams(0, 1, 1f));
            }
            if (!TextUtils.isEmpty(negativeText)) {
                negativeButton = makeButton(negativeText, false, BUTTON_NEGATIVE, negativeListener);
                buttons.addView(negativeButton, new LinearLayout.LayoutParams(-2, dp(42)));
            }
            if (!TextUtils.isEmpty(positiveText)) {
                positiveButton = makeButton(positiveText, true, BUTTON_POSITIVE, positiveListener);
                LinearLayout.LayoutParams positiveLp = new LinearLayout.LayoutParams(-2, dp(42));
                positiveLp.leftMargin = dp(8);
                buttons.addView(positiveButton, positiveLp);
            }
            LinearLayout.LayoutParams buttonsLp = new LinearLayout.LayoutParams(-1, -2);
            buttonsLp.topMargin = dp(18);
            card.addView(buttons, buttonsLp);
        } else {
            card.setPadding(dp(22), dp(22), dp(22), dp(22));
        }

        FrameLayout holder = new FrameLayout(ctx);
        int screenWidth = ctx.getResources().getDisplayMetrics().widthPixels;
        int width = Math.min(screenWidth - dp(40), dp(420));
        holder.addView(card, new FrameLayout.LayoutParams(width, -2, Gravity.CENTER));
        setContentView(holder);

        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams attrs = window.getAttributes();
            attrs.dimAmount = dim;
            attrs.windowAnimations = R.style.RenaDialogAnim;
            attrs.gravity = Gravity.CENTER;
            window.setAttributes(attrs);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }

    private static final class LimitedScroll extends ScrollView {
        LimitedScroll(Context context) {
            super(context);
        }

        @Override
        protected void onMeasure(int widthSpec, int heightSpec) {
            int max = (int) (getResources().getDisplayMetrics().heightPixels * 0.58f);
            super.onMeasure(widthSpec, MeasureSpec.makeMeasureSpec(max, MeasureSpec.AT_MOST));
        }
    }
}
