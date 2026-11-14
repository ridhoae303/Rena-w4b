package com.rena.w4b;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

public final class Ui {
    private Ui() {
    }

    public static int dp(Context context, float value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static Typeface font(Context context, int style) {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                int id = context.getResources().getIdentifier("font", "font", context.getPackageName());
                if (id != 0) {
                    return Typeface.create(context.getResources().getFont(id), style);
                }
            }
        } catch (Throwable ignored) {
        }
        return Typeface.create("sans-serif", style);
    }

    public static TextView text(Context context, CharSequence value, float sp, int color, int style) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(font(context, style));
        view.setIncludeFontPadding(false);
        return view;
    }

    public static TextView body(Context context, CharSequence value) {
        TextView view = text(context, value, 14f, RenaTheme.text(), Typeface.NORMAL);
        view.setLineSpacing(0f, 1.18f);
        return view;
    }

    public static TextView muted(Context context, CharSequence value, float sp) {
        return text(context, value, sp, RenaTheme.muted(), Typeface.NORMAL);
    }

    public static GradientDrawable round(int color, float radiusPx) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radiusPx);
        return drawable;
    }

    public static GradientDrawable roundStroke(int color, float radiusPx, int strokeColor, int strokePx) {
        GradientDrawable drawable = round(color, radiusPx);
        if (strokePx > 0) {
            drawable.setStroke(strokePx, strokeColor);
        }
        return drawable;
    }

    public static Drawable card(Context context, int fill, float radiusDp) {
        float radius = dp(context, radiusDp);
        int stroke = dp(context, RenaTheme.strokeWidthDp());
        return roundStroke(fill, radius, RenaTheme.outline(), stroke);
    }

    public static Drawable ripple(Context context, Drawable content, float radiusDp) {
        int rippleColor = RenaTheme.fg(40);
        GradientDrawable mask = round(Color.WHITE, dp(context, radiusDp));
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), content, mask);
    }

    public static Drawable pressable(Context context, int fill, float radiusDp) {
        return ripple(context, card(context, fill, radiusDp), radiusDp);
    }

    public static void press(final View view) {
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(70).start();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.animate().scaleX(1f).scaleY(1f).setDuration(110).start();
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
    }

    public static View divider(Context context) {
        View view = new View(context);
        view.setBackgroundColor(RenaTheme.outline());
        return view;
    }

    public static LinearLayout.LayoutParams lp(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    public static LinearLayout.LayoutParams lpWeight(int width, int height, float weight) {
        return new LinearLayout.LayoutParams(width, height, weight);
    }

    public static LinearLayout.LayoutParams margins(LinearLayout.LayoutParams params, int l, int t, int r, int b) {
        params.setMargins(l, t, r, b);
        return params;
    }

    public static Switch toggle(Context context, boolean checked) {
        Switch view = new Switch(context);
        view.setText("");
        view.setChecked(checked);
        int on = RenaTheme.accent();
        int offThumb = RenaTheme.isDark() ? Color.rgb(176, 190, 197) : Color.rgb(250, 250, 250);
        int offTrack = RenaTheme.isDark() ? Color.argb(110, 120, 144, 156) : Color.argb(120, 120, 134, 140);
        if (Build.VERSION.SDK_INT >= 23) {
            int[][] states = new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}};
            view.setThumbTintList(new ColorStateList(states, new int[]{on, offThumb}));
            view.setTrackTintList(new ColorStateList(states, new int[]{Color.argb(120, 0, 168, 132), offTrack}));
        }
        return view;
    }

    public static ImageView icon(Context context, int resId, int tint, int sizeDp) {
        ImageView view = new ImageView(context);
        view.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        if (resId != 0) {
            view.setImageResource(resId);
        }
        if (tint != 0) {
            view.setColorFilter(tint);
        }
        view.setLayoutParams(new ViewGroup.LayoutParams(dp(context, sizeDp), dp(context, sizeDp)));
        return view;
    }

    public static TextView sectionTitle(Context context, CharSequence title) {
        TextView view = text(context, title, 12.5f, RenaTheme.accentText(), Typeface.BOLD);
        view.setAllCaps(true);
        view.setLetterSpacing(0.06f);
        view.setPadding(dp(context, 6), 0, 0, 0);
        return view;
    }

    public static FrameLayout.LayoutParams frame(int width, int height, int gravity) {
        return new FrameLayout.LayoutParams(width, height, gravity);
    }

    public static Bitmap drawableToBitmap(Drawable drawable, int width, int height) {
        if (drawable == null) {
            return null;
        }
        Bitmap bitmap = Bitmap.createBitmap(Math.max(1, width), Math.max(1, height), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static void fadeIn(View view, long duration) {
        view.setAlpha(0f);
        view.animate().alpha(1f).setDuration(duration).start();
    }

    public static void slideUp(View view, long delay) {
        view.setAlpha(0f);
        view.setTranslationY(view.getResources().getDisplayMetrics().density * 14f);
        view.animate().alpha(1f).translationY(0f).setStartDelay(delay).setDuration(260).start();
    }

    public static ObjectAnimator spin(View view) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, View.ROTATION, 0f, 360f);
        animator.setDuration(900);
        animator.setRepeatCount(ObjectAnimator.INFINITE);
        animator.setInterpolator(new android.view.animation.LinearInterpolator());
        animator.start();
        return animator;
    }

    public static String ellipsize(String value, int max) {
        if (TextUtils.isEmpty(value) || value.length() <= max) {
            return value == null ? "" : value;
        }
        return value.substring(0, Math.max(1, max - 1)) + "\u2026";
    }

    public static View spacer(Context context, int heightDp) {
        View view = new View(context);
        view.setLayoutParams(new LinearLayout.LayoutParams(1, dp(context, heightDp)));
        return view;
    }

    public static Drawable transparent() {
        return new ColorDrawable(Color.TRANSPARENT);
    }

    public static int gravityCenter() {
        return Gravity.CENTER;
    }
}
