package com.rena.w4b;

import android.app.Activity;
import android.os.Build;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

public final class RenaBack {
    private RenaBack() {
    }

    public static void register(final Activity activity) {
        if (Build.VERSION.SDK_INT < 33 || activity == null) {
            return;
        }
        try {
            if (activity.getClass().getMethod("onBackPressed").getDeclaringClass() == Activity.class) {
                return;
            }
            activity.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    new OnBackInvokedCallback() {
                        @Override
                        public void onBackInvoked() {
                            activity.onBackPressed();
                        }
                    }
            );
        } catch (Throwable ignored) {
        }
    }
}
