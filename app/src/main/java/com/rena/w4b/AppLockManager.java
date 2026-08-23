package com.rena.w4b;

import android.app.Activity;
import android.os.Bundle;

/** Coordinates App Lock across Activities and process recreation. */
public final class AppLockManager {
    private static boolean initialized;
    private static int visibleAppActivities;
    private static boolean processSessionStarted;
    private static boolean lockUiShowing;
    private static boolean suppressNextResumeLock;
    private static android.content.Context applicationContext;

    private AppLockManager() {
    }

    public static synchronized void initialize(android.content.Context context) {
        if (initialized || context == null) {
            return;
        }

        android.content.Context appContext = context.getApplicationContext();
        if (!(appContext instanceof RenaApplication)) {
            return;
        }

        RenaApplication application = (RenaApplication) appContext;
        applicationContext = appContext;
        initialized = true;

        application.registerActivityLifecycleCallbacks(
                new android.app.Application.ActivityLifecycleCallbacks() {
                    @Override
                    public void onActivityCreated(Activity activity, Bundle state) {
                    }

                    @Override
                    public void onActivityStarted(Activity activity) {
                        if (isSplashActivity(activity)) {
                            return;
                        }

                        if (isLockScreen(activity)) {
                            markLockScreenShown();
                            return;
                        }

                        visibleAppActivities++;

                        if (isAppLockSettings(activity)) {
                            // App Lock settings are still inside the protected app.
                            // Never let time spent in settings count as background time.
                            persistBackgroundTimestamp(applicationContext, 0L);
                        }
                    }

                    @Override
                    public void onActivityResumed(Activity activity) {
                        if (isLockScreen(activity) ||
                                isSplashActivity(activity) ||
                                isAppLockSettings(activity)) {
                            return;
                        }

                        boolean coldStart = !processSessionStarted;
                        processSessionStarted = true;

                        if (suppressNextResumeLock) {
                            suppressNextResumeLock = false;
                            return;
                        }

                        maybeLaunchLock(activity, coldStart);
                    }

                    @Override
                    public void onActivityPaused(Activity activity) {
                    }

                    @Override
                    public void onActivityStopped(Activity activity) {
                        if (isSplashActivity(activity) || isLockScreen(activity)) {
                            return;
                        }

                        visibleAppActivities = Math.max(0, visibleAppActivities - 1);


                        // Background time starts only when the entire application
                        // has no visible Activity other than Splash/LockScreen.
                        if (visibleAppActivities == 0 && !lockUiShowing) {
                            recordBackgroundTimestamp(activity);
                        }
                    }

                    @Override
                    public void onActivitySaveInstanceState(
                            Activity activity,
                            Bundle state
                    ) {
                    }

                    @Override
                    public void onActivityDestroyed(Activity activity) {
                    }
                }
        );
    }

    public static synchronized void markSetupCompleted() {
        suppressNextResumeLock = true;
        persistBackgroundTimestamp(applicationContext, 0L);
    }

    public static synchronized void markLockScreenShown() {
        lockUiShowing = true;
    }

    public static synchronized void markLockScreenUnlocked() {
        lockUiShowing = false;
        persistBackgroundTimestamp(applicationContext, 0L);
    }

    public static boolean canUseFingerprint(android.content.Context context) {
        if (context == null || android.os.Build.VERSION.SDK_INT < 26) {
            return false;
        }

        try {
            android.hardware.fingerprint.FingerprintManager manager =
                    (android.hardware.fingerprint.FingerprintManager)
                            context.getSystemService(android.content.Context.FINGERPRINT_SERVICE);

            return manager != null &&
                    manager.isHardwareDetected() &&
                    manager.hasEnrolledFingerprints();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static synchronized void recordBackgroundTimestamp(
            android.content.Context context
    ) {
        SecureAppLockStore.State state =
                SecureAppLockStore.read(context);

        if (!state.enabled ||
                !SecureAppLockStore.hasCredentials(state)) {
            return;
        }

        // Wall-clock time is persistent across process death and device reboot.
        // The same clock is used for timeout comparison in maybeLaunchLock().
        state.lastBackgroundAtMillis = System.currentTimeMillis();
        SecureAppLockStore.write(context, state);
    }

    private static void persistBackgroundTimestamp(
            android.content.Context context,
            long timestamp
    ) {
        if (context == null) {
            return;
        }

        SecureAppLockStore.State state =
                SecureAppLockStore.read(context);
        if (!SecureAppLockStore.hasCredentials(state)) {
            return;
        }

        state.lastBackgroundAtMillis = Math.max(0L, timestamp);
        SecureAppLockStore.write(context, state);
    }

    private static void maybeLaunchLock(
            Activity activity,
            boolean coldStart
    ) {
        if (activity == null ||
                lockUiShowing ||
                isLockScreen(activity) ||
                isAppLockSettings(activity)) {
            return;
        }

        SecureAppLockStore.State state =
                SecureAppLockStore.read(activity);

        if (!state.enabled ||
                !SecureAppLockStore.hasCredentials(state)) {
            return;
        }

        long persistedBackground = state.lastBackgroundAtMillis;

        if (persistedBackground <= 0L) {
            if (coldStart) {
                launchLock(activity);
            }
            return;
        }

        long timeoutMillis =
                Math.max(0L, (long) state.timeoutSeconds) * 1000L;

        long now = System.currentTimeMillis();
        long elapsed = now >= persistedBackground
                ? now - persistedBackground
                : 0L;

        if (timeoutMillis == 0L || elapsed >= timeoutMillis) {
            launchLock(activity);
        }
    }

    private static void launchLock(Activity activity) {
        if (activity == null || lockUiShowing) {
            return;
        }

        lockUiShowing = true;
        try {
            android.content.Intent intent = new android.content.Intent(
                    activity,
                    LockScreenActivity.class
            );
            intent.addFlags(
                    android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP |
                            android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION
            );
            activity.startActivity(intent);
        } catch (Throwable ignored) {
            lockUiShowing = false;
        }
    }

    private static boolean isLockScreen(Activity activity) {
        return activity instanceof LockScreenActivity;
    }

    private static boolean isSplashActivity(Activity activity) {
        return activity instanceof SplashActivity;
    }

    private static boolean isAppLockSettings(Activity activity) {
        return activity instanceof AppLockActivity ||
                activity instanceof PinSetupActivity ||
                activity instanceof ChangePinActivity;
    }
}
