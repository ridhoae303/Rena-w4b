package com.rena.w4b;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.app.ActivityManager;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ConfigurationInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.LinearGradient;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ScaleGestureDetector;
import android.view.GestureDetector;
import android.view.WindowManager;
import android.view.WindowInsets;
import android.view.animation.LinearInterpolator;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.JavascriptInterface;
import android.webkit.WebViewClient;
import android.webkit.URLUtil;
import android.widget.CheckBox;
import android.widget.Switch;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.webkit.Profile;
import androidx.webkit.ProfileStore;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import android.widget.Toast;

import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import android.content.pm.Signature;
import java.util.concurrent.atomic.AtomicBoolean;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    private volatile boolean activityDestroyed;

    private static final int REQ_STARTUP_PERMISSIONS = 7101;
    private static final int REQ_MEDIA_PERMISSION = 7102;
    private static final int PICK_FILE = 7103;
    private static final int REQ_DATA_MANAGER = 7104;

    private FrameLayout root;
    private WebView webView;
    private Profile webProfile;
    private boolean multiProfileSupported;
    private FrameLayout drawer;
    private View drawerScrim;
    private ImageButton menuButton;
    private ProgressBar progress;
    private ImageView updateIconView;
    private ImageView refreshIconView;
    private MarqueeTextView footerView;
    private TextView loadingLabel;

    private boolean drawerOpen = false;
    private boolean drawerAnimating = false;
    private int drawerAnimationGeneration = 0;
    private boolean hideThreeDot = false;
    private boolean hideWarningShown = false;
    private boolean hideNotifications = false;
    private boolean reduceAnimations = false;
    private boolean historyFloorApplied = false;
    private boolean exitingApp = false;
    private float menuRelX = 0.96f;
    private float menuRelY = 0.06f;
    private float menuDragStartX;
    private float menuDragStartY;
    private boolean menuDragging = false;
    private float shakeLastX;
    private float shakeLastY;
    private float shakeLastZ;
    private long shakeLastTime;
    private long shakeCooldownUntil;
    private int shakePeakCount = 0;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private boolean shakeRegistered = false;
    private boolean zoomEnabled = false;
    private int drawerWidthPx = 0;
    private ImageView appIconView;

    private int appIconTapCount = 0;
    private long appIconLastTap = 0L;
    private static final long EASTER_TAP_WINDOW_MS = 4000L;

    private FrameLayout renaPreviewOverlay;
    private View renaPreviewScrim;
    private ZoomImageView renaPreviewImage;
    private AsyncTask<Void, Void, Bitmap> renaImageTask;
    private int renaPreviewRetryCount;
    private boolean webStateRestored = false;
    private int touchSlop = 0;

    private ValueCallback<Uri[]> pendingFileCallback;
    private PermissionRequest pendingWebPermission;
    private boolean pendingWebNeedCamera;
    private boolean pendingWebNeedMicrophone;

    private float baselineScale = 1.0f;
    private boolean baselineScaleCaptured = false;
    private final AtomicBoolean updaterRunning = new AtomicBoolean(false);
    private boolean pendingUpdatePermissionCheck = false;
    private boolean updateCheckRunning = false;
    private String pendingUpdateApkUrl;
    private String pendingUpdateDigest;
    private int pendingStartupPermissionAfterSettings = -1;
    private int pendingSpecialPermissionStage = -1;
    private int activePermissionIndex = -1;
    private AlertDialog activePermissionDialog;
    private AlertDialog activeSpecialDialog;
    private boolean permissionFlowStarted = false;
    private android.animation.ValueAnimator switchDrawerAnimator;
    private boolean specialSettingsInFlight = false;
    private int specialSettingsStage = -1;
    private boolean specialDialogCancelled = false;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback connectivityCallback;
    private boolean internetAvailable = true;
    private boolean runtimeOfflineToastShown = false;
    private boolean slowConnectionToastShown = false;
    private boolean pageLoading = false;
    private final android.os.Handler networkHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable slowConnectionRunnable;
    private boolean startupInternetGateShowing = false;
    private boolean permissionFlowDeferred = false;

    private static final String NOTIFICATION_CHANNEL_ID =
            "rena_whatsapp_web";
    private static final String NOTIFICATION_SILENT_CHANNEL_ID =
            "rena_whatsapp_web_silent";
    private static final String NOTIFICATION_ACTION_OPEN =
            "com.rena.w4b.NOTIFICATION_OPEN";
    private static final String NOTIFICATION_ACTION_REPLY =
            "com.rena.w4b.NOTIFICATION_REPLY";
    private static final String NOTIFICATION_ACTION_READ =
            "com.rena.w4b.NOTIFICATION_READ";
    private static final String NOTIFICATION_ACTION_IGNORE =
            "com.rena.w4b.NOTIFICATION_IGNORE";
    private static final String NOTIFICATION_EXTRA_TAB_ID =
            "tab_id";
    private static final String NOTIFICATION_EXTRA_CHAT =
            "chat";
    private static final String NOTIFICATION_EXTRA_BODY =
            "body";
    private static final String NOTIFICATION_EXTRA_KEY =
            "notification_key";
    private static final String NOTIFICATION_EXTRA_REPLY =
            "reply";

    private static final String SPECIAL_PROMPT_STORAGE =
            "special_prompt_storage_v1";
    private static final String SPECIAL_PROMPT_BACKGROUND =
            "special_prompt_background_v2";
    private static final String SPECIAL_PROMPT_BATTERY =
            "special_prompt_battery_v2";
    private static final String REMOTE_INPUT_REPLY =
            "remote_reply";


    private String pendingWebDownloadUrl;
    private String pendingWebDownloadUserAgent;
    private String pendingWebDownloadContentDisposition;
    private String pendingWebDownloadMimeType;

    private boolean dataManagerOpening = false;
    private int lastRootWidth = -1;
    private int lastRootHeight = -1;
    private final ArrayList<TabState> tabs = new ArrayList<TabState>();
    private int activeTabIndex = 0;
    private LinearLayout tabMenuContainer;

    private static class TabState {
        int id;
        String url;
        String profileId;
        WebView view;
    }
    private static class ApkAssetInfo {
        String url;
        String digest;
    }


    private int detectedGlesMajor = 2;

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String value, float sp, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);

        t.setTypeface(getAppFont(android.graphics.Typeface.NORMAL));

        return t;
    }

    private android.graphics.Typeface getAppFont(int style) {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                int fontId = getResources().getIdentifier(
                        "font", "font", getPackageName());
                if (fontId != 0) {
                    android.graphics.Typeface base = getResources().getFont(fontId);
                    return android.graphics.Typeface.create(base, style);
                }
            }
        } catch (Throwable ignored) {
        }
        return android.graphics.Typeface.create("sans-serif", style);
    }

    private android.graphics.Typeface getDeveloperFont(int style) {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                int fontId = getResources().getIdentifier(
                        "ridhoae303", "font", getPackageName());
                if (fontId != 0) {
                    android.graphics.Typeface base = getResources().getFont(fontId);
                    return android.graphics.Typeface.create(base, style);
                }
            }
        } catch (Throwable ignored) {
        }
        return android.graphics.Typeface.create("sans-serif", style);
    }

    private int currentTabId() {
        if (tabs.isEmpty()) {
            return 1;
        }
        if (activeTabIndex < 0 || activeTabIndex >= tabs.size()) {
            activeTabIndex = 0;
        }
        return Math.max(1, tabs.get(activeTabIndex).id);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        applyImmersiveFullscreen();


        try {
            multiProfileSupported =
                    WebViewFeature.isFeatureSupported(
                            WebViewFeature.MULTI_PROFILE
                    );
        } catch (Throwable ignored) {
            multiProfileSupported = false;
        }

        menuRelX = RenaSettingsStore.getFloat(
                this, "menu_rel_x", 0.96f
        );
        menuRelY = RenaSettingsStore.getFloat(
                this, "menu_rel_y", 0.06f
        );
        zoomEnabled = RenaSettingsStore.getBoolean(
                this, "zoom_enabled", false
        );
        hideThreeDot = RenaSettingsStore.getBoolean(
                this, "hide_three_dot", false
        );
        hideWarningShown = RenaSettingsStore.getBoolean(
                this, "hide_warning_shown", false
        );
        hideNotifications = RenaSettingsStore.getBoolean(
                this, "hide_notifications", false
        );
        reduceAnimations = RenaSettingsStore.getBoolean(
                this, "reduce_animation", false
        );
        restoreTabMetadata();

        if (!NativeConfig.isNativeAvailable() || !integrityGate()) {
            // A failed integrity check deserves the same calm dialog style as
            // the rest of the app, then the process is closed cleanly.
            if (NativeConfig.isNativeAvailable()) {
                showIntegrityFailureDialog();
            } else {
                closeAppBySystem();
            }
            return;
        }

        AppLockManager.initialize(getApplication());
        buildUi();

        touchSlop = android.view.ViewConfiguration
                .get(this)
                .getScaledTouchSlop();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer =
                    sensorManager.getDefaultSensor(
                            Sensor.TYPE_ACCELEROMETER
                    );
        }

        ensureNotificationChannels();
        prepareRenaDirectories();

        registerInternetMonitor();

        final Bundle restoreState = state;

        root.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        if (!isUiAlive()) {
                            return;
                        }
                        presentStartupInternetGate(restoreState);
                    }
                },
                90L
        );

        root.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        if (!isUiAlive()) {
                            return;
                        }
                        startPermissionFlow();
                    }
                },
                140L
        );
    }

    /*
     * Request everything that can reasonably be requested at startup.
     *
     * VIBRATE does not need a runtime dialog.
     * READ_EXTERNAL_STORAGE is requested only on Android 9 and below.
     * POST_NOTIFICATIONS exists as a runtime permission from Android 13.
     */
    private boolean wasSpecialPromptShown(String key) {
        return RenaSettingsStore.getBoolean(this, key, false);
    }

    private void markSpecialPromptShown(String key) {
        RenaSettingsStore.putBoolean(this, key, true);
    }

    private void startPermissionFlow() {
        if (permissionFlowStarted) {
            return;
        }

        if (startupInternetGateShowing) {
            permissionFlowDeferred = true;
            return;
        }

        permissionFlowStarted = true;

        if (Build.VERSION.SDK_INT < 23) {
            continueSpecialStartupFlow();
            return;
        }

        showNextPermissionDialog(0);
    }

    private void continueSpecialStartupFlow() {
        /*
         * These special settings are optional recommendations. The actual
         * state is checked every time, but a declined prompt is never forced
         * again on every application launch.
         */
        if (Build.VERSION.SDK_INT >= 30
                && !hasAllFilesAccess()
                && !wasSpecialPromptShown(SPECIAL_PROMPT_STORAGE)) {
            showSpecialStorageDialog();
            return;
        }

        if (Build.VERSION.SDK_INT >= 28
                && isBackgroundRestricted()
                && !wasSpecialPromptShown(SPECIAL_PROMPT_BACKGROUND)) {
            showBackgroundAccessDialog();
            return;
        }

        if (Build.VERSION.SDK_INT >= 23
                && !isIgnoringBatteryOptimizations()
                && !wasSpecialPromptShown(SPECIAL_PROMPT_BATTERY)) {
            showBatteryOptimizationDialog();
        }
    }

    private boolean isIgnoringBatteryOptimizations() {
        if (Build.VERSION.SDK_INT < 23) return true;
        try {
            android.os.PowerManager pm = (android.os.PowerManager) getSystemService(POWER_SERVICE);
            return pm == null || pm.isIgnoringBatteryOptimizations(getPackageName());
        } catch (Throwable ignored) {
            return true;
        }
    }

    private void dismissActiveSpecialDialog() {
        if (activeSpecialDialog != null) {
            try {
                activeSpecialDialog.setOnDismissListener(null);
                activeSpecialDialog.dismiss();
            } catch (Throwable ignored) {
            }
            activeSpecialDialog = null;
        }
    }

    private void markSpecialSettingsOpened(int stage) {
        specialSettingsInFlight = true;
        specialSettingsStage = stage;
        dismissActiveSpecialDialog();
    }

    private void showSpecialStorageDialog() {
        markSpecialPromptShown(SPECIAL_PROMPT_STORAGE);
        if (Build.VERSION.SDK_INT < 30 || hasAllFilesAccess()) {
            specialSettingsInFlight = false;
            specialSettingsStage = -1;
            continueSpecialStartupFlow();
            return;
        }

        if (activeSpecialDialog != null && activeSpecialDialog.isShowing()) {
            return;
        }

        specialDialogCancelled = false;

        startupInternetGateShowing = true;

        final AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(NativeConfig.storageAccessTitle())
                        .setMessage(NativeConfig.storageAccessMessage())
                        .setNegativeButton(
                                NativeConfig.notNowText(),
                                new android.content.DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(
                                            android.content.DialogInterface dialog,
                                            int which
                                    ) {
                                        specialDialogCancelled = true;
                                        specialSettingsInFlight = false;
                                        specialSettingsStage = -1;
                                        dismissActiveSpecialDialog();
                                    }
                                }
                        )
                        .setPositiveButton(
                                NativeConfig.storageSettingsButton(),
                                null
                        )
                        .create();

        activeSpecialDialog = dialog;
        dialog.setOnShowListener(new android.content.DialogInterface.OnShowListener() {
            @Override
            public void onShow(android.content.DialogInterface d) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                try {
                                    markSpecialSettingsOpened(0);
                                    startActivity(new Intent(
                                            android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                            Uri.parse("package:" + getPackageName())
                                    ));
                                } catch (Throwable firstFailure) {
                                    try {
                                        markSpecialSettingsOpened(0);
                                        startActivity(new Intent(
                                                android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                                        ));
                                    } catch (Throwable ignored) {
                                        specialSettingsInFlight = false;
                                        specialSettingsStage = -1;
                                    }
                                }
                            }
                        });
            }
        });
        dialog.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(android.content.DialogInterface d) {
                if (activeSpecialDialog == dialog) {
                    activeSpecialDialog = null;
                }

                if (specialDialogCancelled) {
                    specialDialogCancelled = false;
                    return;
                }

                if (!specialSettingsInFlight) {
                    continueSpecialStartupFlow();
                }
            }
        });
        dialog.show();
    }

    private boolean isBackgroundRestricted() {
        if (Build.VERSION.SDK_INT < 28) return false;
        try {
            ActivityManager manager =
                    (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            return manager != null && manager.isBackgroundRestricted();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void showBackgroundAccessDialog() {
        markSpecialPromptShown(SPECIAL_PROMPT_BACKGROUND);
        if (Build.VERSION.SDK_INT < 28 || !isBackgroundRestricted()) {
            specialSettingsInFlight = false;
            specialSettingsStage = -1;
            continueSpecialStartupFlow();
            return;
        }

        if (activeSpecialDialog != null && activeSpecialDialog.isShowing()) {
            return;
        }

        specialDialogCancelled = false;

        final AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(NativeConfig.backgroundAccessTitle())
                        .setMessage(NativeConfig.backgroundAccessMessage())
                        .setNegativeButton(
                                NativeConfig.notNowText(),
                                new android.content.DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(
                                            android.content.DialogInterface dialog,
                                            int which
                                    ) {
                                        specialDialogCancelled = true;
                                        specialSettingsInFlight = false;
                                        specialSettingsStage = -1;
                                        dismissActiveSpecialDialog();
                                    }
                                }
                        )
                        .setPositiveButton(
                                NativeConfig.storageSettingsButton(),
                                null
                        )
                        .create();

        activeSpecialDialog = dialog;
        dialog.setOnShowListener(new android.content.DialogInterface.OnShowListener() {
            @Override
            public void onShow(android.content.DialogInterface d) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                try {
                                    markSpecialSettingsOpened(1);
                                    Intent intent =
                                            new Intent(
                                                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                    Uri.parse("package:" + getPackageName())
                                            );
                                    startActivity(intent);
                                } catch (Throwable ignored) {
                                    specialSettingsInFlight = false;
                                    specialSettingsStage = -1;
                                }
                            }
                        });
            }
        });
        dialog.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(android.content.DialogInterface d) {
                if (activeSpecialDialog == dialog) {
                    activeSpecialDialog = null;
                }
                if (!specialSettingsInFlight) {
                    continueSpecialStartupFlow();
                }
            }
        });
        dialog.show();
    }

    private void showBatteryOptimizationDialog() {
        markSpecialPromptShown(SPECIAL_PROMPT_BATTERY);
        if (Build.VERSION.SDK_INT < 23 || isIgnoringBatteryOptimizations()) {
            specialSettingsInFlight = false;
            specialSettingsStage = -1;
            continueSpecialStartupFlow();
            return;
        }

        if (activeSpecialDialog != null && activeSpecialDialog.isShowing()) {
            return;
        }

        specialDialogCancelled = false;

        final AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(NativeConfig.batteryOptimizationTitle())
                        .setMessage(NativeConfig.batteryOptimizationMessage())
                        .setNegativeButton(
                                NativeConfig.notNowText(),
                                new android.content.DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(
                                            android.content.DialogInterface dialog,
                                            int which
                                    ) {
                                        specialDialogCancelled = true;
                                        specialSettingsInFlight = false;
                                        specialSettingsStage = -1;
                                        dismissActiveSpecialDialog();
                                    }
                                }
                        )
                        .setPositiveButton(
                                NativeConfig.storageSettingsButton(),
                                null
                        )
                        .create();

        activeSpecialDialog = dialog;
        dialog.setOnShowListener(new android.content.DialogInterface.OnShowListener() {
            @Override
            public void onShow(android.content.DialogInterface d) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                try {
                                    markSpecialSettingsOpened(2);
                                    startActivity(new Intent(
                                            android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                            Uri.parse("package:" + getPackageName())
                                    ));
                                } catch (Throwable ignored) {
                                    specialSettingsInFlight = false;
                                    specialSettingsStage = -1;
                                }
                            }
                        });
            }
        });
        dialog.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(android.content.DialogInterface d) {
                if (activeSpecialDialog == dialog) {
                    activeSpecialDialog = null;
                }
                if (!specialSettingsInFlight) {
                    continueSpecialStartupFlow();
                }
            }
        });
        dialog.show();
    }

    private void showNextPermissionDialog(final int index) {
        final String permission = getPermissionForIndex(index);

        if (permission == null) {
            activePermissionIndex = -1;
            activePermissionDialog = null;
            continueSpecialStartupFlow();
            return;
        }

        if (hasPermission(permission)) {
            showNextPermissionDialog(index + 1);
            return;
        }

        if (activePermissionDialog != null && activePermissionDialog.isShowing()) {
            return;
        }

        activePermissionIndex = index;
        final AlertDialog dialog = buildPermissionDialog(permission);
        activePermissionDialog = dialog;
        dialog.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(android.content.DialogInterface d) {
                if (activePermissionDialog == dialog) {
                    activePermissionDialog = null;
                }

                if (hasPermission(permission)) {
                    showNextPermissionDialog(index + 1);
                }
            }
        });
        dialog.show();
    }

    private String getPermissionForIndex(int index) {
        switch (index) {
            case 0:
                return Manifest.permission.CAMERA;

            case 1:
                return Manifest.permission.RECORD_AUDIO;

            case 2:
                if (Build.VERSION.SDK_INT >= 33) {
                    return Manifest.permission.POST_NOTIFICATIONS;
                }
                if (Build.VERSION.SDK_INT <= 28) {
                    return Manifest.permission.READ_EXTERNAL_STORAGE;
                }
                return null;

            case 3:
                return null;

            default:
                return null;
        }
    }

    private int permissionIndex(String permission) {
        if (Manifest.permission.CAMERA.equals(permission)) return 0;
        if (Manifest.permission.RECORD_AUDIO.equals(permission)) return 1;
        if (Manifest.permission.POST_NOTIFICATIONS.equals(permission)) return 2;
        if (Manifest.permission.READ_EXTERNAL_STORAGE.equals(permission)) {
            return Build.VERSION.SDK_INT >= 33 ? 3 : 2;
        }
        return -1;
    }

    private AlertDialog buildPermissionDialog(final String permission) {
        String title = NativeConfig.permissionTitle(permission);
        String message = NativeConfig.permissionMessage(permission);
        String allow = NativeConfig.permissionAllowButton();
        String deny = NativeConfig.permissionDenyButton();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setNegativeButton(
                deny,
                new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                            android.content.DialogInterface dialog,
                            int which) {
                        dialog.dismiss();
                        showPermissionDeniedDialog(permission);
                    }
                }
        );
        builder.setPositiveButton(
                allow,
                new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                            android.content.DialogInterface dialog,
                            int which) {
                        dialog.dismiss();

                        if (Build.VERSION.SDK_INT >= 23) {
                            requestPermissions(
                                    new String[]{permission},
                                    REQ_STARTUP_PERMISSIONS
                            );
                        }
                    }
                }
        );

        final AlertDialog dialog = builder.create();

        dialog.setOnShowListener(
                new android.content.DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(android.content.DialogInterface d) {
                        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                    .setTextColor(Color.rgb(0, 168, 132));
                        }

                        if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
                            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                                    .setTextColor(Color.rgb(150, 150, 150));
                        }
                    }
                }
        );

        return dialog;
    }

    private void showPermissionDeniedDialog(final String permission) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(NativeConfig.permissionDeniedTitle());
        builder.setMessage(
                NativeConfig.permissionDeniedMessage(permission)
        );

        builder.setNegativeButton(
                NativeConfig.permissionCloseButton(),
                new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                            android.content.DialogInterface dialog,
                            int which) {
                        dialog.dismiss();
                        showNextPermissionDialog(
                                permissionIndex(permission) + 1
                        );
                    }
                }
        );

        builder.setPositiveButton(
                NativeConfig.permissionSettingsButton(),
                new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                            android.content.DialogInterface dialog,
                            int which) {
                        pendingStartupPermissionAfterSettings =
                                permissionIndex(permission) + 1;
                        openAppSettings();
                        dialog.dismiss();
                    }
                }
        );

        final AlertDialog dialog = builder.create();

        dialog.setOnShowListener(
                new android.content.DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(android.content.DialogInterface d) {
                        AlertDialog ad = (AlertDialog) d;

                        if (ad.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
                            ad.getButton(AlertDialog.BUTTON_POSITIVE)
                                    .setTextColor(Color.rgb(0, 168, 132));
                        }

                        if (ad.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
                            ad.getButton(AlertDialog.BUTTON_NEGATIVE)
                                    .setTextColor(Color.rgb(150, 150, 150));
                        }
                    }
                }
        );

        dialog.show();
    }

    private void openAppSettings() {
        try {
            Intent intent = new Intent(
                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            );

            intent.setData(
                    Uri.parse(
                            "package:" + getPackageName()
                    )
            );

            startActivity(intent);
        } catch (Throwable ignored) {
        }
    }

    private boolean hasPermission(String permission) {
        if (Build.VERSION.SDK_INT < 23) {
            return true;
        }

        return checkSelfPermission(permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void buildUi() {
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(17, 27, 33));

        if (Build.VERSION.SDK_INT >= 23) {
            root.setOnApplyWindowInsetsListener(
                    new View.OnApplyWindowInsetsListener() {
                        @Override
                        public WindowInsets onApplyWindowInsets(
                                View v,
                                WindowInsets insets
                        ) {
                            if (drawer != null) {
                                drawer.setPadding(
                                        0,
                                        insets.getSystemWindowInsetTop(),
                                        0,
                                        insets.getSystemWindowInsetBottom()
                                );
                            }
                            return insets;
                        }
                    }
            );
        }

        /*
         * Small loading layer.
         * This avoids the ugly "pure black flash" while the WebView surface
         * is being recreated or while WhatsApp Web is loading again.
         */
        FrameLayout loading = new FrameLayout(this);
        loading.setBackgroundColor(Color.rgb(17, 27, 33));

        ProgressBar spinner = new ProgressBar(this);
        FrameLayout.LayoutParams spinnerLp = new FrameLayout.LayoutParams(
                dp(42),
                dp(42),
                Gravity.CENTER
        );
        loading.addView(spinner, spinnerLp);

        loadingLabel = text(NativeConfig.loadingText(), 13, Color.argb(190, 255, 255, 255));
        loadingLabel.setGravity(Gravity.CENTER);

        FrameLayout.LayoutParams labelLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(36),
                Gravity.CENTER
        );
        labelLp.topMargin = dp(65);
        loading.addView(loadingLabel, labelLp);

        root.addView(
                loading,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );
        loading.setTag("loading_overlay");

        progress = new ProgressBar(
                this,
                null,
                android.R.attr.progressBarStyleHorizontal
        );
        progress.setMax(100);
        progress.setProgress(0);
        progress.setVisibility(View.VISIBLE);

        root.addView(
                progress,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(2),
                        Gravity.TOP
                )
        );

        menuButton = new ImageButton(this);
        menuButton.setImageDrawable(new ThreeDotDrawable());
        menuButton.setBackground(
                round(Color.argb(225, 32, 33, 36), dp(22))
        );
        menuButton.setPadding(
                dp(14),
                dp(10),
                dp(14),
                dp(10)
        );
        menuButton.setContentDescription(NativeConfig.menuContentDescription());

        FrameLayout.LayoutParams menuLp =
                new FrameLayout.LayoutParams(
                        dp(52),
                        dp(52),
                        Gravity.START | Gravity.TOP
                );
        root.addView(menuButton, menuLp);

        menuButton.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(
                            View view,
                            MotionEvent event
                    ) {
                        if (hideThreeDot) {
                            return false;
                        }

                        switch (event.getActionMasked()) {
                            case MotionEvent.ACTION_DOWN:
                                menuDragStartX = event.getRawX();
                                menuDragStartY = event.getRawY();
                                menuDragging = false;
                                return true;

                            case MotionEvent.ACTION_MOVE:
                                float dx =
                                        event.getRawX() - menuDragStartX;
                                float dy =
                                        event.getRawY() - menuDragStartY;

                                if (!menuDragging &&
                                        Math.hypot(dx, dy) >
                                        Math.max(
                                                dp(6),
                                                touchSlop
                                        )) {
                                    menuDragging = true;
                                }

                                if (menuDragging) {
                                    setMenuPosition(
                                            menuButton.getX() + dx,
                                            menuButton.getY() + dy,
                                            true
                                    );
                                    menuDragStartX = event.getRawX();
                                    menuDragStartY = event.getRawY();
                                }

                                return true;

                            case MotionEvent.ACTION_UP:
                                if (!menuDragging) {
                                    setDrawerOpen(!drawerOpen);
                                }
                                menuDragging = false;
                                return true;

                            case MotionEvent.ACTION_CANCEL:
                                menuDragging = false;
                                return true;
                        }

                        return true;
                    }
                }
        );

        buildDrawer();
        if (Build.VERSION.SDK_INT >= 23) {
            root.requestApplyInsets();
        }
        root.post(new Runnable() {
            @Override
            public void run() {
                if (menuButton != null) {
                    menuButton.setLayerType(
                            View.LAYER_TYPE_HARDWARE,
                            null
                    );
                }

                if (drawer != null) {
                    drawer.setTranslationX(
                            drawerOpen
                                    ? 0f
                                    : -drawerWidthPx
                    );
                }
            }
        });
        setContentView(root);

        root.addOnLayoutChangeListener(
                new View.OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChange(
                            View v,
                            int left,
                            int top,
                            int right,
                            int bottom,
                            int oldLeft,
                            int oldTop,
                            int oldRight,
                            int oldBottom
                    ) {
                        int width = right - left;
                        int height = bottom - top;

                        if (width == lastRootWidth &&
                                height == lastRootHeight) {
                            return;
                        }

                        lastRootWidth = width;
                        lastRootHeight = height;

                        updateDrawerBounds(width);

                        if (menuButton != null &&
                                !menuDragging &&
                                !drawerAnimating &&
                                menuButton.getWidth() > 0 &&
                                menuButton.getHeight() > 0) {
                            float maxX =
                                    Math.max(
                                            0f,
                                            width -
                                            menuButton.getWidth()
                                    );

                            float maxY =
                                    Math.max(
                                            0f,
                                            height -
                                            menuButton.getHeight()
                                    );

                            setMenuPosition(
                                    maxX * clamp01(menuRelX),
                                    maxY * clamp01(menuRelY),
                                    false
                            );
                        }
                    }
                }
        );

        root.post(
                new Runnable() {
                    @Override
                    public void run() {
                        restoreMenuPosition();
                    }
                }
        );
    }

    private void initializeWebView(Bundle restoreState) {
        if (webView != null) {
            return;
        }

        ensureCurrentTabMetadata();

        TabState currentTab = tabStateForActiveTab();
        if (currentTab == null) {
            closeAppBySystem();
            return;
        }

        if (TextUtils.isEmpty(currentTab.profileId)) {
            currentTab.profileId =
                    "tab_profile_" + currentTab.id;
            persistTabs();
        }

        if (currentTab.view != null) {
            webView = currentTab.view;

            try {
                webProfile = null;
                if (multiProfileSupported) {
                    webProfile =
                            ProfileStore
                                    .getInstance()
                                    .getOrCreateProfile(
                                            currentTab.profileId
                                    );
                }
            } catch (Throwable ignored) {
                webProfile = null;
            }

            try {
                ViewParent parent = webView.getParent();
                if (parent == null) {
                    root.addView(
                            webView,
                            0,
                            new FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                            )
                    );
                }

                webView.setVisibility(View.VISIBLE);
                webView.onResume();
            } catch (Throwable ignored) {
            }

            activeTabIndex =
                    findTabIndexById(currentTab.id);
            refreshTabMenu();
            return;
        }

        webProfile = null;
        historyFloorApplied = false;

        webView = new WebView(this);

        boolean profileReady =
                currentTab.id == 1;

        /*
         * androidx.webkit 1.9.0 provides true per-WebView profiles.
         * Profile data includes cookies, local storage, service workers and
         * the profile's WebStorage. The profile must be attached before any
         * other WebView configuration is performed.
         */
        if (multiProfileSupported) {
            try {
                WebViewCompat.setProfile(
                        webView,
                        currentTab.profileId
                );

                ProfileStore store =
                        ProfileStore.getInstance();

                webProfile =
                        store.getOrCreateProfile(
                                currentTab.profileId
                        );

                profileReady =
                        webProfile != null;
            } catch (Throwable ignored) {
                webProfile = null;
                profileReady = false;
            }
        }

        if (currentTab.id > 1 &&
                !profileReady) {
            try {
                webView.destroy();
            } catch (Throwable ignored) {
            }

            webView = null;
            activeTabIndex =
                    Math.max(
                            0,
                            findTabIndexById(1)
                    );
            webStateRestored = false;
            baselineScaleCaptured = false;
            historyFloorApplied = false;

            Toast.makeText(
                    this,
                    NativeConfig.tabProfilesUnavailableText(),
                    Toast.LENGTH_LONG
            ).show();

            root.post(
                    new Runnable() {
                        @Override
                        public void run() {
                            initializeWebView(null);
                        }
                    }
            );
            return;
        }

        root.addView(
                webView,
                0,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        webView.setBackgroundColor(
                Color.rgb(17, 27, 33)
        );

        configureGpuRendering();
        configureWebView();
        applyZoomMode(zoomEnabled, false);

        currentTab.view = webView;
        activeTabIndex =
                findTabIndexById(currentTab.id);

        refreshTabMenu();

        if (restoreState != null) {
            try {
                android.webkit.WebBackForwardList restored =
                        webView.restoreState(
                                restoreState
                        );

                webStateRestored =
                        restored != null &&
                        restored.getSize() > 0;

                // Restored WebView history may contain an older login or
                // redirect chain. The first stable page becomes the new
                // Back-navigation floor for this app launch.
                historyFloorApplied = false;
            } catch (Throwable ignoredRestore) {
                webStateRestored = false;
            }
        }

        if (!webStateRestored) {
            String initialUrl =
                    TextUtils.isEmpty(currentTab.url)
                            ? NativeConfig.webUrl()
                            : currentTab.url;

            webView.loadUrl(
                    initialUrl
            );
        }
    }

    private void configureGpuRendering() {
        detectedGlesMajor = 2;

        try {
            ActivityManager manager =
                    (ActivityManager) getSystemService(
                            ACTIVITY_SERVICE
                    );

            if (manager != null) {
                ConfigurationInfo info =
                        manager.getDeviceConfigurationInfo();

                if (info != null &&
                        info.reqGlEsVersion >= 0x30000) {
                    detectedGlesMajor = 3;
                }
            }
        } catch (Throwable ignoredGpu) {
            detectedGlesMajor = 2;
        }

        // Keep WebView on its normal provider-managed rendering path.
    }


    private void updateDrawerBounds(int rootWidth) {
        if (root == null || drawer == null || rootWidth <= 0) {
            return;
        }

        int width = (int) (rootWidth * 0.78f);
        width = Math.max(width, dp(300));
        width = Math.min(width, dp(430));
        width = Math.min(width, Math.max(dp(1), rootWidth - dp(16)));

        if (width == drawerWidthPx) {
            return;
        }

        drawerWidthPx = width;
        ViewGroup.LayoutParams params = drawer.getLayoutParams();
        if (params != null) {
            params.width = width;
            drawer.setLayoutParams(params);
        }

        // Never overwrite an active animation's translation.
        if (!drawerAnimating) {
            drawer.setTranslationX(drawerOpen ? 0f : -drawerWidthPx);
        }
    }

    private float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
    private void restoreMenuPosition() {
        if (root == null || menuButton == null) {
            return;
        }

        float maxX =
                Math.max(
                        0f,
                        root.getWidth() - menuButton.getWidth()
                );

        float maxY =
                Math.max(
                        0f,
                        root.getHeight() - menuButton.getHeight()
                );

        setMenuPosition(
                maxX * clamp01(menuRelX),
                maxY * clamp01(menuRelY),
                false
        );

        // Position first, then reveal. This prevents the ghost button from
        // ever being drawn at (0,0) during a resume/layout pass.
        menuButton.animate().cancel();
        menuButton.setRotation(drawerOpen ? 90f : 0f);
        menuButton.setScaleX(drawerOpen || hideThreeDot ? 0.72f : 1f);
        menuButton.setScaleY(drawerOpen || hideThreeDot ? 0.72f : 1f);
        menuButton.setAlpha(drawerOpen || hideThreeDot ? 0f : 1f);
        updateMenuVisibility();
    }

    private void setMenuPosition(
            float x,
            float y,
            boolean save
    ) {
        if (root == null || menuButton == null) {
            return;
        }

        float maxX = Math.max(
                0f,
                root.getWidth() - menuButton.getWidth()
        );
        float maxY = Math.max(
                0f,
                root.getHeight() - menuButton.getHeight()
        );

        x = Math.max(0f, Math.min(maxX, x));
        y = Math.max(0f, Math.min(maxY, y));

        menuButton.setX(x);
        menuButton.setY(y);

        if (maxX > 0f) {
            menuRelX = clamp01(x / maxX);
        }

        if (maxY > 0f) {
            menuRelY = clamp01(y / maxY);
        }

        if (save) {
            RenaSettingsStore.putFloat(
                    this,
                    "menu_rel_x",
                    menuRelX
            );
            RenaSettingsStore.putFloat(
                    this,
                    "menu_rel_y",
                    menuRelY
            );
        }
    }
    private void updateMenuVisibility() {
        if (menuButton == null) {
            return;
        }

        if (hideThreeDot) {
            menuButton.animate().cancel();
            menuButton.setClickable(false);
            menuButton.setFocusable(false);
            menuButton.setVisibility(View.INVISIBLE);
            menuButton.setAlpha(0f);
            menuButton.setScaleX(0.72f);
            menuButton.setScaleY(0.72f);
            return;
        }

        menuButton.setClickable(!drawerOpen && !drawerAnimating);
        menuButton.setFocusable(!drawerOpen && !drawerAnimating);

        if (drawerOpen || drawerAnimating) {
            menuButton.setVisibility(View.VISIBLE);
            menuButton.animate().cancel();
            menuButton.animate()
                    .alpha(0f)
                    .scaleX(0.76f)
                    .scaleY(0.76f)
                    .setDuration(animDuration(150L))
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
        } else {
            menuButton.setVisibility(View.VISIBLE);
            menuButton.animate().cancel();
            menuButton.animate()
                    .rotation(0f)
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(animDuration(180L))
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
        }
    }

    private long animDuration(long normal) {
        if (!reduceAnimations) {
            return normal;
        }
        return Math.max(1L, normal / 3L);
    }

    private final SensorEventListener shakeListener =
            new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent event) {
                    if (!hideThreeDot ||
                            drawerOpen ||
                            drawerAnimating ||
                            event.values == null ||
                            event.values.length < 3) {
                        return;
                    }

                    final long now = System.currentTimeMillis();

                    if (now < shakeCooldownUntil) {
                        return;
                    }

                    float dx = event.values[0] - shakeLastX;
                    float dy = event.values[1] - shakeLastY;
                    float dz = event.values[2] - shakeLastZ;

                    float delta =
                            (float) Math.sqrt(
                                    dx * dx +
                                    dy * dy +
                                    dz * dz
                            );

                    shakeLastX = event.values[0];
                    shakeLastY = event.values[1];
                    shakeLastZ = event.values[2];

                    if (now - shakeLastTime > 850L) {
                        shakePeakCount = 0;
                    }

                    if (delta >= 8.0f) {
                        shakePeakCount++;
                    }

                    if (shakePeakCount >= 2 &&
                            now - shakeLastTime <= 850L) {
                        shakePeakCount = 0;
                        shakeCooldownUntil =
                                now + (reduceAnimations ? 700L : 1000L);
                        setDrawerOpen(true);
                    }

                    shakeLastTime = now;
                }

                @Override
                public void onAccuracyChanged(
                        Sensor sensor,
                        int accuracy
                ) {
                }
            };

    private void registerShakeSensor() {
        if (sensorManager == null ||
                accelerometer == null) {
            return;
        }

        if (shakeRegistered) {
            return;
        }

        sensorManager.registerListener(
                shakeListener,
                accelerometer,
                SensorManager.SENSOR_DELAY_GAME
        );

        shakeRegistered = true;
    }

    private void unregisterShakeSensor() {
        if (sensorManager != null && shakeRegistered) {
            sensorManager.unregisterListener(
                    shakeListener
            );
        }

        shakeRegistered = false;
    }
    private LinearLayout buildToggleRow(
            String label,
            boolean checked,
            final android.widget.CompoundButton.OnCheckedChangeListener listener
    ) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), 0, dp(8), 0);
        row.setBackground(
                round(Color.argb(24, 255, 255, 255), dp(16))
        );

        TextView labelView = text(
                label,
                15,
                Color.WHITE
        );

        row.addView(
                labelView,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        1f
                )
        );

        final Switch switchView = new Switch(this);
        switchView.setText("");
        switchView.setChecked(checked);

        row.addView(
                switchView,
                new LinearLayout.LayoutParams(dp(56), dp(52))
        );

        switchView.setOnCheckedChangeListener(listener);

        row.setClickable(true);
        row.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        switchView.setChecked(
                                !switchView.isChecked()
                        );
                    }
                }
        );

        return row;
    }

    private void setHideNotifications(boolean enabled) {
        hideNotifications = enabled;

        RenaSettingsStore.putBoolean(
                this,
                "hide_notifications",
                enabled
        );

        ensureNotificationChannels();

        showToggleToastOnce(
                "hide_notifications",
                enabled
                        ? NativeConfig.hideNotificationEnabledText()
                        : NativeConfig.hideNotificationDisabledText(),
                enabled
        );
    }

    private void setReduceAnimations(boolean enabled) {
        reduceAnimations = enabled;

        RenaSettingsStore.putBoolean(
                this,
                "reduce_animation",
                enabled
        );

        if (enabled && drawer != null) {
            drawer.animate().cancel();
            drawerScrim.animate().cancel();
            menuButton.animate().cancel();
        }

        showToggleToastOnce(
                "reduce_animation",
                enabled
                        ? NativeConfig.reduceAnimationEnabledText()
                        : NativeConfig.reduceAnimationDisabledText(),
                enabled
        );
    }

    private void showToggleToastOnce(
            String key,
            String message,
            boolean enabled
    ) {
        String suffix = enabled ? "_on_toast" : "_off_toast";
        String storeKey = key + suffix;

        if (RenaSettingsStore.getBoolean(
                this,
                storeKey,
                false
        )) {
            return;
        }

        RenaSettingsStore.putBoolean(
                this,
                storeKey,
                true
        );

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
        ).show();
    }

    private TextView makeActionRow(
            String label,
            final Runnable action
    ) {
        TextView row = text(label, 15, Color.WHITE);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), 0, dp(10), 0);
        row.setBackground(
                round(Color.argb(30, 0, 168, 132), dp(16))
        );
        if (label.equals(NativeConfig.newTabText())) {
            row.setTextColor(Color.rgb(0, 220, 172));
        } else if (label.equals(NativeConfig.closeAllTabsText())) {
            row.setTextColor(Color.rgb(255, 110, 110));
        }
        bindSafeClick(row, action);
        return row;
    }

    private void showHideMenuWarning(
            final Switch switchView
    ) {
        new AlertDialog.Builder(this)
                .setTitle(
                        NativeConfig.hideMenuLabel()
                )
                .setMessage(
                        NativeConfig.hideMenuMessage()
                )
                .setNegativeButton(
                        NativeConfig.cancelText(),
                        null
                )
                .setPositiveButton(
                        NativeConfig.okayText(),
                        new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(
                                    android.content.DialogInterface dialog,
                                    int which
                            ) {
                                hideWarningShown = true;

                                RenaSettingsStore.putBoolean(
                                        MainActivity.this,
                                        "hide_warning_shown",
                                        true
                                );

                                switchView.setChecked(true);
                                setHideThreeDot(true);
                            }
                        }
                )
                .show();
    }
    private void setHideThreeDot(boolean enabled) {
        boolean changed = hideThreeDot != enabled;
        hideThreeDot = enabled;

        RenaSettingsStore.putBoolean(
                this,
                "hide_three_dot",
                enabled
        );

        updateMenuVisibility();

        if (enabled) {
            registerShakeSensor();
            if (changed && hideWarningShown) {
                Toast.makeText(
                        this,
                        NativeConfig.hideMenuToastText(),
                        Toast.LENGTH_SHORT
                ).show();
            }
        } else {
            unregisterShakeSensor();
        }
    }

    private void animateTabRows() {
        if (tabMenuContainer == null) {
            return;
        }

        for (int i = 0; i < tabMenuContainer.getChildCount(); i++) {
            animateSingleTabRow(
                    tabMenuContainer.getChildAt(i),
                    i * 12L
            );
        }
    }

    // New tabs should feel like one new item entering, not like the whole list
    // is being rebuilt in front of the user.
    private void animateSingleTabRow(View child, long delay) {
        if (child == null) {
            return;
        }

        child.animate().cancel();
        child.setTranslationX(-dp(10));
        child.setAlpha(0.82f);
        child.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(animDuration(160L))
                .setStartDelay(delay)
                .setInterpolator(
                        new android.view.animation.DecelerateInterpolator()
                )
                .start();
    }

    private void animateSwitchDrawer(
            final LinearLayout body,
            final TextView arrow,
            final boolean open
    ) {
        if (body == null || arrow == null) {
            return;
        }

        // One animator owns the whole accordion. Rapid taps cancel the old
        // run and continue smoothly from the height currently on screen.
        if (switchDrawerAnimator != null) {
            switchDrawerAnimator.cancel();
            switchDrawerAnimator = null;
        }
        arrow.animate().cancel();

        LinearLayout.LayoutParams lp =
                (LinearLayout.LayoutParams) body.getLayoutParams();
        int currentHeight = Math.max(body.getHeight(), 0);

        if (reduceAnimations) {
            body.setTranslationY(0f);
            body.setAlpha(1f);
            body.setClickable(open);
            body.setFocusable(open);
            if (open) {
                body.setVisibility(View.VISIBLE);
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            } else {
                lp.height = 0;
                body.setVisibility(View.GONE);
            }
            body.setLayoutParams(lp);
            arrow.setRotation(open ? 90f : 0f);
            return;
        }

        if (open) {
            body.setVisibility(View.VISIBLE);
            body.setClickable(true);
            body.setFocusable(true);

            // Measure its natural height without changing the visible start
            // position. This keeps a half-finished close/open responsive.
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            body.setLayoutParams(lp);
            int availableWidth = body.getWidth();
            if (availableWidth <= 0 && body.getParent() instanceof View) {
                availableWidth = ((View) body.getParent()).getWidth();
            }
            if (availableWidth <= 0) {
                availableWidth = Math.max(
                        drawerWidthPx - dp(12),
                        getResources().getDisplayMetrics().widthPixels - dp(48)
                );
            }

            body.measure(
                    View.MeasureSpec.makeMeasureSpec(
                            Math.max(availableWidth, dp(1)),
                            View.MeasureSpec.EXACTLY
                    ),
                    View.MeasureSpec.makeMeasureSpec(
                            0,
                            View.MeasureSpec.UNSPECIFIED
                    )
            );
            int targetHeight = Math.max(body.getMeasuredHeight(), dp(1));
            currentHeight = Math.min(currentHeight, targetHeight);
            lp.height = currentHeight;
            body.setLayoutParams(lp);
            body.setAlpha(0.96f);
            body.setTranslationY(-dp(4));

            switchDrawerAnimator = android.animation.ValueAnimator.ofInt(
                    currentHeight,
                    targetHeight
            );
            switchDrawerAnimator.setDuration(animDuration(210L));
            switchDrawerAnimator.setInterpolator(
                    new android.view.animation.DecelerateInterpolator()
            );
            switchDrawerAnimator.addUpdateListener(
                    new android.animation.ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(
                                android.animation.ValueAnimator animation
                        ) {
                            LinearLayout.LayoutParams current =
                                    (LinearLayout.LayoutParams) body.getLayoutParams();
                            current.height =
                                    (Integer) animation.getAnimatedValue();
                            body.setLayoutParams(current);
                            float progress = animation.getAnimatedFraction();
                            body.setAlpha(0.96f + (0.04f * progress));
                            body.setTranslationY(-dp(4) * (1f - progress));
                        }
                    }
            );
            switchDrawerAnimator.addListener(
                    new android.animation.AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(
                                android.animation.Animator animation
                        ) {
                            LinearLayout.LayoutParams current =
                                    (LinearLayout.LayoutParams) body.getLayoutParams();
                            current.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                            body.setLayoutParams(current);
                            body.setAlpha(1f);
                            body.setTranslationY(0f);
                            switchDrawerAnimator = null;
                        }
                    }
            );
            switchDrawerAnimator.start();

            arrow.animate()
                    .rotation(90f)
                    .setDuration(animDuration(210L))
                    .setInterpolator(
                            new android.view.animation.DecelerateInterpolator()
                    )
                    .start();
        } else {
            body.setClickable(false);
            body.setFocusable(false);

            int startHeight = Math.max(
                    currentHeight,
                    body.getMeasuredHeight()
            );

            switchDrawerAnimator = android.animation.ValueAnimator.ofInt(
                    startHeight,
                    0
            );
            switchDrawerAnimator.setDuration(animDuration(240L));
            switchDrawerAnimator.setInterpolator(
                    new android.view.animation.DecelerateInterpolator()
            );
            switchDrawerAnimator.addUpdateListener(
                    new android.animation.ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(
                                android.animation.ValueAnimator animation
                        ) {
                            LinearLayout.LayoutParams current =
                                    (LinearLayout.LayoutParams) body.getLayoutParams();
                            current.height =
                                    (Integer) animation.getAnimatedValue();
                            body.setLayoutParams(current);
                            float progress = animation.getAnimatedFraction();
                            body.setAlpha(1f - (0.04f * progress));
                            body.setTranslationY(-dp(2) * progress);
                        }
                    }
            );
            switchDrawerAnimator.addListener(
                    new android.animation.AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(
                                android.animation.Animator animation
                        ) {
                            LinearLayout.LayoutParams current =
                                    (LinearLayout.LayoutParams) body.getLayoutParams();
                            current.height = 0;
                            body.setLayoutParams(current);
                            body.setVisibility(View.GONE);
                            body.setAlpha(1f);
                            body.setTranslationY(0f);
                            switchDrawerAnimator = null;
                        }
                    }
            );
            switchDrawerAnimator.start();

            arrow.animate()
                    .rotation(0f)
                    .setDuration(animDuration(200L))
                    .setInterpolator(
                            new android.view.animation.DecelerateInterpolator()
                    )
                    .start();
        }
    }

    private void refreshTabMenu() {
        if (tabMenuContainer == null) {
            return;
        }

        tabMenuContainer.removeAllViews();

        for (int i = 0; i < tabs.size(); i++) {
            final int index = i;

            LinearLayout row =
                    new LinearLayout(this);

            row.setOrientation(
                    LinearLayout.HORIZONTAL
            );
            row.setGravity(
                    Gravity.CENTER_VERTICAL
            );
            row.setPadding(
                    dp(12),
                    0,
                    dp(8),
                    0
            );

            row.setBackground(
                    round(
                            index == activeTabIndex
                                    ? Color.argb(42, 0, 168, 132)
                                    : Color.argb(24, 255, 255, 255),
                            dp(16)
                    )
            );

            TextView label = text(
                    NativeConfig.tabPrefix()
                            + tabs.get(index).id,
                    15,
                    Color.WHITE
            );

            row.addView(
                    label,
                    new LinearLayout.LayoutParams(
                            0,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            1f
                    )
            );

            if (index > 0) {
                TextView close =
                        text(NativeConfig.closeTabSymbol(), 22, Color.WHITE);

                close.setGravity(Gravity.CENTER);

                row.addView(
                        close,
                        new LinearLayout.LayoutParams(
                                dp(44),
                                dp(44)
                        )
                );

                bindSafeClick(
                        close,
                        new Runnable() {
                            @Override
                            public void run() {
                                closeTab(index);
                            }
                        }
                );
            }

            bindSafeClick(
                    row,
                    new Runnable() {
                        @Override
                        public void run() {
                            switchToTab(index);
                        }
                    }
            );

            LinearLayout.LayoutParams lp =
                    exact(dp(52));

            lp.topMargin = dp(6);

            tabMenuContainer.addView(
                    row,
                    lp
            );
        }
    }

    private void restoreTabMetadata() {
        TabStore.Snapshot snapshot =
                TabStore.read(this);

        tabs.clear();

        for (TabStore.Record record :
                snapshot.records) {
            TabState tab =
                    new TabState();

            tab.id = record.id;
            tab.url = record.url;
            tab.profileId = record.profileId;

            tabs.add(tab);
        }

        if (tabs.isEmpty()) {
            TabState first =
                    new TabState();

            first.id = 1;
            first.url = NativeConfig.webUrl();
            first.profileId = "tab_profile_1";

            tabs.add(first);
        }

        int currentId =
                currentTabId();

        activeTabIndex =
                findTabIndexById(currentId);

        if (activeTabIndex < 0) {
            activeTabIndex =
                    findTabIndexById(
                            snapshot.activeTabId
                    );
        }

        if (activeTabIndex < 0) {
            activeTabIndex = 0;
        }

        if (!multiProfileSupported) {
            activeTabIndex = 0;
        }

        ensureCurrentTabMetadata();
    }

    private int findTabIndexById(int id) {
        for (int i = 0; i < tabs.size(); i++) {
            if (tabs.get(i).id == id) {
                return i;
            }
        }

        return -1;
    }

    private void ensureCurrentTabMetadata() {
        int id =
                currentTabId();

        int index =
                findTabIndexById(id);

        if (index >= 0) {
            activeTabIndex = index;
            return;
        }

        if (id == 1) {
            TabState first =
                    new TabState();

            first.id = 1;
            first.url = NativeConfig.webUrl();
            first.profileId = "tab_profile_1";

            tabs.add(
                    0,
                    first
            );

            activeTabIndex = 0;
            persistTabs();
        }
    }

    private void persistTabs() {
        saveActiveTab();

        ArrayList<TabStore.Record> records =
                new ArrayList<TabStore.Record>();

        for (TabState tab : tabs) {
            String url =
                    tab.url;

            if (tab.view != null &&
                    !TextUtils.isEmpty(
                            tab.view.getUrl()
                    )) {
                url =
                        tab.view.getUrl();
            }

            records.add(
                    new TabStore.Record(
                            tab.id,
                            TextUtils.isEmpty(url)
                                    ? NativeConfig.webUrl()
                                    : url,
                            TextUtils.isEmpty(
                                    tab.profileId
                            )
                                    ? "tab_profile_" + tab.id
                                    : tab.profileId
                    )
            );
        }

        TabStore.write(
                this,
                records,
                currentTabId()
        );
    }

    private void saveActiveTab() {
        int index =
                findTabIndexById(
                        currentTabId()
                );

        if (index < 0 ||
                index >= tabs.size()) {
            return;
        }

        activeTabIndex =
                index;

        TabState tab =
                tabs.get(index);

        if (webView != null) {
            tab.view =
                    webView;

            if (!TextUtils.isEmpty(
                    webView.getUrl()
            )) {
                tab.url =
                        webView.getUrl();
            }
        }
    }

    private TabState tabStateForActiveTab() {
        int index =
                findTabIndexById(
                        currentTabId()
                );

        if (index < 0) {
            return null;
        }

        return tabs.get(index);
    }

    private void switchToTab(int index) {
        if (index < 0 || index >= tabs.size() || drawerAnimating) {
            return;
        }

        if (drawerOpen || drawerAnimating) {
            setDrawerOpen(false);
        }

        if (!multiProfileSupported && index != 0) {
            Toast.makeText(
                    this,
                    NativeConfig.tabProfilesUnavailableText(),
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        if (index == activeTabIndex && webView != null) {
            refreshTabMenu();
            return;
        }

        persistTabs();
        retainActiveWebView();

        activeTabIndex = index;
        webStateRestored = false;
        baselineScaleCaptured = false;
        historyFloorApplied = false;
        initializeWebView(null);
    }

    private void retainActiveWebView() {
        if (webView == null) {
            return;
        }

        TabState current = tabStateForActiveTab();
        if (current == null) {
            return;
        }

        current.view = webView;
        flushProfileForTab(current);

        try {
            webView.setVisibility(View.GONE);
        } catch (Throwable ignored) {
        }
    }

    private void destroyActiveWebView() {
        if (webView == null) {
            return;
        }

        TabState current = tabStateForActiveTab();
        if (current != null) {
            flushProfileForTab(current);
            current.view = null;
        }

        try {
            ViewParent parent = webView.getParent();
            if (parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(webView);
            }
            webView.stopLoading();
            webView.onPause();
            webView.pauseTimers();
            webView.destroy();
        } catch (Throwable ignored) {
        }
        webView = null;
    }

    private int firstAvailableTabId() {
        for (int id = 2; id <= 10; id++) {
            boolean used = false;

            for (TabState tab : tabs) {
                if (tab.id == id) {
                    used = true;
                    break;
                }
            }

            if (!used) {
                return id;
            }
        }

        return -1;
    }
    private void createTabRequested() {
        if (!multiProfileSupported) {
            Toast.makeText(
                    this,
                    NativeConfig.tabProfilesUnavailableText(),
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        if (tabs.size() >= 10) {
            Toast.makeText(
                    this,
                    NativeConfig.tabLimitText(),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        final boolean needsTab6Warning =
                firstAvailableTabId() == 6 &&
                !RenaSettingsStore.getBoolean(
                        this,
                        "tab6_warning",
                        false
                );

        if (needsTab6Warning) {
            new AlertDialog.Builder(this)
                    .setTitle(
                            NativeConfig.accountsTabsText()
                    )
                    .setMessage(
                            NativeConfig.storageWarningText()
                    )
                    .setNegativeButton(
                            NativeConfig.cancelText(),
                            null
                    )
                    .setPositiveButton(
                            NativeConfig.ignoreText(),
                            new android.content.DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(
                                        android.content.DialogInterface dialog,
                                        int which
                                ) {
                                    RenaSettingsStore.putBoolean(
                                            MainActivity.this,
                                            "tab6_warning",
                                            true
                                    );
                                    createTabNow();
                                }
                            }
                    )
                    .show();
            return;
        }

        createTabNow();
    }

    private void createTabNow() {
        int id = firstAvailableTabId();
        if (id < 2) {
            Toast.makeText(
                    this,
                    NativeConfig.tabLimitText(),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        persistTabs();

        TabState tab = new TabState();
        tab.id = id;
        tab.url = NativeConfig.webUrl();
        tab.profileId =
                "tab_profile_" +
                java.util.UUID.randomUUID().toString();

        tabs.add(tab);
        persistTabs();

        retainActiveWebView();
        activeTabIndex = findTabIndexById(id);
        webStateRestored = false;
        baselineScaleCaptured = false;
        historyFloorApplied = false;
        initializeWebView(null);

        if (tabMenuContainer != null) {
            int newIndex = findTabIndexById(id);
            refreshTabMenu();
            if (newIndex >= 0 && newIndex < tabMenuContainer.getChildCount()) {
                animateSingleTabRow(tabMenuContainer.getChildAt(newIndex), 0L);
            }
        }
    }

    private void destroyTabView(TabState tab) {
        if (tab == null ||
                tab.view == null) {
            return;
        }

        flushProfileForTab(tab);

        try {
            ViewParent parent = tab.view.getParent();
            if (parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(tab.view);
            }

            tab.view.stopLoading();
            tab.view.onPause();
            tab.view.pauseTimers();
            tab.view.destroy();
        } catch (Throwable ignored) {
        }

        tab.view = null;
    }

    private void flushProfileForTab(TabState tab) {
        if (tab == null) {
            return;
        }

        if (!multiProfileSupported) {
            try {
                CookieManager.getInstance().flush();
            } catch (Throwable ignored) {
            }
            return;
        }

        if (TextUtils.isEmpty(tab.profileId)) {
            return;
        }

        try {
            Profile profile = ProfileStore
                    .getInstance()
                    .getOrCreateProfile(
                            tab.profileId
                    );

            profile.getCookieManager().flush();
        } catch (Throwable ignored) {
        }
    }

    private void closeTab(final int index) {
        if (index < 0 || index >= tabs.size()) {
            return;
        }

        if (tabMenuContainer == null ||
                index >= tabMenuContainer.getChildCount()) {
            closeTabNow(index);
            return;
        }

        final View row = tabMenuContainer.getChildAt(index);
        row.animate()
                .translationX(-dp(28))
                .alpha(0f)
                .setDuration(animDuration(170L))
                .setInterpolator(
                        new android.view.animation.DecelerateInterpolator()
                )
                .withEndAction(
                        new Runnable() {
                            @Override
                            public void run() {
                                closeTabNow(index);
                            }
                        }
                )
                .start();
    }

    private void closeTabNow(int index) {
        if (index < 0 ||
                index >= tabs.size()) {
            return;
        }

        TabState tab = tabs.get(index);

        if (tab.id == 1) {
            return;
        }

        int removedId = tab.id;
        boolean closingCurrent =
                removedId == currentTabId();

        if (closingCurrent) {
            destroyActiveWebView();
        }

        tabs.remove(index);

        if (!closingCurrent) {
            if (index < activeTabIndex) {
                activeTabIndex--;
            }

            destroyTabView(tab);
        }

        if (tabs.isEmpty()) {
            TabState first =
                    new TabState();
            first.id = 1;
            first.url = NativeConfig.webUrl();
            first.profileId = "tab_profile_1";
            tabs.add(first);
            activeTabIndex = 0;
        }

        if (closingCurrent) {
            int targetId = 1;
            int candidate = -1;

            for (TabState remaining : tabs) {
                if (remaining.id < removedId &&
                        (candidate < 0 ||
                                remaining.id > candidate)) {
                    candidate = remaining.id;
                }
            }

            if (candidate > 0) {
                targetId = candidate;
            }

            int targetIndex =
                    findTabIndexById(targetId);

            activeTabIndex =
                    targetIndex >= 0
                            ? targetIndex
                            : 0;

            webStateRestored = false;
            baselineScaleCaptured = false;
            historyFloorApplied = false;
        }

        persistTabs();
        refreshTabMenu();
        animateTabRows();

        if (closingCurrent) {
            initializeWebView(null);
        }
    }

    private void closeAllTabsRequested() {
        if (tabs.size() <= 1) {
            Toast.makeText(
                    this,
                    NativeConfig.noJokesText(),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(
                        NativeConfig.closeAllTabsText()
                )
                .setMessage(
                        NativeConfig.closeAllTabsMessage()
                )
                .setNegativeButton(
                        NativeConfig.cancelText(),
                        null
                )
                .setPositiveButton(
                        NativeConfig.clearText(),
                        new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(
                                    android.content.DialogInterface dialog,
                                    int which
                            ) {
                                final int currentId = currentTabId();

                                if (tabMenuContainer != null &&
                                        tabMenuContainer.getChildCount() > 1) {
                                    final ArrayList<View> rows =
                                            new ArrayList<View>();

                                    for (int i = 1;
                                            i < tabMenuContainer.getChildCount();
                                            i++) {
                                        rows.add(
                                                tabMenuContainer.getChildAt(i)
                                        );
                                    }

                                    if (!rows.isEmpty()) {
                                        final View lastRow =
                                                rows.get(rows.size() - 1);

                                        for (int i = 0; i < rows.size() - 1; i++) {
                                            final View row = rows.get(i);

                                            row.animate()
                                                    .translationX(-dp(34))
                                                    .alpha(0f)
                                                    .setDuration(animDuration(180L))
                                                    .setStartDelay(i * 18L)
                                                    .setInterpolator(
                                                            new android.view.animation
                                                                    .DecelerateInterpolator()
                                                    )
                                                    .start();
                                        }

                                        lastRow.animate()
                                                .translationX(-dp(34))
                                                .alpha(0f)
                                                .setDuration(animDuration(180L))
                                                .setStartDelay(
                                                        (rows.size() - 1) * 18L
                                                )
                                                .setInterpolator(
                                                        new android.view.animation
                                                                .DecelerateInterpolator()
                                                )
                                                .withEndAction(
                                                        new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                closeAllTabsNow(currentId);
                                                            }
                                                        }
                                                )
                                                .start();

                                        return;
                                    }
                                }

                                closeAllTabsNow(currentId);
                            }
                        }
                )
                .show();
    }

    private void closeAllTabsNow(int currentId) {
        if (currentId != 1) {
            destroyActiveWebView();
        }

        for (int i = tabs.size() - 1; i >= 0; i--) {
            TabState tab = tabs.get(i);

            if (tab.id != 1) {
                tabs.remove(i);
                destroyTabView(tab);
            }
        }

        activeTabIndex = 0;
        webStateRestored = false;
        baselineScaleCaptured = false;
        historyFloorApplied = false;

        persistTabs();
        refreshTabMenu();
        animateTabRows();

        if (currentId != 1) {
            initializeWebView(null);
        }
    }

    private void loadDeveloperAvatar(final ImageView target) {
        if (target == null) {
            return;
        }

        // Local resource is always first. Network is only used when it is missing.
        try {
            int localId = getResources().getIdentifier(
                    "ridhoae303",
                    "drawable",
                    getPackageName()
            );
            if (localId != 0) {
                target.setImageResource(localId);
                return;
            }
        } catch (Throwable ignored) {
        }

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = null;
                File cache = new File(getCacheDir(), "developer_avatar.png");

                try {
                    if (cache.exists()) {
                        bitmap = BitmapFactory.decodeFile(cache.getAbsolutePath());
                    }
                } catch (Throwable ignored) {
                }

                if (bitmap == null) {
                    InputStream input = null;
                    FileOutputStream output = null;
                    HttpURLConnection connection = null;
                    try {
                        URL url = new URL(NativeConfig.developerAvatarUrl());
                        connection = (HttpURLConnection) url.openConnection();
                        connection.setConnectTimeout(6000);
                        connection.setReadTimeout(8000);
                        connection.setUseCaches(true);
                        connection.setRequestProperty("User-Agent", NativeConfig.developerName());
                        int code = connection.getResponseCode();
                        if (code >= 200 && code < 300) {
                            input = connection.getInputStream();
                            bitmap = BitmapFactory.decodeStream(input);
                            if (bitmap != null) {
                                try {
                                    output = new FileOutputStream(cache);
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
                                } catch (Throwable ignoredCache) {
                                }
                            }
                        }
                    } catch (Throwable ignoredNetwork) {
                    } finally {
                        if (input != null) try { input.close(); } catch (Throwable ignored) { }
                        if (output != null) try { output.close(); } catch (Throwable ignored) { }
                        if (connection != null) connection.disconnect();
                    }
                }

                final Bitmap result = bitmap;
                if (result != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!isUiAlive()) {
                                return;
                            }
                            target.setImageBitmap(result);
                        }
                    });
                }
            }
        });
    }

    private void buildDrawer() {
        drawerScrim = new View(this);
        drawerScrim.setBackgroundColor(Color.argb(145, 0, 0, 0));
        drawerScrim.setVisibility(View.GONE);
        drawerScrim.setClickable(true);

        root.addView(
                drawerScrim,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        if (Build.VERSION.SDK_INT >= 21) {
            drawerScrim.setElevation(2f);
        }

        drawerScrim.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setDrawerOpen(false);
            }
        });

        /*
         * Responsive drawer:
         * - 78% of the screen on compact/normal devices
         * - minimum 300dp
         * - maximum 430dp
         *
         * This prevents a giant drawer on tablets while still being
         * comfortable on phones and small Android/ChromeOS windows.
         */
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int drawerWidth = (int) (screenWidth * 0.78f);
        drawerWidth = Math.max(drawerWidth, dp(300));
        drawerWidth = Math.min(drawerWidth, dp(430));
        drawerWidth = Math.min(drawerWidth, screenWidth - dp(16));
        drawerWidthPx = drawerWidth;

        drawer = new FrameLayout(this);
        drawer.setBackground(roundDrawer());
        drawer.setWillNotDraw(false);
        drawer.setTranslationX(-drawerWidthPx);
        drawer.setVisibility(View.GONE);
        drawer.setClickable(true);
        drawer.setFocusable(true);

        if (!reduceAnimations) {
            drawer.setLayerType(
                    View.LAYER_TYPE_HARDWARE,
                    null
            );
            drawerScrim.setLayerType(
                    View.LAYER_TYPE_HARDWARE,
                    null
            );
        }

        if (Build.VERSION.SDK_INT >= 21) {
            drawer.setElevation(8f);
        }

        FrameLayout.LayoutParams drawerLp =
                new FrameLayout.LayoutParams(
                        drawerWidth,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Gravity.START
                );

        root.addView(drawer, drawerLp);

        final ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setClipToPadding(true);
        scrollView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setHorizontalScrollBarEnabled(false);
        scrollView.setPadding(0, 0, 0, 0);

        final LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(
                dp(18),
                dp(18),
                dp(16),
                dp(14)
        );
        content.setClipToPadding(true);

        /*
         * App header.
         */
        LinearLayout appHeader = new LinearLayout(this);
        appHeader.setOrientation(LinearLayout.HORIZONTAL);
        appHeader.setGravity(Gravity.CENTER_VERTICAL);
        appHeader.setPadding(
                dp(8),
                dp(7),
                dp(8),
                dp(10)
        );

        appIconView = new ImageView(this);
        appIconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        appIconView.setPadding(dp(7), dp(7), dp(7), dp(7));
        appIconView.setBackground(
                round(Color.argb(32, 255, 255, 255), dp(18))
        );

        try {
            android.content.pm.ApplicationInfo info =
                    getPackageManager().getApplicationInfo(
                            getPackageName(), 0
                    );

            appIconView.setImageDrawable(
                    getPackageManager().getApplicationIcon(info)
            );
        } catch (Throwable ignored) {
            appIconView.setImageDrawable(null);
        }

        appIconView.setClickable(true);
        appIconView.setFocusable(false);
        appIconView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleAppIconEasterEgg();
            }
        });

        appHeader.addView(
                appIconView,
                new LinearLayout.LayoutParams(dp(58), dp(58))
        );

        LinearLayout appTextBox = new LinearLayout(this);
        appTextBox.setOrientation(LinearLayout.VERTICAL);
        appTextBox.setGravity(Gravity.CENTER_VERTICAL);
        appTextBox.setPadding(dp(14), 0, 0, 0);

        TextView appNameView = text(
                getApplicationName(),
                19,
                Color.WHITE
        );
        appNameView.setTypeface(
                getAppFont(android.graphics.Typeface.BOLD)
        );

        appTextBox.addView(
                appNameView,
                exact(dp(31))
        );

        String version = getApplicationVersion();
        if (!TextUtils.isEmpty(version)) {
            appTextBox.addView(
                    text(version, 12, Color.argb(165, 255, 255, 255)),
                    exact(dp(20))
            );
        }

        appHeader.addView(
                appTextBox,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                )
        );

        content.addView(
                appHeader,
                exact(dp(76))
        );

        TextView title = text(
                NativeConfig.menuDeveloperTitle(),
                14,
                Color.argb(170, 255, 255, 255)
        );
        title.setTypeface(getAppFont(android.graphics.Typeface.BOLD));
        title.setPadding(dp(8), 0, 0, 0);

        content.addView(title, exact(dp(34)));

        LinearLayout developerRow =
                new LinearLayout(this);

        developerRow.setOrientation(
                LinearLayout.HORIZONTAL
        );
        developerRow.setGravity(
                Gravity.CENTER_VERTICAL
        );
        developerRow.setPadding(
                dp(8),
                0,
                dp(8),
                dp(8)
        );

        final ImageView developerAvatar =
                new ImageView(this);

        developerAvatar.setScaleType(
                ImageView.ScaleType.CENTER_CROP
        );

        GradientDrawable avatarBackground = new GradientDrawable();
        avatarBackground.setColor(Color.argb(32, 255, 255, 255));
        avatarBackground.setShape(GradientDrawable.OVAL);
        developerAvatar.setBackground(avatarBackground);
        if (Build.VERSION.SDK_INT >= 21) {
            developerAvatar.setClipToOutline(true);
        }

        try {
            int localAvatar = getResources().getIdentifier(
                    "ridhoae303",
                    "drawable",
                    getPackageName()
            );
            if (localAvatar != 0) {
                developerAvatar.setImageResource(localAvatar);
            } else {
                android.content.pm.ApplicationInfo avatarInfo =
                        getPackageManager().getApplicationInfo(
                                getPackageName(), 0
                        );
                developerAvatar.setImageDrawable(
                        getPackageManager().getApplicationIcon(avatarInfo)
                );
            }
        } catch (Throwable ignored) {
        }

        developerRow.addView(
                developerAvatar,
                new LinearLayout.LayoutParams(
                        dp(44),
                        dp(44)
                )
        );

        final DeveloperEffectTextView developer =
                new DeveloperEffectTextView(this);
        developer.setText(NativeConfig.developerName());
        developer.setTextSize(19f);
        developer.setTextColor(Color.WHITE);
        developer.setGravity(Gravity.CENTER_VERTICAL);
        developer.setTypeface(
                getDeveloperFont(android.graphics.Typeface.BOLD)
        );
        developer.setPadding(
                dp(12),
                0,
                0,
                0
        );
        developer.setSingleLine(true);

        try {
            int developerFontId = getResources().getIdentifier(
                    "ridhoae303", "font", getPackageName());
            if (developerFontId != 0 && Build.VERSION.SDK_INT >= 26) {
                developer.setTypeface(getResources().getFont(developerFontId));
            }
        } catch (Throwable ignored) {
        }

        final LinearLayout.LayoutParams developerLp =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                );
        developerLp.leftMargin = 0;
        developerLp.rightMargin = dp(6);

        developerRow.addView(
                developer,
                developerLp
        );

        bindSafeClick(
                developerAvatar,
                new Runnable() {
                    @Override
                    public void run() {
                        developerAvatar.animate().cancel();

                        developerAvatar.animate()
                                .scaleX(0.92f)
                                .scaleY(0.92f)
                                .setDuration(animDuration(70L))
                                .setInterpolator(
                                        new android.view.animation.DecelerateInterpolator()
                                )
                                .withEndAction(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                developerAvatar.animate()
                                                        .scaleX(1f)
                                                        .scaleY(1f)
                                                        .setDuration(animDuration(120L))
                                                        .setInterpolator(
                                                                new android.view.animation.OvershootInterpolator(1.6f)
                                                        )
                                                        .start();
                                            }
                                        }
                                )
                                .start();

                        developer.toggleEffects();
                    }
                }
        );

        content.addView(
                developerRow,
                exact(dp(58))
        );

        LinearLayout.LayoutParams teamDeveloperLp = rowLp();
        teamDeveloperLp.topMargin = dp(12);
        teamDeveloperLp.bottomMargin = dp(16);
        content.addView(
                makeActionRow(
                        NativeConfig.teamDeveloperText(),
                        new Runnable() {
                            @Override
                            public void run() {
                                openTeamDeveloper();
                            }
                        }
                ),
                teamDeveloperLp
        );

        loadDeveloperAvatar(
                developerAvatar
        );

        View divider = new View(this);
        divider.setBackgroundColor(
                Color.argb(40, 255, 255, 255)
        );
        content.addView(
                divider,
                exact(dp(1))
        );


        TextView socialMediaTitle = text(
                NativeConfig.socialMediaText(),
                14,
                Color.argb(170, 255, 255, 255)
        );
        socialMediaTitle.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );
        socialMediaTitle.setPadding(dp(8), dp(8), 0, 0);
        content.addView(
                socialMediaTitle,
                exact(dp(34))
        );

        content.addView(
                makeLinkRow(
                        NativeConfig.menuGithub(),
                        NativeConfig.githubUrl(),
                        "github"
                ),
                rowLp()
        );

        content.addView(
                makeLinkRow(
                        NativeConfig.menuTelegram(),
                        NativeConfig.telegramUrl(),
                        "telegram"
                ),
                rowLp()
        );

        content.addView(
                makeLinkRow(
                        NativeConfig.menuCommunity(),
                        NativeConfig.communityUrl(),
                        "whatsapp"
                ),
                rowLp()
        );

        TextView accountTitle = text(
                NativeConfig.accountsTabsText(),
                14,
                Color.argb(170, 255, 255, 255)
        );
        accountTitle.setTypeface(
                getAppFont(android.graphics.Typeface.BOLD)
        );
        accountTitle.setPadding(
                dp(8),
                dp(10),
                0,
                0
        );
        content.addView(
                accountTitle,
                exact(dp(38))
        );

        LinearLayout tabActionRow = new LinearLayout(this);
        tabActionRow.setOrientation(LinearLayout.HORIZONTAL);
        tabActionRow.setGravity(Gravity.CENTER_VERTICAL);

        View closeAllButton = makeActionRow(
                NativeConfig.closeAllTabsText(),
                new Runnable() {
                    @Override
                    public void run() {
                        closeAllTabsRequested();
                    }
                }
        );

        View newTabButton = makeActionRow(
                NativeConfig.newTabText(),
                new Runnable() {
                    @Override
                    public void run() {
                        createTabRequested();
                    }
                }
        );

        LinearLayout.LayoutParams firstButtonLp =
                new LinearLayout.LayoutParams(0, dp(58), 1f);
        firstButtonLp.topMargin = dp(7);
        tabActionRow.addView(closeAllButton, firstButtonLp);

        LinearLayout.LayoutParams secondButtonLp =
                new LinearLayout.LayoutParams(0, dp(58), 1f);
        secondButtonLp.topMargin = dp(7);
        secondButtonLp.leftMargin = dp(7);
        tabActionRow.addView(newTabButton, secondButtonLp);

        content.addView(
                tabActionRow,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(65)
                )
        );

        tabMenuContainer =
                new LinearLayout(this);

        tabMenuContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        content.addView(
                tabMenuContainer,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        refreshTabMenu();

        TextView toolsTitle = text(
                NativeConfig.toolsText(),
                14,
                Color.argb(170, 255, 255, 255)
        );
        toolsTitle.setTypeface(
                getAppFont(android.graphics.Typeface.BOLD)
        );
        toolsTitle.setPadding(
                dp(8),
                dp(12),
                0,
                0
        );
        content.addView(
                toolsTitle,
                exact(dp(38))
        );

        // Collapsible switch drawer. The body is a real child of the drawer,
        // not an overlay, and its touchability follows its visible state.
        final LinearLayout switchDrawer =
                new LinearLayout(this);
        switchDrawer.setOrientation(LinearLayout.VERTICAL);
        switchDrawer.setBackground(
                round(
                        Color.argb(18, 255, 255, 255),
                        dp(16)
                )
        );

        final LinearLayout switchDrawerHeader =
                new LinearLayout(this);
        switchDrawerHeader.setOrientation(LinearLayout.HORIZONTAL);
        switchDrawerHeader.setGravity(Gravity.CENTER_VERTICAL);
        switchDrawerHeader.setPadding(
                dp(10),
                0,
                dp(8),
                0
        );
        switchDrawerHeader.setBackground(
                round(Color.argb(24, 255, 255, 255), dp(14))
        );
        switchDrawerHeader.setClickable(true);
        switchDrawerHeader.setFocusable(true);

        final TextView switchDrawerLabel =
                text(
                        NativeConfig.switchTogglesText(),
                        15,
                        Color.WHITE
                );

        switchDrawerHeader.addView(
                switchDrawerLabel,
                new LinearLayout.LayoutParams(
                        0,
                        dp(50),
                        1f
                )
        );

        final TextView switchDrawerIcon =
                text(
                        "▸",
                        20,
                        Color.argb(210, 255, 255, 255)
                );
        switchDrawerIcon.setGravity(Gravity.CENTER);

        switchDrawerHeader.addView(
                switchDrawerIcon,
                new LinearLayout.LayoutParams(
                        dp(36),
                        dp(50)
                )
        );

        final LinearLayout switchDrawerBody =
                new LinearLayout(this);
        switchDrawerBody.setOrientation(
                LinearLayout.VERTICAL
        );
        switchDrawerBody.setPadding(
                dp(6),
                0,
                dp(6),
                dp(6)
        );
        switchDrawerBody.setVisibility(View.GONE);
        switchDrawerBody.setClickable(false);
        switchDrawerBody.setFocusable(false);

        switchDrawer.addView(
                switchDrawerHeader,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(50)
                )
        );

        switchDrawer.addView(
                switchDrawerBody,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        final boolean[] switchDrawerOpened =
                new boolean[]{false};

        switchDrawerHeader.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean open = !switchDrawerOpened[0];
                        switchDrawerOpened[0] = open;
                        animateSwitchDrawer(
                                switchDrawerBody,
                                switchDrawerIcon,
                                open
                        );
                    }
                }
        );

        LinearLayout hideRow =
                buildToggleRow(
                        NativeConfig.hideMenuLabel(),
                        hideThreeDot,
                        new android.widget.CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(
                                    android.widget.CompoundButton button,
                                    boolean checked
                            ) {
                                if (checked && !hideWarningShown) {
                                    button.setChecked(false);
                                    showHideMenuWarning(
                                            (Switch) button
                                    );
                                    return;
                                }

                                setHideThreeDot(checked);
                            }
                        }
                );

        LinearLayout zoomRow =
                buildToggleRow(
                        NativeConfig.zoomLabel(),
                        zoomEnabled,
                        new android.widget.CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(
                                    android.widget.CompoundButton button,
                                    boolean checked
                            ) {
                                setZoomMode(checked);
                            }
                        }
                );

        LinearLayout hideNotificationRow =
                buildToggleRow(
                        NativeConfig.hideNotificationLabel(),
                        hideNotifications,
                        new android.widget.CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(
                                    android.widget.CompoundButton button,
                                    boolean checked
                            ) {
                                setHideNotifications(checked);
                            }
                        }
                );

        LinearLayout reduceAnimationRow =
                buildToggleRow(
                        NativeConfig.reduceAnimationLabel(),
                        reduceAnimations,
                        new android.widget.CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(
                                    android.widget.CompoundButton button,
                                    boolean checked
                            ) {
                                setReduceAnimations(checked);
                            }
                        }
                );

        switchDrawerBody.addView(
                hideRow,
                compactToggleLp()
        );
        switchDrawerBody.addView(
                zoomRow,
                compactToggleLp()
        );
        switchDrawerBody.addView(
                hideNotificationRow,
                compactToggleLp()
        );
        switchDrawerBody.addView(
                reduceAnimationRow,
                compactToggleLp()
        );

        LinearLayout.LayoutParams switchDrawerLp =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
        switchDrawerLp.topMargin = dp(6);
        switchDrawerLp.bottomMargin = dp(6);

        content.addView(
                switchDrawer,
                switchDrawerLp
        );

        content.addView(
                makeActionRow(
                        NativeConfig.dataStorageText(),
                        new Runnable() {
                            @Override
                            public void run() {
                                openDataManager();
                            }
                        }
                ),
                rowLp()
        );

        content.addView(
                makeIconActionRow(
                        NativeConfig.checkUpdatesText(),
                        new Runnable() {
                            @Override
                            public void run() {
                                if (updateCheckRunning) {
                                    return;
                                }
                                beginUpdateCheckAnimation();
                                checkForUpdates();
                            }
                        },
                        "check_updates"
                ),
                rowLp()
        );

        FrameLayout refreshRow =
                new FrameLayout(this);

        refreshRow.setBackground(
                round(
                        Color.argb(24, 255, 255, 255),
                        dp(16)
                )
        );

        TextView refreshLabel =
                text(
                        NativeConfig.refreshText(),
                        15,
                        Color.WHITE
                );

        refreshLabel.setGravity(Gravity.CENTER);
        refreshLabel.setTextAlignment(
                View.TEXT_ALIGNMENT_CENTER
        );

        FrameLayout.LayoutParams refreshTextLp =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER
                );
        refreshTextLp.leftMargin = dp(24);
        refreshTextLp.rightMargin = dp(24);

        refreshRow.addView(
                refreshLabel,
                refreshTextLp
        );

        ImageView refreshIcon = new ImageView(this);
        refreshIconView = refreshIcon;
        refreshIcon.setScaleType(
                ImageView.ScaleType.CENTER_INSIDE
        );
        refreshIcon.setPadding(dp(6), dp(6), dp(6), dp(6));

        refreshIcon.setImageResource(R.drawable.refresh);
        refreshIcon.setContentDescription(
                NativeConfig.refreshContentDescription()
        );

        FrameLayout.LayoutParams refreshIconLp =
                new FrameLayout.LayoutParams(
                        dp(36),
                        dp(36),
                        Gravity.CENTER_VERTICAL |
                                Gravity.START
                );
        refreshIconLp.leftMargin = dp(18);

        refreshRow.addView(
                refreshIcon,
                refreshIconLp
        );

        bindSafeClick(
                refreshRow,
                new Runnable() {
                    @Override
                    public void run() {
                        if (refreshIconView != null &&
                                Boolean.TRUE.equals(
                                        refreshIconView.getTag()
                                )) {
                            return;
                        }

                        beginRefreshAnimation();
                        refreshWhatsAppPage();
                    }
                }
        );

        content.addView(refreshRow, rowLp());

        // Keep utility actions together. The developer header stays focused on the creator;
        // App Lock belongs with the other practical tools.
        // Keep App Lock aligned with the same vertical rhythm as the other tool rows.
        LinearLayout.LayoutParams appLockLp = rowLp();
        content.addView(
                makeActionRow(
                        NativeConfig.appLockText(),
                        new Runnable() {
                            @Override
                            public void run() {
                                openAppLock();
                            }
                        }
                ),
                appLockLp
        );

        TextView supportTitle = text(
                NativeConfig.supportDevelopersText(),
                14,
                Color.argb(170, 255, 255, 255)
        );

        supportTitle.setTypeface(
                getAppFont(android.graphics.Typeface.BOLD)
        );
        supportTitle.setPadding(
                dp(8),
                dp(14),
                0,
                0
        );

        content.addView(
                supportTitle,
                exact(dp(40))
        );

        content.addView(
                makeDonateRow(),
                donateLp()
        );

        content.addView(
                makeSpecialRepositoryRow(),
                rowLp()
        );

        /*
         * Flexible footer: ScrollView handles small landscape screens while
         * weight=1 keeps the footer near the bottom on large displays.
         */
        final MarqueeTextView footer =
                new MarqueeTextView(this);
        footerView = footer;

        footer.setText(
                NativeConfig.footerText()
        );

        try {
            int footerFontId =
                    getResources().getIdentifier(
                            "ridhoae303",
                            "font",
                            getPackageName()
                    );

            if (footerFontId != 0 &&
                    Build.VERSION.SDK_INT >= 26) {
                footer.setTypeface(
                        getResources().getFont(
                                footerFontId
                        )
                );
            } else {
                footer.setTypeface(
                        android.graphics.Typeface.create(
                                "sans-serif-medium",
                                android.graphics.Typeface.NORMAL
                        )
                );
            }
        } catch (Throwable ignored) {
            footer.setTypeface(
                    android.graphics.Typeface.create(
                            "sans-serif-medium",
                            android.graphics.Typeface.NORMAL
                    )
            );
        }
        footer.setTextSize(13);
        footer.setTextColor(
                Color.argb(210, 255, 255, 255)
        );
        footer.setGravity(
                Gravity.CENTER_VERTICAL
        );
        footer.setPadding(
                dp(12),
                0,
                dp(12),
                0
        );
        footer.setBackground(
                round(
                        Color.argb(
                                28,
                                255,
                                255,
                                255
                        ),
                        dp(18)
                )
        );

        LinearLayout.LayoutParams footerLp =
                exact(dp(40));

        footerLp.topMargin = dp(18);

        content.addView(
                footer,
                footerLp
        );

        /*
         * IMPORTANT:
         * The content must first be attached to the ScrollView and then the
         * ScrollView must be attached to the drawer. The old version missed
         * this hierarchy, which produced the empty black navigation panel.
         */
        scrollView.addView(
                content,
                new ScrollView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        drawer.addView(
                scrollView,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        drawer.post(
                new Runnable() {
                    @Override
                    public void run() {
                        drawer.requestLayout();
                        scrollView.requestLayout();
                        content.requestLayout();
                        footer.requestLayout();
                    }
                }
        );
    }

    private String getApplicationName() {
        try {
            android.content.pm.ApplicationInfo info =
                    getPackageManager().getApplicationInfo(
                            getPackageName(),
                            0
                    );

            CharSequence label =
                    getPackageManager().getApplicationLabel(info);

            if (label != null && label.length() > 0) {
                return label.toString();
            }
        } catch (Throwable ignored) {
        }

        return NativeConfig.appFallbackName();
    }

    private String getApplicationVersion() {
        try {
            PackageInfo info =
                    getPackageManager().getPackageInfo(
                            getPackageName(),
                            0
                    );

            if (info.versionName != null &&
                    info.versionName.length() > 0) {
                return "v" + info.versionName;
            }
        } catch (Throwable ignored) {
        }

        return "";
    }

    private LinearLayout.LayoutParams exact(int height) {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                height
        );
    }

    private LinearLayout.LayoutParams compactToggleLp() {
        LinearLayout.LayoutParams lp = exact(dp(52));
        lp.topMargin = dp(3);
        lp.bottomMargin = dp(3);
        return lp;
    }

    private LinearLayout.LayoutParams rowLp() {
        LinearLayout.LayoutParams lp = exact(dp(58));
        lp.topMargin = dp(7);
        return lp;
    }

    private LinearLayout.LayoutParams donateLp() {
        LinearLayout.LayoutParams lp = exact(dp(58));
        lp.topMargin = dp(12);
        return lp;
    }

    private void bindSafeClick(
            final View view,
            final Runnable action
    ) {
        view.setClickable(true);

        view.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View clicked) {
                        if (action != null) {
                            action.run();
                        }
                    }
                }
        );

        /*
         * Let Android's normal touch/click machinery and ScrollView handle
         * movement. Standard View click handling already respects touch slop,
         * so a press-then-drag is not converted into a click.
         */
        view.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(
                            View v,
                            MotionEvent event
                    ) {
                        if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                            // Do not consume MOVE; this allows ScrollView to
                            // take over when the finger becomes a scroll.
                            return false;
                        }
                        return false;
                    }
                }
        );
    }


    private boolean hasAllFilesAccess() {
        if (Build.VERSION.SDK_INT < 30) {
            return true;
        }

        try {
            return Environment.isExternalStorageManager();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void prepareRenaDirectories() {
        if (Build.VERSION.SDK_INT >= 30 &&
                !hasAllFilesAccess()) {
            return;
        }

        File rootDir =
                new File(
                        Environment.getExternalStorageDirectory(),
                        "Rena"
                );

        if (!rootDir.exists()) {
            rootDir.mkdirs();
        }

        String[] names = new String[]{
                "Audio",
                "Documents",
                "Images",
                "Videos",
                "Voice Notes",
                "Other",
                "Updates"
        };

        for (String name : names) {
            File folder =
                    new File(
                            rootDir,
                            name
                    );

            if (!folder.exists()) {
                folder.mkdirs();
            }
        }
    }

    private void requestAllFilesAccessForDownload() {
        new AlertDialog.Builder(this)
                .setTitle(
                        NativeConfig.storageAccessTitle()
                )
                .setMessage(
                        NativeConfig.storageAccessMessage()
                )
                .setNegativeButton(
                        NativeConfig.cancelText(),
                        null
                )
                .setPositiveButton(
                        NativeConfig.storageSettingsButton(),
                        new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(
                                    android.content.DialogInterface dialog,
                                    int which
                            ) {
                                try {
                                    Intent intent =
                                            new Intent(
                                                    android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                                    Uri.parse(
                                                            "package:" +
                                                            getPackageName()
                                                    )
                                            );

                                    startActivity(
                                            intent
                                    );
                                } catch (Throwable ignored) {
                                    try {
                                        startActivity(
                                                new Intent(
                                                        android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                                                )
                                        );
                                    } catch (Throwable ignoredAgain) {
                                    }
                                }
                            }
                        }
                )
                .show();
    }

    private String downloadCategory(
            String mimeType,
            String fileName
    ) {
        String name =
                fileName == null
                        ? ""
                        : fileName.toLowerCase(
                                Locale.US
                        );

        if (mimeType != null) {
            if (mimeType.startsWith("image/")) {
                return "Images";
            }

            if (mimeType.startsWith("video/")) {
                return "Videos";
            }

            if (mimeType.startsWith("audio/")) {
                if (name.contains("voice") ||
                        name.contains("ptt")) {
                    return "Voice Notes";
                }

                return "Audio";
            }
        }

        if (name.endsWith(".pdf") ||
                name.endsWith(".doc") ||
                name.endsWith(".docx") ||
                name.endsWith(".xls") ||
                name.endsWith(".xlsx") ||
                name.endsWith(".ppt") ||
                name.endsWith(".pptx") ||
                name.endsWith(".txt")) {
            return "Documents";
        }

        return "Other";
    }

    private void enqueueWebDownload(
            String url,
            String userAgent,
            String contentDisposition,
            String mimetype
    ) {
        if (Build.VERSION.SDK_INT >= 30 &&
                !hasAllFilesAccess()) {
            pendingWebDownloadUrl = url;
            pendingWebDownloadUserAgent = userAgent;
            pendingWebDownloadContentDisposition =
                    contentDisposition;
            pendingWebDownloadMimeType = mimetype;
            requestAllFilesAccessForDownload();
            return;
        }

        try {
            prepareRenaDirectories();

            String fileName =
                    URLUtil.guessFileName(
                            url,
                            contentDisposition,
                            mimetype
                    );

            if (TextUtils.isEmpty(fileName)) {
                fileName = "download";
            }

            File category =
                    new File(
                            new File(
                                    Environment.getExternalStorageDirectory(),
                                    "Rena"
                            ),
                            downloadCategory(
                                    mimetype,
                                    fileName
                            )
                    );

            if (!category.exists() &&
                    !category.mkdirs()) {
                return;
            }

            File target =
                    new File(
                            category,
                            fileName
                    );

            int suffix = 2;

            while (target.exists()) {
                String base = fileName;
                String ext = "";

                int dot =
                        fileName.lastIndexOf('.');

                if (dot > 0) {
                    base =
                            fileName.substring(
                                    0,
                                    dot
                            );
                    ext =
                            fileName.substring(
                                    dot
                            );
                }

                target =
                        new File(
                                category,
                                base +
                                " (" +
                                suffix +
                                ")" +
                                ext
                        );

                suffix++;
            }

            DownloadManager manager =
                    (DownloadManager)
                            getSystemService(
                                    DOWNLOAD_SERVICE
                            );

            if (manager == null) {
                return;
            }

            DownloadManager.Request request =
                    new DownloadManager.Request(
                            Uri.parse(url)
                    );

            request.setNotificationVisibility(
                    DownloadManager.Request
                            .VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            );

            request.setTitle(
                    fileName
            );

            request.setMimeType(
                    TextUtils.isEmpty(mimetype)
                            ? "application/octet-stream"
                            : mimetype
            );

            if (!TextUtils.isEmpty(userAgent)) {
                request.addRequestHeader(
                        "User-Agent",
                        userAgent
                );
            }

            request.setDestinationUri(
                    Uri.fromFile(target)
            );

            manager.enqueue(request);

        } catch (Throwable ignored) {
            Toast.makeText(
                    this,
                    NativeConfig.downloadDeniedText(),
                    Toast.LENGTH_SHORT
            ).show();
        }
    }
    private void openAppLock() {
        try {
            setDrawerOpen(false);
            Intent intent = new Intent(
                    this,
                    AppLockActivity.class
            );
            startActivity(intent);
            overridePendingTransition(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
            );
        } catch (Throwable ignored) {
        }
    }

    private void openTeamDeveloper() {
        try {
            Intent intent =
                    new Intent(
                            MainActivity.this,
                            TeamDeveloperActivity.class
                    );

            startActivity(intent);

            if (!reduceAnimations) {
                overridePendingTransition(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left
                );
            } else {
                overridePendingTransition(0, 0);
            }
        } catch (Throwable ignored) {
        }
    }

    private void openDataManager() {
        if (dataManagerOpening) {
            return;
        }

        dataManagerOpening = true;

        root.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Intent dataIntent =
                            new Intent(
                                    MainActivity.this,
                                    DataManagerActivity.class
                            );

                    dataIntent.putExtra(
                            "tab_id",
                            currentTabId()
                    );

                    TabState dataTab = tabStateForActiveTab();
                    if (dataTab != null &&
                            !TextUtils.isEmpty(dataTab.profileId)) {
                        dataIntent.putExtra(
                                "profile_id",
                                dataTab.profileId
                        );
                    }

                    startActivityForResult(
                            dataIntent,
                            REQ_DATA_MANAGER
                    );

                    setDrawerOpen(false);

                    overridePendingTransition(
                            R.anim.slide_in_right,
                            R.anim.slide_out_left
                    );
                } catch (Throwable ignored) {
                    dataManagerOpening = false;
                }
            }
        });
    }

    private void beginUpdateCheckAnimation() {
        Toast.makeText(
                this,
                NativeConfig.checkingUpdatesText(),
                Toast.LENGTH_SHORT
        ).show();

        if (updateIconView == null) {
            return;
        }

        updateIconView.animate().cancel();
        updateIconView.setRotation(0f);
        updateIconView.setTag(Boolean.TRUE);
        updateIconView.animate()
                .rotationBy(360f)
                .setDuration(animDuration(820L))
                .setInterpolator(new android.view.animation.LinearInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (updateIconView != null &&
                                Boolean.TRUE.equals(updateIconView.getTag())) {
                            beginUpdateIconLoop();
                        }
                    }
                })
                .start();
    }

    private void beginUpdateIconLoop() {
        if (updateIconView == null ||
                !Boolean.TRUE.equals(updateIconView.getTag())) {
            return;
        }
        updateIconView.setRotation(0f);
        updateIconView.animate()
                .rotationBy(360f)
                .setDuration(animDuration(820L))
                .setInterpolator(new android.view.animation.LinearInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (updateIconView != null &&
                                Boolean.TRUE.equals(updateIconView.getTag())) {
                            beginUpdateIconLoop();
                        }
                    }
                })
                .start();
    }

    private void finishUpdateCheckAnimation() {
        if (updateIconView == null) {
            return;
        }

        updateIconView.setTag(Boolean.FALSE);
        float current = updateIconView.getRotation();
        float normalized = current % 360f;
        if (normalized < 0f) {
            normalized += 360f;
        }
        float distance = 360f - normalized;
        if (distance < 30f) {
            distance += 360f;
        }

        updateIconView.animate().cancel();
        updateIconView.animate()
                .rotationBy(distance)
                .setDuration(animDuration(380L))
                .setInterpolator(new android.view.animation.DecelerateInterpolator(2.0f))
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (updateIconView != null) {
                            updateIconView.setRotation(0f);
                        }
                    }
                })
                .start();
    }

    private void beginRefreshAnimation() {
        if (refreshIconView == null) {
            return;
        }
        refreshIconView.animate().cancel();
        refreshIconView.setRotation(0f);
        refreshIconView.setTag(Boolean.TRUE);
        refreshIconView.animate()
                .rotationBy(360f)
                .setDuration(820L)
                .setInterpolator(new android.view.animation.LinearInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (refreshIconView != null &&
                                Boolean.TRUE.equals(refreshIconView.getTag())) {
                            beginRefreshLoop();
                        }
                    }
                })
                .start();
    }

    private void beginRefreshLoop() {
        if (refreshIconView == null ||
                !Boolean.TRUE.equals(refreshIconView.getTag())) {
            return;
        }
        refreshIconView.setRotation(0f);
        refreshIconView.animate()
                .rotationBy(360f)
                .setDuration(820L)
                .setInterpolator(new android.view.animation.LinearInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (refreshIconView != null &&
                                Boolean.TRUE.equals(refreshIconView.getTag())) {
                            beginRefreshLoop();
                        }
                    }
                })
                .start();
    }

    private void finishRefreshAnimation() {
        if (refreshIconView == null) {
            return;
        }
        refreshIconView.setTag(Boolean.FALSE);
        float current = refreshIconView.getRotation();
        float normalized = current % 360f;
        if (normalized < 0f) {
            normalized += 360f;
        }
        float distance = 360f - normalized;
        if (distance < 24f) {
            distance += 360f;
        }
        refreshIconView.animate().cancel();
        refreshIconView.animate()
                .rotationBy(distance)
                .setDuration(animDuration(420L))
                .setInterpolator(new android.view.animation.DecelerateInterpolator(2.0f))
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (refreshIconView != null) {
                            refreshIconView.setRotation(0f);
                        }
                    }
                })
                .start();
    }

    private void checkForUpdates() {
        if (updateCheckRunning) {
            return;
        }

        if (Build.VERSION.SDK_INT >= 26 &&
                !getPackageManager()
                        .canRequestPackageInstalls()) {

            new AlertDialog.Builder(this)
                    .setTitle(
                            NativeConfig.updateInstallPermissionTitle()
                    )
                    .setMessage(
                            NativeConfig.updateInstallPermissionMessage()
                    )
                    .setNegativeButton(
                            NativeConfig.cancelText(),
                            new android.content.DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(
                                        android.content.DialogInterface dialog,
                                        int which
                                ) {
                                    pendingUpdatePermissionCheck = false;
                                    finishUpdateCheckAnimation();
                                }
                            }
                    )
                    .setPositiveButton(
                            NativeConfig.storageSettingsButton(),
                            new android.content.DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(
                                        android.content.DialogInterface dialog,
                                        int which
                                ) {
                                    pendingUpdatePermissionCheck = true;

                                    try {
                                        startActivity(
                                                new Intent(
                                                        android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                                        Uri.parse(
                                                                "package:" +
                                                                getPackageName()
                                                        )
                                                )
                                        );
                                    } catch (Throwable ignored) {
                                    }
                                }
                            }
                    )
                    .show();

            return;
        }

        if (!updaterRunning.compareAndSet(
                false,
                true
        )) {
            return;
        }

        updateCheckRunning = true;

        AsyncTask.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        performUpdateCheck();
                    }
                }
        );
    }

    private void performUpdateCheck() {
        HttpURLConnection connection = null;

        try {
            URL api =
                    new URL(
                            NativeConfig.updaterApiUrl()
                    );

            connection =
                    (HttpURLConnection)
                            api.openConnection();

            connection.setConnectTimeout(
                    8000
            );
            connection.setReadTimeout(
                    10000
            );

            connection.setRequestProperty(
                    "Accept",
                    "application/vnd.github+json"
            );

            connection.setRequestProperty(
                    "User-Agent",
                    NativeConfig.developerName()
            );

            if (connection.getResponseCode() < 200 ||
                    connection.getResponseCode() >= 300) {
                throw new java.io.IOException(
                        NativeConfig.githubRequestFailedText()
                );
            }

            InputStream input =
                    new BufferedInputStream(
                            connection.getInputStream()
                    );

            StringBuilder body =
                    new StringBuilder();

            byte[] buffer =
                    new byte[4096];

            int read;

            while (
                    (read =
                            input.read(buffer)) != -1
            ) {
                body.append(
                        new String(
                                buffer,
                                0,
                                read,
                                "UTF-8"
                        )
                );

                if (body.length() >
                        1024 * 1024) {
                    break;
                }
            }

            input.close();

            final String json =
                    body.toString();

            final String remoteVersion =
                    parseJsonString(
                            json,
                            "tag_name"
                    );

            final int remoteCode =
                    parseJsonInt(
                            json,
                            "versionCode",
                            -1
                    );

            final ApkAssetInfo apkAsset =
                    parseApkAsset(json);

            final String apkUrl =
                    apkAsset.url;

            final String digest =
                    apkAsset.digest;

            runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            if (!isUiAlive()) {
                                return;
                            }
                            finishUpdateCheckAnimation();
                            updaterRunning
                                    .set(false);
                            updateCheckRunning = false;

                            int comparison;

                            if (remoteCode >= 0) {
                                int installedCode =
                                        getInstalledVersionCode();

                                comparison =
                                        remoteCode < installedCode
                                                ? -1
                                                : remoteCode > installedCode
                                                        ? 1
                                                        : 0;
                            } else {
                                comparison =
                                        compareVersions(
                                                getInstalledVersionName(),
                                                remoteVersion
                                        );
                            }

                            if (comparison <= 0) {
                                Toast.makeText(
                                        MainActivity.this,
                                        comparison < 0
                                                ? NativeConfig.updateNotAvailableText()
                                                : NativeConfig.upToDateText(),
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            if (TextUtils.isEmpty(
                                    apkUrl
                            ) ||
                                    TextUtils.isEmpty(
                                            digest
                                    )) {

                                openExternal(
                                        NativeConfig.githubRepositoryUrl() +
                                        "/releases/latest"
                                );

                                Toast.makeText(
                                        MainActivity.this,
                                        NativeConfig.updateDigestMissingText(),
                                        Toast.LENGTH_LONG
                                ).show();

                                return;
                            }

                            showUpdateDialog(
                                    remoteVersion,
                                    apkUrl,
                                    digest
                            );
                        }
                    }
            );

        } catch (Throwable ignored) {
            runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            if (!isUiAlive()) {
                                return;
                            }
                            finishUpdateCheckAnimation();
                            updaterRunning.set(false);
                            updateCheckRunning = false;

                            Toast.makeText(
                                    MainActivity.this,
                                    NativeConfig.updateDownloadFailedText(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
            );

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private int parseJsonInt(
            String json,
            String key,
            int fallback
    ) {
        try {
            java.util.regex.Matcher matcher =
                    java.util.regex.Pattern
                            .compile(
                                    "\\\"" +
                                    java.util.regex.Pattern.quote(key) +
                                    "\\\"\\s*:\\s*(\\d+)"
                            )
                            .matcher(
                                    json
                            );

            if (matcher.find()) {
                return Integer.parseInt(
                        matcher.group(1)
                );
            }
        } catch (Throwable ignored) {
        }

        return fallback;
    }

    private int getInstalledVersionCode() {
        try {
            PackageInfo info =
                    getPackageManager()
                            .getPackageInfo(
                                    getPackageName(),
                                    0
                            );

            if (Build.VERSION.SDK_INT >= 28) {
                return (int) info.getLongVersionCode();
            }

            return info.versionCode;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private String parseJsonString(
            String json,
            String key
    ) {
        try {
            java.util.regex.Matcher matcher =
                    java.util.regex.Pattern
                            .compile(
                                    "\\\"" +
                                    java.util.regex.Pattern.quote(key) +
                                    "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\""
                            )
                            .matcher(
                                    json
                            );

            if (matcher.find()) {
                return matcher.group(
                        1
                );
            }
        } catch (Throwable ignored) {
        }

        return "";
    }

    private ApkAssetInfo parseApkAsset(String json) {
        ApkAssetInfo info = new ApkAssetInfo();

        try {
            org.json.JSONObject release =
                    new org.json.JSONObject(json);

            org.json.JSONArray assets =
                    release.optJSONArray("assets");

            if (assets == null) {
                return info;
            }

            for (int i = 0; i < assets.length(); i++) {
                org.json.JSONObject asset =
                        assets.optJSONObject(i);

                if (asset == null) {
                    continue;
                }

                String name =
                        asset.optString("name", "");

                if (!name.toLowerCase(Locale.US)
                        .endsWith(".apk")) {
                    continue;
                }

                String url =
                        asset.optString(
                                "browser_download_url",
                                ""
                        );

                String digest =
                        asset.optString(
                                "digest",
                                ""
                        );

                if (digest.startsWith("sha256:")) {
                    digest = digest.substring(7);
                }

                if (TextUtils.isEmpty(url) ||
                        !digest.matches(
                                "[0-9a-fA-F]{64}"
                        )) {
                    continue;
                }

                info.url = url;
                info.digest = digest.toLowerCase(Locale.US);
                return info;
            }
        } catch (Throwable ignored) {
        }

        return info;
    }

    private String getInstalledVersionName() {
        try {
            PackageInfo info =
                    getPackageManager()
                            .getPackageInfo(
                                    getPackageName(),
                                    0
                            );

            return info.versionName == null
                    ? ""
                    : info.versionName;

        } catch (Throwable ignored) {
            return "";
        }
    }

    private int compareVersions(
            String installed,
            String remote
    ) {
        String a =
                stripVersionPrefix(
                        installed
                );

        String b =
                stripVersionPrefix(
                        remote
                );

        if (a.length() == 0 ||
                b.length() == 0) {
            return 0;
        }

        String[] pa =
                a.split("\\.");

        String[] pb =
                b.split("\\.");

        int max =
                Math.max(
                        pa.length,
                        pb.length
                );

        for (int i = 0; i < max; i++) {
            int va =
                    i < pa.length
                            ? safeVersionPart(
                                    pa[i]
                            )
                            : 0;

            int vb =
                    i < pb.length
                            ? safeVersionPart(
                                    pb[i]
                            )
                            : 0;

            if (va < vb) {
                return 1;
            }

            if (va > vb) {
                return -1;
            }
        }

        return 0;
    }

    private String stripVersionPrefix(
            String version
    ) {
        if (version == null) {
            return "";
        }

        String result =
                version.trim();

        if (result.startsWith("v") ||
                result.startsWith("V")) {
            result =
                    result.substring(1);
        }

        return result;
    }

    private int safeVersionPart(
            String value
    ) {
        try {
            String digits =
                    value.replaceAll(
                            "[^0-9].*$",
                            ""
                    );

            return digits.length() == 0
                    ? 0
                    : Integer.parseInt(
                            digits
                    );

        } catch (Throwable ignored) {
            return 0;
        }
    }

    private void showUpdateDialog(
            final String version,
            final String apkUrl,
            final String digest
    ) {
        new AlertDialog.Builder(this)
                .setTitle(
                        NativeConfig.updateAvailableTitle()
                )
                .setMessage(
                        version
                )
                .setNegativeButton(
                        NativeConfig.cancelText(),
                        null
                )
                .setPositiveButton(
                        NativeConfig.downloadUpdateText(),
                        new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(
                                    android.content.DialogInterface dialog,
                                    int which
                            ) {
                                downloadAndInstallApk(
                                        apkUrl,
                                        digest
                                );
                            }
                        }
                )
                .show();
    }

    private void downloadAndInstallApk(
            final String apkUrl,
            final String expectedDigest
    ) {
        if (Build.VERSION.SDK_INT >= 30 &&
                !hasAllFilesAccess()) {
            pendingUpdateApkUrl = apkUrl;
            pendingUpdateDigest = expectedDigest;
            requestAllFilesAccessForDownload();
            return;
        }

        AsyncTask.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        File output = null;
                        HttpURLConnection connection = null;

                        try {
                            File updates =
                                    new File(
                                            new File(
                                                    Environment.getExternalStorageDirectory(),
                                                    "Rena"
                                            ),
                                            "Updates"
                                    );

                            if (!updates.exists() &&
                                    !updates.mkdirs()) {
                                throw new java.io.IOException(
                                        "Unable to create update directory"
                                );
                            }

                            output =
                                    new File(
                                            updates,
                                            "Rena-W4B-update.apk"
                                    );

                            URL url =
                                    new URL(
                                            apkUrl
                                    );

                            connection =
                                    (HttpURLConnection)
                                            url.openConnection();

                            connection.setConnectTimeout(
                                    10000
                            );

                            connection.setReadTimeout(
                                    30000
                            );

                            connection.setRequestProperty(
                                    "User-Agent",
                                    NativeConfig.developerName()
                            );

                            if (connection.getResponseCode() < 200 ||
                                    connection.getResponseCode() >= 300) {
                                throw new java.io.IOException(
                                        "APK download failed"
                                );
                            }

                            InputStream input =
                                    new BufferedInputStream(
                                            connection.getInputStream()
                                    );

                            FileOutputStream outputStream =
                                    new FileOutputStream(
                                            output
                                    );

                            MessageDigest digest =
                                    MessageDigest.getInstance(
                                            "SHA-256"
                                    );

                            byte[] buffer =
                                    new byte[8192];

                            int read;

                            while (
                                    (read =
                                            input.read(
                                                    buffer
                                            )) != -1
                            ) {
                                digest.update(
                                        buffer,
                                        0,
                                        read
                                );

                                outputStream.write(
                                        buffer,
                                        0,
                                        read
                                );
                            }

                            outputStream.flush();
                            outputStream.close();
                            input.close();

                            String actual =
                                    toHex(
                                            digest.digest()
                                    );

                            if (!expectedDigest
                                    .equalsIgnoreCase(
                                            actual
                                    )) {
                                throw new SecurityException(
                                        "APK digest mismatch"
                                );
                            }

                            if (!verifyApkSigner(
                                    output
                            )) {
                                throw new SecurityException(
                                        "APK signer mismatch"
                                );
                            }

                            final File installerFile =
                                    output;

                            runOnUiThread(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            if (!isUiAlive()) {
                                                return;
                                            }
                                            launchApkInstaller(
                                                    installerFile
                                            );
                                        }
                                    }
                            );

                        } catch (Throwable ignored) {
                            if (output != null) {
                                try {
                                    output.delete();
                                } catch (Throwable ignoredDelete) {
                                }
                            }

                            runOnUiThread(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            if (!isUiAlive()) {
                                                return;
                                            }
                                            Toast.makeText(
                                                    MainActivity.this,
                                                    NativeConfig.updateDownloadFailedText(),
                                                    Toast.LENGTH_LONG
                                            ).show();
                                        }
                                    }
                            );

                        } finally {
                            if (connection != null) {
                                connection.disconnect();
                            }
                        }
                    }
                }
        );
    }

    private String toHex(
            byte[] bytes
    ) {
        StringBuilder result =
                new StringBuilder(
                        bytes.length * 2
                );

        for (byte b : bytes) {
            result.append(
                    String.format(
                            Locale.US,
                            "%02x",
                            b & 0xff
                    )
            );
        }

        return result.toString();
    }

    private boolean verifyApkSigner(File apk) {
        if (apk == null || !apk.isFile()) {
            return false;
        }
        try {
            return NativeConfig.verifyApkSigner(this, apk.getAbsolutePath());
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void launchApkInstaller(
            File apk
    ) {
        try {
            Uri uri =
                    UpdateFileProvider.buildUri(
                            this,
                            apk
                    );

            Intent install =
                    new Intent(
                            Intent.ACTION_VIEW
                    );

            install.setDataAndType(
                    uri,
                    "application/vnd.android.package-archive"
            );

            install.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            startActivity(
                    install
            );

        } catch (Throwable ignored) {
            Toast.makeText(
                    this,
                    NativeConfig.updateDownloadFailedText(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }
    private View makeSpecialRepositoryRow() {
        final ShimmerDonateView star =
                new ShimmerDonateView(this);

        star.setText(
                NativeConfig.starRepositoryText()
        );
        star.setIcon(
                loadIconDrawable("repository_star")
        );

        bindSafeClick(
                star,
                new Runnable() {
                    @Override
                    public void run() {
                        star.playShimmer();
                        openExternal(
                                NativeConfig.githubRepositoryUrl()
                        );
                    }
                }
        );

        return star;
    }

    private View makeDonateRow() {
        final ShimmerDonateView donate =
                new ShimmerDonateView(this);

        donate.setText(
                NativeConfig.donateText()
        );
        donate.setIcon(
                loadIconDrawable("donate")
        );

        bindSafeClick(
                donate,
                new Runnable() {
                    @Override
                    public void run() {
                        donate.playShimmer();
                        openExternal(
                                NativeConfig.donateUrl()
                        );
                    }
                }
        );

        return donate;
    }

    private View makeIconActionRow(
            String label,
            final Runnable action,
            String assetName
    ) {
        FrameLayout row = new FrameLayout(this);
        row.setBackground(
                round(
                        Color.argb(24, 255, 255, 255),
                        dp(16)
                )
        );

        ImageView icon = new ImageView(this);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        icon.setPadding(dp(6), dp(6), dp(6), dp(6));

        int iconResourceId;
        if ("check_updates".equals(assetName)) {
            // Keep a direct R reference so shrinkResources/R8 cannot remove
            // the drawable merely because the action name is resolved dynamically.
            iconResourceId = R.drawable.check_updates;
        } else {
            iconResourceId = getResources().getIdentifier(
                    assetName,
                    "drawable",
                    getPackageName()
            );
        }

        if (iconResourceId != 0) {
            icon.setImageResource(iconResourceId);
            icon.setVisibility(View.VISIBLE);
            icon.setAlpha(1.0f);
        }

        if ("check_updates".equals(assetName)) {
            updateIconView = icon;
            icon.setContentDescription(NativeConfig.checkUpdatesText());
        }

        FrameLayout.LayoutParams iconLp =
                new FrameLayout.LayoutParams(
                        dp(36),
                        dp(36),
                        Gravity.CENTER_VERTICAL | Gravity.START
                );
        iconLp.leftMargin = dp(18);
        row.addView(icon, iconLp);

        // The label is centered against the whole button; the icon keeps its
        // original left position instead of stealing part of the text's center.
        TextView labelView = text(label, 15, Color.WHITE);
        labelView.setGravity(Gravity.CENTER);
        labelView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        FrameLayout.LayoutParams labelLp =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER
                );
        labelLp.leftMargin = dp(8);
        labelLp.rightMargin = dp(8);
        row.addView(labelView, labelLp);
        icon.bringToFront();

        bindSafeClick(row, action);
        return row;
    }

    private View makeLinkRow(
            String label,
            final String url,
            String assetName
    ) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(
                dp(12),
                0,
                dp(10),
                0
        );
        row.setBackground(
                round(Color.argb(24, 255, 255, 255), dp(16))
        );

        FrameLayout iconSlot = new FrameLayout(this);
        iconSlot.setBackground(
                round(
                        Color.argb(22, 255, 255, 255),
                        dp(13)
                )
        );

        ImageView iconView = new ImageView(this);
        iconView.setScaleType(
                ImageView.ScaleType.CENTER_INSIDE
        );
        iconView.setAdjustViewBounds(false);
        iconView.setPadding(
                dp(7),
                dp(7),
                dp(7),
                dp(7)
        );

        Drawable asset = loadIconDrawable(assetName);

        if (asset != null) {
            iconView.setImageDrawable(asset);
        }

        iconSlot.addView(
                iconView,
                new FrameLayout.LayoutParams(
                        dp(34),
                        dp(34),
                        Gravity.CENTER
                )
        );

        /*
         * The slot is the rounded shape. The actual icon stays visually
         * independent inside it. Large source images therefore never alter
         * row sizing or turn the icon itself into a giant card.
         */
        row.addView(
                iconSlot,
                new LinearLayout.LayoutParams(
                        dp(44),
                        dp(44)
                )
        );

        TextView labelView = text(
                label,
                15,
                Color.WHITE
        );
        labelView.setPadding(
                dp(8),
                0,
                0,
                0
        );

        row.addView(
                labelView,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        1f
                )
        );

        bindSafeClick(
                row,
                new Runnable() {
                    @Override
                    public void run() {
                        openExternal(url);
                    }
                }
        );

        return row;
    }

    private Drawable loadIconDrawable(
            String resourceName
    ) {
        try {
            int resId =
                    getResources().getIdentifier(
                            resourceName,
                            "drawable",
                            getPackageName()
                    );

            if (resId == 0) {
                return null;
            }

            return getResources().getDrawable(
                    resId,
                    getTheme()
            );

        } catch (Throwable ignored) {
            return null;
        }
    }

    private void handleAppIconEasterEgg() {
        long now =
                System.currentTimeMillis();

        if (now - appIconLastTap >
                EASTER_TAP_WINDOW_MS) {
            appIconTapCount = 0;
        }

        appIconLastTap = now;
        appIconTapCount++;

        shakeAppIcon();

        if (appIconTapCount >= 3) {
            appIconTapCount = 0;
            showRenaPreview();

            Toast.makeText(
                    this,
                    NativeConfig.easterEggFoundText(),
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void shakeAppIcon() {
        if (appIconView == null) {
            return;
        }

        appIconView.animate().cancel();
        appIconView.setRotation(0f);
        appIconView.setScaleX(1f);
        appIconView.setScaleY(1f);

        appIconView.animate()
                .rotation(-9f)
                .scaleX(1.03f)
                .scaleY(1.03f)
                .setDuration(110L)
                .withEndAction(
                        new Runnable() {
                            @Override
                            public void run() {
                                appIconView.animate()
                                        .rotation(9f)
                                        .setDuration(110L)
                                        .withEndAction(
                                                new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        appIconView.animate()
                                                                .rotation(-6f)
                                                                .setDuration(100L)
                                                                .withEndAction(
                                                                        new Runnable() {
                                                                            @Override
                                                                            public void run() {
                                                                                appIconView.animate()
                                                                                        .rotation(0f)
                                                                                        .scaleX(1f)
                                                                                        .scaleY(1f)
                                                                                        .setDuration(100L)
                                                                                        .start();
                                                                            }
                                                                        }
                                                                )
                                                                .start();
                                                    }
                                                }
                                        )
                                        .start();
                            }
                        }
                )
                .start();
    }

    private void showRenaPreview() {
        renaPreviewRetryCount = 0;

        if (renaPreviewOverlay != null) {
            if (renaImageTask == null &&
                    renaPreviewImage != null) {
                renaPreviewImage.setAlpha(0f);

                renaImageTask =
                        new RenaImageTask(
                                renaPreviewImage,
                                renaPreviewOverlay
                        );

                renaImageTask.executeOnExecutor(
                        AsyncTask.THREAD_POOL_EXECUTOR
                );
            }

            if (!reduceAnimations) {
                renaPreviewOverlay.animate()
                        .alpha(1f)
                        .setDuration(140L)
                        .start();
            }

            return;
        }

        renaPreviewOverlay = new FrameLayout(this);

        renaPreviewScrim = new View(this);
        renaPreviewScrim.setBackgroundColor(
                Color.argb(235, 0, 0, 0)
        );
        renaPreviewScrim.setClickable(true);
        renaPreviewScrim.setOnTouchListener(
                new View.OnTouchListener() {
                    private float downX;
                    private float downY;
                    private boolean moved;

                    @Override
                    public boolean onTouch(
                            View view,
                            MotionEvent event
                    ) {
                        switch (event.getActionMasked()) {
                            case MotionEvent.ACTION_DOWN:
                                downX = event.getX();
                                downY = event.getY();
                                moved = false;
                                return true;

                            case MotionEvent.ACTION_MOVE:
                                if (Math.abs(event.getX() - downX) > touchSlop ||
                                        Math.abs(event.getY() - downY) > touchSlop) {
                                    moved = true;
                                }
                                return true;

                            case MotionEvent.ACTION_CANCEL:
                                moved = true;
                                return true;

                            case MotionEvent.ACTION_UP:
                                if (!moved) {
                                    closeRenaPreview();
                                }
                                return true;
                        }
                        return true;
                    }
                }
        );

        renaPreviewOverlay.addView(
                renaPreviewScrim,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        renaPreviewImage = new ZoomImageView(this);
        renaPreviewImage.setScaleType(
                ImageView.ScaleType.MATRIX
        );

        FrameLayout.LayoutParams imageLp =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER
                );

        int margin = dp(18);
        imageLp.leftMargin = margin;
        imageLp.rightMargin = margin;
        imageLp.topMargin = margin;
        imageLp.bottomMargin = margin;

        renaPreviewOverlay.addView(
                renaPreviewImage,
                imageLp
        );

        renaPreviewOverlay.setAlpha(0f);

        if (Build.VERSION.SDK_INT >= 21) {
            // Above the drawer's elevation.
            renaPreviewOverlay.setElevation(100f);
        }

        root.addView(
                renaPreviewOverlay,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        applyImmersiveFullscreen();

        renaImageTask =
                new RenaImageTask(
                        renaPreviewImage,
                        renaPreviewOverlay
                );

        renaImageTask.executeOnExecutor(
                AsyncTask.THREAD_POOL_EXECUTOR
        );
    }

    private void closeRenaPreview() {
        if (renaPreviewOverlay == null) {
            return;
        }

        final FrameLayout closing =
                renaPreviewOverlay;

        renaPreviewOverlay = null;

        if (renaImageTask != null) {
            try {
                renaImageTask.cancel(true);
            } catch (Throwable ignored) {
            }
            renaImageTask = null;
        }

        closing.animate()
                .alpha(0f)
                .setDuration(160)
                .withEndAction(
                        new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    root.removeView(closing);
                                } catch (Throwable ignored) {
                                }

                                renaPreviewImage = null;
                                renaPreviewScrim = null;
                                applyImmersiveFullscreen();
                            }
                        }
                )
                .start();
    }

    private static class RenaImageTask
            extends AsyncTask<Void, Void, Bitmap> {

        private final WeakReference<ImageView> target;
        private final WeakReference<Activity> activityRef;
        private final WeakReference<FrameLayout> overlayRef;

        RenaImageTask(
                ImageView target,
                FrameLayout overlay
        ) {
            this.target =
                    new WeakReference<ImageView>(target);
            this.activityRef =
                    new WeakReference<Activity>(
                            (Activity) target.getContext()
                    );
            this.overlayRef =
                    new WeakReference<FrameLayout>(
                            overlay
                    );
        }

        @Override
        protected Bitmap doInBackground(Void... taskArgs) {
            Activity activity = activityRef.get();
            if (activity == null || isCancelled()) {
                return null;
            }

            InputStream input = null;

            try {
                String assetName =
                        NativeConfig.renaImageAssetName();

                input = activity.getAssets()
                        .open(assetName);

                BitmapFactory.Options bounds =
                        new BitmapFactory.Options();
                bounds.inJustDecodeBounds = true;

                BitmapFactory.decodeStream(
                        input,
                        null,
                        bounds
                );

                input.close();
                input = null;

                if (bounds.outWidth <= 0 ||
                        bounds.outHeight <= 0 ||
                        isCancelled()) {
                    return null;
                }

                int sample =
                        calculateRenaSampleSize(
                                bounds.outWidth,
                                bounds.outHeight
                        );

                input = activity.getAssets()
                        .open(assetName);

                BitmapFactory.Options options =
                        new BitmapFactory.Options();
                options.inSampleSize = sample;
                options.inScaled = false;
                options.inPreferredConfig =
                        Bitmap.Config.RGB_565;

                Bitmap bitmap =
                        BitmapFactory.decodeStream(
                                input,
                                null,
                                options
                        );

                if (bitmap == null && !isCancelled()) {
                    try {
                        input.close();
                    } catch (Throwable ignoredClose) {
                    }
                    input = activity.getAssets().open(assetName);
                    options.inPreferredConfig =
                            Bitmap.Config.ARGB_8888;
                    bitmap =
                            BitmapFactory.decodeStream(
                                    input,
                                    null,
                                    options
                            );
                }

                if (bitmap != null) {
                    bitmap.setDensity(0);
                }

                return bitmap;

            } catch (Throwable ignored) {
                return null;

            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (Throwable ignored) {
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                return;
            }

            Activity activity =
                    activityRef.get();

            if (activity instanceof MainActivity) {
                MainActivity mainActivity =
                        (MainActivity) activity;

                mainActivity.renaImageTask = null;
                if (!mainActivity.isUiAlive()) {
                    return;
                }
            }

            ImageView image = target.get();

            if (image != null && bitmap != null) {
                image.setAlpha(0f);
                image.setImageBitmap(bitmap);

                FrameLayout overlay =
                        overlayRef.get();

                if (overlay != null) {
                    overlay.animate()
                            .alpha(1f)
                            .setDuration(180)
                            .start();

                    image.animate()
                            .alpha(1f)
                            .setDuration(220)
                            .start();
                }
            } else if (activity instanceof MainActivity) {
                final MainActivity mainActivity =
                        (MainActivity) activity;
                final FrameLayout overlay =
                        overlayRef.get();

                if (overlay != null &&
                        mainActivity.renaPreviewImage != null &&
                        mainActivity.renaPreviewRetryCount < 1) {
                    mainActivity.renaPreviewRetryCount++;

                    overlay.postDelayed(
                            new Runnable() {
                                @Override
                                public void run() {
                                    if (!mainActivity.isUiAlive()) {
                                        return;
                                    }
                                    if (mainActivity.renaPreviewOverlay == overlay &&
                                            mainActivity.renaImageTask == null) {
                                        mainActivity.renaImageTask =
                                                new RenaImageTask(
                                                        mainActivity.renaPreviewImage,
                                                        overlay
                                                );
                                        mainActivity.renaImageTask.executeOnExecutor(
                                                AsyncTask.THREAD_POOL_EXECUTOR
                                        );
                                    }
                                }
                            },
                            120L
                    );
                }
            }
        }
    }

    private static int calculateRenaSampleSize(
            int width,
            int height
    ) {
        long maxPixels =
                18000000L;

        int sample = 1;

        while (
                ((long) width / sample) *
                ((long) height / sample)
                > maxPixels
        ) {
            sample *= 2;
        }

        return Math.max(
                1,
                sample
        );
    }

    private void setZoomMode(boolean enabled) {
        applyZoomMode(enabled, true);
    }

    private void applyZoomMode(
            boolean enabled,
            boolean showToast
    ) {
        zoomEnabled = enabled;

        if (showToast) {
            showToggleToastOnce(
                    "zoom",
                    enabled
                            ? NativeConfig.zoomEnabledText()
                            : NativeConfig.zoomDisabledText(),
                    enabled
            );
        }

        if (webView == null) {
            return;
        }

        WebSettings settings = webView.getSettings();

        settings.setSupportZoom(enabled);

        // The built-in zoom mechanism stays initialized for the WebView's
        // lifetime. We only change whether user zoom is allowed.
        settings.setDisplayZoomControls(false);

        if (enabled) {
            if (!baselineScaleCaptured) {
                baselineScaleCaptured = true;
                baselineScale = webView.getScale();

                if (baselineScale <= 0f) {
                    baselineScale = 1.0f;
                }
            }
        } else {
            /*
             * Return to the exact baseline instead of merely disabling the
             * zoom controls. Disabling the checkbox does not itself reset the
             * viewport, so we step the WebView back toward the baseline.
             */
            resetZoomToBaseline();
        }
    }

    @SuppressWarnings("deprecation")
    private void resetZoomToBaseline() {
        if (webView == null) {
            return;
        }

        try {
            final int maxSteps = 18;

            for (int i = 0; i < maxSteps; i++) {
                float current = webView.getScale();

                if (current <= 0f ||
                        current <= baselineScale + 0.02f) {
                    break;
                }

                if (!webView.zoomOut()) {
                    break;
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private void refreshWhatsAppPage() {
        if (webView == null) {
            finishRefreshAnimation();
            return;
        }

        try {
            getProfileCookieManager().flush();
        } catch (Throwable ignored) {
        }

        try {
            webView.reload();
        } catch (Throwable ignored) {
            finishRefreshAnimation();
        }
    }


    private void installNotificationBridge() {
        if (webView == null ||
                !isTrustedWhatsAppUrl(webView.getUrl())) {
            return;
        }

        try {
            webView.removeJavascriptInterface(
                    "RenaNotifications"
            );
        } catch (Throwable ignored) {
        }

        webView.addJavascriptInterface(
                new NotificationBridge(),
                "RenaNotifications"
        );

        injectNotificationObserver();
    }

    private void removeNotificationBridge() {
        if (webView == null) {
            return;
        }

        try {
            webView.removeJavascriptInterface(
                    "RenaNotifications"
            );
        } catch (Throwable ignored) {
        }
    }

    private void injectNotificationObserver() {
        if (webView == null ||
                !isTrustedWhatsAppUrl(webView.getUrl())) {
            return;
        }

        String script =
                "(function(){"
                + "if(window.__renaNotificationObserver){return;}"
                + "function send(){"
                + "try{"
                + "var nodes=[].slice.call(document.querySelectorAll('[data-testid=\"msg-container\"],.message-in'));"
                + "if(!nodes.length)return;"
                + "var node=nodes[nodes.length-1];"
                + "var body=(node.innerText||node.textContent||'').trim();"
                + "if(!body)return;"
                + "var header=document.querySelector('[data-testid=\"conversation-info-header-chat-title\"]');"
                + "var chat=header?(header.innerText||header.textContent||'').trim():'';"
                + "if(!chat){"
                + "var h=document.querySelector('[data-testid=\"conversation-info-header\"]');"
                + "chat=h?(h.innerText||h.textContent||'').trim():'';"
                + "}"
                + "var key=chat+'|'+body+'|'+(node.getAttribute('data-id')||'');"
                + "if(window.RenaNotifications){window.RenaNotifications.onMessage(chat,body,key);}"
                + "}catch(e){}"
                + "}"
                + "var timer=null;"
                + "function schedule(){if(timer){clearTimeout(timer);}timer=setTimeout(send,250);}"
                + "window.__renaNotificationObserver=new MutationObserver(function(){schedule();});"
                + "if(document.body){window.__renaNotificationObserver.observe(document.body,{childList:true,subtree:true,characterData:true});}"
                + "schedule();"
                + "})();";

        webView.evaluateJavascript(
                script,
                null
        );
    }

    private final class NotificationBridge {
        @JavascriptInterface
        public void onMessage(
                final String chat,
                final String body,
                final String key
        ) {
            runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            if (!isUiAlive()) {
                                return;
                            }
                            postWebNotification(
                                    chat,
                                    body,
                                    key
                            );
                        }
                    }
            );
        }
    }

    private void ensureNotificationChannels() {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }

        NotificationManager manager =
                (NotificationManager) getSystemService(
                        NOTIFICATION_SERVICE
                );

        if (manager == null) {
            return;
        }

        NotificationChannel normal =
                new NotificationChannel(
                        NOTIFICATION_CHANNEL_ID,
                        NativeConfig.notificationChannelName(),
                        NotificationManager.IMPORTANCE_HIGH
                );
        normal.setDescription(
                NativeConfig.notificationChannelDescription()
        );

        NotificationChannel silent =
                new NotificationChannel(
                        NOTIFICATION_SILENT_CHANNEL_ID,
                        NativeConfig.notificationSilentChannelName(),
                        NotificationManager.IMPORTANCE_LOW
                );
        silent.setDescription(
                NativeConfig.notificationSilentChannelDescription()
        );
        silent.setSound(null, null);
        silent.enableVibration(false);

        manager.createNotificationChannel(normal);
        manager.createNotificationChannel(silent);
    }

    private void postWebNotification(
            String chat,
            String body,
            String key
    ) {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(
                        Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if (TextUtils.isEmpty(body)) {
            return;
        }

        android.content.SharedPreferences prefs =
                getSharedPreferences(
                        "rena_notification_state",
                        MODE_PRIVATE
                );

        String dedupeKey =
                "notification_keys_" + currentTabId();

        if (!TextUtils.isEmpty(key)) {
            java.util.Set<String> stored =
                    prefs.getStringSet(
                            dedupeKey,
                            null
                    );

            java.util.HashSet<String> recent =
                    stored == null
                            ? new java.util.HashSet<String>()
                            : new java.util.HashSet<String>(
                                    stored
                            );

            if (recent.contains(key)) {
                return;
            }

            recent.add(key);

            while (recent.size() > 16) {
                java.util.Iterator<String> iterator =
                        recent.iterator();

                if (!iterator.hasNext()) {
                    break;
                }

                recent.remove(
                        iterator.next()
                );
            }

            prefs.edit()
                    .putStringSet(
                            dedupeKey,
                            recent
                    )
                    .apply();
        }

        final int notificationId =
                ("rena_notification_" + currentTabId())
                        .hashCode();

        NotificationManager manager =
                (NotificationManager)
                        getSystemService(
                                NOTIFICATION_SERVICE
                        );

        if (manager == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel =
                    new NotificationChannel(
                            NOTIFICATION_CHANNEL_ID,
                            NativeConfig.notificationChannelName(),
                            NotificationManager.IMPORTANCE_HIGH
                    );

            channel.setDescription(
                    NativeConfig.notificationChannelDescription()
            );

            manager.createNotificationChannel(channel);

            NotificationChannel silentChannel =
                    new NotificationChannel(
                            NOTIFICATION_SILENT_CHANNEL_ID,
                            NativeConfig.notificationSilentChannelName(),
                            NotificationManager.IMPORTANCE_LOW
                    );

            silentChannel.setDescription(
                    NativeConfig.notificationSilentChannelDescription()
            );
            silentChannel.setSound(null, null);
            silentChannel.enableVibration(false);

            manager.createNotificationChannel(silentChannel);
        }

        Intent openIntent =
                new Intent(
                        this,
                        NotificationActionReceiver.class
                );

        openIntent.setAction(
                NOTIFICATION_ACTION_OPEN
        );

        openIntent.putExtra(
                NOTIFICATION_EXTRA_TAB_ID,
                currentTabId()
        );

        openIntent.putExtra(
                NOTIFICATION_EXTRA_CHAT,
                chat
        );

        openIntent.putExtra(
                NOTIFICATION_EXTRA_BODY,
                body
        );

        PendingIntent openPending =
                PendingIntent.getBroadcast(
                        this,
                        notificationId + 1,
                        openIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT |
                                PendingIntent.FLAG_IMMUTABLE
                );

        Intent readIntent =
                new Intent(
                        this,
                        NotificationActionReceiver.class
                );

        readIntent.setAction(
                NOTIFICATION_ACTION_READ
        );

        readIntent.putExtras(
                openIntent
        );

        PendingIntent readPending =
                PendingIntent.getBroadcast(
                        this,
                        notificationId + 2,
                        readIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT |
                                PendingIntent.FLAG_IMMUTABLE
                );

        Intent ignoreIntent =
                new Intent(
                        this,
                        NotificationActionReceiver.class
                );

        ignoreIntent.setAction(
                NOTIFICATION_ACTION_IGNORE
        );

        ignoreIntent.putExtra(
                NOTIFICATION_EXTRA_TAB_ID,
                currentTabId()
        );

        PendingIntent ignorePending =
                PendingIntent.getBroadcast(
                        this,
                        notificationId + 3,
                        ignoreIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT |
                                PendingIntent.FLAG_IMMUTABLE
                );

        Intent replyIntent =
                new Intent(
                        this,
                        NotificationActionReceiver.class
                );

        replyIntent.setAction(
                NOTIFICATION_ACTION_REPLY
        );

        replyIntent.putExtras(
                openIntent
        );

        PendingIntent replyPending =
                PendingIntent.getBroadcast(
                        this,
                        notificationId + 4,
                        replyIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT |
                                PendingIntent.FLAG_MUTABLE
                );

        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= 26) {
            builder =
                    new Notification.Builder(
                            this,
                            hideNotifications
                                    ? NOTIFICATION_SILENT_CHANNEL_ID
                                    : NOTIFICATION_CHANNEL_ID
                    );
        } else {
            builder =
                    new Notification.Builder(
                            this
                    );
        }

        builder.setSmallIcon(
                getApplicationInfo().icon
        );

        builder.setContentTitle(
                TextUtils.isEmpty(chat)
                        ? NativeConfig.appFallbackName()
                        : chat
        );

        builder.setContentText(body);
        builder.setAutoCancel(true);
        builder.setContentIntent(openPending);

        builder.addAction(
                new Notification.Action.Builder(
                        null,
                        NativeConfig.notificationOpenText(),
                        openPending
                ).build()
        );

        RemoteInput remoteInput =
                new RemoteInput.Builder(
                        REMOTE_INPUT_REPLY
                )
                        .setLabel(
                                NativeConfig.notificationReplyText()
                        )
                        .build();

        Notification.Action.Builder replyAction =
                new Notification.Action.Builder(
                        null,
                        NativeConfig.notificationReplyText(),
                        replyPending
                );

        replyAction.addRemoteInput(
                remoteInput
        );

        builder.addAction(
                replyAction.build()
        );

        builder.addAction(
                new Notification.Action.Builder(
                        null,
                        NativeConfig.notificationReadText(),
                        readPending
                ).build()
        );

        builder.addAction(
                new Notification.Action.Builder(
                        null,
                        NativeConfig.notificationIgnoreText(),
                        ignorePending
                ).build()
        );

        manager.notify(
                notificationId,
                builder.build()
        );
    }

    private android.webkit.CookieManager getProfileCookieManager() {
        try {
            if (webProfile != null) {
                return webProfile.getCookieManager();
            }
        } catch (Throwable ignored) {
        }

        return android.webkit.CookieManager.getInstance();
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        // Keep Chromium's zoom machinery initialized for the lifetime of
        // the WebView. Only supportZoom is toggled at runtime; scale changes
        // are never modified from a WebViewClient scale callback.
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        settings.setUserAgentString(
                NativeConfig.desktopUserAgent()
        );

        CookieManager cookies =
                getProfileCookieManager();

        cookies.setAcceptCookie(true);

        if (Build.VERSION.SDK_INT >= 21) {
            cookies.setAcceptThirdPartyCookies(
                    webView,
                    true
            );
        }

        // Let WebView/Chromium manage its own rendering layer.

        webView.setWebChromeClient(
                new WebChromeClient() {

                    @Override
                    public void onPermissionRequest(
                            final PermissionRequest request
                    ) {
                        runOnUiThread(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        if (!isUiAlive()) {
                                            return;
                                        }
                                        requestWebPermissions(
                                                request
                                        );
                                    }
                                }
                        );
                    }

                    @Override
                    public boolean onShowFileChooser(
                            WebView view,
                            ValueCallback<Uri[]> callback,
                            FileChooserParams params
                    ) {
                        if (pendingFileCallback != null) {
                            pendingFileCallback.onReceiveValue(
                                    null
                            );
                        }

                        pendingFileCallback = callback;

                        boolean multiple = false;

                        try {
                            multiple =
                                    params.getMode() ==
                                    FileChooserParams
                                            .MODE_OPEN_MULTIPLE;
                        } catch (Throwable ignored) {
                        }

                        openSystemFilePicker(multiple);
                        return true;
                    }
                }
        );

        webView.setWebViewClient(
                new WebViewClient() {

                    @Override
                    public void onPageStarted(
                            WebView view,
                            String url,
                            android.graphics.Bitmap favicon
                    ) {
                        pageLoading = true;
                        slowConnectionToastShown = false;
                        cancelSlowConnectionWatch();

                        progress.setVisibility(
                                View.VISIBLE
                        );
                        progress.setProgress(12);

                        setLoadingOverlayVisible(true);
                        removeNotificationBridge();

                        scheduleSlowConnectionWatch();
                    }

                    @Override
                    public void onPageFinished(
                            WebView view,
                            String url
                    ) {
                        pageLoading = false;
                        cancelSlowConnectionWatch();
                        finishRefreshAnimation();
                        progress.setProgress(100);
                        progress.setVisibility(
                                View.GONE
                        );

                        captureBaselineIfNeeded();

                        if (!historyFloorApplied) {
                            try {
                                view.clearHistory();
                                historyFloorApplied = true;
                            } catch (Throwable ignoredHistory) {
                            }
                        }

                        try {
                            getProfileCookieManager().flush();
                        } catch (Throwable ignored) {
                        }

                        installNotificationBridge();
                        handleNotificationIntent(getIntent());

                        setLoadingOverlayVisible(false);
                    }

                    @Override
                    public boolean shouldOverrideUrlLoading(
                            WebView view,
                            WebResourceRequest request
                    ) {
                        return false;
                    }

                    @Override
                    public void onReceivedError(
                            WebView view,
                            WebResourceRequest request,
                            WebResourceError error
                    ) {
                        if (Build.VERSION.SDK_INT >= 23 &&
                                request.isForMainFrame()) {
                            pageLoading = false;
                            cancelSlowConnectionWatch();
                            finishRefreshAnimation();
                            progress.setVisibility(
                                    View.GONE
                            );
                            if (!hasInternetConnection()) {
                                showRuntimeOfflineToast();
                            } else {
                                showSlowConnectionToast();
                            }
                        }
                    }
                }
        );

        webView.setDownloadListener(
                new android.webkit.DownloadListener() {
                    @Override
                    public void onDownloadStart(
                            String url,
                            String userAgent,
                            String contentDisposition,
                            String mimetype,
                            long contentLength
                    ) {
                        enqueueWebDownload(
                                url,
                                userAgent,
                                contentDisposition,
                                mimetype
                        );
                    }
                }
        );
    }

    private void captureBaselineIfNeeded() {
        try {
            float scale = webView.getScale();

            if (scale > 0f && !baselineScaleCaptured) {
                baselineScale = scale;
                baselineScaleCaptured = true;
            }
        } catch (Throwable ignored) {
        }
    }

    private void setLoadingOverlayVisible(boolean visible) {
        if (root == null) {
            return;
        }

        View overlay = root.findViewWithTag(
                "loading_overlay"
        );

        if (overlay == null) {
            return;
        }

        if (visible) {
            overlay.setVisibility(View.VISIBLE);
            overlay.setAlpha(1f);
        } else {
            overlay.animate()
                    .alpha(0f)
                    .setDuration(180)
                    .withEndAction(
                            new Runnable() {
                                @Override
                                public void run() {
                                    View v =
                                            root.findViewWithTag(
                                                    "loading_overlay"
                                            );

                                    if (v != null) {
                                        v.setVisibility(
                                                View.GONE
                                        );
                                    }
                                }
                            }
                    )
                    .start();
        }
    }

    private void requestWebPermissions(
            PermissionRequest request
    ) {
        if (request == null) {
            return;
        }

        if (pendingWebPermission != null) {
            try {
                request.deny();
            } catch (Throwable ignored) {
            }
            return;
        }

        if (!isTrustedWhatsAppUrl(webView == null ? null : webView.getUrl())) {
            try {
                request.deny();
            } catch (Throwable ignored) {
            }
            return;
        }

        boolean needCamera = false;
        boolean needMicrophone = false;

        String[] resources = request.getResources();

        for (String resource : resources) {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) {
                needCamera = true;
            }
            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) {
                needMicrophone = true;
            }
        }

        pendingWebPermission = request;
        pendingWebNeedCamera = needCamera &&
                !hasPermission(Manifest.permission.CAMERA);
        pendingWebNeedMicrophone = needMicrophone &&
                !hasPermission(Manifest.permission.RECORD_AUDIO);

        requestNextWebPermission();
    }

    private void requestNextWebPermission() {
        if (pendingWebPermission == null) {
            return;
        }

        if (pendingWebNeedCamera) {
            pendingWebNeedCamera = false;
            requestPermissions(
                    new String[]{Manifest.permission.CAMERA},
                    REQ_MEDIA_PERMISSION
            );
            return;
        }

        if (pendingWebNeedMicrophone) {
            pendingWebNeedMicrophone = false;
            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQ_MEDIA_PERMISSION
            );
            return;
        }

        PermissionRequest request = pendingWebPermission;
        pendingWebPermission = null;
        grantAllowedWebResources(request);
    }

    private void grantAllowedWebResources(
            PermissionRequest request
    ) {
        if (request == null) {
            return;
        }

        ArrayList<String> allowed =
                new ArrayList<String>();

        for (String resource :
                request.getResources()) {

            if (PermissionRequest
                    .RESOURCE_VIDEO_CAPTURE
                    .equals(resource) &&
                    hasPermission(
                            Manifest.permission.CAMERA
                    )) {

                allowed.add(resource);
            }

            if (PermissionRequest
                    .RESOURCE_AUDIO_CAPTURE
                    .equals(resource) &&
                    hasPermission(
                            Manifest.permission.RECORD_AUDIO
                    )) {

                allowed.add(resource);
            }
        }

        if (!allowed.isEmpty()) {
            request.grant(
                    allowed.toArray(
                            new String[allowed.size()]
                    )
            );
        } else {
            request.deny();
        }
    }

    private void openSystemFilePicker(
            boolean multiple
    ) {
        Intent intent =
                new Intent(Intent.ACTION_OPEN_DOCUMENT);

        intent.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        intent.setType("*/*");

        intent.putExtra(
                Intent.EXTRA_ALLOW_MULTIPLE,
                multiple
        );

        try {
            startActivityForResult(
                    intent,
                    PICK_FILE
            );
        } catch (Throwable ignored) {
            try {
                Intent fallback =
                        new Intent(
                                Intent.ACTION_GET_CONTENT
                        );

                fallback.addCategory(
                        Intent.CATEGORY_OPENABLE
                );

                fallback.setType("*/*");

                fallback.putExtra(
                        Intent.EXTRA_ALLOW_MULTIPLE,
                        multiple
                );

                startActivityForResult(
                        fallback,
                        PICK_FILE
                );
            } catch (Throwable error) {
                if (pendingFileCallback != null) {
                    pendingFileCallback
                            .onReceiveValue(null);

                    pendingFileCallback = null;
                }
            }
        }
    }

    private void clearApplicationCache() {
        try {
            deleteCacheContents(
                    getCacheDir()
            );

            if (Build.VERSION.SDK_INT >= 21) {
                deleteCacheContents(
                        getCodeCacheDir()
                );
            }
        } catch (Throwable ignored) {
        }
    }

    private void deleteCacheContents(File directory) {
        if (directory == null ||
                !directory.exists()) {
            return;
        }

        File[] children =
                directory.listFiles();

        if (children == null) {
            return;
        }

        for (File child : children) {
            try {
                if (child.isDirectory()) {
                    deleteCacheContents(child);
                }

                child.delete();
            } catch (Throwable ignored) {
            }
        }
    }

    private void openExternal(String url) {
        try {
            startActivity(
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(url)
                    )
            );
        } catch (Throwable ignored) {
        }
    }

    private void setDrawerOpen(final boolean open) {
        if (drawer == null ||
                drawerScrim == null ||
                menuButton == null) {
            return;
        }

        if (drawerOpen == open && !drawerAnimating) {
            return;
        }

        boolean wasAnimating = drawerAnimating;
        drawerAnimationGeneration++;
        final int animationGeneration =
                drawerAnimationGeneration;

        drawerAnimating = true;

        drawer.animate().cancel();
        drawerScrim.animate().cancel();
        menuButton.animate().cancel();

        if (open) {
            drawerOpen = true;
            drawer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            drawerScrim.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            if (footerView != null) {
                footerView.resumeMarquee();
            }

            drawerScrim.setVisibility(
                    View.VISIBLE
            );
            drawerScrim.setAlpha(
                    0f
            );

            drawer.setVisibility(
                    View.VISIBLE
            );
            if (!wasAnimating) {
                drawer.setTranslationX(
                        -drawerWidthPx
                );
            }

            drawerScrim.bringToFront();
            drawer.bringToFront();

            updateMenuVisibility();

            drawer.animate()
                    .translationX(0f)
                    .setDuration(animDuration(235L))
                    .setInterpolator(
                            new android.view.animation
                                    .DecelerateInterpolator()
                    )
                    .withEndAction(
                            new Runnable() {
                                @Override
                                public void run() {
                                    if (animationGeneration !=
                                            drawerAnimationGeneration) {
                                        return;
                                    }

                                    drawerAnimating =
                                            false;
                                                                        updateMenuVisibility();
                                }
                            }
                    )
                    .start();

            drawerScrim.animate()
                    .alpha(1f)
                    .setDuration(animDuration(180L))
                    .start();

            menuButton.animate()
                    .rotation(90f)
                    .alpha(0f)
                    .setDuration(animDuration(190L))
                    .start();

        } else {
            drawerOpen = false;
            if (footerView != null) {
                footerView.pauseMarquee();
            }

            drawer.setVisibility(
                    View.VISIBLE
            );

            drawer.bringToFront();

            drawer.animate()
                    .translationX(
                            -drawerWidthPx
                    )
                    .setDuration(animDuration(220L))
                    .setInterpolator(
                            new android.view.animation
                                    .DecelerateInterpolator()
                    )
                    .withEndAction(
                            new Runnable() {
                                @Override
                                public void run() {
                                    if (animationGeneration !=
                                            drawerAnimationGeneration) {
                                        return;
                                    }

                                    drawer.setVisibility(
                                            View.GONE
                                    );

                                    drawerScrim.setVisibility(
                                            View.GONE
                                    );

                                    drawerScrim.setAlpha(
                                            0f
                                    );

                                    drawerAnimating =
                                            false;

                                    menuButton.setRotation(
                                            0f
                                    );
                                    menuButton.setAlpha(
                                            1f
                                    );

                                    updateMenuVisibility();
                                }
                            }
                    )
                    .start();

            drawerScrim.animate()
                    .alpha(0f)
                    .setDuration(animDuration(180L))
                    .start();
        }
    }

    private GradientDrawable round(
            int color,
            int radius
    ) {
        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(color);
        drawable.setCornerRadius(radius);

        return drawable;
    }

    private GradientDrawable roundDrawer() {
        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(
                Color.rgb(18, 18, 20)
        );

        return drawable;
    }
    private void showIntegrityFailureDialog() {
        try {
            new AlertDialog.Builder(this)
                    .setTitle(NativeConfig.integrityFailedTitle())
                    .setMessage(
                            NativeConfig.integrityErrorMessage()
                    )
                    .setCancelable(false)
                    .setPositiveButton(
                            NativeConfig.integrityCloseText(),
                            new android.content.DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(
                                        android.content.DialogInterface dialog,
                                        int which
                                ) {
                                    closeAppBySystem();
                                }
                            }
                    )
                    .show();
        } catch (Throwable ignored) {
            closeAppBySystem();
        }
    }

    private boolean integrityGate() {
        try {
            return RenaApplication.isApplicationIntegrityValid(this) &&
                    NativeConfig.isNativeAvailable() &&
                    NativeConfig.verifyIntegrity(this);
        } catch (Throwable ignored) {
            return false;
        }
    }



    private void closeAppBySystem() {
        exitingApp = true;
        try {
            if (Build.VERSION.SDK_INT >= 21) {
                finishAndRemoveTask();
            } else {
                finish();
            }
        } catch (Throwable ignored) {
            finish();
        }
    }
    private void applyImmersiveFullscreen() {
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                getWindow().setDecorFitsSystemWindows(false);
            }

            if (Build.VERSION.SDK_INT >= 21) {
                int flags =
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
                getWindow().getDecorView().setSystemUiVisibility(flags);
                getWindow().setStatusBarColor(Color.TRANSPARENT);
                getWindow().setNavigationBarColor(Color.BLACK);
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            applyImmersiveFullscreen();

            if (hideThreeDot) {
                registerShakeSensor();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        boolean currentInternet = hasInternetConnection();
        if (internetAvailable && !currentInternet && webView != null) {
            showRuntimeOfflineToast();
        }
        internetAvailable = currentInternet;

        applyImmersiveFullscreen();

        resumeRetainedTabViews();

        if (hideThreeDot) {
            registerShakeSensor();
        } else {
            unregisterShakeSensor();
        }

        dataManagerOpening = false;

        if (root != null && menuButton != null) {
            root.post(new Runnable() {
                @Override
                public void run() {
                    restoreMenuPosition();
                }
            });
        }

        if (specialSettingsInFlight) {
            final int stage = specialSettingsStage;
            specialSettingsInFlight = false;
            specialSettingsStage = -1;

            boolean granted = false;

            if (stage == 0) {
                granted = Build.VERSION.SDK_INT < 30 || hasAllFilesAccess();
                if (granted) {
                    prepareRenaDirectories();
                }
            } else if (stage == 1) {
                granted = Build.VERSION.SDK_INT < 28 || !isBackgroundRestricted();
            } else if (stage == 2) {
                granted = Build.VERSION.SDK_INT < 23 || isIgnoringBatteryOptimizations();
            }

            if (granted) {
                // Only advance to the next optional recommendation after
                // Android confirms that the requested state was granted.
                continueSpecialStartupFlow();
            }
            // Not granted: do nothing. The optional prompt must not
            // immediately reappear or chain into another settings screen.
        }

        if (pendingStartupPermissionAfterSettings >= 0) {
            int index = pendingStartupPermissionAfterSettings;
            pendingStartupPermissionAfterSettings = -1;

            String permission = getPermissionForIndex(index);
            if (permission != null) {
                if (hasPermission(permission)) {
                    showNextPermissionDialog(index + 1);
                } else {
                    showPermissionDeniedDialog(permission);
                }
            }
        }

        if (pendingUpdatePermissionCheck) {
            boolean allowed =
                    Build.VERSION.SDK_INT < 26 ||
                    getPackageManager().canRequestPackageInstalls();

            pendingUpdatePermissionCheck = false;

            if (allowed) {
                checkForUpdates();
            } else {
                finishUpdateCheckAnimation();
            }
        }

        if (pendingUpdateApkUrl != null &&
                (Build.VERSION.SDK_INT < 30 || hasAllFilesAccess())) {
            String apkUrl = pendingUpdateApkUrl;
            String digest = pendingUpdateDigest;

            pendingUpdateApkUrl = null;
            pendingUpdateDigest = null;

            downloadAndInstallApk(apkUrl, digest);
        }

        if (pendingWebDownloadUrl != null &&
                (Build.VERSION.SDK_INT < 30 || hasAllFilesAccess())) {
            String url = pendingWebDownloadUrl;
            String userAgent = pendingWebDownloadUserAgent;
            String contentDisposition = pendingWebDownloadContentDisposition;
            String mimetype = pendingWebDownloadMimeType;

            pendingWebDownloadUrl = null;
            pendingWebDownloadUserAgent = null;
            pendingWebDownloadContentDisposition = null;
            pendingWebDownloadMimeType = null;

            enqueueWebDownload(
                    url,
                    userAgent,
                    contentDisposition,
                    mimetype
            );
        }

        if (webView != null) {
            try {
                webView.onResume();
                webView.resumeTimers();
            } catch (Throwable ignored) {
            }
        }
    }

    private boolean isTrustedWhatsAppUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }

        try {
            Uri uri = Uri.parse(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();

            return "https".equalsIgnoreCase(scheme)
                    && "web.whatsapp.com".equalsIgnoreCase(host);
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotificationIntent(intent);
    }

    private void handleNotificationIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        boolean hasRequestedTab =
                intent.hasExtra(
                        NOTIFICATION_EXTRA_TAB_ID
                );

        int requestedTab = intent.getIntExtra(
                NOTIFICATION_EXTRA_TAB_ID,
                currentTabId()
        );

        int requestedIndex =
                findTabIndexById(
                        requestedTab
                );

        if (hasRequestedTab &&
                requestedIndex < 0) {
            consumeNotificationIntent();
            return;
        }

        if (requestedIndex >= 0 &&
                requestedIndex != activeTabIndex) {
            switchToTab(requestedIndex);
            return;
        }

        if (webView == null ||
                !isTrustedWhatsAppUrl(webView.getUrl())) {
            return;
        }

        String action = intent.getAction();
        if (TextUtils.isEmpty(action)) {
            return;
        }

        final String chat =
                intent.getStringExtra(
                        NOTIFICATION_EXTRA_CHAT
                );

        final String reply =
                intent.getStringExtra(
                        NOTIFICATION_EXTRA_REPLY
                );

        if (NOTIFICATION_ACTION_READ.equals(action)) {
            markNotificationChatRead(chat);
            consumeNotificationIntent();
        } else if (NOTIFICATION_ACTION_REPLY.equals(action) &&
                !TextUtils.isEmpty(reply)) {
            sendNotificationReply(chat, reply);
            consumeNotificationIntent();
        } else if (NOTIFICATION_ACTION_OPEN.equals(action)) {
            consumeNotificationIntent();
        }
    }

    private void consumeNotificationIntent() {
        Intent current = getIntent();

        if (current == null) {
            return;
        }

        Intent clean = new Intent();
        clean.setComponent(
                new android.content.ComponentName(
                        this,
                        MainActivity.class
                )
        );

        setIntent(clean);
    }

    private void markNotificationChatRead(String chat) {
        if (webView == null ||
                !isTrustedWhatsAppUrl(webView.getUrl())) {
            return;
        }

        String safeChat = jsQuote(chat);

        webView.evaluateJavascript(
                "(function(){"
                + "var q=" + safeChat + ";"
                + "var all=[].slice.call(document.querySelectorAll('[aria-label],[title],[data-testid]'));"
                + "var hit=null;"
                + "for(var i=0;i<all.length;i++){"
                + "var e=all[i];"
                + "var s=(e.getAttribute('aria-label')||e.getAttribute('title')||e.innerText||'').trim();"
                + "if(q&&s&&s.toLowerCase().indexOf(q.toLowerCase())>=0){hit=e;break;}"
                + "}"
                + "if(hit){try{hit.click();}catch(e){}}"
                + "})();",
                null
        );
    }

    private void sendNotificationReply(
            final String chat,
            final String reply
    ) {
        if (webView == null ||
                !isTrustedWhatsAppUrl(webView.getUrl())) {
            return;
        }

        final String safeChat = jsQuote(chat);
        final String safeReply = jsQuote(reply);

        webView.evaluateJavascript(
                "(function(){"
                + "var q=" + safeChat + ";"
                + "var all=[].slice.call(document.querySelectorAll('[aria-label],[title],[data-testid]'));"
                + "for(var i=0;i<all.length;i++){"
                + "var e=all[i];"
                + "var s=(e.getAttribute('aria-label')||e.getAttribute('title')||e.innerText||'').trim();"
                + "if(q&&s&&s.toLowerCase().indexOf(q.toLowerCase())>=0){try{e.click();}catch(ex){}break;}"
                + "}"
                + "setTimeout(function(){"
                + "var boxes=[].slice.call(document.querySelectorAll('div[contenteditable=\"true\"],textarea'));"
                + "var box=boxes.length?boxes[boxes.length-1]:null;"
                + "if(!box)return;"
                + "box.focus();"
                + "try{document.execCommand('insertText',false,"+safeReply+");}"
                + "catch(ex){box.innerText="+safeReply+";}"
                + "var evt;"
                + "try{evt=new InputEvent('input',{bubbles:true,inputType:'insertText',data:"+safeReply+"});}"
                + "catch(ex){evt=document.createEvent('Event');evt.initEvent('input',true,true);}"
                + "box.dispatchEvent(evt);"
                + "var buttons=[].slice.call(document.querySelectorAll('[data-testid=\"send\"],[aria-label*=\"Send\"],[title*=\"Send\"]'));"
                + "if(buttons.length){try{buttons[buttons.length-1].click();}catch(ex){}}"
                + "},650);"
                + "})();",
                null
        );
    }

    private String jsQuote(String value) {
        if (value == null) {
            return "''";
        }

        String escaped = value
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\r", "\\r")
                .replace("\n", "\\n");

        return "'" + escaped + "'";
    }

    @Override
    protected void onSaveInstanceState(
            Bundle outState
    ) {
        flushWebSessionState();

        try {
            if (webView != null) {
                webView.saveState(outState);
            }
        } catch (Throwable ignored) {
        }

        super.onSaveInstanceState(outState);
    }

    private void resumeRetainedTabViews() {
        for (TabState tab : tabs) {
            if (tab == null || tab.view == null) {
                continue;
            }

            try {
                if (tab.id == currentTabId()) {
                    tab.view.setVisibility(View.VISIBLE);
                    tab.view.onResume();
                } else {
                    tab.view.setVisibility(View.GONE);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    protected void onPause() {
        unregisterShakeSensor();
        saveActiveTab();
        persistTabs();

        flushAllProfileCookies();

        super.onPause();
    }

    @Override
    protected void onStop() {
        /*
         * Keep Chromium session data durable when Android moves the Activity
         * out of the foreground. This is persistence work only; it never
         * clears cookies, local storage, or the WebView profile.
         */
        flushWebSessionState();
        super.onStop();
    }

    private boolean isUiAlive() {
        return !activityDestroyed &&
                !isFinishing() &&
                !isDestroyed();
    }

    @Override
    protected void onDestroy() {
        activityDestroyed = true;
        if (renaImageTask != null) {
            try {
                renaImageTask.cancel(true);
            } catch (Throwable ignored) {
            }
            renaImageTask = null;
        }
        cancelSlowConnectionWatch();
        unregisterInternetMonitor();
        /*
         * Final persistence point for normal Activity destruction. Do not
         * clear WebView data here; Chromium owns the persistent profile.
         */
        flushWebSessionState();
        super.onDestroy();
    }

    @Override
    public void onTrimMemory(int level) {
        /*
         * Android may trim the process after it has been in the background for
         * a while. Flush the WebView session before the process is reclaimed.
         */
        flushWebSessionState();
        super.onTrimMemory(level);
    }

    @Override
    public void onLowMemory() {
        flushWebSessionState();
        super.onLowMemory();
    }

    private void flushWebSessionState() {
        saveActiveTab();
        persistTabs();

        flushAllProfileCookies();
    }

    /**
     * Flush every persistent WebView profile, not only the currently visible tab.
     * This is intentionally a flush-only operation: no cookies, cache, history,
     * local storage or profile data are deleted here.
     */
    private void flushAllProfileCookies() {
        try {
            CookieManager.getInstance().setAcceptCookie(true);
        } catch (Throwable ignored) {
        }

        try {
            getProfileCookieManager().flush();
        } catch (Throwable ignored) {
        }

        if (!multiProfileSupported) {
            return;
        }

        try {
            ProfileStore store = ProfileStore.getInstance();

            for (TabState tab : tabs) {
                if (tab == null || TextUtils.isEmpty(tab.profileId)) {
                    continue;
                }

                try {
                    Profile profile =
                            store.getOrCreateProfile(tab.profileId);

                    if (profile != null) {
                        CookieManager manager =
                                profile.getCookieManager();

                        manager.setAcceptCookie(true);

                        if (Build.VERSION.SDK_INT >= 21 && tab.view != null) {
                            manager.setAcceptThirdPartyCookies(
                                    tab.view,
                                    true
                            );
                        }

                        manager.flush();
                    }
                } catch (Throwable ignoredProfile) {
                }
            }
        } catch (Throwable ignored) {
        }
    }


    private boolean hasInternetConnection() {
        try {
            ConnectivityManager cm =
                    (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

            if (cm == null) {
                return false;
            }

            if (Build.VERSION.SDK_INT < 23) {
                NetworkInfo info = cm.getActiveNetworkInfo();
                return info != null && info.isConnected();
            }

            Network network = cm.getActiveNetwork();
            if (network == null) {
                return false;
            }

            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            if (caps == null || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                return false;
            }

            return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                    || caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                    || caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void registerInternetMonitor() {
        try {
            connectivityManager =
                    (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

            internetAvailable = hasInternetConnection();

            if (connectivityManager == null || Build.VERSION.SDK_INT < 24) {
                return;
            }

            connectivityCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!isUiAlive()) {
                                return;
                            }
                            internetAvailable = true;
                            runtimeOfflineToastShown = false;
                        }
                    });
                }

                @Override
                public void onCapabilitiesChanged(
                        Network network,
                        NetworkCapabilities capabilities
                ) {
                    final boolean available =
                            capabilities != null &&
                            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!isUiAlive()) {
                                return;
                            }
                            internetAvailable = available;
                            if (available) {
                                runtimeOfflineToastShown = false;
                            }
                        }
                    });
                }

                @Override
                public void onLost(Network network) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!isUiAlive()) {
                                return;
                            }
                            internetAvailable = false;
                            networkHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (!isUiAlive()) {
                                        return;
                                    }
                                    if (!hasInternetConnection()) {
                                        showRuntimeOfflineToast();
                                    }
                                }
                            }, 900L);
                        }
                    });
                }
            };

            connectivityManager.registerDefaultNetworkCallback(connectivityCallback);
        } catch (Throwable ignored) {
        }
    }

    private void unregisterInternetMonitor() {
        try {
            if (connectivityManager != null &&
                    connectivityCallback != null &&
                    Build.VERSION.SDK_INT >= 24) {
                connectivityManager.unregisterNetworkCallback(connectivityCallback);
            }
        } catch (Throwable ignored) {
        }
        connectivityCallback = null;
        connectivityManager = null;
    }

    private void showRuntimeOfflineToast() {
        if (runtimeOfflineToastShown) {
            return;
        }
        runtimeOfflineToastShown = true;
        Toast.makeText(
                this,
                NativeConfig.noInternetConnectionText(),
                Toast.LENGTH_SHORT
        ).show();
    }

    private void showSlowConnectionToast() {
        if (slowConnectionToastShown) {
            return;
        }
        slowConnectionToastShown = true;
        Toast.makeText(
                this,
                NativeConfig.poorInternetConnectionText(),
                Toast.LENGTH_SHORT
        ).show();
    }

    private void scheduleSlowConnectionWatch() {
        cancelSlowConnectionWatch();

        slowConnectionRunnable = new Runnable() {
            @Override
            public void run() {
                if (!pageLoading) {
                    return;
                }

                if (!hasInternetConnection()) {
                    showRuntimeOfflineToast();
                } else {
                    showSlowConnectionToast();
                }
            }
        };

        networkHandler.postDelayed(
                slowConnectionRunnable,
                12000L
        );
    }

    private void cancelSlowConnectionWatch() {
        if (slowConnectionRunnable != null) {
            networkHandler.removeCallbacks(slowConnectionRunnable);
            slowConnectionRunnable = null;
        }
    }

    private void presentStartupInternetGate(final Bundle restoreState) {
        internetAvailable = hasInternetConnection();

        if (internetAvailable) {
            initializeWebView(restoreState);
            return;
        }

        final AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(NativeConfig.noInternetTitle())
                        .setMessage(NativeConfig.noInternetMessage())
                        .setNegativeButton(
                                NativeConfig.exitText(),
                                new android.content.DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(
                                            android.content.DialogInterface dialog,
                                            int which
                                    ) {
                                        closeAppBySystem();
                                    }
                                }
                        )
                        .setPositiveButton(
                                NativeConfig.ignoreText(),
                                new android.content.DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(
                                            android.content.DialogInterface dialog,
                                            int which
                                    ) {
                                        showRuntimeOfflineToast();
                                        initializeWebView(restoreState);
                                    }
                                }
                        )
                        .create();

        dialog.setOnDismissListener(
                new android.content.DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(android.content.DialogInterface dialogInterface) {
                        startupInternetGateShowing = false;

                        if (webView == null && !exitingApp) {
                            showRuntimeOfflineToast();
                            initializeWebView(restoreState);
                        }

                        if (permissionFlowDeferred && !permissionFlowStarted) {
                            permissionFlowDeferred = false;
                            startPermissionFlow();
                        }
                    }
                }
        );
        dialog.show();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        applyImmersiveFullscreen();

        if (requestCode ==
                REQ_MEDIA_PERMISSION) {

            PermissionRequest request =
                    pendingWebPermission;

            pendingWebPermission = null;

            if (request != null) {
                pendingWebPermission = request;
                requestNextWebPermission();
            }

        } else if (requestCode ==
                REQ_STARTUP_PERMISSIONS) {

            String grantedPermission = null;

            if (permissions != null && permissions.length > 0) {
                grantedPermission = permissions[0];
            }

            if (grantedPermission != null) {
                int next = permissionIndex(grantedPermission);

                if (next >= 0) {
                    if (hasPermission(grantedPermission)) {
                        showNextPermissionDialog(next + 1);
                    } else {
                        showPermissionDeniedDialog(grantedPermission);
                    }
                }
            }
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {
        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        applyImmersiveFullscreen();

        if (requestCode == REQ_DATA_MANAGER) {
            dataManagerOpening = false;

            if (resultCode == RESULT_OK &&
                    data != null) {
                boolean webDataCleared =
                        data.getBooleanExtra(
                                "web_data_cleared",
                                false
                        );

                boolean clearCache =
                        data.getBooleanExtra(
                                "clear_cache",
                                false
                        );

                try {
                    if (clearCache) {
                        if (webView != null) {
                            webView.clearCache(true);
                        }

                        clearApplicationCache();

                        Toast.makeText(
                                MainActivity.this,
                                NativeConfig.cacheClearedText(),
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    if (webView != null &&
                            webDataCleared) {
                        webView.clearHistory();
                        historyFloorApplied = true;
                        webView.loadUrl(
                                NativeConfig.webUrl()
                        );
                    }
                } catch (Throwable ignored) {
                }
            }

            return;
        }

        if (requestCode != PICK_FILE ||
                pendingFileCallback == null) {
            return;
        }

        Uri[] results = null;

        if (resultCode == RESULT_OK &&
                data != null) {

            if (data.getClipData() != null) {

                int count =
                        data.getClipData()
                                .getItemCount();

                results = new Uri[count];

                for (int i = 0; i < count; i++) {
                    results[i] =
                            data.getClipData()
                                    .getItemAt(i)
                                    .getUri();
                }

            } else if (data.getData() != null) {

                results =
                        new Uri[]{
                                data.getData()
                        };
            }
        }

        pendingFileCallback
                .onReceiveValue(results);

        pendingFileCallback = null;
    }

    @Override
    public void onBackPressed() {
        if (exitingApp) {
            return;
        }

        if (renaPreviewOverlay != null) {
            closeRenaPreview();
            return;
        }

        if (drawerOpen || drawerAnimating) {
            setDrawerOpen(false);
            return;
        }

        /*
         * Only navigate through history that was created after the current
         * activity/session floor. The initial authenticated page is kept as
         * the floor, so Back can never walk into an older login page from a
         * previous WebView history chain.
         */
        if (webView != null) {
            try {
                android.webkit.WebBackForwardList history =
                        webView.copyBackForwardList();

                int currentIndex =
                        history == null
                                ? -1
                                : history.getCurrentIndex();

                if (currentIndex > 0 &&
                        webView.canGoBack()) {
                    webView.goBack();
                    return;
                }
            } catch (Throwable ignoredHistory) {
                // Fall through to the normal exit path.
            }
        }

        exitingApp = true;

        try {
            Toast.makeText(
                    this,
                    NativeConfig.exitToastText(),
                    Toast.LENGTH_SHORT
            ).show();
        } catch (Throwable ignoredToast) {
        }

        try {
            if (Build.VERSION.SDK_INT >= 21) {
                finishAndRemoveTask();
            } else {
                finish();
            }
        } catch (Throwable ignoredExit) {
            finish();
        }
    }

    private static class MarqueeTextView
            extends FrameLayout {

        private final TextView label;
        private ValueAnimator animator;
        private boolean shouldRun = false;
        private boolean hasStarted = false;
        private final Runnable restartMarqueeRunnable = new Runnable() {
            @Override
            public void run() {
                startMarquee();
            }
        };

        MarqueeTextView(Context context) {
            super(context);

            setClipChildren(true);
            setClipToPadding(true);

            label = new TextView(context);
            label.setSingleLine(true);
            label.setEllipsize(null);
            label.setGravity(Gravity.CENTER_VERTICAL);

            addView(
                    label,
                    new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            Gravity.CENTER_VERTICAL
                    )
            );
        }

        void setText(String value) {
            label.setText(value);
            if (shouldRun) {
                restartMarqueeSoon();
            }
        }

        void setTextSize(float value) {
            label.setTextSize(value);
            if (shouldRun) {
                restartMarqueeSoon();
            }
        }

        void setTypeface(android.graphics.Typeface typeface) {
            label.setTypeface(typeface);
        }

        void setTypeface(
                android.graphics.Typeface typeface,
                int style
        ) {
            label.setTypeface(typeface, style);
        }

        void setTextColor(int value) {
            label.setTextColor(value);
        }

        void setGravity(int value) {
            label.setGravity(value);
        }

        void pauseMarquee() {
            shouldRun = false;
            removeCallbacks(restartMarqueeRunnable);
            stopMarquee();
            // Deliberately keep the current X position.
        }

        void resumeMarquee() {
            shouldRun = true;
            removeCallbacks(restartMarqueeRunnable);

            if (label != null &&
                    (label.getX() > getWidth() ||
                     label.getX() < -Math.max(1f, label.getMeasuredWidth()))) {
                label.setX(getWidth());
                hasStarted = false;
            }

            restartMarqueeSoon();
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
        }

        @Override
        protected void onDetachedFromWindow() {
            stopMarquee();
            super.onDetachedFromWindow();
        }

        @Override
        protected void onSizeChanged(
                int w,
                int h,
                int oldw,
                int oldh
        ) {
            super.onSizeChanged(w, h, oldw, oldh);

            if (shouldRun && !hasStarted) {
                restartMarqueeSoon();
            }
        }

        private void restartMarqueeSoon() {
            if (!shouldRun) {
                return;
            }

            removeCallbacks(restartMarqueeRunnable);
            postDelayed(restartMarqueeRunnable, 50L);
        }

        private void startMarquee() {
            if (!shouldRun) {
                return;
            }

            stopMarquee();

            if (getWidth() <= 0) {
                restartMarqueeSoon();
                return;
            }

            label.measure(
                    MeasureSpec.makeMeasureSpec(
                            0,
                            MeasureSpec.UNSPECIFIED
                    ),
                    MeasureSpec.makeMeasureSpec(
                            getHeight(),
                            MeasureSpec.EXACTLY
                    )
            );

            int textWidth = label.getMeasuredWidth();
            if (textWidth <= 0) {
                restartMarqueeSoon();
                return;
            }

            final float endX = -textWidth;

            float currentX;
            if (!hasStarted || label.getX() == 0f) {
                currentX = getWidth();
            } else {
                currentX = label.getX();
            }

            if (currentX <= endX) {
                currentX = getWidth();
            }

            label.setX(currentX);
            hasStarted = true;

            final float distance = Math.max(
                    1f,
                    currentX - endX
            );

            animator =
                    ValueAnimator.ofFloat(
                            currentX,
                            endX
                    );

            animator.setDuration(
                    Math.max(
                            4800L,
                            (long) (distance * 18L)
                    )
            );

            animator.setInterpolator(new LinearInterpolator());
            animator.setRepeatCount(0);

            animator.addUpdateListener(
                    new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(
                                ValueAnimator valueAnimator
                        ) {
                            if (shouldRun) {
                                label.setX(
                                        (Float) valueAnimator
                                                .getAnimatedValue()
                                );
                            }
                        }
                    }
            );

            animator.addListener(
                    new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(
                                Animator animation
                        ) {
                            animator = null;

                            if (shouldRun) {
                                hasStarted = false;
                                label.setX(getWidth());
                                restartMarqueeSoon();
                            }
                        }

                        @Override
                        public void onAnimationCancel(
                                Animator animation
                        ) {
                            animator = null;
                        }
                    }
            );

            animator.start();
        }

        private void stopMarquee() {
            if (animator != null) {
                animator.cancel();
                animator = null;
            }
        }
    }

    private static class BitmapDrawableWrapper
            extends android.graphics.drawable.BitmapDrawable {

        public BitmapDrawableWrapper(
                android.content.res.Resources resources,
                Bitmap bitmap
        ) {
            super(resources, bitmap);
        }
    }

    private static class ShimmerDonateView
            extends FrameLayout {

        private final TextView label;
        private final ImageView iconView;
        private final Paint paint =
                new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF bounds =
                new RectF();

        private float shineX = -1f;
        private ValueAnimator animator;

        ShimmerDonateView(Context context) {
            super(context);

            setWillNotDraw(false);
            setClipChildren(true);
            setBackground(createBackground(context));

            float density =
                    context.getResources()
                            .getDisplayMetrics()
                            .density;

            iconView = new ImageView(context);
            iconView.setScaleType(
                    ImageView.ScaleType.CENTER_INSIDE
            );
            iconView.setPadding(
                    (int) (5f * density),
                    (int) (5f * density),
                    (int) (5f * density),
                    (int) (5f * density)
            );

            addView(
                    iconView,
                    new FrameLayout.LayoutParams(
                            (int) (42f * density),
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            Gravity.START | Gravity.CENTER_VERTICAL
                    )
            );

            label =
                    new TextView(context);
            label.setTextSize(15f);
            label.setTextColor(Color.WHITE);
            label.setGravity(Gravity.CENTER);
            try {
                android.graphics.Typeface typeface =
                        android.graphics.Typeface.DEFAULT_BOLD;
                if (android.os.Build.VERSION.SDK_INT >= 26) {
                    int fontId = getContext().getResources().getIdentifier(
                            "font", "font", getContext().getPackageName());
                    if (fontId != 0) {
                        typeface = getContext().getResources().getFont(fontId);
                        typeface = android.graphics.Typeface.create(
                                typeface,
                                android.graphics.Typeface.BOLD
                        );
                    }
                }
                label.setTypeface(typeface);
            } catch (Throwable ignored) {
                label.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            }
            label.setSingleLine(true);
            label.setClickable(false);

            FrameLayout.LayoutParams labelLp =
                    new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    );
            labelLp.leftMargin = (int) (34f * density);
            labelLp.rightMargin = (int) (8f * density);

            addView(label, labelLp);
        }

        private Drawable createBackground(Context context) {
            GradientDrawable bg =
                    new GradientDrawable();
            bg.setColor(Color.rgb(0, 168, 132));
            bg.setCornerRadius(
                    18f *
                    context.getResources()
                            .getDisplayMetrics()
                            .density
            );
            return bg;
        }

        void setText(String value) {
            label.setText(value);
        }

        void setIcon(Drawable drawable) {
            iconView.setImageDrawable(drawable);
            if (drawable != null) {
                iconView.setColorFilter(Color.WHITE);
            }
        }

        void playShimmer() {
            if (getWidth() <= 0) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        playShimmer();
                    }
                });
                return;
            }

            if (animator != null) {
                animator.cancel();
            }

            shineX = -getWidth() * 0.35f;

            animator =
                    ValueAnimator.ofFloat(
                            -getWidth() * 0.35f,
                            getWidth() * 1.35f
                    );

            animator.setDuration(1000L);
            animator.setInterpolator(
                    new LinearInterpolator()
            );

            animator.addUpdateListener(
                    new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(
                                ValueAnimator valueAnimator
                        ) {
                            shineX =
                                    (Float) valueAnimator
                                            .getAnimatedValue();
                            invalidate();
                        }
                    }
            );

            animator.addListener(
                    new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(
                                Animator animation
                        ) {
                            shineX = -1f;
                            invalidate();
                        }

                        @Override
                        public void onAnimationCancel(
                                Animator animation
                        ) {
                            shineX = -1f;
                            invalidate();
                        }
                    }
            );

            animator.start();
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            super.dispatchDraw(canvas);

            if (shineX < 0f) {
                return;
            }

            float density =
                    getResources()
                            .getDisplayMetrics()
                            .density;

            bounds.set(
                    0f,
                    0f,
                    getWidth(),
                    getHeight()
            );

            Path clip = new Path();
            clip.addRoundRect(
                    bounds,
                    18f * density,
                    18f * density,
                    Path.Direction.CW
            );

            canvas.save();
            canvas.clipPath(clip);

            float band =
                    Math.max(
                            30f * density,
                            getWidth() * 0.12f
                    );

            LinearGradient gradient =
                    new LinearGradient(
                            shineX - band,
                            0f,
                            shineX + band,
                            0f,
                            new int[]{
                                    Color.TRANSPARENT,
                                    Color.argb(55, 255, 255, 255),
                                    Color.argb(220, 255, 255, 255),
                                    Color.argb(55, 255, 255, 255),
                                    Color.TRANSPARENT
                            },
                            null,
                            Shader.TileMode.CLAMP
                    );

            paint.setShader(gradient);

            canvas.rotate(
                    -9f,
                    getWidth() * 0.5f,
                    getHeight() * 0.5f
            );

            canvas.drawRect(
                    -getWidth(),
                    -getHeight(),
                    getWidth() * 2f,
                    getHeight() * 2f,
                    paint
            );

            paint.setShader(null);
            canvas.restore();
        }

        @Override
        protected void onDetachedFromWindow() {
            if (animator != null) {
                animator.cancel();
                animator = null;
            }
            super.onDetachedFromWindow();
        }
    }

    private static class ZoomImageView
            extends ImageView {

        private final Matrix matrix = new Matrix();
        private final ScaleGestureDetector scaleDetector;
        private final GestureDetector gestureDetector;
        private final float[] values = new float[9];
        private final RectF mappedImageBounds = new RectF();

        private float currentScale = 1f;
        private float minScale = 1f;
        private float maxScale = 5f;
        private float lastX;
        private float lastY;
        private boolean gestureStarted = false;
        private boolean dragging = false;
        private boolean scaling = false;
        private float downX;
        private float downY;
        private int localTouchSlop;
        private float baseDrawableWidth;
        private float baseDrawableHeight;
        private ValueAnimator zoomAnimator;
        private long zoomAnimationGeneration;
        private boolean targetZoomed;

        public ZoomImageView(Context context) {
            super(context);
            setScaleType(ImageView.ScaleType.MATRIX);

            localTouchSlop =
                    android.view.ViewConfiguration
                            .get(context)
                            .getScaledTouchSlop();

            gestureDetector =
                    new GestureDetector(
                            context,
                            new GestureDetector.SimpleOnGestureListener() {
                                @Override
                                public boolean onDoubleTap(MotionEvent e) {
                                    if (getDrawable() == null ||
                                            getWidth() <= 0 ||
                                            getHeight() <= 0) {
                                        return false;
                                    }

                                    // The toggle decision is based on the last requested
                                    // target, not on currentScale while an animation is
                                    // still in flight. This prevents spam double-taps from
                                    // interpreting an intermediate scale as a new state.
                                    boolean nextZoomed = !targetZoomed;
                                    targetZoomed = nextZoomed;

                                    if (nextZoomed) {
                                        animateZoomToPoint(
                                                e.getX(),
                                                e.getY()
                                        );
                                    } else {
                                        animateBackToFit();
                                    }

                                    return true;
                                }

                                @Override
                                public boolean onDown(MotionEvent e) {
                                    return true;
                                }
                            }
                    );

            scaleDetector =
                    new ScaleGestureDetector(
                            context,
                            new ScaleGestureDetector
                                    .SimpleOnScaleGestureListener() {
                                @Override
                                public boolean onScaleBegin(
                                        ScaleGestureDetector detector
                                ) {
                                    if (!gestureStarted) {
                                        return false;
                                    }
                                    scaling = true;
                                    dragging = false;
                                    return true;
                                }

                                @Override
                                public boolean onScale(
                                        ScaleGestureDetector detector
                                ) {
                                    if (!gestureStarted) {
                                        return false;
                                    }

                                    float factor =
                                            detector.getScaleFactor();

                                    float next =
                                            Math.max(
                                                    minScale,
                                                    Math.min(
                                                            maxScale,
                                                            currentScale * factor
                                                    )
                                            );

                                    float applied =
                                            next / currentScale;

                                    currentScale = next;
                                    targetZoomed = currentScale >
                                            minScale + 0.02f;

                                    matrix.postScale(
                                            applied,
                                            applied,
                                            detector.getFocusX(),
                                            detector.getFocusY()
                                    );

                                    clampTranslation();
                                    setImageMatrix(matrix);
                                    return true;
                                }

                                @Override
                                public void onScaleEnd(
                                        ScaleGestureDetector detector
                                ) {
                                    scaling = false;
                                }
                            }
                    );
        }

        @Override
        protected void onSizeChanged(
                int w,
                int h,
                int oldw,
                int oldh
        ) {
            super.onSizeChanged(w, h, oldw, oldh);
            cancelZoomAnimation();
            fitImage();
        }

        @Override
        public void setImageBitmap(Bitmap bitmap) {
            cancelZoomAnimation();
            super.setImageBitmap(bitmap);
            post(new Runnable() {
                @Override
                public void run() {
                    fitImage();
                }
            });
        }

        private void fitImage() {
            if (getDrawable() == null ||
                    getWidth() <= 0 ||
                    getHeight() <= 0) {
                return;
            }

            float viewWidth = getWidth();
            float viewHeight = getHeight();
            float drawableWidth = getDrawable().getIntrinsicWidth();
            float drawableHeight = getDrawable().getIntrinsicHeight();

            if (drawableWidth <= 0 || drawableHeight <= 0) {
                return;
            }

            baseDrawableWidth = drawableWidth;
            baseDrawableHeight = drawableHeight;

            minScale = Math.min(
                    viewWidth / drawableWidth,
                    viewHeight / drawableHeight
            );

            if (minScale <= 0f) {
                minScale = 1f;
            }

            maxScale = Math.max(
                    3f,
                    minScale * 5f
            );

            currentScale = minScale;
            targetZoomed = false;
            matrix.reset();
            matrix.postScale(minScale, minScale);

            matrix.postTranslate(
                    (viewWidth - drawableWidth * minScale) / 2f,
                    (viewHeight - drawableHeight * minScale) / 2f
            );

            setImageMatrix(matrix);
        }

        private long zoomAnimationDuration() {
            return RenaSettingsStore.getBoolean(
                    getContext(),
                    "reduce_animation",
                    false
            )
                    ? 100L
                    : 220L;
        }

        private void cancelZoomAnimation() {
            zoomAnimationGeneration++;
            ValueAnimator running = zoomAnimator;
            zoomAnimator = null;
            if (running != null) {
                running.cancel();
            }
        }

        private void animateToMatrix(
                final Matrix target,
                final float targetScale
        ) {
            if (getDrawable() == null ||
                    getWidth() <= 0 ||
                    getHeight() <= 0) {
                return;
            }

            cancelZoomAnimation();

            final long generation = zoomAnimationGeneration;
            final Matrix start = new Matrix(matrix);
            final float startScale = currentScale;

            final float[] startValues = new float[9];
            final float[] targetValues = new float[9];
            final float[] frameValues = new float[9];

            start.getValues(startValues);
            target.getValues(targetValues);

            zoomAnimator =
                    ValueAnimator.ofFloat(0f, 1f);

            zoomAnimator.setDuration(
                    zoomAnimationDuration()
            );
            zoomAnimator.setInterpolator(
                    new android.view.animation.DecelerateInterpolator()
            );

            zoomAnimator.addUpdateListener(
                    new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(
                                ValueAnimator animation
                        ) {
                            if (generation != zoomAnimationGeneration) {
                                return;
                            }

                            float p =
                                    (Float) animation.getAnimatedValue();

                            for (int i = 0; i < 9; i++) {
                                frameValues[i] =
                                        startValues[i] +
                                        (targetValues[i] -
                                                startValues[i]) *
                                                p;
                            }

                            matrix.setValues(frameValues);

                            currentScale =
                                    startScale +
                                    (targetScale - startScale) *
                                            p;

                            // The start and target matrices are already valid.
                            // Clamping every interpolated frame changes the Y
                            // translation while the scale is changing, which
                            // causes the visible "drop" near the end of zoom
                            // and the matching upward snap when returning to fit.
                            setImageMatrix(matrix);
                        }
                    }
            );

            zoomAnimator.addListener(
                    new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(
                                Animator animation
                        ) {
                            if (generation != zoomAnimationGeneration) {
                                return;
                            }

                            if (zoomAnimator == animation) {
                                zoomAnimator = null;
                            }

                            matrix.set(target);
                            currentScale = targetScale;
                            clampTranslation();
                            setImageMatrix(matrix);
                        }

                        @Override
                        public void onAnimationCancel(
                                Animator animation
                        ) {
                            if (zoomAnimator == animation) {
                                zoomAnimator = null;
                            }
                        }
                    }
            );

            zoomAnimator.start();
        }

        private void animateZoomToPoint(
                float focusX,
                float focusY
        ) {
            if (getDrawable() == null ||
                    getWidth() <= 0 ||
                    getHeight() <= 0) {
                return;
            }

            final Matrix inverse = new Matrix();

            if (!matrix.invert(inverse)) {
                return;
            }

            final float[] focusPoint = {
                    focusX,
                    focusY
            };

            inverse.mapPoints(focusPoint);

            float targetScale =
                    Math.min(
                            maxScale,
                            minScale * 2.5f
                    );

            if (targetScale <= minScale + 0.02f) {
                targetScale = Math.min(
                        maxScale,
                        minScale * 2f
                );
            }

            if (targetScale <= minScale + 0.02f) {
                return;
            }

            final Matrix target = new Matrix();

            target.setScale(
                    targetScale,
                    targetScale
            );

            target.postTranslate(
                    focusX - focusPoint[0] * targetScale,
                    focusY - focusPoint[1] * targetScale
            );

            clampMatrixTranslation(
                    target,
                    targetScale
            );

            animateToMatrix(
                    target,
                    targetScale
            );
        }

        private void animateBackToFit() {
            if (getDrawable() == null ||
                    getWidth() <= 0 ||
                    getHeight() <= 0) {
                return;
            }

            float drawableWidth = baseDrawableWidth > 0f
                    ? baseDrawableWidth
                    : getDrawable().getIntrinsicWidth();

            float drawableHeight = baseDrawableHeight > 0f
                    ? baseDrawableHeight
                    : getDrawable().getIntrinsicHeight();

            if (drawableWidth <= 0f ||
                    drawableHeight <= 0f) {
                return;
            }

            float targetScale =
                    Math.min(
                            getWidth() / drawableWidth,
                            getHeight() / drawableHeight
                    );

            if (targetScale <= 0f) {
                targetScale = 1f;
            }

            final Matrix target = new Matrix();

            target.postScale(
                    targetScale,
                    targetScale
            );

            target.postTranslate(
                    (getWidth() -
                            drawableWidth * targetScale) / 2f,
                    (getHeight() -
                            drawableHeight * targetScale) / 2f
            );

            animateToMatrix(
                    target,
                    targetScale
            );
        }

        private boolean isPointInsideImage(float x, float y) {
            if (getDrawable() == null) {
                return false;
            }

            float drawableWidth = baseDrawableWidth > 0f
                    ? baseDrawableWidth
                    : getDrawable().getIntrinsicWidth();
            float drawableHeight = baseDrawableHeight > 0f
                    ? baseDrawableHeight
                    : getDrawable().getIntrinsicHeight();

            mappedImageBounds.set(
                    0f,
                    0f,
                    drawableWidth,
                    drawableHeight
            );

            matrix.mapRect(mappedImageBounds);

            return mappedImageBounds.contains(x, y);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();

            if (action == MotionEvent.ACTION_DOWN) {
                cancelZoomAnimation();

                if (!isPointInsideImage(
                        event.getX(),
                        event.getY()
                )) {
                    gestureStarted = false;
                    dragging = false;
                    scaling = false;
                    return false;
                }

                gestureStarted = true;
                dragging = false;
                scaling = false;
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                downX = event.getX();
                downY = event.getY();
                lastX = downX;
                lastY = downY;

                gestureDetector.onTouchEvent(event);
                scaleDetector.onTouchEvent(event);
                return true;
            }

            if (!gestureStarted) {
                return false;
            }

            gestureDetector.onTouchEvent(event);
            scaleDetector.onTouchEvent(event);

            switch (action) {
                case MotionEvent.ACTION_POINTER_DOWN:
                    int pointerDownIndex = event.getActionIndex() == 0 ? 1 : 0;
                    lastX = event.getX(pointerDownIndex);
                    lastY = event.getY(pointerDownIndex);
                    return true;

                case MotionEvent.ACTION_POINTER_UP:
                    int actionIndex = event.getActionIndex();
                    int remainingIndex = actionIndex == 0 ? 1 : 0;
                    if (event.getPointerCount() > remainingIndex) {
                        lastX = event.getX(remainingIndex);
                        lastY = event.getY(remainingIndex);
                    }
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (Math.abs(event.getX() - downX) > localTouchSlop ||
                            Math.abs(event.getY() - downY) > localTouchSlop) {
                        dragging = true;
                    }

                    if (scaling || event.getPointerCount() > 1) {
                        return true;
                    }

                    if (dragging) {
                        float dx = event.getX() - lastX;
                        float dy = event.getY() - lastY;

                        matrix.postTranslate(dx, dy);
                        clampTranslation();
                        setImageMatrix(matrix);

                        lastX = event.getX();
                        lastY = event.getY();
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    dragging = false;
                    scaling = false;
                    gestureStarted = false;
                    if (getParent() != null) {
                        getParent().requestDisallowInterceptTouchEvent(false);
                    }
                    return true;
            }

            return true;
        }

        private void clampMatrixTranslation(
                Matrix target,
                float scale
        ) {
            if (getDrawable() == null) {
                return;
            }

            float[] targetValues = new float[9];
            target.getValues(targetValues);

            float drawableWidth =
                    baseDrawableWidth * scale;

            float drawableHeight =
                    baseDrawableHeight * scale;

            float viewWidth = getWidth();
            float viewHeight = getHeight();

            float minX;
            float maxX;
            float minY;
            float maxY;

            if (drawableWidth <= viewWidth) {
                minX = maxX =
                        (viewWidth - drawableWidth) / 2f;
            } else {
                minX = viewWidth - drawableWidth;
                maxX = 0f;
            }

            if (drawableHeight <= viewHeight) {
                minY = maxY =
                        (viewHeight - drawableHeight) / 2f;
            } else {
                minY = viewHeight - drawableHeight;
                maxY = 0f;
            }

            targetValues[Matrix.MTRANS_X] = Math.max(
                    minX,
                    Math.min(
                            maxX,
                            targetValues[Matrix.MTRANS_X]
                    )
            );

            targetValues[Matrix.MTRANS_Y] = Math.max(
                    minY,
                    Math.min(
                            maxY,
                            targetValues[Matrix.MTRANS_Y]
                    )
            );

            target.setValues(targetValues);
        }

        private void clampTranslation() {
            if (getDrawable() == null) {
                return;
            }

            matrix.getValues(values);

            float scaleX = values[Matrix.MSCALE_X];
            float transX = values[Matrix.MTRANS_X];
            float transY = values[Matrix.MTRANS_Y];

            float drawableWidth =
                    baseDrawableWidth * scaleX;

            float drawableHeight =
                    baseDrawableHeight * scaleX;

            float viewWidth = getWidth();
            float viewHeight = getHeight();

            float minX;
            float maxX;
            float minY;
            float maxY;

            if (drawableWidth <= viewWidth) {
                minX = maxX =
                        (viewWidth - drawableWidth) / 2f;
            } else {
                minX = viewWidth - drawableWidth;
                maxX = 0f;
            }

            if (drawableHeight <= viewHeight) {
                minY = maxY =
                        (viewHeight - drawableHeight) / 2f;
            } else {
                minY = viewHeight - drawableHeight;
                maxY = 0f;
            }

            values[Matrix.MTRANS_X] = Math.max(
                    minX,
                    Math.min(maxX, transX)
            );

            values[Matrix.MTRANS_Y] = Math.max(
                    minY,
                    Math.min(maxY, transY)
            );

            matrix.setValues(values);
        }

        @Override
        protected void onDetachedFromWindow() {
            cancelZoomAnimation();
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(false);
            }
            super.onDetachedFromWindow();
        }
    }

    private static class ThreeDotDrawable
            extends android.graphics.drawable.Drawable {

        private final android.graphics.Paint paint =
                new android.graphics.Paint(3);

        ThreeDotDrawable() {
            paint.setColor(Color.WHITE);
        }

        @Override
        public void draw(
                android.graphics.Canvas canvas
        ) {
            float x =
                    getBounds().centerX();

            float y =
                    getBounds().centerY();

            canvas.drawCircle(
                    x,
                    y - 7,
                    2.6f,
                    paint
            );

            canvas.drawCircle(
                    x,
                    y,
                    2.6f,
                    paint
            );

            canvas.drawCircle(
                    x,
                    y + 7,
                    2.6f,
                    paint
            );
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(
                android.graphics.ColorFilter filter
        ) {
            paint.setColorFilter(filter);
        }

        @Override
        public int getOpacity() {
            return android.graphics.PixelFormat
                    .TRANSLUCENT;
        }
    }

    private static class DeveloperEffectTextView extends TextView {
        private boolean effectsEnabled;
        private ValueAnimator effectAnimator;
        private float effectProgress = -1f;

        DeveloperEffectTextView(Context context) {
            super(context);
            setWillNotDraw(false);
            effectsEnabled = false;
        }

        void toggleEffects() {
            if (effectsEnabled) {
                effectsEnabled = false;

                if (effectAnimator != null) {
                    effectAnimator.cancel();
                    effectAnimator = null;
                }

                final float from =
                        effectProgress < 0f ? 1f : effectProgress;

                effectAnimator =
                        ValueAnimator.ofFloat(from, -0.2f);

                effectAnimator.setDuration(RenaSettingsStore.getBoolean(getContext(), "reduce_animation", false) ? 100L : 260L);
                effectAnimator.setInterpolator(
                        new android.view.animation.DecelerateInterpolator()
                );

                effectAnimator.addUpdateListener(
                        new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(
                                    ValueAnimator animation
                            ) {
                                effectProgress =
                                        (Float) animation.getAnimatedValue();
                                invalidate();
                            }
                        }
                );

                effectAnimator.addListener(
                        new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(
                                    Animator animation
                            ) {
                                effectProgress = -1f;
                                effectAnimator = null;
                                invalidate();
                            }
                        }
                );

                effectAnimator.start();
                return;
            }

            effectsEnabled = true;

            if (effectAnimator != null) {
                effectAnimator.cancel();
                effectAnimator = null;
            }

            effectAnimator =
                    ValueAnimator.ofFloat(
                            0f,
                            1f
                    );

            effectAnimator.setDuration(
                    RenaSettingsStore.getBoolean(
                            getContext(),
                            "reduce_animation",
                            false
                    )
                            ? 620L
                            : 1500L
            );
            effectAnimator.setInterpolator(
                    new LinearInterpolator()
            );
            effectAnimator.setRepeatCount(
                    ValueAnimator.INFINITE
            );
            effectAnimator.setRepeatMode(
                    ValueAnimator.REVERSE
            );

            effectAnimator.addUpdateListener(
                    new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(
                                ValueAnimator animation
                        ) {
                            effectProgress =
                                    (Float) animation.getAnimatedValue();
                            invalidate();
                        }
                    }
            );

            effectAnimator.start();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (!effectsEnabled &&
                    effectProgress < 0f) {
                super.onDraw(canvas);
                return;
            }

            int save = canvas.save();

            if (effectProgress >= 0f) {
                // Keep the shimmer/smoke strictly inside the measured text area.
                canvas.clipRect(
                        getPaddingLeft(),
                        0f,
                        getPaddingLeft() + Math.max(1f, getPaint().measureText(getText() == null ? "" : getText().toString())),
                        getHeight()
                );
                float textWidth =
                        Math.max(
                                1f,
                                getPaint().measureText(
                                        getText() == null
                                                ? ""
                                                : getText().toString()
                                )
                        );

                float left =
                        getPaddingLeft();

                float right =
                        left + textWidth;

                float center =
                        left +
                        effectProgress * textWidth;

                Shader previousShader =
                        getPaint().getShader();

                getPaint().setShader(
                        new LinearGradient(
                                center - dpStatic(getContext(), 58),
                                0f,
                                center + dpStatic(getContext(), 58),
                                0f,
                                new int[]{
                                        Color.TRANSPARENT,
                                        Color.argb(175, 255, 255, 255),
                                        Color.argb(70, 255, 255, 255),
                                        Color.TRANSPARENT
                                },
                                null,
                                Shader.TileMode.CLAMP
                        )
                );

                super.onDraw(canvas);
                getPaint().setShader(previousShader);

                // The old particle bubbles were rigid and visually disconnected from
                // the text. Keep the effect as a single soft shimmer that travels
                // smoothly across the developer name.
            } else {
                super.onDraw(canvas);
            }

            canvas.restoreToCount(save);
        }

        private static float dpStatic(
                Context context,
                float value
        ) {
            return value *
                    context.getResources()
                            .getDisplayMetrics()
                            .density;
        }

        @Override
        protected void onDetachedFromWindow() {
            if (effectAnimator != null) {
                effectAnimator.cancel();
                effectAnimator = null;
            }
            super.onDetachedFromWindow();
        }
    }

}
