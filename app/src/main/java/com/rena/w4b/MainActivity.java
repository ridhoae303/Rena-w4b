package com.rena.w4b;

import com.ridhoae303.expert.Takane;
import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Looper;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.ActivityManager;
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
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;
import android.graphics.Typeface;
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
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
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
import com.ridhoae303.expert.AppFragment;

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
    private BrowserController browser;
    private boolean browserMode;
    private long lastBackPressAt;
    private long uiEpochSeen;
    private final Handler thumbHandler = new Handler(Looper.getMainLooper());
    private boolean drawerAnimating = false;
    private int drawerAnimationGeneration = 0;
    private ValueAnimator drawerAnimator;
    private float drawerProgress = 0f;
    private boolean hideThreeDot = false;
    private boolean hideWarningShown = false;
    private boolean hideNotifications = false;
    private boolean hideBarNotification = false;
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

    private final AtomicBoolean updaterRunning = new AtomicBoolean(false);
    private boolean updateCheckRunning = false;
    private boolean updateInstallInProgress = false;
    private long updateDownloadId = -1L;
    private boolean updateDownloadHidden = false;
    private final android.os.Handler updateHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable updatePollRunnable;
    private String pendingUpdateVersion;
    private String pendingInstalledCleanupPath;
    private String pendingInstalledCleanupVersion;
    private boolean startupFlowFinished = false;
    private static final long AUTOMATIC_UPDATE_CHECK_INTERVAL_MS = 6L * 60L * 60L * 1000L;
    private static final String PREF_UPDATE_LAST_AUTO_CHECK = "update_last_auto_check";
    private static final String PREF_UPDATE_LAST_OFFERED_TAG = "update_last_offered_tag";
    private static final String PREF_UPDATE_PENDING_PATH = "update_pending_path";
    private static final String PREF_UPDATE_PENDING_VERSION = "update_pending_version";
    private static final String PREF_UPDATE_READY = "update_ready";
    private static final String PREF_UPDATE_CACHED_TAG = "update_cached_tag";
    private static final String PREF_UPDATE_CACHED_NAME = "update_cached_name";
    private static final String PREF_UPDATE_CACHED_NOTES = "update_cached_notes";
    private static final String PREF_UPDATE_CACHED_APK_URL = "update_cached_apk_url";
    private static final String PREF_UPDATE_CACHED_DIGEST = "update_cached_digest";
    private RenaDialog updateDownloadDialog;
    private ProgressBar updateDownloadProgress;
    private TextView updateDownloadStatus;
    private long updateDownloadNotificationId = 94013L;
    private long lastUpdateNotificationAt = 0L;
    private String pendingUpdateApkUrl;
    private String pendingUpdateDigest;
    private String pendingUpdateReleaseName;
    private String pendingUpdateReleaseNotes;
    private boolean pendingAutomaticUpdateDialog = false;
    private int pendingStartupPermissionAfterSettings = -1;
    private int pendingSpecialPermissionStage = -1;
    private int activePermissionIndex = -1;
    private RenaDialog activePermissionDialog;
    private RenaDialog activeSpecialDialog;
    private boolean permissionFlowStarted = false;
    private android.animation.ValueAnimator switchDrawerAnimator;
    private int switchDrawerGeneration = 0;
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
        long lastUsedAt;
        Bitmap thumb;
        boolean thumbLoaded;
    }
    private static class ApkAssetInfo {
        String url;
        String digest;
    }

    private int detectedGlesMajor = 2;

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(CharSequence value, float sp, int color) {
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
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(RenaLocale.wrap(base));
    }

    @Override
    protected void onCreate(Bundle state) {
        RenaTheme.apply(this);
        super.onCreate(state);
        uiEpochSeen = RenaPrefs.lng(this, "ui_epoch", 0L);
        RenaDownloadManager.initialize(this);
        DnsManager.apply(this);
        AppFragment.onCreate(this);
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
        hideThreeDot = RenaSettingsStore.getBoolean(
                this, "hide_three_dot", false
        );
        hideWarningShown = RenaSettingsStore.getBoolean(
                this, "hide_warning_shown", false
        );
        hideNotifications = RenaSettingsStore.getBoolean(
                this, "hide_notifications", false
        );
        hideBarNotification = RenaSettingsStore.getBoolean(
                this, "hide_bar_notification", false
        );
        reduceAnimations = RenaSettingsStore.getBoolean(
                this, "reduce_animation", false
        );
        restoreTabMetadata();
        loadPendingUpdateCleanupState();
        cleanupInstalledUpdateArtifact();

        if (!NativeConfig.isNativeAvailable() || !integrityGate()) {

            closeAppBySystem();
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
        root.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        if (!isUiAlive()) {
                            return;
                        }
                        if (RenaPrefs.bool(MainActivity.this, RenaPrefs.MODE_BROWSER, false)) {
                            enterBrowserMode();
                        }
                        handleViewIntent(getIntent());
                    }
                },
                380L
        );

        root.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        if (!isUiAlive() || !RenaPrefs.autoUpdate(MainActivity.this) || !hasInternetConnection() ||
                                updateCheckRunning || updateInstallInProgress) {
                            return;
                        }
                        RenaSettingsStore.putLong(MainActivity.this, PREF_UPDATE_LAST_AUTO_CHECK, System.currentTimeMillis());
                        startAutomaticUpdateCheck();
                    }
                },
                220L
        );
    }

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
            return;
        }

        scheduleAutomaticUpdateCheck();
        showPendingAutomaticUpdateDialogIfReady();
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

        final RenaDialog dialog =
                new RenaDialog.Builder(this)
                        .setTitle(getString(R.string.storage_access_title))
                        .setMessage(getString(R.string.storage_access_message))
                        .setNegativeButton(
                                getString(R.string.not_now_text),
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
                                getString(R.string.storage_settings_button),
                                null
                        )
                        .create();

        activeSpecialDialog = dialog;
        dialog.setOnShowListener(new android.content.DialogInterface.OnShowListener() {
            @Override
            public void onShow(android.content.DialogInterface d) {
                dialog.getButton(RenaDialog.BUTTON_POSITIVE)
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
                    showPendingAutomaticUpdateDialogIfReady();
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

        final RenaDialog dialog =
                new RenaDialog.Builder(this)
                        .setTitle(getString(R.string.background_access_title))
                        .setMessage(getString(R.string.background_access_message))
                        .setNegativeButton(
                                getString(R.string.not_now_text),
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
                                getString(R.string.storage_settings_button),
                                null
                        )
                        .create();

        activeSpecialDialog = dialog;
        dialog.setOnShowListener(new android.content.DialogInterface.OnShowListener() {
            @Override
            public void onShow(android.content.DialogInterface d) {
                dialog.getButton(RenaDialog.BUTTON_POSITIVE)
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

        final RenaDialog dialog =
                new RenaDialog.Builder(this)
                        .setTitle(getString(R.string.battery_optimization_title))
                        .setMessage(getString(R.string.battery_optimization_message))
                        .setNegativeButton(
                                getString(R.string.not_now_text),
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
                                getString(R.string.storage_settings_button),
                                null
                        )
                        .create();

        activeSpecialDialog = dialog;
        dialog.setOnShowListener(new android.content.DialogInterface.OnShowListener() {
            @Override
            public void onShow(android.content.DialogInterface d) {
                dialog.getButton(RenaDialog.BUTTON_POSITIVE)
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
        final RenaDialog dialog = buildPermissionDialog(permission);
        activePermissionDialog = dialog;
        dialog.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(android.content.DialogInterface d) {
                if (activePermissionDialog == dialog) {
                    activePermissionDialog = null;
                }

                if (hasPermission(permission)) {
                    showNextPermissionDialog(index + 1);
                } else {
                    showPendingAutomaticUpdateDialogIfReady();
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

    private RenaDialog buildPermissionDialog(final String permission) {
        String title = PermissionTexts.title(this, permission);
        String message = PermissionTexts.message(this, permission);
        String allow = getString(R.string.permission_allow_button);
        String deny = getString(R.string.permission_deny_button);

        RenaDialog.Builder builder = new RenaDialog.Builder(this);
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

        final RenaDialog dialog = builder.create();

        dialog.setOnShowListener(
                new android.content.DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(android.content.DialogInterface d) {
                        if (dialog.getButton(RenaDialog.BUTTON_POSITIVE) != null) {
                            dialog.getButton(RenaDialog.BUTTON_POSITIVE)
                                    .setTextColor(Color.rgb(0, 168, 132));
                        }

                        if (dialog.getButton(RenaDialog.BUTTON_NEGATIVE) != null) {
                            dialog.getButton(RenaDialog.BUTTON_NEGATIVE)
                                    .setTextColor(Color.rgb(150, 150, 150));
                        }
                    }
                }
        );

        return dialog;
    }

    private void showPermissionDeniedDialog(final String permission) {
        RenaDialog.Builder builder = new RenaDialog.Builder(this);
        builder.setTitle(getString(R.string.permission_denied_title));
        builder.setMessage(
                PermissionTexts.denied(this, permission)
        );

        builder.setNegativeButton(
                getString(R.string.permission_close_button),
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
                getString(R.string.permission_settings_button),
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

        final RenaDialog dialog = builder.create();

        dialog.setOnShowListener(
                new android.content.DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(android.content.DialogInterface d) {
                        RenaDialog ad = (RenaDialog) d;

                        if (ad.getButton(RenaDialog.BUTTON_POSITIVE) != null) {
                            ad.getButton(RenaDialog.BUTTON_POSITIVE)
                                    .setTextColor(Color.rgb(0, 168, 132));
                        }

                        if (ad.getButton(RenaDialog.BUTTON_NEGATIVE) != null) {
                            ad.getButton(RenaDialog.BUTTON_NEGATIVE)
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
        root.setBackgroundColor(RenaTheme.bg());

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

        FrameLayout loading = new FrameLayout(this);
        loading.setBackgroundColor(RenaTheme.bg());

        ProgressBar spinner = new ProgressBar(this);
        FrameLayout.LayoutParams spinnerLp = new FrameLayout.LayoutParams(
                dp(42),
                dp(42),
                Gravity.CENTER
        );
        loading.addView(spinner, spinnerLp);

        loadingLabel = text(getString(R.string.loading_text), 13, RenaTheme.fg(190));
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
        menuButton.setContentDescription(getString(R.string.menu_content_description));

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
                                    toggleNavigation();
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
        attachBrowser();
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
                            getProfileStoreInstance()
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
                currentTab.lastUsedAt = System.currentTimeMillis();
                installNotificationBridge();
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

        if (multiProfileSupported) {
            try {
                WebViewCompat.setProfile(
                        webView,
                        currentTab.profileId
                );

                ProfileStore store =
                        getProfileStoreInstance();

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
            historyFloorApplied = false;

            Toast.makeText(
                    this,
                    getString(R.string.tab_profiles_unavailable_text),
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
                RenaTheme.bg()
        );

        configureGpuRendering();
        configureWebView();

        currentTab.view = webView;
        currentTab.lastUsedAt = System.currentTimeMillis();
        activeTabIndex =
                findTabIndexById(currentTab.id);

        refreshTabMenu();
        pruneInactiveWebViews();

        if (restoreState != null) {
            try {
                android.webkit.WebBackForwardList restored =
                        webView.restoreState(
                                restoreState
                        );

                webStateRestored =
                        restored != null &&
                        restored.getSize() > 0;

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

        if (!drawerAnimating) {
            drawerProgress = drawerOpen ? 1f : 0f;
        }
        drawer.setTranslationX(-drawerWidthPx * (1f - clamp01(drawerProgress)));
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

        menuButton.animate().cancel();
        menuButton.setRotation(drawerProgress * 90f);
        menuButton.setScaleX(1f - (drawerProgress * 0.08f));
        menuButton.setScaleY(1f - (drawerProgress * 0.08f));
        menuButton.setAlpha(1f);
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

        menuButton.setVisibility(View.VISIBLE);
        menuButton.setClickable(true);
        menuButton.setFocusable(true);
        menuButton.setAlpha(1f);
        menuButton.setRotation(drawerProgress * 90f);
        float scale = 1f - (drawerProgress * 0.08f);
        menuButton.setScaleX(scale);
        menuButton.setScaleY(scale);
        menuButton.bringToFront();
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
                            (browserMode && browser != null && browser.isDrawerOpen()) ||
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
                        openNavigation();
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
                round(RenaTheme.fg(24), dp(16))
        );

        TextView labelView = text(
                label,
                15,
                RenaTheme.text()
        );

        row.addView(
                labelView,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        1f
                )
        );

        final Switch switchView = Ui.toggle(this, checked);

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

        if (enabled) {
            cancelAllWebNotifications();
        }

        RenaSettingsStore.putBoolean(
                this,
                "hide_notifications",
                enabled
        );

        ensureNotificationChannels();

        showToggleToastOnce(
                "hide_notifications",
                enabled
                        ? getString(R.string.hide_notification_enabled_text)
                        : getString(R.string.hide_notification_disabled_text),
                enabled
        );
    }

    private void setHideBarNotification(boolean enabled) {
        hideBarNotification = enabled;

        RenaSettingsStore.putBoolean(
                this,
                "hide_bar_notification",
                enabled
        );

        applyImmersiveFullscreen();

        showToggleToastOnce(
                "hide_bar_notification",
                enabled
                        ? getString(R.string.hide_bar_notification_enabled_text)
                        : getString(R.string.hide_bar_notification_disabled_text),
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
                        ? getString(R.string.reduce_animation_enabled_text)
                        : getString(R.string.reduce_animation_disabled_text),
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
        TextView row = text(label, 15, RenaTheme.text());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), 0, dp(10), 0);
        row.setBackground(
                round(RenaTheme.accentSoft(), dp(16))
        );
        if (label.equals(getString(R.string.new_tab_text))) {
            row.setTextColor(RenaTheme.accentText());
        } else if (label.equals(getString(R.string.close_all_tabs_text))) {
            row.setTextColor(RenaTheme.danger());
        }
        bindSafeClick(row, action);
        return row;
    }

    private void showHideMenuWarning(
            final Switch switchView
    ) {
        new RenaDialog.Builder(this)
                .setTitle(
                        getString(R.string.hide_menu_label)
                )
                .setMessage(
                        getString(R.string.hide_menu_message)
                )
                .setNegativeButton(
                        getString(R.string.cancel_text),
                        null
                )
                .setPositiveButton(
                        getString(R.string.okay_text),
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
                        getString(R.string.hide_menu_toast_text),
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

    private int resolveSwitchDrawerMeasureWidth(LinearLayout body) {
        int width = body == null ? 0 : body.getWidth();
        if (width <= 0 && body != null && body.getParent() instanceof View) {
            width = ((View) body.getParent()).getWidth();
        }
        if (width > 0) {
            return width;
        }

        int rootWidth = root == null ? 0 : root.getWidth();
        if (rootWidth <= 0) {
            rootWidth = getResources().getDisplayMetrics().widthPixels;
        }

        int resolvedDrawerWidth = drawerWidthPx;
        if (resolvedDrawerWidth <= 0) {
            resolvedDrawerWidth = (int) (rootWidth * 0.78f);
            resolvedDrawerWidth = Math.max(resolvedDrawerWidth, dp(300));
            resolvedDrawerWidth = Math.min(resolvedDrawerWidth, dp(430));
            resolvedDrawerWidth = Math.min(
                    resolvedDrawerWidth,
                    Math.max(dp(1), rootWidth - dp(16))
            );
        }
        return Math.max(dp(1), resolvedDrawerWidth - dp(36));
    }

    private void animateSwitchDrawer(
            final LinearLayout body,
            final TextView arrow,
            final boolean open
    ) {
        if (body == null || arrow == null || body.getLayoutParams() == null) {
            return;
        }

        switchDrawerGeneration++;
        final int generation = switchDrawerGeneration;

        if (switchDrawerAnimator != null) {
            switchDrawerAnimator.cancel();
            switchDrawerAnimator = null;
        }
        body.animate().cancel();
        arrow.animate().cancel();

        final LinearLayout.LayoutParams params =
                (LinearLayout.LayoutParams) body.getLayoutParams();
        int startHeight = params.height;
        if (startHeight == ViewGroup.LayoutParams.WRAP_CONTENT || startHeight < 0) {
            startHeight = body.getHeight();
        }
        startHeight = Math.max(0, startHeight);

        if (reduceAnimations) {
            body.setTranslationY(0f);
            body.setAlpha(1f);
            body.setClickable(open);
            body.setFocusable(open);
            params.height = open
                    ? ViewGroup.LayoutParams.WRAP_CONTENT
                    : 0;
            body.setLayoutParams(params);
            body.setVisibility(open ? View.VISIBLE : View.GONE);
            arrow.setRotation(open ? 90f : 0f);
            return;
        }

        if (open) {
            body.setVisibility(View.VISIBLE);
            body.setClickable(true);
            body.setFocusable(true);

            int availableWidth = resolveSwitchDrawerMeasureWidth(body);

            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            body.setLayoutParams(params);
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

            final int measuredHeight = body.getMeasuredHeight();
            if (measuredHeight <= dp(1) && body.getChildCount() > 0) {
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                body.setLayoutParams(params);
                body.setAlpha(1f);
                body.setTranslationY(0f);
                arrow.setRotation(90f);
                switchDrawerAnimator = null;
                return;
            }
            final int targetHeight = Math.max(measuredHeight, dp(1));
            startHeight = Math.min(startHeight, targetHeight);
            params.height = startHeight;
            body.setLayoutParams(params);
            body.setAlpha(0.96f);
            body.setTranslationY(-dp(4));

            switchDrawerAnimator = android.animation.ValueAnimator.ofInt(
                    startHeight,
                    targetHeight
            );
            long openDistance = Math.max(1L, targetHeight - startHeight);
            long openDuration = Math.max(75L, (long) (210L *
                    (openDistance / (float) Math.max(1, targetHeight))));
            switchDrawerAnimator.setDuration(animDuration(openDuration));
            switchDrawerAnimator.setInterpolator(
                    new android.view.animation.DecelerateInterpolator(1.25f)
            );
            final float startRotation = arrow.getRotation();
            switchDrawerAnimator.addUpdateListener(
                    new android.animation.ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(
                                android.animation.ValueAnimator animation
                        ) {
                            if (generation != switchDrawerGeneration) {
                                return;
                            }
                            LinearLayout.LayoutParams current =
                                    (LinearLayout.LayoutParams) body.getLayoutParams();
                            current.height = (Integer) animation.getAnimatedValue();
                            body.setLayoutParams(current);
                            float progress = animation.getAnimatedFraction();
                            body.setAlpha(0.96f + (0.04f * progress));
                            body.setTranslationY(-dp(4) * (1f - progress));
                            arrow.setRotation(startRotation + ((90f - startRotation) * progress));
                        }
                    }
            );
            switchDrawerAnimator.addListener(
                    new android.animation.AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(android.animation.Animator animation) {
                            if (generation != switchDrawerGeneration) {
                                return;
                            }
                            LinearLayout.LayoutParams current =
                                    (LinearLayout.LayoutParams) body.getLayoutParams();
                            current.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                            body.setLayoutParams(current);
                            body.setAlpha(1f);
                            body.setTranslationY(0f);
                            arrow.setRotation(90f);
                            switchDrawerAnimator = null;
                        }
                    }
            );
            switchDrawerAnimator.start();
        } else {
            if (body.getVisibility() == View.GONE) {
                params.height = 0;
                body.setLayoutParams(params);
                arrow.setRotation(0f);
                return;
            }

            if (startHeight <= 0) {
                int availableWidth = resolveSwitchDrawerMeasureWidth(body);
                body.measure(
                        View.MeasureSpec.makeMeasureSpec(availableWidth, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                );
                startHeight = body.getMeasuredHeight();
            }
            if (startHeight <= dp(1)) {
                params.height = 0;
                body.setLayoutParams(params);
                body.setVisibility(View.GONE);
                body.setAlpha(1f);
                body.setTranslationY(0f);
                arrow.setRotation(0f);
                switchDrawerAnimator = null;
                return;
            }

            body.setClickable(false);
            body.setFocusable(false);
            params.height = startHeight;
            body.setLayoutParams(params);

            final float startRotation = arrow.getRotation();
            switchDrawerAnimator = android.animation.ValueAnimator.ofInt(startHeight, 0);
            int measuredCloseHeight = Math.max(startHeight, body.getHeight());
            long closeDuration = Math.max(75L, (long) (210L *
                    (startHeight / (float) Math.max(1, measuredCloseHeight))));
            switchDrawerAnimator.setDuration(animDuration(closeDuration));
            switchDrawerAnimator.setInterpolator(
                    new android.view.animation.DecelerateInterpolator(1.25f)
            );
            switchDrawerAnimator.addUpdateListener(
                    new android.animation.ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(
                                android.animation.ValueAnimator animation
                        ) {
                            if (generation != switchDrawerGeneration) {
                                return;
                            }
                            LinearLayout.LayoutParams current =
                                    (LinearLayout.LayoutParams) body.getLayoutParams();
                            current.height = (Integer) animation.getAnimatedValue();
                            body.setLayoutParams(current);
                            float progress = animation.getAnimatedFraction();
                            body.setAlpha(1f - (0.04f * progress));
                            body.setTranslationY(-dp(2) * progress);
                            arrow.setRotation(startRotation * (1f - progress));
                        }
                    }
            );
            switchDrawerAnimator.addListener(
                    new android.animation.AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(android.animation.Animator animation) {
                            if (generation != switchDrawerGeneration) {
                                return;
                            }
                            LinearLayout.LayoutParams current =
                                    (LinearLayout.LayoutParams) body.getLayoutParams();
                            current.height = 0;
                            body.setLayoutParams(current);
                            body.setVisibility(View.GONE);
                            body.setAlpha(1f);
                            body.setTranslationY(0f);
                            arrow.setRotation(0f);
                            switchDrawerAnimator = null;
                        }
                    }
            );
            switchDrawerAnimator.start();
        }
    }
    private void refreshTabMenu() {
        if (tabMenuContainer == null) {
            return;
        }
        tabMenuContainer.removeAllViews();
        for (int i = 0; i < tabs.size(); i++) {
            final int index = i;
            final TabState tab = tabs.get(i);
            boolean isActive = index == activeTabIndex;
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(8), dp(8), dp(6), dp(8));
            row.setBackground(Ui.ripple(this, Ui.roundStroke(
                    isActive ? RenaTheme.accentSoft() : RenaTheme.fg(24), dp(16),
                    isActive ? RenaTheme.accent() : RenaTheme.outline(),
                    isActive ? dp(2) : dp(RenaTheme.strokeWidthDp())), 16));
            FrameLayout box = new FrameLayout(this);
            box.setBackground(Ui.round(RenaTheme.surface2(), dp(10)));
            box.setClipToOutline(true);
            box.setOutlineProvider(new android.view.ViewOutlineProvider() {
                @Override
                public void getOutline(View view, android.graphics.Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(10));
                }
            });
            Bitmap thumb = tabThumbnail(tab);
            if (thumb != null) {
                ImageView image = new ImageView(this);
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                image.setImageBitmap(thumb);
                box.addView(image, new FrameLayout.LayoutParams(-1, -1));
            } else {
                TextView letter = Ui.text(this, "W", 20f, RenaTheme.accentText(), android.graphics.Typeface.BOLD);
                letter.setGravity(Gravity.CENTER);
                box.addView(letter, new FrameLayout.LayoutParams(-1, -1));
            }
            row.addView(box, new LinearLayout.LayoutParams(dp(72), dp(54)));
            LinearLayout column = new LinearLayout(this);
            column.setOrientation(LinearLayout.VERTICAL);
            TextView label = Ui.text(this, getString(R.string.tab_prefix) + tab.id, 15f, RenaTheme.text(),
                    android.graphics.Typeface.BOLD);
            column.addView(label, new LinearLayout.LayoutParams(-2, -2));
            TextView sub = Ui.text(this, "WhatsApp Web", 11.5f, RenaTheme.muted(), android.graphics.Typeface.NORMAL);
            LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-2, -2);
            subLp.topMargin = dp(3);
            column.addView(sub, subLp);
            LinearLayout.LayoutParams columnLp = new LinearLayout.LayoutParams(0, -2, 1f);
            columnLp.leftMargin = dp(12);
            row.addView(column, columnLp);
            if (index > 0) {
                TextView close = Ui.text(this, NativeConfig.closeTabSymbol(), 22f, RenaTheme.muted(), android.graphics.Typeface.NORMAL);
                close.setGravity(Gravity.CENTER);
                row.addView(close, new LinearLayout.LayoutParams(dp(44), dp(44)));
                bindSafeClick(close, new Runnable() {
                    @Override
                    public void run() {
                        closeTab(index);
                    }
                });
            }
            bindSafeClick(row, new Runnable() {
                @Override
                public void run() {
                    switchToTab(index);
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(8);
            tabMenuContainer.addView(row, lp);
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

        int restoredId = snapshot.activeTabId;
        activeTabIndex = findTabIndexById(restoredId);

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
        TabState activeForSettings = activeTabState();
        if (activeForSettings != null) {
            String idValue = String.valueOf(activeForSettings.id);
            String profileValue = activeForSettings.profileId == null ? "" : activeForSettings.profileId;
            if (!idValue.equals(RenaPrefs.str(this, "active_tab_id", "")) ||
                    !profileValue.equals(RenaPrefs.str(this, "active_profile_id", "x"))) {
                RenaPrefs.setStr(this, "active_tab_id", idValue);
                RenaPrefs.setStr(this, "active_profile_id", profileValue);
            }
        }

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
        if (index < 0 || index >= tabs.size()) {
            return;
        }

        if (drawerOpen || drawerAnimating) {
            setDrawerOpen(false);
        }

        if (!multiProfileSupported && index != 0) {
            Toast.makeText(
                    this,
                    getString(R.string.tab_profiles_unavailable_text),
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
        tabs.get(index).lastUsedAt = System.currentTimeMillis();
        webStateRestored = false;
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
        current.lastUsedAt = System.currentTimeMillis();
        flushProfileForTab(current);
        removeNotificationBridge();

        try {
            if (!RenaDownloadManager.hasPendingBlobDownloadForSource(sourceKey(current.id))) {
                webView.onPause();
            }
            webView.setVisibility(View.GONE);
        } catch (Throwable ignored) {
        }

        webView = null;
    }

    private void pruneInactiveWebViews() {
        int retained = 0;
        for (TabState tab : tabs) {
            if (tab.view != null) {
                retained++;
            }
        }

        while (retained > 2) {
            TabState oldest = null;
            for (TabState tab : tabs) {
                if (tab.view == null || tab.id == currentTabId() ||
                        RenaDownloadManager.hasPendingBlobDownloadForSource(sourceKey(tab.id))) {
                    continue;
                }
                if (oldest == null || tab.lastUsedAt < oldest.lastUsedAt) {
                    oldest = tab;
                }
            }
            if (oldest == null) {
                break;
            }
            destroyTabView(oldest);
            retained--;
        }
    }

    private void destroyActiveWebView() {
        if (webView == null) {
            return;
        }

        TabState current = tabStateForActiveTab();
        if (current != null) {
            RenaDownloadManager.cancelBlobDownloadsForSource(sourceKey(current.id));
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
                    getString(R.string.tab_profiles_unavailable_text),
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        if (tabs.size() >= 10) {
            Toast.makeText(
                    this,
                    getString(R.string.tab_limit_text),
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

        if (firstAvailableTabId() == 6 && !needsTab6Warning) {
            Toast.makeText(
                    this,
                    getString(R.string.storage_warning_toast_text),
                    Toast.LENGTH_SHORT
            ).show();
        }

        if (needsTab6Warning) {
            RenaSettingsStore.putBoolean(
                    this,
                    "tab6_warning",
                    true
            );
            new RenaDialog.Builder(this)
                    .setTitle(
                            getString(R.string.accounts_tabs_text)
                    )
                    .setMessage(
                            getString(R.string.storage_warning_text)
                    )
                    .setNegativeButton(
                            getString(R.string.cancel_text),
                            null
                    )
                    .setPositiveButton(
                            getString(R.string.ignore_text),
                            new android.content.DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(
                                        android.content.DialogInterface dialog,
                                        int which
                                ) {
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
                    getString(R.string.tab_limit_text),
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

        RenaDownloadManager.cancelBlobDownloadsForSource(sourceKey(tab.id));
        flushProfileForTab(tab);

        try {
            tab.view.removeJavascriptInterface("RenaDownloads");
            tab.view.setTag(null);
            ViewParent parent = tab.view.getParent();
            if (parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(tab.view);
            }

            tab.view.stopLoading();
            tab.view.onPause();
            tab.view.destroy();
        } catch (Throwable ignored) {
        }

        tab.view = null;
    }

    private ProfileStore getProfileStoreInstance() throws Exception {
        java.lang.reflect.Method method =
                ProfileStore.class.getMethod("getInstance");
        Object value = method.invoke(null);
        if (value instanceof ProfileStore) {
            return (ProfileStore) value;
        }
        throw new IllegalStateException("ProfileStore.getInstance() returned an invalid value");
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
            Profile profile = getProfileStoreInstance()
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
        deleteTabThumbnail(removedId);
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
                    getString(R.string.no_jokes_text),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        new RenaDialog.Builder(this)
                .setTitle(
                        getString(R.string.close_all_tabs_text)
                )
                .setMessage(
                        getString(R.string.close_all_tabs_message)
                )
                .setNegativeButton(
                        getString(R.string.cancel_text),
                        null
                )
                .setPositiveButton(
                        getString(R.string.clear_text),
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
        persistTabs();
        TabState firstTab = null;
        for (TabState tab : tabs) {
            if (tab.id == 1) {
                firstTab = tab;
                break;
            }
        }

        if (webView != null) {
            destroyActiveWebView();
        }

        for (int i = tabs.size() - 1; i >= 0; i--) {
            TabState tab = tabs.get(i);
            if (tab.id != 1) {
                tabs.remove(i);
                destroyTabView(tab);
            }
        }

        if (firstTab == null) {
            firstTab = new TabState();
            firstTab.id = 1;
            firstTab.profileId = "tab_profile_1";
            firstTab.url = NativeConfig.webUrl();
            tabs.clear();
            tabs.add(firstTab);
        } else {
            destroyTabView(firstTab);
            firstTab.url = TextUtils.isEmpty(firstTab.url)
                    ? NativeConfig.webUrl()
                    : firstTab.url;
        }

        activeTabIndex = 0;
        firstTab.lastUsedAt = System.currentTimeMillis();
        webStateRestored = false;
        historyFloorApplied = false;

        persistTabs();
        refreshTabMenu();
        animateTabRows();
        initializeWebView(null);
    }

    private void loadDeveloperAvatar(final ImageView target) {
        if (target == null) {
            return;
        }

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
        drawerScrim.setBackgroundColor(Color.argb(128, 0, 0, 0));
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

        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int drawerWidth = (int) (screenWidth * 0.78f);
        drawerWidth = Math.max(drawerWidth, dp(300));
        drawerWidth = Math.min(drawerWidth, dp(430));
        drawerWidth = Math.min(drawerWidth, screenWidth - dp(16));
        drawerWidthPx = drawerWidth;

        drawer = new FrameLayout(this);
        drawer.setBackground(roundDrawer());
        drawer.setWillNotDraw(false);
        drawerProgress = 0f;
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
                round(RenaTheme.fg(32), dp(18))
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
                RenaTheme.text()
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
                    text(version, 12, RenaTheme.fg(165)),
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
        LinearLayout.LayoutParams browserRowLp = rowLp();
        browserRowLp.topMargin = dp(6);
        browserRowLp.bottomMargin = dp(10);
        content.addView(
                makePrimaryRow(
                        "\uD83C\uDF10",
                        getString(R.string.menu_browser),
                        getString(R.string.menu_browser_sub),
                        new Runnable() {
                            @Override
                            public void run() {
                                confirmOpenBrowser();
                            }
                        }
                ),
                browserRowLp
        );

        TextView title = text(
                getString(R.string.menu_developer_title),
                14,
                RenaTheme.fg(170)
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
        avatarBackground.setColor(RenaTheme.fg(32));
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
        developer.setTextColor(RenaTheme.text());
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
                        getString(R.string.team_developer_text),
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
                RenaTheme.fg(40)
        );
        content.addView(
                divider,
                exact(dp(1))
        );

        TextView socialMediaTitle = text(
                getString(R.string.social_media_text),
                14,
                RenaTheme.fg(170)
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
                        getString(R.string.menu_community),
                        NativeConfig.communityUrl(),
                        "whatsapp"
                ),
                rowLp()
        );

        TextView accountTitle = text(
                getString(R.string.accounts_tabs_text),
                14,
                RenaTheme.fg(170)
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
                getString(R.string.close_all_tabs_text),
                new Runnable() {
                    @Override
                    public void run() {
                        closeAllTabsRequested();
                    }
                }
        );

        View newTabButton = makeActionRow(
                getString(R.string.new_tab_text),
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
                getString(R.string.tools_text),
                14,
                RenaTheme.fg(170)
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

        final LinearLayout switchDrawer =
                new LinearLayout(this);
        switchDrawer.setOrientation(LinearLayout.VERTICAL);
        switchDrawer.setBackground(
                round(
                        RenaTheme.fg(18),
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
                round(RenaTheme.fg(24), dp(14))
        );
        switchDrawerHeader.setClickable(true);
        switchDrawerHeader.setFocusable(true);

        final TextView switchDrawerLabel =
                text(
                        getString(R.string.switch_toggles_text),
                        15,
                        RenaTheme.text()
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
                        RenaTheme.fg(210)
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
                        getString(R.string.hide_menu_label),
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

        LinearLayout translateRow =
                buildToggleRow(
                        getString(R.string.translate_title),
                        RenaPrefs.bool(this, RenaPrefs.TRANSLATE_W4B, false),
                        new android.widget.CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(
                                    android.widget.CompoundButton button,
                                    boolean checked
                            ) {
                                if (checked == RenaPrefs.bool(MainActivity.this, RenaPrefs.TRANSLATE_W4B, false)) {
                                    return;
                                }
                                setW4bTranslate(checked);
                            }
                        }
                );
        LinearLayout antiDeleteRow =
                buildToggleRow(
                        getString(R.string.antidelete_toggle),
                        RenaPrefs.bool(this, RenaPrefs.ANTI_DELETE, true),
                        new android.widget.CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(
                                    android.widget.CompoundButton button,
                                    boolean checked
                            ) {
                                setW4bFeature(RenaPrefs.ANTI_DELETE, checked);
                            }
                        }
                );
        LinearLayout statusSaverRow =
                buildToggleRow(
                        getString(R.string.status_saver_toggle),
                        RenaPrefs.bool(this, RenaPrefs.STATUS_SAVER, false),
                        new android.widget.CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(
                                    android.widget.CompoundButton button,
                                    boolean checked
                            ) {
                                setW4bFeature(RenaPrefs.STATUS_SAVER, checked);
                                RenaPrefs.setBool(MainActivity.this, RenaPrefs.STATUS_AUTO, checked);
                            }
                        }
                );
        LinearLayout offlineRow =
                buildToggleRow(
                        getString(R.string.offline_mode_title),
                        RenaPrefs.offlineMode(this),
                        new android.widget.CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(
                                    android.widget.CompoundButton button,
                                    boolean checked
                            ) {
                                RenaPrefs.setBool(MainActivity.this, RenaPrefs.OFFLINE_MODE, checked);
                                applyOfflineMode();
                                Toast.makeText(MainActivity.this, checked
                                        ? R.string.offline_mode_enabled : R.string.offline_mode_disabled,
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                );
        LinearLayout hideNotificationRow =
                buildToggleRow(
                        getString(R.string.hide_notification_label),
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

        LinearLayout hideBarNotificationRow =
                buildToggleRow(
                        getString(R.string.hide_bar_notification_label),
                        hideBarNotification,
                        new android.widget.CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(
                                    android.widget.CompoundButton button,
                                    boolean checked
                            ) {
                                setHideBarNotification(checked);
                            }
                        }
                );

        LinearLayout reduceAnimationRow =
                buildToggleRow(
                        getString(R.string.reduce_animation_label),
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
                translateRow,
                compactToggleLp()
        );
        switchDrawerBody.addView(
                antiDeleteRow,
                compactToggleLp()
        );
        switchDrawerBody.addView(
                statusSaverRow,
                compactToggleLp()
        );
        switchDrawerBody.addView(
                offlineRow,
                compactToggleLp()
        );
        switchDrawerBody.addView(
                hideNotificationRow,
                compactToggleLp()
        );
        switchDrawerBody.addView(
                hideBarNotificationRow,
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
                makeIconRow(
                        "\uD83D\uDD0D",
                        getString(R.string.inspect_title),
                        getString(R.string.menu_inspect_sub),
                        new Runnable() {
                            @Override
                            public void run() {
                                openW4bInspector();
                            }
                        }
                ),
                rowLp()
        );
        content.addView(
                makeIconRow(
                        "\uD83D\uDDD1",
                        getString(R.string.deleted_messages_title),
                        getString(R.string.menu_deleted_sub),
                        new Runnable() {
                            @Override
                            public void run() {
                                setDrawerOpen(false);
                                startActivity(new Intent(MainActivity.this, DeletedMessagesActivity.class));
                            }
                        }
                ),
                rowLp()
        );
        content.addView(
                makeIconRow(
                        "\uD83D\uDCF1",
                        getString(R.string.device_title),
                        DeviceProfiles.current(this).label,
                        new Runnable() {
                            @Override
                            public void run() {
                                DeviceDialog.show(MainActivity.this, new Runnable() {
                                    @Override
                                    public void run() {
                                        refreshW4bDevice();
                                        if (browser != null) {
                                            browser.applyDeviceChanges();
                                        }
                                        drawerDirty = true;
                                    }
                                });
                            }
                        }
                ),
                rowLp()
        );
        content.addView(
                makeIconRow(
                        "\uD83D\uDC64",
                        getString(R.string.profile_title),
                        UserProfileStore.summary(this),
                        new Runnable() {
                            @Override
                            public void run() {
                                setDrawerOpen(false);
                                startActivity(new Intent(MainActivity.this, ProfileActivity.class));
                            }
                        }
                ),
                rowLp()
        );
        content.addView(
                makeIconRow(
                        "\u2699",
                        getString(R.string.settings_title),
                        getString(R.string.menu_settings_sub),
                        new Runnable() {
                            @Override
                            public void run() {
                                setDrawerOpen(false);
                                openSettings();
                            }
                        }
                ),
                rowLp()
        );
        content.addView(
                makeActionRow(
                        getString(R.string.data_storage_text),
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
                        getString(R.string.check_updates_text),
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
                        RenaTheme.fg(24),
                        dp(16)
                )
        );

        TextView refreshLabel =
                text(
                        getString(R.string.refresh_text),
                        15,
                        RenaTheme.text()
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
                getString(R.string.refresh_content_description)
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
        LinearLayout.LayoutParams appLockLp = rowLp();
        content.addView(
                makeActionRow(
                        getString(R.string.app_lock_text),
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
                getString(R.string.support_developers_text),
                14,
                RenaTheme.fg(170)
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

        final MarqueeTextView footer =
                new MarqueeTextView(this);
        footerView = footer;

        footer.setText(
                getString(R.string.footer_text)
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
                RenaTheme.fg(210)
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
                        RenaTheme.fg(28),
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

        view.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(
                            View v,
                            MotionEvent event
                    ) {
                        if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {

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
                        NativeConfig.downloadStorageRootName()
                );

        if (!rootDir.exists()) {
            rootDir.mkdirs();
        }

        String[] names = new String[]{
                getString(R.string.download_category_audio),
                getString(R.string.download_category_documents),
                getString(R.string.download_category_images),
                getString(R.string.download_category_videos),
                getString(R.string.download_category_voice_notes),
                getString(R.string.download_category_packages),
                getString(R.string.download_category_other)
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
    private boolean canWriteRenaDirectory() {
        try {
            String state = Environment.getExternalStorageState();
            if (!Environment.MEDIA_MOUNTED.equals(state)) {
                return false;
            }

            File rootDir = new File(
                    Environment.getExternalStorageDirectory(),
                    NativeConfig.downloadStorageRootName()
            );

            if (!rootDir.exists() && !rootDir.mkdirs()) {
                return false;
            }

            if (!rootDir.isDirectory()) {
                return false;
            }

            File probe = new File(rootDir, ".rena_write_test");
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(probe, false);
                output.write(0);
                output.flush();
            } finally {
                if (output != null) {
                    try {
                        output.close();
                    } catch (Throwable ignored) {
                    }
                }
                try {
                    probe.delete();
                } catch (Throwable ignored) {
                }
            }
            return true;
        } catch (Throwable ignored) {
            return false;
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
                getString(R.string.checking_updates_text),
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

    private void scheduleAutomaticUpdateCheck() {
        if (startupFlowFinished) return;
        startupFlowFinished = true;
        root.postDelayed(new Runnable() {
            @Override public void run() {
                if (!isUiAlive() || !RenaPrefs.autoUpdate(MainActivity.this) || !hasInternetConnection() || updateCheckRunning || updateInstallInProgress) return;
                long last = RenaSettingsStore.getLong(MainActivity.this, PREF_UPDATE_LAST_AUTO_CHECK, 0L);
                long now = System.currentTimeMillis();
                if (last > 0L && now - last < AUTOMATIC_UPDATE_CHECK_INTERVAL_MS) return;
                RenaSettingsStore.putLong(MainActivity.this, PREF_UPDATE_LAST_AUTO_CHECK, now);
                startAutomaticUpdateCheck();
            }
        }, 450L);
    }

    private void checkForUpdates() {
        startUpdateCheck(false);
    }

    private void startAutomaticUpdateCheck() {
        startUpdateCheck(true);
    }

    private void startUpdateCheck(final boolean automatic) {
        if (updateCheckRunning) {
            return;
        }

        if (!updaterRunning.compareAndSet(false, true)) {
            return;
        }

        updateCheckRunning = true;

        AsyncTask.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        performUpdateCheck(automatic);
                    }
                }
        );
    }

    private void performUpdateCheck(final boolean automatic) {
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
            connection.setRequestProperty(
                    "X-GitHub-Api-Version",
                    "2026-03-10"
            );

            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
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

            final org.json.JSONObject releaseObject =
                    new org.json.JSONObject(json);

            final String remoteVersion =
                    releaseObject.optString(
                            "tag_name",
                            ""
                    );

            final String releaseName =
                    releaseObject.optString(
                            "name",
                            ""
                    );

            final String releaseNotes =
                    releaseObject.optString(
                            "body",
                            ""
                    );

            final ApkAssetInfo apkAsset =
                    parseApkAsset(json);

            final String apkUrl =
                    apkAsset.url;

            final String digest =
                    apkAsset.digest;

            RenaSettingsStore.putString(
                    MainActivity.this,
                    PREF_UPDATE_CACHED_TAG,
                    remoteVersion
            );
            RenaSettingsStore.putString(
                    MainActivity.this,
                    PREF_UPDATE_CACHED_NAME,
                    releaseName
            );
            RenaSettingsStore.putString(
                    MainActivity.this,
                    PREF_UPDATE_CACHED_NOTES,
                    releaseNotes
            );
            RenaSettingsStore.putString(
                    MainActivity.this,
                    PREF_UPDATE_CACHED_APK_URL,
                    apkUrl
            );
            RenaSettingsStore.putString(
                    MainActivity.this,
                    PREF_UPDATE_CACHED_DIGEST,
                    digest
            );

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

                            int comparison = compareVersions(
                                    getInstalledVersionName(),
                                    remoteVersion
                            );

                            if (comparison <= 0) {
                                if (!automatic) {
                                    Toast.makeText(
                                            MainActivity.this,
                                            comparison < 0
                                                    ? getString(R.string.update_not_available_text)
                                                    : getString(R.string.up_to_date_text),
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }
                                return;
                            }

                            if (TextUtils.isEmpty(
                                    apkUrl
                            ) ||
                                    TextUtils.isEmpty(
                                            digest
                                    )) {

                                if (!automatic) {
                                    openExternal(
                                            NativeConfig.githubRepositoryUrl() +
                                            "/releases/latest"
                                    );

                                    Toast.makeText(
                                            MainActivity.this,
                                            getString(R.string.update_digest_missing_text),
                                            Toast.LENGTH_LONG
                                    ).show();
                                }

                                return;
                            }

                            String lastOffered = RenaSettingsStore.getString(
                                    MainActivity.this,
                                    PREF_UPDATE_LAST_OFFERED_TAG,
                                    ""
                            );
                            if (automatic && remoteVersion.equalsIgnoreCase(lastOffered)) {
                                return;
                            }

                            if (automatic) {
                                RenaSettingsStore.putString(
                                        MainActivity.this,
                                        PREF_UPDATE_LAST_OFFERED_TAG,
                                        remoteVersion
                                );
                            }

                            if (automatic && (startupInternetGateShowing ||
                                    (activePermissionDialog != null && activePermissionDialog.isShowing()) ||
                                    (activeSpecialDialog != null && activeSpecialDialog.isShowing()))) {
                                pendingUpdateVersion = remoteVersion;
                                pendingUpdateReleaseName = releaseName;
                                pendingUpdateReleaseNotes = releaseNotes;
                                pendingUpdateApkUrl = apkUrl;
                                pendingUpdateDigest = digest;
                                pendingAutomaticUpdateDialog = true;
                                return;
                            }

                            showUpdateDialog(
                                    remoteVersion,
                                    releaseName,
                                    releaseNotes,
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

                            showCachedUpdateAfterCheckFailure(automatic);
                        }
                    }
            );

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void showCachedUpdateAfterCheckFailure(final boolean automatic) {
        final String cachedVersion = RenaSettingsStore.getString(
                this,
                PREF_UPDATE_CACHED_TAG,
                ""
        );
        final String cachedName = RenaSettingsStore.getString(
                this,
                PREF_UPDATE_CACHED_NAME,
                ""
        );
        final String cachedNotes = RenaSettingsStore.getString(
                this,
                PREF_UPDATE_CACHED_NOTES,
                ""
        );
        final String cachedApkUrl = RenaSettingsStore.getString(
                this,
                PREF_UPDATE_CACHED_APK_URL,
                ""
        );
        final String cachedDigest = RenaSettingsStore.getString(
                this,
                PREF_UPDATE_CACHED_DIGEST,
                ""
        );

        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        if (!isUiAlive()) {
                            return;
                        }

                        int comparison = compareVersions(
                                getInstalledVersionName(),
                                cachedVersion
                        );

                        if (comparison <= 0 ||
                                TextUtils.isEmpty(cachedVersion) ||
                                TextUtils.isEmpty(cachedApkUrl) ||
                                TextUtils.isEmpty(cachedDigest)) {
                            if (!automatic) {
                                Toast.makeText(
                                        MainActivity.this,
                                        NativeConfig.githubRequestFailedText(),
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                            return;
                        }

                        String lastOffered = RenaSettingsStore.getString(
                                MainActivity.this,
                                PREF_UPDATE_LAST_OFFERED_TAG,
                                ""
                        );
                        if (automatic && cachedVersion.equalsIgnoreCase(lastOffered)) {
                            return;
                        }

                        if (automatic) {
                            RenaSettingsStore.putString(
                                    MainActivity.this,
                                    PREF_UPDATE_LAST_OFFERED_TAG,
                                    cachedVersion
                            );
                        }

                        if (automatic && (startupInternetGateShowing ||
                                (activePermissionDialog != null && activePermissionDialog.isShowing()) ||
                                (activeSpecialDialog != null && activeSpecialDialog.isShowing()))) {
                            pendingUpdateVersion = cachedVersion;
                            pendingUpdateReleaseName = cachedName;
                            pendingUpdateReleaseNotes = cachedNotes;
                            pendingUpdateApkUrl = cachedApkUrl;
                            pendingUpdateDigest = cachedDigest;
                            pendingAutomaticUpdateDialog = true;
                            return;
                        }

                        showUpdateDialog(
                                cachedVersion,
                                cachedName,
                                cachedNotes,
                                cachedApkUrl,
                                cachedDigest
                        );
                    }
                }
        );
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
            String nativeVersionName =
                    NativeConfig.authoritativeVersionName();
            int nativeVersionCode =
                    NativeConfig.authoritativeVersionCode();

            if (!TextUtils.isEmpty(nativeVersionName) &&
                    nativeVersionCode > 0) {
                return nativeVersionName;
            }
        } catch (Throwable ignored) {
        }

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

    private TextView createCenteredDialogTitle(String title) {
        TextView view = text(
                title,
                20,
                RenaTheme.text()
        );
        view.setGravity(Gravity.CENTER);
        view.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        view.setTypeface(getAppFont(android.graphics.Typeface.BOLD));
        view.setPadding(dp(24), dp(18), dp(24), dp(8));
        return view;
    }

    private void showPendingAutomaticUpdateDialogIfReady() {
        if (!pendingAutomaticUpdateDialog || !isUiAlive()) {
            return;
        }
        if (startupInternetGateShowing ||
                (activePermissionDialog != null && activePermissionDialog.isShowing()) ||
                (activeSpecialDialog != null && activeSpecialDialog.isShowing())) {
            return;
        }

        final String version = pendingUpdateVersion;
        final String releaseName = pendingUpdateReleaseName;
        final String notes = pendingUpdateReleaseNotes;
        final String apkUrl = pendingUpdateApkUrl;
        final String digest = pendingUpdateDigest;

        pendingAutomaticUpdateDialog = false;
        pendingUpdateVersion = null;
        pendingUpdateReleaseName = null;
        pendingUpdateReleaseNotes = null;
        pendingUpdateApkUrl = null;
        pendingUpdateDigest = null;

        if (!TextUtils.isEmpty(version) && !TextUtils.isEmpty(apkUrl) && !TextUtils.isEmpty(digest)) {
            showUpdateDialog(version, releaseName, notes, apkUrl, digest);
        }
    }

    private void showUpdateDialog(
            final String version,
            final String releaseName,
            final String releaseNotes,
            final String apkUrl,
            final String digest
    ) {
        int screenHeight =
                getResources()
                        .getDisplayMetrics()
                        .heightPixels;

        int scrollHeight =
                Math.min(
                        dp(320),
                        Math.max(
                                dp(190),
                                (int) (screenHeight * 0.42f)
                        )
                );

        LinearLayout content =
                new LinearLayout(this);
        content.setOrientation(
                LinearLayout.VERTICAL
        );
        content.setPadding(
                dp(6),
                dp(2),
                dp(6),
                dp(2)
        );

        String cleanVersion =
                TextUtils.isEmpty(version)
                        ? getInstalledVersionName()
                        : version;

        String meta =
                "Version " +
                stripVersionPrefix(cleanVersion);

        if (!TextUtils.isEmpty(releaseName) &&
                !releaseName.equalsIgnoreCase(cleanVersion)) {
            meta += "\n" + releaseName.trim();
        }

        TextView versionView =
                text(
                        meta,
                        16,
                        RenaTheme.text()
                );
        versionView.setGravity(Gravity.CENTER);
        versionView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        versionView.setPadding(
                dp(4),
                dp(2),
                dp(4),
                dp(12)
        );

        TextView notesLabel =
                text(
                        "What's new",
                        14,
                        RenaTheme.accent()
                );
        notesLabel.setGravity(Gravity.CENTER);
        notesLabel.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        notesLabel.setTypeface(
                getAppFont(
                        android.graphics.Typeface.BOLD
                )
        );
        notesLabel.setPadding(
                dp(4),
                dp(2),
                dp(4),
                dp(8)
        );

        ScrollView notesScroll =
                new ScrollView(this);
        notesScroll.setFillViewport(false);
        notesScroll.setClipToPadding(true);
        notesScroll.setVerticalScrollBarEnabled(true);
        notesScroll.setHorizontalScrollBarEnabled(false);
        notesScroll.setOverScrollMode(
                View.OVER_SCROLL_IF_CONTENT_SCROLLS
        );
        notesScroll.setPadding(0, 0, 0, 0);
        notesScroll.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);

        TextView notesView =
                text(
                        formatReleaseNotes(releaseNotes),
                        14,
                        RenaTheme.fg(232)
                );
        notesView.setGravity(
                Gravity.TOP | Gravity.START
        );
        notesView.setLineSpacing(
                dp(4),
                1.04f
        );
        notesView.setIncludeFontPadding(true);
        notesView.setPadding(
                dp(14),
                dp(12),
                dp(14),
                dp(16)
        );
        notesView.setMovementMethod(
                LinkMovementMethod.getInstance()
        );

        notesScroll.addView(
                notesView,
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        TextView scrollHint = text(
                getString(R.string.update_scroll_hint),
                12,
                RenaTheme.fg(155)
        );
        scrollHint.setGravity(Gravity.CENTER);
        scrollHint.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        content.addView(
                versionView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );
        content.addView(
                notesLabel,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );
        FrameLayout notesContainer = new FrameLayout(this);
        notesContainer.addView(
                notesScroll,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        content.addView(
                notesContainer,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        scrollHeight
                )
        );
        content.addView(
                scrollHint,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(28)
                )
        );

        final RenaDialog dialog =
                new RenaDialog.Builder(this)
                        .setCustomTitle(
                                createCenteredDialogTitle(
                                        getString(R.string.update_available_title)
                                )
                        )
                        .setView(content)
                        .setNegativeButton(
                                getString(R.string.cancel_text),
                                null
                        )
                        .setPositiveButton(
                                getString(R.string.download_update_text),
                                new android.content.DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(
                                            android.content.DialogInterface dialog,
                                            int which
                                    ) {
                                        RenaSettingsStore.putString(
                                                MainActivity.this,
                                                PREF_UPDATE_LAST_OFFERED_TAG,
                                                version
                                        );
                                        downloadAndInstallApk(
                                                apkUrl,
                                                digest,
                                                version
                                        );
                                    }
                                }
                        )
                        .create();

        dialog.show();

        if (dialog.getWindow() != null) {
            int screenWidth =
                    getResources()
                            .getDisplayMetrics()
                            .widthPixels;
            int width =
                    Math.min(
                            dp(560),
                            Math.max(
                                    dp(290),
                                    screenWidth - dp(28)
                            )
                    );
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private CharSequence formatReleaseNotes(String notes) {
        if (TextUtils.isEmpty(notes)) {
            return "No release notes were provided for this release.";
        }

        String value = notes.replace("\r\n", "\n").replace("\r", "\n").trim();
        value = value.replaceAll("!\\[([^\\]]*)\\]\\([^\\)]+\\)", "$1");
        value = value.replaceAll("<[^>]+>", "");

        SpannableStringBuilder result = new SpannableStringBuilder();
        String[] lines = value.split("\n", -1);
        boolean fencedCode = false;

        for (int i = 0; i < lines.length; i++) {
            String raw = lines[i];
            String line = raw.trim();

            if (line.startsWith("```") || line.startsWith("~~~")) {
                fencedCode = !fencedCode;
                if (fencedCode && result.length() > 0 && result.charAt(result.length() - 1) != '\n') result.append("\n");
                if (i < lines.length - 1) result.append("\n");
                continue;
            }

            if (fencedCode) {
                int start = result.length();
                result.append(raw);
                if (result.length() > start) {
                    result.setSpan(new TypefaceSpan("monospace"), start, result.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    result.setSpan(new BackgroundColorSpan(RenaTheme.fg(36)), start, result.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                if (i < lines.length - 1) result.append("\n");
                continue;
            }

            if (line.matches("[-_=]{3,}")) {
                if (i < lines.length - 1) result.append("\n");
                continue;
            }

            boolean heading = line.matches("^#{1,6}\\s*.+$");
            boolean bullet = line.matches("^[-*+]\\s+.*$");
            boolean ordered = line.matches("^\\d+[.)]\\s+.*$");
            boolean task = line.matches("^[-*+]\\s+\\[[ xX]\\]\\s+.*$");
            boolean quote = line.startsWith(">") ;

            if (heading) {
                line = line.replaceFirst("^#{1,6}\\s*", "");
            } else if (task) {
                boolean checked = line.matches("^[-*+]\\s+\\[[xX]\\]\\s+.*$");
                line = (checked ? "☑ " : "☐ ") + line.replaceFirst("^[-*+]\\s+\\[[ xX]\\]\\s+", "");
            } else if (bullet) {
                line = "• " + line.replaceFirst("^[-*+]\\s+", "");
            } else if (ordered) {
                line = line.replaceFirst("^(\\d+)[.)]\\s+", "$1. ");
            } else if (quote) {
                line = "│ " + (line.length() > 1 ? line.substring(1).trim() : "");
            }

            int lineStart = result.length();
            appendInlineMarkdown(result, line);
            int lineEnd = result.length();

            if (heading && lineEnd > lineStart) {
                result.setSpan(new StyleSpan(Typeface.BOLD), lineStart, lineEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                result.setSpan(new RelativeSizeSpan(1.18f), lineStart, lineEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                result.setSpan(new ForegroundColorSpan(RenaTheme.accentText()), lineStart, lineEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (quote && lineEnd > lineStart) {
                result.setSpan(new ForegroundColorSpan(RenaTheme.accentText()), lineStart, lineEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            if (i < lines.length - 1) result.append("\n");
        }

        while (result.length() > 0 && result.charAt(result.length() - 1) == '\n') result.delete(result.length() - 1, result.length());
        return result;
    }

    private void appendInlineMarkdown(SpannableStringBuilder out, String line) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "\\[([^\\]]+)\\]\\(([^\\)]+)\\)" +
                "|`([^`\\n]+)`" +
                "|\\*\\*\\s*(.+?)\\s*\\*\\*" +
                "|__(\\s*.+?\\s*)__" +
                "|~~\\s*(.+?)\\s*~~" +
                "|\\*(?!\\*)\\s*(.+?)\\s*\\*(?!\\*)" +
                "|(?<!_)_(?!_)\\s*(.+?)\\s*_(?!_)"
        );

        java.util.regex.Matcher matcher = pattern.matcher(line);
        int cursor = 0;
        while (matcher.find()) {
            if (matcher.start() > cursor) out.append(line.substring(cursor, matcher.start()));
            int start = out.length();

            if (matcher.group(1) != null) {
                out.append(matcher.group(1));
                out.setSpan(new URLSpan(matcher.group(2)), start, out.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                out.setSpan(new ForegroundColorSpan(RenaTheme.info()), start, out.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (matcher.group(3) != null) {
                out.append(matcher.group(3));
                out.setSpan(new TypefaceSpan("monospace"), start, out.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                out.setSpan(new BackgroundColorSpan(RenaTheme.fg(42)), start, out.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (matcher.group(4) != null || matcher.group(5) != null) {
                out.append(matcher.group(4) != null ? matcher.group(4).trim() : matcher.group(5).trim());
                out.setSpan(new StyleSpan(Typeface.BOLD), start, out.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (matcher.group(6) != null) {
                out.append(matcher.group(6).trim());
                out.setSpan(new StrikethroughSpan(), start, out.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (matcher.group(7) != null || matcher.group(8) != null) {
                out.append(matcher.group(7) != null ? matcher.group(7).trim() : matcher.group(8).trim());
                out.setSpan(new StyleSpan(Typeface.ITALIC), start, out.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            cursor = matcher.end();
        }
        if (cursor < line.length()) out.append(line.substring(cursor));
    }

    private void showUpdateDownloadDialog() {
        if (updateDownloadDialog != null && updateDownloadDialog.isShowing()) {
            return;
        }

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(dp(10), dp(6), dp(10), dp(2));

        updateDownloadStatus = text(getString(R.string.update_preparing), 14, RenaTheme.text());
        updateDownloadStatus.setGravity(Gravity.CENTER);
        updateDownloadStatus.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        updateDownloadStatus.setSingleLine(false);
        updateDownloadStatus.setPadding(0, dp(4), 0, dp(12));

        updateDownloadProgress = new ProgressBar(
                this, null, android.R.attr.progressBarStyleHorizontal
        );
        updateDownloadProgress.setMax(1000);
        updateDownloadProgress.setProgress(0);

        content.addView(updateDownloadStatus, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        LinearLayout.LayoutParams progressLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(8)
        );
        progressLp.bottomMargin = dp(8);
        content.addView(updateDownloadProgress, progressLp);

        updateDownloadDialog = new RenaDialog.Builder(this)
                .setCustomTitle(createCenteredDialogTitle(getString(R.string.update_downloading_title)))
                .setView(content)
                .setNegativeButton(getString(R.string.cancel_text), null)
                .setPositiveButton(getString(R.string.update_hide), null)
                .create();

        updateDownloadDialog.setCanceledOnTouchOutside(false);
        updateDownloadDialog.show();

        if (updateDownloadDialog.getButton(RenaDialog.BUTTON_NEGATIVE) != null) {
            updateDownloadDialog.getButton(RenaDialog.BUTTON_NEGATIVE)
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showCancelUpdateDownloadConfirmation();
                        }
                    });
        }
        if (updateDownloadDialog.getButton(RenaDialog.BUTTON_POSITIVE) != null) {
            updateDownloadDialog.getButton(RenaDialog.BUTTON_POSITIVE)
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            updateDownloadHidden = true;
                            Toast.makeText(MainActivity.this, getString(R.string.update_background_toast), Toast.LENGTH_SHORT).show();
                            updateDownloadDialog.dismiss();
                        }
                    });
        }

        if (updateDownloadDialog.getWindow() != null) {
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int width = Math.min(dp(420), Math.max(dp(280), screenWidth - dp(40)));
            updateDownloadDialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void showCancelUpdateDownloadConfirmation() {
        new RenaDialog.Builder(this)
                .setTitle(getString(R.string.update_cancel_title))
                .setMessage(getString(R.string.update_cancel_message))
                .setNegativeButton(getString(R.string.update_keep), null)
                .setPositiveButton(getString(R.string.update_cancel_confirm), new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        cancelUpdateDownload();
                    }
                })
                .show();
    }
    private void cancelUpdateDownload() {
        stopUpdatePolling();
        try {
            String id = TextUtils.isEmpty(updateItemId) ? RenaSettingsStore.getString(this, "update_item_id", "") : updateItemId;
            if (!TextUtils.isEmpty(id)) {
                RenaDownloadManager.cancel(id);
                RenaDownloadManager.delete(id, true);
            }
        } catch (Throwable ignored) {
        }
        deletePendingUpdateFile();
        clearPendingUpdateState();
        updateInstallInProgress = false;
        updateDownloadId = -1L;
        updateItemId = null;
        if (updateDownloadDialog != null) {
            updateDownloadDialog.dismiss();
            updateDownloadDialog = null;
        }
        updateDownloadProgress = null;
        updateDownloadStatus = null;
    }

    private void updateDownloadUi(
            final long downloaded,
            final long total
    ) {
        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        if (!isUiAlive()) {
                            return;
                        }

                        int progressValue = 0;
                        if (total > 0L) {
                            progressValue = (int) Math.max(
                                    0L,
                                    Math.min(
                                            1000L,
                                            downloaded * 1000L / total
                                    )
                            );
                        }

                        if (updateDownloadProgress != null) {
                            updateDownloadProgress.setProgress(progressValue);
                        }

                        if (updateDownloadStatus != null) {
                            if (total > 0L) {
                                updateDownloadStatus.setText(
                                        formatBytes(downloaded) + " / " + formatBytes(total) +
                                                "  •  " + (progressValue / 10) + "%"
                                );
                            } else {
                                updateDownloadStatus.setText(
                                        formatBytes(downloaded) + " downloaded"
                                );
                            }
                        }
                    }
                }
        );
    }

    private String formatBytes(long value) {
        if (value < 1024L) {
            return value + " B";
        }
        if (value < 1024L * 1024L) {
            return String.format(Locale.US, "%.1f KB", value / 1024.0);
        }
        if (value < 1024L * 1024L * 1024L) {
            return String.format(Locale.US, "%.1f MB", value / (1024.0 * 1024.0));
        }
        return String.format(Locale.US, "%.2f GB", value / (1024.0 * 1024.0 * 1024.0));
    }

    private void downloadAndInstallApk(
            final String apkUrl,
            final String expectedDigest,
            final String targetVersion
    ) {
        if (Build.VERSION.SDK_INT >= 26 &&
                !getPackageManager().canRequestPackageInstalls()) {
            pendingUpdateApkUrl = apkUrl;
            pendingUpdateDigest = expectedDigest;
            pendingUpdateVersion = targetVersion;

            new RenaDialog.Builder(this)
                    .setTitle(getString(R.string.update_install_permission_title))
                    .setMessage(getString(R.string.update_install_permission_message))
                    .setNegativeButton(getString(R.string.cancel_text), new android.content.DialogInterface.OnClickListener() {
                        @Override public void onClick(android.content.DialogInterface dialog, int which) {
                            pendingUpdateApkUrl = null;
                            pendingUpdateDigest = null;
                            pendingUpdateVersion = null;
                        }
                    })
                    .setPositiveButton(getString(R.string.storage_settings_button), new android.content.DialogInterface.OnClickListener() {
                        @Override public void onClick(android.content.DialogInterface dialog, int which) {
                            try {
                                startActivity(new Intent(
                                        android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                        Uri.parse("package:" + getPackageName())
                                ));
                            } catch (Throwable ignored) {
                            }
                        }
                    })
                    .show();
            return;
        }

        startManagedApkDownload(apkUrl, expectedDigest, targetVersion);
    }
    private String updateItemId;
    private boolean pendingUpdateStorage;
    private boolean updateInstalledChecked;

    private void startManagedApkDownload(final String apkUrl, final String expectedDigest, final String targetVersion) {
        if (!RenaPaths.canWrite()) {
            pendingUpdateApkUrl = apkUrl;
            pendingUpdateDigest = expectedDigest;
            pendingUpdateVersion = targetVersion;
            pendingUpdateStorage = true;
            RenaDownloadHelper.askStorageAccess(this, new Runnable() {
                @Override
                public void run() {
                    pendingUpdateStorage = false;
                    pendingUpdateApkUrl = null;
                    pendingUpdateDigest = null;
                    pendingUpdateVersion = null;
                    Toast.makeText(MainActivity.this, getString(R.string.download_denied_text), Toast.LENGTH_LONG).show();
                }
            });
            return;
        }
        RenaPaths.ensureDirs();
        stopUpdatePolling();
        deletePendingUpdateFile();
        File output = new File(RenaPaths.dir(RenaPaths.DOCUMENT), "Rena-W4B-update.apk");
        try {
            output.delete();
            new File(output.getPath() + ".part").delete();
        } catch (Throwable ignored) {
        }
        try {
            RenaDownloadManager.DownloadItem item = RenaDownloadManager.createHttp(this, apkUrl, output.getName(),
                    RenaPaths.DOCUMENT, "application/vnd.android.package-archive",
                    "Rena-W4B-Updater/" + stripVersionPrefix(targetVersion), null, null, -1L, false);
            if (item == null) {
                throw new IllegalStateException("download");
            }
            final File apk = new File(item.path);
            updateItemId = item.id;
            updateDownloadId = System.currentTimeMillis();
            updateDownloadHidden = false;
            updateInstallInProgress = true;
            RenaSettingsStore.putLong(this, "update_download_id", updateDownloadId);
            RenaSettingsStore.putString(this, "update_item_id", item.id);
            RenaSettingsStore.putString(this, "update_expected_digest", expectedDigest);
            RenaSettingsStore.putString(this, PREF_UPDATE_PENDING_PATH, apk.getAbsolutePath());
            RenaSettingsStore.putString(this, PREF_UPDATE_PENDING_VERSION, targetVersion);
            updatePollRunnable = new Runnable() {
                @Override public void run() { pollUpdateDownload(apkUrl, expectedDigest, targetVersion, apk); }
            };
            showUpdateDownloadDialog();
            updateHandler.post(updatePollRunnable);
        } catch (Throwable ignored) {
            updateInstallInProgress = false;
            deletePendingUpdateFile();
            Toast.makeText(this, getString(R.string.update_download_failed_text), Toast.LENGTH_LONG).show();
        }
    }
    private void pollUpdateDownload(final String apkUrl, final String expectedDigest, final String targetVersion, final File output) {
        if (!isUiAlive()) return;
        String id = updateItemId;
        if (TextUtils.isEmpty(id)) {
            id = RenaSettingsStore.getString(this, "update_item_id", "");
            updateItemId = id;
        }
        RenaDownloadManager.DownloadItem item = RenaDownloadManager.find(id);
        if (item == null) {
            stopUpdatePolling();
            handleManagedDownloadFailure(output);
            return;
        }
        updateDownloadUi(item.downloaded, item.total);
        if (item.status == RenaDownloadManager.COMPLETED) {
            stopUpdatePolling();
            verifyAndLaunchManagedApk(new File(item.path), expectedDigest, targetVersion);
            return;
        }
        if (item.status == RenaDownloadManager.FAILED || item.status == RenaDownloadManager.CANCELLED) {
            stopUpdatePolling();
            handleManagedDownloadFailure(new File(item.path));
            return;
        }
        if (updatePollRunnable != null) updateHandler.postDelayed(updatePollRunnable, 450L);
    }

    private void verifyAndLaunchManagedApk(final File apk, final String expectedDigest, final String targetVersion) {
        AsyncTask.execute(new Runnable() {
            @Override public void run() {
                boolean valid = false;
                try {
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    InputStream in = new BufferedInputStream(new java.io.FileInputStream(apk));
                    byte[] buffer = new byte[32 * 1024];
                    int read;
                    while ((read = in.read(buffer)) != -1) md.update(buffer, 0, read);
                    in.close();
                    valid = expectedDigest != null && expectedDigest.equalsIgnoreCase(toHex(md.digest())) && verifyApkSigner(apk);
                } catch (Throwable ignored) {
                }
                final boolean result = valid;
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        if (!result) {
                            handleManagedDownloadFailure(apk);
                            return;
                        }
                        pendingInstalledCleanupPath = apk.getAbsolutePath();
                        pendingInstalledCleanupVersion = targetVersion;
                        RenaSettingsStore.putString(MainActivity.this, PREF_UPDATE_PENDING_PATH, pendingInstalledCleanupPath);
                        RenaSettingsStore.putString(MainActivity.this, PREF_UPDATE_PENDING_VERSION, targetVersion);
                        RenaSettingsStore.putBoolean(MainActivity.this, PREF_UPDATE_READY, true);
                        if (updateDownloadDialog != null) { updateDownloadDialog.dismiss(); updateDownloadDialog = null; }
                        updateDownloadProgress = null;
                        updateDownloadStatus = null;
                        clearDownloadManagerStateOnly();
                        launchApkInstaller(apk);
                    }
                });
            }
        });
    }

    private void handleManagedDownloadFailure(File output) {
        updateInstallInProgress = false;
        if (output != null) { try { output.delete(); } catch (Throwable ignored) {} }
        clearPendingUpdateState();
        if (updateDownloadDialog != null) { updateDownloadDialog.dismiss(); updateDownloadDialog = null; }
        updateDownloadProgress = null;
        updateDownloadStatus = null;
        Toast.makeText(this, getString(R.string.update_download_failed_text), Toast.LENGTH_LONG).show();
    }

    private void stopUpdatePolling() {
        if (updatePollRunnable != null) updateHandler.removeCallbacks(updatePollRunnable);
        updatePollRunnable = null;
    }

    private void clearDownloadManagerStateOnly() {
        RenaSettingsStore.remove(this, "update_download_id");
        RenaSettingsStore.remove(this, "update_expected_digest");
        updateDownloadId = -1L;
        updateInstallInProgress = false;
    }
    private void clearPendingUpdateState() {
        clearDownloadManagerStateOnly();
        RenaSettingsStore.remove(this, PREF_UPDATE_PENDING_PATH);
        RenaSettingsStore.remove(this, PREF_UPDATE_PENDING_VERSION);
        RenaSettingsStore.remove(this, PREF_UPDATE_READY);
        RenaSettingsStore.remove(this, "update_item_id");
        pendingInstalledCleanupPath = null;
        pendingInstalledCleanupVersion = null;
    }
    private void deletePendingUpdateFile() {
        String path = RenaSettingsStore.getString(this, PREF_UPDATE_PENDING_PATH, "");
        if (TextUtils.isEmpty(path)) path = pendingInstalledCleanupPath;
        String id = RenaSettingsStore.getString(this, "update_item_id", "");
        if (!TextUtils.isEmpty(id)) {
            try { RenaDownloadManager.delete(id, true); } catch (Throwable ignored) {}
        }
        if (!TextUtils.isEmpty(path)) {
            try { new File(path).delete(); } catch (Throwable ignored) {}
            try { new File(path + ".part").delete(); } catch (Throwable ignored) {}
        }
    }

    private void deleteAllUpdateArtifacts() {
        deletePendingUpdateFile();
        try {
            File dir = RenaPaths.dir(RenaPaths.DOCUMENT);
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    String name = file.getName();
                    if (name.startsWith("Rena-W4B-update")) {
                        file.delete();
                    }
                }
            }
            File legacyRoot = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (legacyRoot != null) {
                File legacy = new File(legacyRoot, "Rena/Updates/Rena-W4B-update.apk");
                if (legacy.exists()) {
                    legacy.delete();
                }
            }
        } catch (Throwable ignored) {
        }
        clearPendingUpdateState();
    }

    private long installedVersionCode() {
        try {
            android.content.pm.PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            return Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode;
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    private void maybeShowUpdateInstalledDialog() {
        if (updateInstalledChecked || !isUiAlive()) {
            return;
        }
        if (startupInternetGateShowing ||
                (activePermissionDialog != null && activePermissionDialog.isShowing()) ||
                (activeSpecialDialog != null && activeSpecialDialog.isShowing())) {
            return;
        }
        updateInstalledChecked = true;
        long current = installedVersionCode();
        long last = RenaPrefs.lng(this, RenaPrefs.LAST_SEEN_VERSION, 0L);
        RenaPrefs.setLng(this, RenaPrefs.LAST_SEEN_VERSION, current);
        if (last <= 0L) {
            try {
                android.content.pm.PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
                if (info.lastUpdateTime - info.firstInstallTime < 5000L) {
                    return;
                }
            } catch (Throwable ignored) {
                return;
            }
        } else if (current <= last) {
            return;
        }
        deleteAllUpdateArtifacts();
        UpdateSuccessDialog.show(this, getInstalledVersionName());
    }

    private void loadPendingUpdateCleanupState() {
        pendingInstalledCleanupPath = RenaSettingsStore.getString(this, PREF_UPDATE_PENDING_PATH, "");
        pendingInstalledCleanupVersion = RenaSettingsStore.getString(this, PREF_UPDATE_PENDING_VERSION, "");
        updateDownloadId = RenaSettingsStore.getLong(this, "update_download_id", -1L);
    }
    private void cleanupInstalledUpdateArtifact() {
        if (TextUtils.isEmpty(pendingInstalledCleanupPath) || TextUtils.isEmpty(pendingInstalledCleanupVersion)) return;
        String installed = getInstalledVersionName();
        if (compareVersions(installed, pendingInstalledCleanupVersion) >= 0) {
            deleteAllUpdateArtifacts();
        }
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
            RenaSettingsStore.putBoolean(this, PREF_UPDATE_READY, true);
            Toast.makeText(
                    this,
                    getString(R.string.update_download_failed_text),
                    Toast.LENGTH_LONG
            ).show();
        }
    }
    private View makeSpecialRepositoryRow() {
        final ShimmerDonateView star =
                new ShimmerDonateView(this);

        star.setText(
                getString(R.string.star_repository_text)
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
                getString(R.string.donate_text)
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
                        RenaTheme.fg(24),
                        dp(16)
                )
        );

        ImageView icon = new ImageView(this);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        icon.setPadding(
                "check_updates".equals(assetName) ? dp(10) : dp(6),
                "check_updates".equals(assetName) ? dp(10) : dp(6),
                "check_updates".equals(assetName) ? dp(10) : dp(6),
                "check_updates".equals(assetName) ? dp(10) : dp(6)
        );

        int iconResourceId;
        if ("check_updates".equals(assetName)) {

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
            icon.setContentDescription(getString(R.string.check_updates_text));
        }

        FrameLayout.LayoutParams iconLp =
                new FrameLayout.LayoutParams(
                        dp(36),
                        dp(36),
                        Gravity.CENTER_VERTICAL | Gravity.START
                );
        iconLp.leftMargin = dp(18);
        row.addView(icon, iconLp);

        TextView labelView = text(label, 15, RenaTheme.text());
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
                round(RenaTheme.fg(24), dp(16))
        );

        FrameLayout iconSlot = new FrameLayout(this);
        iconSlot.setBackground(
                round(
                        RenaTheme.fg(22),
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
                RenaTheme.text()
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
                    getString(R.string.easter_egg_found_text),
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
                4000000L;

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
                        getString(R.string.notification_channel_name),
                        NotificationManager.IMPORTANCE_HIGH
                );
        normal.setDescription(
                getString(R.string.notification_channel_description)
        );

        NotificationChannel silent =
                new NotificationChannel(
                        NOTIFICATION_SILENT_CHANNEL_ID,
                        getString(R.string.notification_silent_channel_name),
                        NotificationManager.IMPORTANCE_LOW
                );
        silent.setDescription(
                getString(R.string.notification_silent_channel_description)
        );
        silent.setSound(null, null);
        silent.enableVibration(false);

        manager.createNotificationChannel(normal);
        manager.createNotificationChannel(silent);
    }

    private void cancelAllWebNotifications() {
        try {
            NotificationManager manager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager == null) return;
            for (int tabId = 1; tabId <= 10; tabId++) {
                manager.cancel(("rena_notification_" + tabId).hashCode());
            }
        } catch (Throwable ignored) {
        }
    }

    private void postWebNotification(
            String chat,
            String body,
            String key
    ) {
        if (hideNotifications) {
            return;
        }

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
                            getString(R.string.notification_channel_name),
                            NotificationManager.IMPORTANCE_HIGH
                    );

            channel.setDescription(
                    getString(R.string.notification_channel_description)
            );

            manager.createNotificationChannel(channel);

            NotificationChannel silentChannel =
                    new NotificationChannel(
                            NOTIFICATION_SILENT_CHANNEL_ID,
                            getString(R.string.notification_silent_channel_name),
                            NotificationManager.IMPORTANCE_LOW
                    );

            silentChannel.setDescription(
                    getString(R.string.notification_silent_channel_description)
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
                        getString(R.string.notification_open_text),
                        openPending
                ).build()
        );

        RemoteInput remoteInput =
                new RemoteInput.Builder(
                        REMOTE_INPUT_REPLY
                )
                        .setLabel(
                                getString(R.string.notification_reply_text)
                        )
                        .build();

        Notification.Action.Builder replyAction =
                new Notification.Action.Builder(
                        null,
                        getString(R.string.notification_reply_text),
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
                        getString(R.string.notification_read_text),
                        readPending
                ).build()
        );

        builder.addAction(
                new Notification.Action.Builder(
                        null,
                        getString(R.string.notification_ignore_text),
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
    private String sourceKey(int tabId) {
        return "w4b:" + tabId;
    }

    private boolean isWhatsAppHost(String host) {
        return host != null && host.equalsIgnoreCase("web.whatsapp.com");
    }

    private void configureWebView() {
        final WebView target = webView;
        final TabState owner = activeTabState();
        final int tabId = owner == null ? 0 : owner.id;
        final String profile = owner == null ? "default" : owner.profileId;
        final String tabKey = sourceKey(tabId);

        WebEngine.applyBase(this, target, false);
        WebEngine.applyDevice(this, target, true);

        CookieManager cookies = getProfileCookieManager();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(target, true);

        RenaDownloadHelper.attach(target);
        target.addJavascriptInterface(new TranslateBridge(this, target, new TranslateBridge.Host() {
            @Override
            public void onTranslateClosed() {
                setW4bTranslate(false);
            }
        }), "RenaTranslate");
        target.addJavascriptInterface(new AntiDeleteStore.Bridge(AntiDeleteStore.forProfile(this, profile)), "RenaAntiDelete");
        target.addJavascriptInterface(new StatusBridge(this, target, tabKey, tabId), "RenaStatus");
        target.addJavascriptInterface(new InspectorHub.Bridge(InspectorHub.forKey(tabKey)), "RenaInspector");
        WebEngine.install(this, target, false);

        target.setWebChromeClient(
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
                        boolean multiple = false;

                        try {
                            multiple =
                                    params.getMode() ==
                                    FileChooserParams
                                            .MODE_OPEN_MULTIPLE;
                        } catch (Throwable ignored) {
                        }

                        return browserFileChooser(callback, multiple);
                    }

                    @Override
                    public boolean onConsoleMessage(android.webkit.ConsoleMessage message) {
                        String level = "log";
                        if (message.messageLevel() == android.webkit.ConsoleMessage.MessageLevel.ERROR) {
                            level = "error";
                        } else if (message.messageLevel() == android.webkit.ConsoleMessage.MessageLevel.WARNING) {
                            level = "warn";
                        }
                        InspectorHub.forKey(tabKey).addConsole(level, message.message());
                        return true;
                    }
                }
        );

        target.setWebViewClient(
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
                        WebEngine.injectFallback(view);

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

                        try {
                            getProfileCookieManager().flush();
                        } catch (Throwable ignored) {
                        }

                        installNotificationBridge();
                        handleNotificationIntent(getIntent());

                        if (!WebEngine.hasDocStart(view)) {
                            WebEngine.switchTheme(MainActivity.this, view);
                            if (RenaPrefs.bool(MainActivity.this, RenaPrefs.TRANSLATE_W4B, false)) {
                                WebEngine.setTranslate(MainActivity.this, view, true);
                            }
                        }
                        scheduleTabThumbnail();

                        setLoadingOverlayVisible(false);
                    }

                    @Override
                    public boolean shouldOverrideUrlLoading(
                            WebView view,
                            WebResourceRequest request
                    ) {
                        if (request == null || request.getUrl() == null) {
                            return false;
                        }
                        Uri uri = request.getUrl();
                        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(java.util.Locale.US);
                        if (!request.isForMainFrame()) {
                            return false;
                        }
                        if ("http".equals(scheme) || "https".equals(scheme)) {
                            if (isWhatsAppHost(uri.getHost())) {
                                return false;
                            }
                            openUrlInBrowser(uri.toString());
                            return true;
                        }
                        if ("blob".equals(scheme) || "data".equals(scheme) || "about".equals(scheme) || "javascript".equals(scheme)) {
                            return false;
                        }
                        if ("tel".equals(scheme) || "mailto".equals(scheme) || "sms".equals(scheme) || "geo".equals(scheme)) {
                            try {
                                startActivity(new Intent(Intent.ACTION_VIEW, uri));
                            } catch (Throwable ignored) {
                            }
                        }
                        return true;
                    }

                    @Override
                    public android.webkit.WebResourceResponse shouldInterceptRequest(
                            WebView view,
                            WebResourceRequest request
                    ) {
                        try {
                            Uri uri = request.getUrl();
                            String scheme = uri.getScheme();
                            if (scheme != null && (scheme.equals("http") || scheme.equals("https"))) {
                                InspectorHub.forKey(tabKey).addNative(request.getMethod(), uri.toString(),
                                        request.isForMainFrame() ? "document" : "other", "");
                            }
                        } catch (Throwable ignored) {
                        }
                        return null;
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

        target.setDownloadListener(
                new android.webkit.DownloadListener() {
                    @Override
                    public void onDownloadStart(
                            final String url,
                            final String userAgent,
                            final String contentDisposition,
                            final String mimetype,
                            final long contentLength
                    ) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!isUiAlive()) {
                                    return;
                                }
                                String cookie = null;
                                try {
                                    cookie = getProfileCookieManager().getCookie(url);
                                } catch (Throwable ignored) {
                                }
                                RenaDownloadHelper.handle(MainActivity.this, target, tabKey, tabId, url,
                                        userAgent, contentDisposition, mimetype, contentLength, cookie, target.getUrl());
                            }
                        });
                    }
                }
        );
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

        startWebPermissionFlow(request);
    }

    private void startWebPermissionFlow(
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
    private TabState activeTabState() {
        if (tabs.isEmpty()) {
            return null;
        }
        if (activeTabIndex < 0 || activeTabIndex >= tabs.size()) {
            return tabs.get(0);
        }
        return tabs.get(activeTabIndex);
    }

    private void attachBrowser() {
        browser = new BrowserController(this, root);
        root.addView(browser.container(), Math.max(0, root.indexOfChild(progress) + 1), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        View scrimView = browser.scrimView();
        root.addView(scrimView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        View drawerView = browser.drawerView();
        FrameLayout.LayoutParams drawerParams = new FrameLayout.LayoutParams(
                browser.drawerWidth(), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.END);
        root.addView(drawerView, drawerParams);
        drawerView.setTranslationX(browser.drawerWidth());
        if (menuButton != null) {
            menuButton.bringToFront();
        }
    }

    private void toggleNavigation() {
        if (browserMode && browser != null) {
            browser.toggleDrawer();
        } else {
            setDrawerOpen(!drawerOpen);
        }
    }

    private void openNavigation() {
        if (browserMode && browser != null) {
            if (!browser.isDrawerOpen()) {
                browser.setDrawerOpen(true);
            }
        } else {
            setDrawerOpen(true);
        }
    }

    boolean isBrowserMode() {
        return browserMode;
    }

    void enterBrowserMode() {
        if (browser == null || browserMode) {
            return;
        }
        browserMode = true;
        RenaPrefs.setBool(this, RenaPrefs.MODE_BROWSER, true);
        if (drawerOpen || drawerAnimating) {
            setDrawerOpen(false);
        }
        captureTabThumbnail(activeTabState());
        if (webView != null) {
            webView.setVisibility(View.INVISIBLE);
        }
        browser.show();
        if (menuButton != null) {
            menuButton.bringToFront();
        }
    }

    void exitBrowserMode(boolean openDrawer) {
        if (browser == null) {
            return;
        }
        if (browserMode) {
            browserMode = false;
            RenaPrefs.setBool(this, RenaPrefs.MODE_BROWSER, false);
            browser.hide();
            if (webView != null) {
                webView.setVisibility(View.VISIBLE);
                try {
                    webView.onResume();
                } catch (Throwable ignored) {
                }
            }
        }
        if (openDrawer) {
            root.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isUiAlive() && !browserMode) {
                        setDrawerOpen(true);
                    }
                }
            }, 120L);
        }
    }

    void browserSwitchToW4b() {
        exitBrowserMode(false);
    }

    void browserSwitchToW4b(boolean openDrawer) {
        exitBrowserMode(openDrawer);
    }

    void openUrlInBrowser(final String url) {
        if (!isUiAlive() || browser == null || TextUtils.isEmpty(url)) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isUiAlive()) {
                    return;
                }
                enterBrowserMode();
                browser.openUrl(url, true);
            }
        });
    }

    void confirmOpenBrowser() {
        new RenaDialog.Builder(this)
                .setTitle(R.string.switch_to_browser_title)
                .setMessage(R.string.switch_to_browser_message)
                .setNegativeButton(R.string.cancel_text, null)
                .setPositiveButton(R.string.yes_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setDrawerOpen(false);
                        root.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (!isUiAlive()) {
                                    return;
                                }
                                enterBrowserMode();
                                root.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (isUiAlive() && browserMode && browser != null) {
                                            browser.setDrawerOpen(true);
                                        }
                                    }
                                }, 120L);
                            }
                        }, 260L);
                    }
                })
                .show();
    }

    boolean browserFileChooser(ValueCallback<Uri[]> callback, boolean multiple) {
        if (pendingFileCallback != null) {
            pendingFileCallback.onReceiveValue(null);
        }
        pendingFileCallback = callback;
        openSystemFilePicker(multiple);
        return true;
    }

    void browserPermission(PermissionRequest request) {
        startWebPermissionFlow(request);
    }

    void browserSetFullscreen(boolean fullscreen) {
        try {
            if (fullscreen) {
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
                setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            } else {
                setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                applyImmersiveFullscreen();
            }
        } catch (Throwable ignored) {
        }
    }

    void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    void applyOfflineMode() {
        for (TabState tab : tabs) {
            if (tab.view != null) {
                WebEngine.applyCache(this, tab.view);
            }
        }
    }

    void refreshW4bDevice() {
        for (TabState tab : tabs) {
            if (tab.view != null) {
                WebEngine.applyDevice(this, tab.view, true);
                WebEngine.refresh(this, tab.view, false);
            }
        }
        if (webView != null) {
            try {
                webView.reload();
            } catch (Throwable ignored) {
            }
        }
    }

    void setW4bTranslate(boolean on) {
        RenaPrefs.setBool(this, RenaPrefs.TRANSLATE_W4B, on);
        for (TabState tab : tabs) {
            if (tab.view != null) {
                WebEngine.refresh(this, tab.view, false);
            }
        }
        WebEngine.setTranslate(this, webView, on);
        if (!drawerOpen) {
            drawerDirty = true;
        }
    }

    private void handleViewIntent(Intent intent) {
        if (intent == null || browser == null) {
            return;
        }
        if (intent.getBooleanExtra("check_updates", false)) {
            intent.removeExtra("check_updates");
            root.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isUiAlive()) {
                        checkForUpdates();
                    }
                }
            }, 400L);
        }
        String action = intent.getAction();
        String target = null;
        if (Intent.ACTION_VIEW.equals(action) && intent.getData() != null) {
            target = intent.getData().toString();
        } else if (Intent.ACTION_SEND.equals(action) && "text/plain".equals(intent.getType())) {
            CharSequence shared = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
            target = shared == null ? null : shared.toString();
        } else if (Intent.ACTION_WEB_SEARCH.equals(action)) {
            target = intent.getStringExtra(android.app.SearchManager.QUERY);
        }
        if (TextUtils.isEmpty(target)) {
            return;
        }
        intent.setAction(Intent.ACTION_MAIN);
        intent.setData(null);
        openUrlInBrowser(target);
    }

    private void scheduleTabThumbnail() {
        thumbHandler.removeCallbacks(tabThumbRunnable);
        thumbHandler.postDelayed(tabThumbRunnable, 2200L);
    }

    private final Runnable tabThumbRunnable = new Runnable() {
        @Override
        public void run() {
            captureTabThumbnail(activeTabState());
        }
    };

    private java.io.File tabThumbFile(int id) {
        return new java.io.File(new java.io.File(getFilesDir(), "thumbs"), "w4b_" + id + ".jpg.enc");
    }

    private void captureTabThumbnail(final TabState tab) {
        if (tab == null || tab.view == null || browserMode || tab.view.getWidth() <= 0 || tab.view.getHeight() <= 0) {
            return;
        }
        try {
            int width = 260;
            int height = Math.min(360, Math.max(120, (int) (width * (float) tab.view.getHeight() / tab.view.getWidth())));
            final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(bitmap);
            canvas.scale(width / (float) tab.view.getWidth(), height / (float) tab.view.getHeight());
            tab.view.draw(canvas);
            tab.thumb = bitmap;
            tab.thumbLoaded = true;
            final int id = tab.id;
            final Context appContext = getApplicationContext();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        java.io.File file = tabThumbFile(id);
                        java.io.File parent = file.getParentFile();
                        if (parent != null && !parent.exists()) {
                            parent.mkdirs();
                        }
                        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 62, out);
                        RenaCrypto.writeFile(appContext, file, out.toByteArray());
                    } catch (Throwable ignored) {
                    }
                }
            }).start();
        } catch (Throwable ignored) {
        }
    }

    private Bitmap tabThumbnail(TabState tab) {
        if (tab.thumb != null) {
            return tab.thumb;
        }
        if (!tab.thumbLoaded) {
            tab.thumbLoaded = true;
            try {
                byte[] data = RenaCrypto.readFile(this, tabThumbFile(tab.id));
                if (data != null) {
                    tab.thumb = android.graphics.BitmapFactory.decodeByteArray(data, 0, data.length);
                }
            } catch (Throwable ignored) {
            }
        }
        return tab.thumb;
    }

    private void deleteTabThumbnail(int id) {
        try {
            tabThumbFile(id).delete();
        } catch (Throwable ignored) {
        }
    }
    private LinearLayout makePrimaryRow(String glyph, String title, String subtitle, final Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(8), dp(14), dp(8));
        GradientDrawable fill = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.rgb(0, 168, 132), Color.rgb(0, 128, 106)});
        fill.setCornerRadius(dp(18));
        row.setBackground(Ui.ripple(this, fill, 18));
        TextView chip = Ui.text(this, glyph, 20f, Color.WHITE, android.graphics.Typeface.NORMAL);
        chip.setGravity(Gravity.CENTER);
        chip.setBackground(Ui.round(Color.argb(46, 255, 255, 255), dp(14)));
        row.addView(chip, new LinearLayout.LayoutParams(dp(44), dp(44)));
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.addView(Ui.text(this, title, 16f, Color.WHITE, android.graphics.Typeface.BOLD), new LinearLayout.LayoutParams(-2, -2));
        TextView sub = Ui.text(this, subtitle, 12f, Color.argb(215, 255, 255, 255), android.graphics.Typeface.NORMAL);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-2, -2);
        subLp.topMargin = dp(3);
        column.addView(sub, subLp);
        LinearLayout.LayoutParams columnLp = new LinearLayout.LayoutParams(0, -2, 1f);
        columnLp.leftMargin = dp(12);
        row.addView(column, columnLp);
        row.addView(Ui.text(this, "\u203A", 24f, Color.WHITE, android.graphics.Typeface.NORMAL), new LinearLayout.LayoutParams(-2, -2));
        bindSafeClick(row, action);
        return row;
    }

    private LinearLayout makeIconRow(String glyph, String title, String subtitle, final Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(7), dp(10), dp(7));
        row.setBackground(Ui.ripple(this, round(RenaTheme.fg(24), dp(16)), 16));
        TextView chip = Ui.text(this, glyph, 18f, RenaTheme.accentText(), android.graphics.Typeface.NORMAL);
        chip.setGravity(Gravity.CENTER);
        chip.setBackground(Ui.round(RenaTheme.accentSoft(), dp(12)));
        row.addView(chip, new LinearLayout.LayoutParams(dp(38), dp(38)));
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        TextView titleView = Ui.text(this, title, 15f, RenaTheme.text(), android.graphics.Typeface.BOLD);
        titleView.setSingleLine(true);
        column.addView(titleView, new LinearLayout.LayoutParams(-2, -2));
        if (!TextUtils.isEmpty(subtitle)) {
            TextView sub = Ui.text(this, subtitle, 11.5f, RenaTheme.muted(), android.graphics.Typeface.NORMAL);
            sub.setSingleLine(true);
            sub.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-2, -2);
            subLp.topMargin = dp(2);
            column.addView(sub, subLp);
        }
        LinearLayout.LayoutParams columnLp = new LinearLayout.LayoutParams(0, -2, 1f);
        columnLp.leftMargin = dp(12);
        row.addView(column, columnLp);
        row.addView(Ui.text(this, "\u203A", 22f, RenaTheme.faint(), android.graphics.Typeface.NORMAL), new LinearLayout.LayoutParams(-2, -2));
        bindSafeClick(row, action);
        return row;
    }

    private void setW4bFeature(String key, boolean enabled) {
        RenaPrefs.setBool(this, key, enabled);
        for (TabState tab : tabs) {
            if (tab.view != null) {
                WebEngine.refresh(this, tab.view, false);
            }
        }
        if (webView == null) {
            return;
        }
        if (enabled) {
            try {
                String cfg = "(function(){window.__RENA_CFG=window.__RENA_CFG||{};window.__RENA_CFG.antiDelete="
                        + RenaPrefs.bool(this, RenaPrefs.ANTI_DELETE, true) + ";window.__RENA_CFG.statusSaver="
                        + RenaPrefs.bool(this, RenaPrefs.STATUS_SAVER, false) + ";window.__RENA_CFG.statusAuto="
                        + RenaPrefs.bool(this, RenaPrefs.STATUS_AUTO, false) + ";})();";
                webView.evaluateJavascript(cfg + PageScripts.load(this, "antidelete") + "\n" + PageScripts.load(this, "status"), null);
            } catch (Throwable ignored) {
            }
        } else {
            try {
                webView.reload();
            } catch (Throwable ignored) {
            }
        }
    }

    private InspectorPanel w4bInspector;

    private void openW4bInspector() {
        if (webView == null || root == null) {
            return;
        }
        setDrawerOpen(false);
        final TabState owner = activeTabState();
        final String key = sourceKey(owner == null ? 0 : owner.id);
        final WebView target = webView;
        w4bInspector = new InspectorPanel(this, new InspectorPanel.Target() {
            @Override
            public WebView webView() {
                return target;
            }

            @Override
            public String hubKey() {
                return key;
            }

            @Override
            public CookieManager cookieManager() {
                return getProfileCookieManager();
            }

            @Override
            public String userAgent() {
                return target.getSettings().getUserAgentString();
            }
        }, new Runnable() {
            @Override
            public void run() {
                closeW4bInspector();
            }
        });
        root.addView(w4bInspector, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void closeW4bInspector() {
        if (w4bInspector != null) {
            w4bInspector.stopRefresh();
            root.removeView(w4bInspector);
            w4bInspector = null;
        }
    }

    private boolean drawerDirty;

    private void rebuildDrawerContent() {
        if (root == null) {
            return;
        }
        if (drawer != null) {
            root.removeView(drawer);
        }
        if (drawerScrim != null) {
            root.removeView(drawerScrim);
        }
        buildDrawer();
        if (menuButton != null) {
            menuButton.bringToFront();
        }
        if (Build.VERSION.SDK_INT >= 23) {
            root.requestApplyInsets();
        }
    }

    private void setDrawerOpen(final boolean open) {
        if (drawer == null || drawerScrim == null || menuButton == null) {
            return;
        }

        final float targetProgress = open ? 1f : 0f;
        final float startProgress = clamp01(drawerProgress);
        if (Math.abs(targetProgress - startProgress) < 0.001f) {
            drawerOpen = open;
            drawerProgress = targetProgress;
            drawerAnimating = false;
            if (!open) {
                drawer.setVisibility(View.GONE);
                drawerScrim.setVisibility(View.GONE);
                drawerScrim.setAlpha(0f);
                drawer.setLayerType(View.LAYER_TYPE_NONE, null);
                drawerScrim.setLayerType(View.LAYER_TYPE_NONE, null);
                if (footerView != null) {
                    footerView.pauseMarquee();
                }
            } else {
                drawer.setVisibility(View.VISIBLE);
                drawerScrim.setVisibility(View.VISIBLE);
                drawerScrim.setAlpha(0.56f);
                if (footerView != null) {
                    footerView.resumeMarquee();
                }
            }
            drawer.setTranslationX(-drawerWidthPx * (1f - targetProgress));
            menuButton.setRotation(90f * targetProgress);
            updateMenuVisibility();
            return;
        }

        drawerAnimationGeneration++;
        final int generation = drawerAnimationGeneration;
        if (drawerAnimator != null) {
            drawerAnimator.cancel();
            drawerAnimator = null;
        }
        drawer.animate().cancel();
        drawerScrim.animate().cancel();
        menuButton.animate().cancel();

        drawerOpen = open;
        drawerAnimating = true;
        if (open) {
            drawer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            drawerScrim.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            drawer.setVisibility(View.VISIBLE);
            drawerScrim.setVisibility(View.VISIBLE);
            if (footerView != null) {
                footerView.resumeMarquee();
            }
        } else if (footerView != null) {
            footerView.pauseMarquee();
        }

        drawer.bringToFront();
        menuButton.bringToFront();
        updateMenuVisibility();

        if (reduceAnimations) {
            drawerProgress = targetProgress;
            drawer.setTranslationX(-drawerWidthPx * (1f - targetProgress));
            drawerScrim.setAlpha(0.56f * targetProgress);
            drawerAnimating = false;
            if (!open) {
                drawer.setVisibility(View.GONE);
                drawerScrim.setVisibility(View.GONE);
                drawer.setLayerType(View.LAYER_TYPE_NONE, null);
                drawerScrim.setLayerType(View.LAYER_TYPE_NONE, null);
            }
            updateMenuVisibility();
            return;
        }

        drawerAnimator = ValueAnimator.ofFloat(startProgress, targetProgress);
        long baseDuration = open ? 235L : 225L;
        long duration = Math.max(85L,
                (long) (animDuration(baseDuration) * Math.abs(targetProgress - startProgress)));
        drawerAnimator.setDuration(duration);
        drawerAnimator.setInterpolator(new android.view.animation.PathInterpolator(
                0.20f, 0f, 0f, 1f));
        drawerAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (generation != drawerAnimationGeneration) {
                    return;
                }
                float progressValue = ((Float) animation.getAnimatedValue()).floatValue();
                drawerProgress = clamp01(progressValue);
                drawer.setTranslationX(-drawerWidthPx * (1f - drawerProgress));
                drawerScrim.setAlpha(0.56f * drawerProgress);
                menuButton.setRotation(90f * drawerProgress);
                float scale = 1f - (drawerProgress * 0.08f);
                menuButton.setScaleX(scale);
                menuButton.setScaleY(scale);
                menuButton.setAlpha(1f);
            }
        });
        drawerAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (generation != drawerAnimationGeneration) {
                    return;
                }
                drawerProgress = targetProgress;
                drawerAnimating = false;
                drawer.setTranslationX(-drawerWidthPx * (1f - targetProgress));
                drawerScrim.setAlpha(0.56f * targetProgress);
                if (!open) {
                    drawer.setVisibility(View.GONE);
                    drawerScrim.setVisibility(View.GONE);
                    drawer.setLayerType(View.LAYER_TYPE_NONE, null);
                    drawerScrim.setLayerType(View.LAYER_TYPE_NONE, null);
                }
                updateMenuVisibility();
                drawerAnimator = null;
            }
        });
        drawerAnimator.start();
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
                RenaTheme.sheet()
        );

        return drawable;
    }
    private void showIntegrityFailureDialog() {
        try {
            new RenaDialog.Builder(this)
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

            return NativeConfig.isNativeAvailable() && Takane.b(this);
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
            RenaWindowHelper.applyMainWindow(this);
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

        long epoch = RenaPrefs.lng(this, "ui_epoch", 0L);
        if (epoch != uiEpochSeen) {
            uiEpochSeen = epoch;
            recreate();
            return;
        }
        if (browser != null) {
            browser.onResume();
        }

        boolean currentInternet = hasInternetConnection();
        if (internetAvailable && !currentInternet && webView != null) {
            showRuntimeOfflineToast();
        }
        internetAvailable = currentInternet;

        applyImmersiveFullscreen();
        cleanupInstalledUpdateArtifact();
        resumeManagedDownloadIfNeeded();

        resumeRetainedTabViews();
        pruneInactiveWebViews();

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

                continueSpecialStartupFlow();
            }

        }

        showPendingAutomaticUpdateDialogIfReady();
        maybeShowUpdateInstalledDialog();

        if (pendingUpdateStorage && RenaPaths.canWrite()) {
            pendingUpdateStorage = false;
            String url = pendingUpdateApkUrl;
            String digest = pendingUpdateDigest;
            String version = pendingUpdateVersion;
            pendingUpdateApkUrl = null;
            pendingUpdateDigest = null;
            pendingUpdateVersion = null;
            if (!TextUtils.isEmpty(url) && !TextUtils.isEmpty(version)) {
                startManagedApkDownload(url, digest, version);
            }
        }

        if (startupFlowFinished && RenaPrefs.autoUpdate(this) && !updateCheckRunning && !updateInstallInProgress &&
                hasInternetConnection()) {
            long last = RenaSettingsStore.getLong(this, PREF_UPDATE_LAST_AUTO_CHECK, 0L);
            long now = System.currentTimeMillis();
            if (last == 0L || now - last >= AUTOMATIC_UPDATE_CHECK_INTERVAL_MS) {
                RenaSettingsStore.putLong(this, PREF_UPDATE_LAST_AUTO_CHECK, now);
                startAutomaticUpdateCheck();
            }
        }

        resumeReadyUpdateInstall();

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

        if (pendingUpdateApkUrl != null) {
            boolean canInstallPackages =
                    Build.VERSION.SDK_INT < 26 ||
                    getPackageManager().canRequestPackageInstalls();

            if (canInstallPackages) {
                String apkUrl = pendingUpdateApkUrl;
                String digest = pendingUpdateDigest;
                String targetVersion = pendingUpdateVersion;

                pendingUpdateApkUrl = null;
                pendingUpdateDigest = null;
                pendingUpdateVersion = null;

                if (!TextUtils.isEmpty(apkUrl) && !TextUtils.isEmpty(digest) && !TextUtils.isEmpty(targetVersion)) {
                    startManagedApkDownload(apkUrl, digest, targetVersion);
                }
            }
        }

        RenaDownloadHelper.resumePending(this);

        if (webView != null) {
            try {
                webView.onResume();
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
        handleViewIntent(intent);
    }

    private void resumeReadyUpdateInstall() {
        if (!RenaSettingsStore.getBoolean(this, PREF_UPDATE_READY, false)) return;
        String path = RenaSettingsStore.getString(this, PREF_UPDATE_PENDING_PATH, "");
        String version = RenaSettingsStore.getString(this, PREF_UPDATE_PENDING_VERSION, "");
        if (TextUtils.isEmpty(path) || TextUtils.isEmpty(version)) return;
        File apk = new File(path);
        if (!apk.isFile()) return;
        if (Build.VERSION.SDK_INT >= 26 && !getPackageManager().canRequestPackageInstalls()) return;
        RenaSettingsStore.putBoolean(this, PREF_UPDATE_READY, false);
        launchApkInstaller(apk);
    }

    private void resumeManagedDownloadIfNeeded() {
        if (updateDownloadId < 0L || updatePollRunnable != null) return;
        String path = RenaSettingsStore.getString(this, PREF_UPDATE_PENDING_PATH, "");
        final String version = RenaSettingsStore.getString(this, PREF_UPDATE_PENDING_VERSION, "");
        final String digest = RenaSettingsStore.getString(this, "update_expected_digest", "");
        if (TextUtils.isEmpty(path) || TextUtils.isEmpty(version) || TextUtils.isEmpty(digest)) return;
        final File apk = new File(path);
        if (!apk.getParentFile().exists() && !apk.getParentFile().mkdirs()) return;
        String resumeId = RenaSettingsStore.getString(this, "update_item_id", "");
        RenaDownloadManager.DownloadItem resumeItem = RenaDownloadManager.find(resumeId);
        if (resumeItem != null && resumeItem.status == RenaDownloadManager.PAUSED) {
            RenaDownloadManager.resume(resumeId);
        }
        updateInstallInProgress = true;
        updatePollRunnable = new Runnable() {
            @Override public void run() { pollUpdateDownload("", digest, version, apk); }
        };
        updateHandler.post(updatePollRunnable);
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

        if (hasRequestedTab && browserMode) {
            exitBrowserMode(false);
        }
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
        captureTabThumbnail(activeTabState());
        if (browser != null) {
            browser.onPause();
        }
        saveActiveTab();
        persistTabs();

        flushAllProfileCookies();

        super.onPause();
    }

    @Override
    protected void onStop() {

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
        thumbHandler.removeCallbacksAndMessages(null);
        if (browser != null) {
            browser.onDestroy();
        }
        stopUpdatePolling();
        if (renaImageTask != null) {
            try {
                renaImageTask.cancel(true);
            } catch (Throwable ignored) {
            }
            renaImageTask = null;
        }
        cancelSlowConnectionWatch();
        unregisterInternetMonitor();

        flushWebSessionState();
        super.onDestroy();
    }

    @Override
    public void onTrimMemory(int level) {
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
            ProfileStore store = getProfileStoreInstance();

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
                getString(R.string.no_internet_connection_text),
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
                getString(R.string.poor_internet_connection_text),
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

        final RenaDialog dialog =
                new RenaDialog.Builder(this)
                        .setTitle(getString(R.string.no_internet_title))
                        .setMessage(getString(R.string.no_internet_message))
                        .setNegativeButton(
                                getString(R.string.exit_text),
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
                                getString(R.string.ignore_text),
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
                        showPendingAutomaticUpdateDialogIfReady();
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
                                getString(R.string.cache_cleared_text),
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    if (webView != null &&
                            webDataCleared) {
                        webView.clearHistory();
                        historyFloorApplied = true;
                        String reloadUrl = NativeConfig.webUrl();
                        webView.loadUrl(reloadUrl);
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

    public void onBackPressed() {
        if (exitingApp) {
            return;
        }

        if (renaPreviewOverlay != null) {
            closeRenaPreview();
            return;
        }

        if (w4bInspector != null) {
            closeW4bInspector();
            return;
        }

        if (browserMode && browser != null) {
            if (!browser.onBack()) {
                confirmExitOrToast();
            }
            return;
        }

        if (drawerOpen || drawerAnimating) {
            setDrawerOpen(false);
            return;
        }

        if (webView != null) {
            final WebView target = webView;
            try {
                target.evaluateJavascript(
                        "(function(){try{return window.__renaBack?window.__renaBack():false;}catch(e){return false;}})()",
                        new ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String value) {
                                if ("true".equals(value)) {
                                    return;
                                }
                                if (target.canGoBack()) {
                                    target.goBack();
                                    return;
                                }
                                confirmExitOrToast();
                            }
                        }
                );
                return;
            } catch (Throwable ignoredScript) {
            }
        }

        confirmExitOrToast();
    }

    private void confirmExitOrToast() {
        long now = SystemClock.elapsedRealtime();
        if (now - lastBackPressAt > 2000L) {
            lastBackPressAt = now;
            try {
                Toast.makeText(this, getString(R.string.exit_toast_text), Toast.LENGTH_SHORT).show();
            } catch (Throwable ignoredToast) {
            }
            return;
        }

        exitingApp = true;

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

        }

        void resumeMarquee() {
            shouldRun = true;
            removeCallbacks(restartMarqueeRunnable);

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
            postOnAnimation(restartMarqueeRunnable);
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
            label.setTextColor(RenaTheme.text());
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
                iconView.setColorFilter(RenaTheme.text());
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
                                    RenaTheme.fg(55),
                                    RenaTheme.fg(220),
                                    RenaTheme.fg(55),
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
                    1f,
                    minScale * 10f
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
            paint.setColor(RenaTheme.text());
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
                                        RenaTheme.fg(175),
                                        RenaTheme.fg(70),
                                        Color.TRANSPARENT
                                },
                                null,
                                Shader.TileMode.CLAMP
                        )
                );

                super.onDraw(canvas);
                getPaint().setShader(previousShader);
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
