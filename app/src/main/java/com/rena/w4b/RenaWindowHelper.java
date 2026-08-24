package com.rena.w4b;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;

public final class RenaWindowHelper {
    private RenaWindowHelper() {
    }

    public static boolean isHideBarNotificationEnabled(Activity activity) {
        return RenaSettingsStore.getBoolean(
                activity,
                "hide_bar_notification",
                false
        );
    }

    public static void applyMainWindow(Activity activity) {
        Window window = activity.getWindow();

        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false);
        }

        if (Build.VERSION.SDK_INT < 21) {
            return;
        }

        int flags =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;

        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.BLACK);

        if (isHideBarNotificationEnabled(activity)) {
            flags |= View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }

        window.getDecorView().setSystemUiVisibility(flags);

        if (Build.VERSION.SDK_INT >= 30 &&
                window.getInsetsController() != null) {
            if (isHideBarNotificationEnabled(activity)) {
                window.getInsetsController().hide(
                        android.view.WindowInsets.Type.statusBars()
                );
            } else {
                window.getInsetsController().show(
                        android.view.WindowInsets.Type.statusBars()
                );
            }
        }
    }

    public static void applyIfEnabled(Activity activity) {
        if (!isHideBarNotificationEnabled(activity)) {
            return;
        }

        Window window = activity.getWindow();

        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false);
        }

        if (Build.VERSION.SDK_INT < 21) {
            return;
        }

        window.setStatusBarColor(Color.TRANSPARENT);

        int flags =
                window.getDecorView().getSystemUiVisibility() |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        window.getDecorView().setSystemUiVisibility(flags);

        if (Build.VERSION.SDK_INT >= 30 &&
                window.getInsetsController() != null) {
            window.getInsetsController().hide(
                    android.view.WindowInsets.Type.statusBars()
            );
        }
    }
}
