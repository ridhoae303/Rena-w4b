package com.rena.w4b;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;

public final class RenaTheme {
    public static final int LIGHT = 0;
    public static final int DARK = 1;
    public static final int AMOLED = 2;
    public static final int PURE_BLACK = 3;

    private static final String KEY = "app_theme";
    private static volatile int current = -1;

    private RenaTheme() {
    }

    public static int mode(Context context) {
        int value = current;
        if (value >= 0) {
            return value;
        }
        value = RenaSettingsStore.getInt(context, KEY, DARK);
        if (value < LIGHT || value > PURE_BLACK) {
            value = DARK;
        }
        current = value;
        return value;
    }

    public static void setMode(Context context, int mode) {
        current = mode;
        RenaSettingsStore.putInt(context, KEY, mode);
    }

    public static boolean isDark() {
        return current != LIGHT;
    }

    public static boolean isDark(Context context) {
        return mode(context) != LIGHT;
    }

    public static void init(Context context) {
        mode(context);
    }

    public static void apply(Activity activity) {
        activity.setTheme(mode(activity) == LIGHT ? R.style.AppThemeLight : R.style.AppTheme);
    }

    public static int bg() {
        switch (current) {
            case LIGHT:
                return Color.rgb(245, 247, 248);
            case AMOLED:
            case PURE_BLACK:
                return Color.BLACK;
            default:
                return Color.rgb(17, 27, 33);
        }
    }

    public static int surface() {
        switch (current) {
            case LIGHT:
                return Color.WHITE;
            case AMOLED:
                return Color.rgb(11, 15, 18);
            case PURE_BLACK:
                return Color.BLACK;
            default:
                return Color.rgb(29, 43, 51);
        }
    }

    public static int surface2() {
        switch (current) {
            case LIGHT:
                return Color.rgb(236, 241, 243);
            case AMOLED:
                return Color.rgb(20, 26, 30);
            case PURE_BLACK:
                return Color.BLACK;
            default:
                return Color.rgb(38, 56, 66);
        }
    }

    public static int sheet() {
        switch (current) {
            case LIGHT:
                return Color.WHITE;
            case AMOLED:
                return Color.rgb(8, 11, 13);
            case PURE_BLACK:
                return Color.BLACK;
            default:
                return Color.rgb(24, 36, 43);
        }
    }

    public static int outline() {
        switch (current) {
            case LIGHT:
                return Color.argb(34, 0, 0, 0);
            case PURE_BLACK:
                return Color.rgb(52, 52, 52);
            case AMOLED:
                return Color.argb(26, 255, 255, 255);
            default:
                return Color.argb(30, 255, 255, 255);
        }
    }

    public static int strokeWidthDp() {
        return current == PURE_BLACK ? 1 : 0;
    }

    public static int text() {
        switch (current) {
            case LIGHT:
                return Color.rgb(16, 27, 32);
            case PURE_BLACK:
                return Color.WHITE;
            default:
                return Color.rgb(236, 240, 242);
        }
    }

    public static int muted() {
        switch (current) {
            case LIGHT:
                return Color.rgb(93, 108, 116);
            case PURE_BLACK:
                return Color.rgb(176, 176, 176);
            default:
                return Color.rgb(160, 176, 184);
        }
    }

    public static int faint() {
        switch (current) {
            case LIGHT:
                return Color.rgb(140, 152, 158);
            default:
                return Color.rgb(112, 128, 136);
        }
    }

    public static int fg(int alpha) {
        if (current == PURE_BLACK && alpha <= 36) {
            return Color.TRANSPARENT;
        }
        return current == LIGHT ? Color.argb(alpha, 0, 0, 0) : Color.argb(alpha, 255, 255, 255);
    }

    public static int accent() {
        return Color.rgb(0, 168, 132);
    }

    public static int accentText() {
        return current == LIGHT ? Color.rgb(0, 128, 102) : Color.rgb(0, 220, 172);
    }

    public static int accentSoft() {
        return current == LIGHT ? Color.argb(34, 0, 168, 132) : Color.argb(52, 0, 168, 132);
    }

    public static int danger() {
        return current == LIGHT ? Color.rgb(207, 48, 48) : Color.rgb(255, 120, 120);
    }

    public static int dangerSoft() {
        return current == LIGHT ? Color.argb(30, 207, 48, 48) : Color.argb(40, 255, 90, 90);
    }

    public static int info() {
        return current == LIGHT ? Color.rgb(21, 101, 192) : Color.rgb(125, 200, 255);
    }

    public static int scrim(int alpha) {
        return Color.argb(alpha, 0, 0, 0);
    }

    public static String cssBackground() {
        switch (current) {
            case LIGHT:
                return "#f5f7f8";
            case AMOLED:
            case PURE_BLACK:
                return "#000000";
            default:
                return "#111b21";
        }
    }

    public static String cssSurface() {
        switch (current) {
            case LIGHT:
                return "#ffffff";
            case AMOLED:
                return "#0b0f12";
            case PURE_BLACK:
                return "#000000";
            default:
                return "#1d2b33";
        }
    }

    public static String cssText() {
        return current == LIGHT ? "#101b20" : "#ecf0f2";
    }

    public static String cssMuted() {
        return current == LIGHT ? "#5d6c74" : "#a0b0b8";
    }

    public static String cssOutline() {
        switch (current) {
            case LIGHT:
                return "rgba(0,0,0,.14)";
            case PURE_BLACK:
                return "#343434";
            default:
                return "rgba(255,255,255,.12)";
        }
    }

    public static void applyWindow(Activity activity) {
        if (activity == null) {
            return;
        }
        mode(activity);
        Window window = activity.getWindow();
        int background = bg();
        try {
            window.setStatusBarColor(background);
            window.setNavigationBarColor(background);
            int flags = window.getDecorView().getSystemUiVisibility();
            if (current == LIGHT) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                if (Build.VERSION.SDK_INT >= 26) {
                    flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
            } else {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                if (Build.VERSION.SDK_INT >= 26) {
                    flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
            }
            window.getDecorView().setSystemUiVisibility(flags);
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(background));
        } catch (Throwable ignored) {
        }
    }

    public static String name(Context context, int mode) {
        switch (mode) {
            case LIGHT:
                return context.getString(R.string.theme_light);
            case AMOLED:
                return context.getString(R.string.theme_amoled);
            case PURE_BLACK:
                return context.getString(R.string.theme_pure_black);
            default:
                return context.getString(R.string.theme_dark);
        }
    }
}
