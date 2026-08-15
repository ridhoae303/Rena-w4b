package com.rena.w4b;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
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
import android.net.Uri;
import android.os.Build;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Debug;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ScaleGestureDetector;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String EXPECTED_PACKAGE = "com.rena.w4b";

    private static final int REQ_STARTUP_PERMISSIONS = 7101;
    private static final int REQ_MEDIA_PERMISSION = 7102;
    private static final int PICK_FILE = 7103;

    private FrameLayout root;
    private WebView webView;
    private FrameLayout drawer;
    private View drawerScrim;
    private ImageButton menuButton;
    private ProgressBar progress;
    private TextView loadingLabel;

    private boolean drawerOpen = false;
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
    private boolean webStateRestored = false;
    private int touchSlop = 0;

    private ValueCallback<Uri[]> pendingFileCallback;
    private PermissionRequest pendingWebPermission;

    private float baselineScale = 1.0f;
    private boolean baselineScaleCaptured = false;

    private boolean exitBackArmed = false;
    private long exitBackDeadline = 0L;
    private static final long EXIT_BACK_WINDOW_MS = 2200L;

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
        return t;
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        );

        // Hide the status bar at window level before the first layout frame.
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );

        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
        }

        // Edge-to-edge configuration is applied before any visible content.
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
        }

        applyImmersiveFullscreen();

        if (!integrityGate()) {
            Toast.makeText(
                    this,
                    NativeConfig.integrityErrorMessage(),
                    Toast.LENGTH_LONG
            ).show();

            new android.os.Handler().postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            closeAppBySystem();
                        }
                    },
                    650L
            );
            return;
        }

        buildUi();

        touchSlop = android.view.ViewConfiguration
                .get(this)
                .getScaledTouchSlop();

        final Bundle restoreState = state;

        root.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        initializeWebView(restoreState);
                    }
                },
                90L
        );

        root.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
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
    private void startPermissionFlow() {
        if (Build.VERSION.SDK_INT < 23) {
            return;
        }
        showNextPermissionDialog(0);
    }

    private void showNextPermissionDialog(final int index) {
        final String permission = getPermissionForIndex(index);

        if (permission == null) {
            return;
        }

        if (hasPermission(permission)) {
            showNextPermissionDialog(index + 1);
            return;
        }

        final AlertDialog dialog = buildPermissionDialog(permission);

        dialog.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(android.content.DialogInterface d) {
                // Nothing here. The result callback determines whether the next
                // permission dialog should be displayed.
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
                if (Build.VERSION.SDK_INT >= 33 &&
                        Build.VERSION.SDK_INT <= 32) {
                    return Manifest.permission.READ_EXTERNAL_STORAGE;
                }
                if (Build.VERSION.SDK_INT <= 28) {
                    return Manifest.permission.READ_EXTERNAL_STORAGE;
                }
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

    private String[] getRuntimePermissionsInOrder() {
        java.util.ArrayList<String> result = new java.util.ArrayList<String>();

        if (Build.VERSION.SDK_INT >= 23) {
            result.add(Manifest.permission.CAMERA);
            result.add(Manifest.permission.RECORD_AUDIO);

            if (Build.VERSION.SDK_INT >= 33) {
                result.add(Manifest.permission.POST_NOTIFICATIONS);
            } else if (Build.VERSION.SDK_INT <= 28) {
                result.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        return result.toArray(new String[result.size()]);
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
                        openAppSettings();
                        dialog.dismiss();
                        showNextPermissionDialog(
                                permissionIndex(permission) + 1
                        );
                    }
                }
        );

        AlertDialog dialog = builder.create();

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
                        Gravity.START | Gravity.BOTTOM
                );
        menuLp.leftMargin = dp(14);
        menuLp.bottomMargin = dp(20);

        root.addView(menuButton, menuLp);

        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setDrawerOpen(!drawerOpen);
            }
        });

        buildDrawer();
        setContentView(root);

        if (Build.VERSION.SDK_INT >= 21) {
            root.setFitsSystemWindows(false);
        }

        applyImmersiveFullscreen();
    }

    private void initializeWebView(Bundle restoreState) {
        if (webView != null) {
            return;
        }

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(17, 27, 33));

        root.addView(
                webView,
                0,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        configureGpuRendering();
        configureWebView();

        if (restoreState != null) {
            try {
                android.webkit.WebBackForwardList restored =
                        webView.restoreState(restoreState);

                webStateRestored =
                        restored != null &&
                        restored.getSize() > 0;
            } catch (Throwable ignoredRestore) {
                webStateRestored = false;
            }
        }

        if (!webStateRestored) {
            webView.loadUrl(NativeConfig.webUrl());
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

        // WebView/Chromium owns the actual EGL context. Hardware layer
        // rendering lets Android use its best available GPU path.
        webView.setLayerType(
                View.LAYER_TYPE_HARDWARE,
                null
        );
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
        drawer.setTranslationX(-drawerWidthPx);
        drawer.setClickable(true);
        drawer.setFocusable(true);
        if (Build.VERSION.SDK_INT >= 21) {
            drawer.setElevation(2f);
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
                null,
                android.graphics.Typeface.BOLD
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
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(dp(8), 0, 0, 0);

        content.addView(title, exact(dp(34)));

        TextView developer = text(
                NativeConfig.developerName(),
                27,
                Color.WHITE
        );
        developer.setTypeface(null, android.graphics.Typeface.BOLD);
        developer.setPadding(dp(8), 0, 0, dp(12));

        content.addView(
                developer,
                exact(dp(56))
        );

        View divider = new View(this);
        divider.setBackgroundColor(
                Color.argb(40, 255, 255, 255)
        );
        content.addView(
                divider,
                exact(dp(1))
        );

        /*
         * Zoom row.
         */
        LinearLayout zoomRow = new LinearLayout(this);
        zoomRow.setOrientation(LinearLayout.HORIZONTAL);
        zoomRow.setGravity(Gravity.CENTER_VERTICAL);
        zoomRow.setPadding(
                dp(10), 0, dp(8), 0
        );
        zoomRow.setBackground(
                round(Color.argb(24, 255, 255, 255), dp(16))
        );

        TextView zoomLabel = text(
                NativeConfig.zoomLabel(),
                15,
                Color.WHITE
        );

        zoomRow.addView(
                zoomLabel,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        1f
                )
        );

        final android.widget.CheckBox zoomCheck =
                new android.widget.CheckBox(this);
        zoomCheck.setText("");
        zoomCheck.setChecked(zoomEnabled);

        zoomRow.addView(
                zoomCheck,
                new LinearLayout.LayoutParams(
                        dp(52),
                        dp(52)
                )
        );

        zoomCheck.setOnCheckedChangeListener(
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

        bindSafeClick(
                zoomRow,
                new Runnable() {
                    @Override
                    public void run() {
                        zoomCheck.setChecked(
                                !zoomCheck.isChecked()
                        );
                    }
                }
        );

        LinearLayout.LayoutParams zoomLp = exact(dp(58));
        zoomLp.topMargin = dp(8);
        content.addView(zoomRow, zoomLp);

        TextView refreshRow = text(
                NativeConfig.refreshText(),
                15,
                Color.WHITE
        );
        refreshRow.setGravity(Gravity.CENTER);
        refreshRow.setPadding(dp(10), 0, dp(10), 0);
        refreshRow.setBackground(
                round(Color.argb(24, 255, 255, 255), dp(16))
        );
        refreshRow.setClickable(true);
        refreshRow.setFocusable(true);
        refreshRow.setContentDescription(
                NativeConfig.refreshContentDescription()
        );
        bindSafeClick(
                refreshRow,
                new Runnable() {
                    @Override
                    public void run() {
                        refreshWhatsAppPage();
                    }
                }
        );

        LinearLayout.LayoutParams refreshLp = exact(dp(50));
        refreshLp.topMargin = dp(8);
        content.addView(refreshRow, refreshLp);

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
                        "tg"
                ),
                rowLp()
        );

        content.addView(
                makeLinkRow(
                        NativeConfig.menuCommunity(),
                        NativeConfig.communityUrl(),
                        "wa"
                ),
                rowLp()
        );

        content.addView(
                makeDonateRow(),
                donateLp()
        );

        /*
         * Flexible footer: ScrollView handles small landscape screens while
         * weight=1 keeps the footer near the bottom on large displays.
         */
        final MarqueeTextView footer =
                new MarqueeTextView(this);

        footer.setText(
                NativeConfig.footerText()
        );
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

    private int getStatusSafeTopPadding() {
        if (Build.VERSION.SDK_INT < 23) {
            return dp(28);
        }

        int inset = 0;

        try {
            android.view.WindowInsets wi = getWindow().getDecorView().getRootWindowInsets();

            if (wi != null) {
                if (Build.VERSION.SDK_INT >= 30) {
                    android.graphics.Insets insets =
                            wi.getInsets(android.view.WindowInsets.Type.systemBars());
                    inset = insets.top;
                } else {
                    inset = wi.getSystemWindowInsetTop();
                }
            }
        } catch (Throwable ignored) {
        }

        return Math.max(dp(18), inset + dp(8));
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

    private View makeDonateRow() {
        final ShimmerDonateView donate =
                new ShimmerDonateView(this);

        donate.setText(
                NativeConfig.donateText()
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
                        setDrawerOpen(false);
                    }
                }
        );

        donate.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        if (donate.getWindowToken() != null) {
                            donate.playShimmer();
                        }
                        donate.postDelayed(this, 4200L);
                    }
                },
                900L
        );

        return donate;
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
                        setDrawerOpen(false);
                    }
                }
        );

        return row;
    }

    private Drawable loadIconDrawable(
            String resourceName
    ) {
        InputStream input = null;

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

            input =
                    getResources().openRawResource(
                            resId
                    );

            BitmapFactory.Options options =
                    new BitmapFactory.Options();

            options.inScaled = false;
            options.inPreferredConfig =
                    Bitmap.Config.ARGB_8888;

            Bitmap bitmap =
                    BitmapFactory.decodeStream(
                            input,
                            null,
                            options
                    );

            if (bitmap == null) {
                return null;
            }

            bitmap.setDensity(
                    0
            );

            BitmapDrawableWrapper drawable =
                    new BitmapDrawableWrapper(
                            getResources(),
                            bitmap
                    );

            drawable.setAntiAlias(true);
            drawable.setFilterBitmap(true);

            return drawable;

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
        if (renaPreviewOverlay != null) {
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
                        Bitmap.Config.ARGB_8888;

                Bitmap bitmap =
                        BitmapFactory.decodeStream(
                                input,
                                null,
                                options
                        );

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
        zoomEnabled = enabled;

        if (webView == null) {
            return;
        }

        WebSettings settings = webView.getSettings();

        settings.setSupportZoom(enabled);
        settings.setBuiltInZoomControls(enabled);
        settings.setDisplayZoomControls(false);

        if (enabled) {
            webView.requestFocus();

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

        /*
         * Also restore the normal zoom-independent reading scale.
         * This does not reload WhatsApp Web and therefore does not interrupt
         * the login session.
         */
        webView.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            float current = webView.getScale();

                            if (current >
                                    baselineScale + 0.02f) {
                                for (int i = 0; i < 10; i++) {
                                    if (!webView.zoomOut()) {
                                        break;
                                    }

                                    current = webView.getScale();

                                    if (current <=
                                            baselineScale + 0.02f) {
                                        break;
                                    }
                                }
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                },
                70
        );
    }

    private void refreshWhatsAppPage() {
        if (webView == null) {
            return;
        }

        try {
            CookieManager
                    .getInstance()
                    .flush();

            webView.reload();
        } catch (Throwable ignored) {
        }
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
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        settings.setUserAgentString(
                NativeConfig.desktopUserAgent()
        );

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);

        if (Build.VERSION.SDK_INT >= 21) {
            cookies.setAcceptThirdPartyCookies(
                    webView,
                    true
            );
        }

        webView.setLayerType(
                View.LAYER_TYPE_HARDWARE,
                null
        );

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
                        progress.setVisibility(
                                View.VISIBLE
                        );
                        progress.setProgress(12);

                        setLoadingOverlayVisible(true);
                    }

                    @Override
                    public void onPageFinished(
                            WebView view,
                            String url
                    ) {
                        progress.setProgress(100);
                        progress.setVisibility(
                                View.GONE
                        );

                        captureBaselineIfNeeded();

                        try {
                            CookieManager
                                    .getInstance()
                                    .flush();
                        } catch (Throwable ignored) {
                        }

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
                    public void onScaleChanged(
                            WebView view,
                            float oldScale,
                            float newScale
                    ) {
                        /*
                         * The first stable page scale becomes our "zoom off"
                         * baseline. User zooming is only permitted while the
                         * checkbox is enabled.
                         */
                        if (!baselineScaleCaptured &&
                                newScale > 0f) {
                            baselineScale = newScale;
                            baselineScaleCaptured = true;
                        }

                        if (!zoomEnabled &&
                                newScale >
                                baselineScale + 0.05f) {
                            resetZoomToBaseline();
                        }
                    }

                    @Override
                    public void onReceivedError(
                            WebView view,
                            WebResourceRequest request,
                            WebResourceError error
                    ) {
                        if (Build.VERSION.SDK_INT >= 23 &&
                                request.isForMainFrame()) {
                            progress.setVisibility(
                                    View.GONE
                            );
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
                        openExternal(url);
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

        boolean needCamera = false;
        boolean needMicrophone = false;

        String[] resources =
                request.getResources();

        for (String resource : resources) {
            if (PermissionRequest
                    .RESOURCE_VIDEO_CAPTURE
                    .equals(resource)) {
                needCamera = true;
            }

            if (PermissionRequest
                    .RESOURCE_AUDIO_CAPTURE
                    .equals(resource)) {
                needMicrophone = true;
            }
        }

        ArrayList<String> missing =
                new ArrayList<String>();

        if (needCamera &&
                !hasPermission(Manifest.permission.CAMERA)) {
            missing.add(
                    Manifest.permission.CAMERA
            );
        }

        if (needMicrophone &&
                !hasPermission(
                        Manifest.permission.RECORD_AUDIO
                )) {
            missing.add(
                    Manifest.permission.RECORD_AUDIO
            );
        }

        if (missing.isEmpty()) {
            grantAllowedWebResources(request);
        } else {
            pendingWebPermission = request;

            requestPermissions(
                    missing.toArray(
                            new String[missing.size()]
                    ),
                    REQ_MEDIA_PERMISSION
            );
        }
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

    private void setDrawerOpen(boolean open) {
        if (drawerOpen == open) {
            return;
        }

        drawerOpen = open;

        if (open) {
            drawerScrim.setVisibility(
                    View.VISIBLE
            );

            drawerScrim.setAlpha(0f);

            drawerScrim.animate()
                    .alpha(1f)
                    .setDuration(180)
                    .start();

            drawer.animate()
                    .translationX(0f)
                    .setDuration(240)
                    .setInterpolator(
                            new android.view.animation
                                    .DecelerateInterpolator()
                    )
                    .start();

            menuButton.animate()
                    .rotation(90f)
                    .translationX(-dp(20))
                    .alpha(0f)
                    .setDuration(220L)
                    .start();

        } else {
            drawer.animate()
                    .translationX(-drawerWidthPx)
                    .setDuration(220)
                    .setInterpolator(
                            new android.view.animation
                                    .DecelerateInterpolator()
                    )
                    .start();

            drawerScrim.animate()
                    .alpha(0f)
                    .setDuration(160)
                    .withEndAction(
                            new Runnable() {
                                @Override
                                public void run() {
                                    if (!drawerOpen) {
                                        drawerScrim
                                                .setVisibility(
                                                        View.GONE
                                                );
                                    }
                                }
                            }
                    )
                    .start();

            menuButton.setVisibility(View.VISIBLE);
            menuButton.setTranslationX(-dp(20));
            menuButton.setAlpha(0f);
            menuButton.animate()
                    .rotation(0f)
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(220L)
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

    private boolean integrityGate() {
        try {
            if (!EXPECTED_PACKAGE.equals(
                    getPackageName()
            )) {
                return false;
            }

            android.content.pm.ApplicationInfo appInfo =
                    getPackageManager()
                            .getApplicationInfo(
                                    getPackageName(),
                                    0
                            );

            if (appInfo == null) {
                return false;
            }

            if (!"android.app.Application"
                    .equals(appInfo.className)) {
                return false;
            }

            String actual =
                    getSigningCertificateSha256();

            String expected =
                    NativeConfig
                            .expectedSignatureSha256();

            if (TextUtils.isEmpty(actual) ||
                    TextUtils.isEmpty(expected)) {
                return false;
            }

            if (!expected.equalsIgnoreCase(actual)) {
                return false;
            }

            if (Debug.isDebuggerConnected() ||
                    Debug.waitingForDebugger()) {
                return false;
            }

            return tracerPidIsZero();

        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean tracerPidIsZero() {
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(
                    new FileReader(
                            "/proc/self/status"
                    )
            );

            String line;

            while ((line =
                    reader.readLine()) != null) {

                if (line.startsWith(
                        "TracerPid:"
                )) {
                    String value =
                            line.substring(10)
                                    .trim();

                    return "0".equals(value);
                }
            }
        } catch (Throwable ignored) {
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Throwable ignored) {
                }
            }
        }

        /*
         * No observable value is treated as neutral instead of inventing
         * a positive tamper finding.
         */
        return true;
    }

    @SuppressWarnings("deprecation")
    private String getSigningCertificateSha256()
            throws Exception {

        Signature[] signatures;

        if (Build.VERSION.SDK_INT >= 28) {
            PackageInfo info =
                    getPackageManager()
                            .getPackageInfo(
                                    getPackageName(),
                                    PackageManager
                                            .GET_SIGNING_CERTIFICATES
                            );

            if (info.signingInfo == null) {
                return "";
            }

            if (info.signingInfo
                    .hasMultipleSigners()) {

                signatures =
                        info.signingInfo
                                .getApkContentsSigners();

            } else {

                signatures =
                        info.signingInfo
                                .getSigningCertificateHistory();
            }

        } else {
            PackageInfo info =
                    getPackageManager()
                            .getPackageInfo(
                                    getPackageName(),
                                    PackageManager
                                            .GET_SIGNATURES
                            );

            signatures = info.signatures;
        }

        if (signatures == null ||
                signatures.length == 0) {
            return "";
        }

        MessageDigest digest =
                MessageDigest.getInstance(
                        "SHA-256"
                );

        byte[] bytes =
                digest.digest(
                        signatures[0]
                                .toByteArray()
                );

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

    private void closeAppBySystem() {
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
            android.view.Window window = getWindow();

            window.setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
            );

            if (Build.VERSION.SDK_INT >= 21) {
                window.setStatusBarColor(Color.TRANSPARENT);
                window.setNavigationBarColor(Color.TRANSPARENT);
            }

            final View decor = window.getDecorView();

            int uiFlags =
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;

            decor.setSystemUiVisibility(uiFlags);

            if (Build.VERSION.SDK_INT >= 30) {
                window.setDecorFitsSystemWindows(false);

                android.view.WindowInsetsController controller =
                        window.getInsetsController();

                if (controller != null) {
                    controller.hide(
                            android.view.WindowInsets.Type.statusBars()
                            | android.view.WindowInsets.Type.navigationBars()
                            | android.view.WindowInsets.Type.captionBar()
                    );

                    controller.setSystemBarsBehavior(
                            android.view.WindowInsetsController
                            .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    );
                }
            }

            /*
             * Do not add status-bar inset padding to the drawer. In true
             * fullscreen mode the content starts at y=0.
             */
        } catch (Throwable ignoredFullscreen) {
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            /*
             * Re-apply after permission dialogs, pickers, notification shade,
             * split-screen transitions, and other system UI interactions.
             */
            applyImmersiveFullscreen();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        applyImmersiveFullscreen();

        if (webView != null) {
            try {
                webView.onResume();
                webView.resumeTimers();
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    protected void onSaveInstanceState(
            Bundle outState
    ) {
        try {
            if (webView != null) {
                webView.saveState(outState);
            }
        } catch (Throwable ignored) {
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        try {
            CookieManager
                    .getInstance()
                    .flush();
        } catch (Throwable ignored) {
        }

        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();

        /*
         * Do not reload, clear cache, clear cookies, or destroy the WebView
         * merely because the Activity is no longer visible.
         */
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
                grantAllowedWebResources(request);
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
        if (renaPreviewOverlay != null) {
            closeRenaPreview();
            return;
        }

        if (drawerOpen) {
            setDrawerOpen(false);
            return;
        }

        long now = System.currentTimeMillis();

        if (exitBackArmed &&
                now <= exitBackDeadline) {
            exitBackArmed = false;
            moveTaskToBack(true);
            return;
        }

        exitBackArmed = true;
        exitBackDeadline =
                now + EXIT_BACK_WINDOW_MS;

        Toast.makeText(
                this,
                NativeConfig.backPressToast(),
                Toast.LENGTH_SHORT
        ).show();
    }

    private static class MarqueeTextView
            extends FrameLayout {

        private final TextView label;
        private android.animation.ValueAnimator animator;

        public MarqueeTextView(Context context) {
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

        public void setText(String value) {
            label.setText(value);
            restartMarqueeSoon();
        }

        public void setTextSize(float value) {
            label.setTextSize(value);
            restartMarqueeSoon();
        }

        public void setTextColor(int value) {
            label.setTextColor(value);
        }

        public void setGravity(int value) {
            label.setGravity(value);
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            restartMarqueeSoon();
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

            restartMarqueeSoon();
        }

        private void restartMarqueeSoon() {
            postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            startMarquee();
                        }
                    },
                    80L
            );
        }

        private void startMarquee() {
            stopMarquee();

            if (getWidth() <= 0) {
                restartMarqueeSoon();
                return;
            }

            /*
             * Force the child to measure its real text width. This is the
             * reason the old marquee occasionally stayed completely still.
             */
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

            int textWidth =
                    label.getMeasuredWidth();

            if (textWidth <= 0) {
                restartMarqueeSoon();
                return;
            }

            final float startX =
                    getWidth();

            final float endX =
                    -textWidth;

            label.setX(startX);

            animator =
                    android.animation.ValueAnimator
                            .ofFloat(
                                    startX,
                                    endX
                            );

            animator.setDuration(
                    Math.max(
                            4800L,
                            (long) (
                                    (startX - endX)
                                    * 18L
                            )
                    )
            );

            animator.setInterpolator(
                    new android.view.animation
                            .LinearInterpolator()
            );

            animator.setRepeatCount(
                    android.animation
                            .ValueAnimator
                            .INFINITE
            );

            animator.addUpdateListener(
                    new android.animation.ValueAnimator
                            .AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(
                                android.animation.ValueAnimator valueAnimator
                        ) {
                            label.setX(
                                    ((Float) valueAnimator
                                            .getAnimatedValue())
                            );
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
        private final Paint paint =
                new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF bounds = new RectF();
        private float shineX = -1f;
        private android.animation.ValueAnimator animator;

        public ShimmerDonateView(Context context) {
            super(context);

            setWillNotDraw(false);
            setClipChildren(true);
            setBackground(createBackground(context));

            label = new TextView(context);
            label.setTextSize(15f);
            label.setTextColor(Color.WHITE);
            label.setGravity(Gravity.CENTER);
            label.setTypeface(null, android.graphics.Typeface.BOLD);
            label.setSingleLine(true);
            label.setClickable(false);

            addView(
                    label,
                    new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    )
            );
        }

        private Drawable createBackground(Context context) {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.rgb(0, 168, 132));
            bg.setCornerRadius(
                    18f * context.getResources()
                            .getDisplayMetrics().density
            );
            return bg;
        }

        public void setText(String value) {
            label.setText(value);
        }

        public void playShimmer() {
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

            final float startX = -getWidth() * 0.35f;
            final float endX = getWidth() * 1.35f;

            animator =
                    android.animation.ValueAnimator.ofFloat(
                            startX,
                            endX
                    );
            animator.setDuration(900L);
            animator.setInterpolator(
                    new android.view.animation.LinearInterpolator()
            );

            animator.addUpdateListener(
                    new android.animation.ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(
                                android.animation.ValueAnimator valueAnimator
                        ) {
                            shineX =
                                    ((Float) valueAnimator
                                            .getAnimatedValue());
                            invalidate();
                        }
                    }
            );

            animator.addListener(
                    new android.animation.AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(
                                android.animation.Animator animation
                        ) {
                            shineX = -1f;
                            invalidate();
                        }

                        @Override
                        public void onAnimationCancel(
                                android.animation.Animator animation
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
                    getResources().getDisplayMetrics().density;
            float radius = 18f * density;

            bounds.set(
                    0f,
                    0f,
                    getWidth(),
                    getHeight()
            );

            Path clip = new Path();
            clip.addRoundRect(
                    bounds,
                    radius,
                    radius,
                    Path.Direction.CW
            );

            canvas.save();
            canvas.clipPath(clip);

            float band = Math.max(
                    28f * density,
                    getWidth() * 0.12f
            );

            LinearGradient gradient =
                    new LinearGradient(
                            shineX - band,
                            0f,
                            shineX + band,
                            0f,
                            new int[] {
                                    Color.TRANSPARENT,
                                    Color.argb(35, 255, 255, 255),
                                    Color.argb(210, 255, 255, 255),
                                    Color.argb(35, 255, 255, 255),
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

        public ZoomImageView(Context context) {
            super(context);
            setScaleType(ImageView.ScaleType.MATRIX);

            localTouchSlop =
                    android.view.ViewConfiguration
                            .get(context)
                            .getScaledTouchSlop();

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
            fitImage();
        }

        @Override
        public void setImageBitmap(Bitmap bitmap) {
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
            matrix.reset();
            matrix.postScale(minScale, minScale);

            matrix.postTranslate(
                    (viewWidth - drawableWidth * minScale) / 2f,
                    (viewHeight - drawableHeight * minScale) / 2f
            );

            setImageMatrix(matrix);
        }

        private boolean isPointInsideImage(float x, float y) {
            if (getDrawable() == null) {
                return false;
            }

            mappedImageBounds.set(
                    0f,
                    0f,
                    getDrawable().getIntrinsicWidth(),
                    getDrawable().getIntrinsicHeight()
            );

            matrix.mapRect(mappedImageBounds);

            return mappedImageBounds.contains(x, y);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();

            if (action == MotionEvent.ACTION_DOWN) {
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
                dragging = true;
                scaling = false;
                downX = event.getX();
                downY = event.getY();
                lastX = downX;
                lastY = downY;
                scaleDetector.onTouchEvent(event);
                return true;
            }

            if (!gestureStarted) {
                return false;
            }

            scaleDetector.onTouchEvent(event);

            switch (action) {
                case MotionEvent.ACTION_POINTER_DOWN:
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
                    return true;
            }

            return true;
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
                    getDrawable().getIntrinsicWidth() * scaleX;

            float drawableHeight =
                    getDrawable().getIntrinsicHeight() * scaleX;

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
}
