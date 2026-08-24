package com.rena.w4b;

import android.content.Context;
import android.content.pm.ApplicationInfo;

/**
 * Rena W4B's concrete Application implementation.
 *
 * The integrity checks require this exact class to be declared by the
 * AndroidManifest and to extend android.app.Application directly.
 */
public final class RenaApplication extends android.app.Application {

    @Override
    public void onCreate() {
        super.onCreate();

        registerActivityLifecycleCallbacks(
                new android.app.Application.ActivityLifecycleCallbacks() {
                    @Override
                    public void onActivityCreated(
                            android.app.Activity activity,
                            android.os.Bundle state
                    ) {
                        RenaWindowHelper.applyIfEnabled(activity);
                    }

                    @Override
                    public void onActivityStarted(
                            android.app.Activity activity
                    ) {
                        RenaWindowHelper.applyIfEnabled(activity);
                    }

                    @Override
                    public void onActivityResumed(
                            android.app.Activity activity
                    ) {
                        RenaWindowHelper.applyIfEnabled(activity);
                    }

                    @Override
                    public void onActivityPaused(
                            android.app.Activity activity
                    ) {
                    }

                    @Override
                    public void onActivityStopped(
                            android.app.Activity activity
                    ) {
                    }

                    @Override
                    public void onActivitySaveInstanceState(
                            android.app.Activity activity,
                            android.os.Bundle state
                    ) {
                    }

                    @Override
                    public void onActivityDestroyed(
                            android.app.Activity activity
                    ) {
                    }
                }
        );
    }

    public static boolean isApplicationIntegrityValid(Context context) {
        if (context == null) {
            return false;
        }

        try {
            Context applicationContext = context.getApplicationContext();
            if (!(applicationContext instanceof RenaApplication)) {
                return false;
            }

            if (applicationContext.getClass() != RenaApplication.class) {
                return false;
            }

            if (RenaApplication.class.getSuperclass() != android.app.Application.class) {
                return false;
            }

            ApplicationInfo info =
                    context.getPackageManager().getApplicationInfo(
                            context.getPackageName(),
                            0
                    );

            return info != null &&
                    RenaApplication.class.getName().equals(info.className);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
