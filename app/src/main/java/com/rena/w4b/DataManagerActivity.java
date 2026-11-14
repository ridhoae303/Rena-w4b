package com.rena.w4b;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Environment;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.concurrent.atomic.AtomicBoolean;
import android.widget.Toast;
import androidx.webkit.Profile;
import androidx.webkit.ProfileStore;
import androidx.webkit.WebViewFeature;
import android.webkit.CookieManager;
import android.webkit.WebStorage;
import android.webkit.ValueCallback;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DataManagerActivity extends Activity {
    private boolean closing;
    private final ExecutorService cleanupExecutor = Executors.newSingleThreadExecutor();
    private TextView cacheSizeView;
    private TextView dataSizeView;
    private final Handler storageHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean storageCalculationActive = new AtomicBoolean(false);
    private boolean storageUpdatesActive;
    private final Runnable storageRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (!storageUpdatesActive || isFinishing() || isDestroyed()) {
                return;
            }
            refreshStorageSizes();
            storageHandler.postDelayed(this, 1800L);
        }
    };

    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(RenaLocale.wrap(base));
    }

    @Override
    protected void onCreate(Bundle state) {
        RenaTheme.apply(this);
        super.onCreate(state);
        RenaDownloadManager.initialize(this);
        configureWindow();
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        configureWindow();
        storageUpdatesActive = true;
        storageHandler.removeCallbacks(storageRefreshRunnable);
        storageHandler.post(storageRefreshRunnable);
    }

    @Override
    protected void onPause() {
        storageUpdatesActive = false;
        storageHandler.removeCallbacks(storageRefreshRunnable);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        storageUpdatesActive = false;
        storageHandler.removeCallbacksAndMessages(null);
        try {
            cleanupExecutor.shutdownNow();
        } catch (Throwable ignored) {
        }
        super.onDestroy();
    }

    private void configureWindow() {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false);
        }
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(RenaTheme.bg());
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
        }
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(RenaTheme.bg());
        root.setPadding(dp(18), dp(12), dp(18), dp(12));

        if (android.os.Build.VERSION.SDK_INT >= 23) {
            root.setOnApplyWindowInsetsListener(
                    new View.OnApplyWindowInsetsListener() {
                        @Override
                        public android.view.WindowInsets onApplyWindowInsets(
                                View v,
                                android.view.WindowInsets insets
                        ) {
                            v.setPadding(
                                    dp(18),
                                    dp(12) + insets.getSystemWindowInsetTop(),
                                    dp(18),
                                    dp(12) + insets.getSystemWindowInsetBottom()
                            );
                            return insets;
                        }
                    }
            );
        }

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(4));

        TextView back = new TextView(this);
        back.setText("←");
        back.setTextColor(RenaTheme.text());
        back.setTextSize(30);
        back.setGravity(Gravity.CENTER);
        back.setBackgroundColor(Color.TRANSPARENT);
        back.setContentDescription(getString(R.string.team_back_content_description));
        back.setClickable(true);
        back.setFocusable(true);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        header.addView(
                back,
                new LinearLayout.LayoutParams(dp(52), dp(52))
        );

        TextView title = text(
                getString(R.string.data_storage_text),
                22
        );
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setTypeface(
                loadAppFont(android.graphics.Typeface.BOLD)
        );

        header.addView(
                title,
                new LinearLayout.LayoutParams(
                        0,
                        dp(52),
                        1f
                )
        );

        TextView headerSpacer = new TextView(this);
        header.addView(
                headerSpacer,
                new LinearLayout.LayoutParams(dp(52), dp(52))
        );

        root.addView(
                header,
                exact(dp(58))
        );

        LinearLayout storageCard = buildStorageCard();
        LinearLayout.LayoutParams storageCardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(88)
        );
        storageCardLp.topMargin = dp(8);
        storageCardLp.bottomMargin = dp(2);
        root.addView(storageCard, storageCardLp);
        refreshStorageSizes();

        TextView cookies =
                action(getString(R.string.data_cookies_text));
        root.addView(cookies, row());
        root.addView(description(getString(R.string.data_cookies_description)), descriptionRow());
        bind(cookies, new Runnable() {
            @Override
            public void run() {
                confirmClearCookies();
            }
        });

        TextView cache =
                action(getString(R.string.data_cache_text));
        root.addView(cache, row());
        root.addView(description(getString(R.string.data_cache_description)), descriptionRow());
        bind(cache, new Runnable() {
            @Override
            public void run() {
                clearCacheOnly();
            }
        });

        TextView all =
                action(getString(R.string.data_all_web_text));
        root.addView(all, row());
        root.addView(description(getString(R.string.data_all_web_description)), descriptionRow());
        bind(all, new Runnable() {
            @Override
            public void run() {
                confirmClearAll();
            }
        });

        TextView downloads = action(getString(R.string.data_downloads_text));
        root.addView(downloads, row());
        root.addView(description(getString(R.string.data_downloads_description)), descriptionRow());
        bind(downloads, new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(DataManagerActivity.this, DownloadActivity.class));
            }
        });

        TextView clearDownloads = action(getString(R.string.data_clear_downloads_text));
        root.addView(clearDownloads, row());
        root.addView(description(getString(R.string.data_clear_downloads_description)), descriptionRow());
        bind(clearDownloads, new Runnable() {
            @Override
            public void run() {
                confirmClearDownloads();
            }
        });

        TextView browserHistory = action(getString(R.string.data_browser_history_text));
        root.addView(browserHistory, row());
        root.addView(description(getString(R.string.data_browser_history_description)), descriptionRow());
        bind(browserHistory, new Runnable() {
            @Override
            public void run() {
                confirmSimple(R.string.data_browser_history_text, R.string.data_browser_history_confirm, new Runnable() {
                    @Override
                    public void run() {
                        new BrowserStore(DataManagerActivity.this).clearHistory();
                    }
                });
            }
        });

        TextView browserSite = action(getString(R.string.data_browser_site_text));
        root.addView(browserSite, row());
        root.addView(description(getString(R.string.data_browser_site_description)), descriptionRow());
        bind(browserSite, new Runnable() {
            @Override
            public void run() {
                confirmSimple(R.string.data_browser_site_text, R.string.data_browser_site_confirm, new Runnable() {
                    @Override
                    public void run() {
                        BrowserProfile.clearSiteData(DataManagerActivity.this, true, true);
                    }
                });
            }
        });

        TextView deletedBackup = action(getString(R.string.data_deleted_text));
        root.addView(deletedBackup, row());
        root.addView(description(getString(R.string.data_deleted_description)), descriptionRow());
        bind(deletedBackup, new Runnable() {
            @Override
            public void run() {
                confirmSimple(R.string.data_deleted_text, R.string.data_deleted_confirm, new Runnable() {
                    @Override
                    public void run() {
                        AntiDeleteStore.clearAll(DataManagerActivity.this);
                    }
                });
            }
        });

        TextView storage = text(
                Environment.getExternalStorageDirectory()
                        .getAbsolutePath() + "/" + NativeConfig.downloadStorageRootName() + "/",
                12
        );
        storage.setTextColor(
                RenaTheme.fg(145)
        );
        storage.setGravity(
                Gravity.CENTER
        );
        storage.setPadding(
                dp(8),
                dp(18),
                dp(8),
                0
        );

        root.addView(
                storage,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(48)
                )
        );

        ScrollView scroll =
                new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setHorizontalScrollBarEnabled(false);

        scroll.addView(
                root,
                new ScrollView.LayoutParams(
                        ScrollView.LayoutParams.MATCH_PARENT,
                        ScrollView.LayoutParams.WRAP_CONTENT
                )
        );

        setContentView(scroll);
    }
    private void confirmSimple(int titleRes, int messageRes, final Runnable action) {
        new RenaDialog.Builder(this)
                .setTitle(titleRes)
                .setMessage(messageRes)
                .setNegativeButton(R.string.cancel_text, null)
                .setPositiveButton(R.string.clear_text, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        try {
                            action.run();
                            Toast.makeText(DataManagerActivity.this, R.string.data_cleared_text, Toast.LENGTH_SHORT).show();
                            refreshStorageSizes();
                        } catch (Throwable ignored) {
                        }
                    }
                })
                .show();
    }

    private void confirmClearCookies() {
        final RenaDialog dialog =
                new RenaDialog.Builder(this)
                        .setTitle(getString(R.string.data_cookies_text))
                        .setMessage(getString(R.string.clear_cookies_message))
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
                                        clearCookiesAndSiteData();
                                    }
                                }
                        )
                        .create();

        styleDialog(dialog);
        dialog.show();
        styleDialog(dialog);
    }
    private void confirmClearAll() {
        final RenaDialog dialog =
                new RenaDialog.Builder(this)
                        .setTitle(getString(R.string.data_all_web_text))
                        .setMessage(getString(R.string.clear_all_web_message))
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
                                        clearAllWebData();
                                    }
                                }
                        )
                        .create();

        styleDialog(dialog);
        dialog.show();
        styleDialog(dialog);
    }

    private void clearCookiesAndSiteData() {
        if (closing) {
            return;
        }
        closing = true;
        clearProfileData(false);
    }

    private void clearCacheOnly() {
        if (closing) {
            return;
        }
        closing = true;

        Intent result =
                new Intent();

        result.putExtra(
                "web_data_cleared",
                false
        );
        result.putExtra(
                "clear_cache",
                true
        );

        setResult(
                RESULT_OK,
                result
        );

        finish();
    }

    private void deleteDirectoryContents(
            File directory
    ) {
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
                    deleteDirectoryContents(
                            child
                    );
                }

                child.delete();
            } catch (Throwable ignored) {
            }
        }
    }

    private void clearAllWebData() {
        if (closing) {
            return;
        }
        closing = true;
        clearProfileData(true);
    }

    private void clearProfileData(final boolean clearCache) {
        int tabId =
                Math.max(
                        1,
                        Math.min(
                                10,
                                getIntent().getIntExtra(
                                        "tab_id",
                                        1
                                )
                        )
                );

        String profileId = getIntent().getStringExtra(
                "profile_id"
        );

        if (TextUtils.isEmpty(profileId)) {
            profileId = "tab_profile_" + tabId;
        }

        try {
            final CookieManager cookies;

            if (WebViewFeature.isFeatureSupported(
                    WebViewFeature.MULTI_PROFILE
            )) {
                Profile profile =
                        getProfileStoreInstance()
                                .getOrCreateProfile(
                                        profileId
                                );

                cookies =
                        profile.getCookieManager();

                profile.getWebStorage()
                        .deleteAllData();
            } else {
                cookies =
                        CookieManager.getInstance();

                WebStorage.getInstance()
                        .deleteAllData();
            }

            final String finalProfileId = profileId;

            cookies.removeAllCookies(
                    new ValueCallback<Boolean>() {
                        @Override
                        public void onReceiveValue(
                                Boolean removed
                        ) {
                            try {
                                cookies.flush();
                            } catch (Throwable ignored) {
                            }

                            if (clearCache) {
                                final File cacheDirectory = getCacheDir();
                                final File codeCacheDirectory =
                                        android.os.Build.VERSION.SDK_INT >= 21
                                                ? getCodeCacheDir()
                                                : null;

                                cleanupExecutor.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            deleteDirectoryContents(cacheDirectory);
                                            if (codeCacheDirectory != null) {
                                                deleteDirectoryContents(codeCacheDirectory);
                                            }
                                        } catch (Throwable ignored) {
                                        }

                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (isFinishing() || isDestroyed()) {
                                                    return;
                                                }
                                                finishDataClear(
                                                        true,
                                                        finalProfileId
                                                );
                                            }
                                        });
                                    }
                                });
                                return;
                            }

                            finishDataClear(
                                    false,
                                    finalProfileId
                            );
                        }
                    }
            );

        } catch (Throwable ignored) {
            finishDataClear(
                    clearCache,
                    profileId
            );
        }
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

    private void finishDataClear(
            boolean clearCache,
            String profileId
    ) {
        Intent result =
                new Intent();

        result.putExtra(
                "web_data_cleared",
                true
        );
        result.putExtra(
                "clear_cache",
                clearCache
        );
        result.putExtra(
                "profile_id",
                profileId
        );

        setResult(
                RESULT_OK,
                result
        );

        Toast.makeText(
                this,
                getString(R.string.data_cleared_text),
                Toast.LENGTH_SHORT
        ).show();

        finish();
    }
    private void styleDialog(RenaDialog dialog) {
    }

    private void ButtonLike(TextView button) {
        if (button == null) {
            return;
        }

        try {
            button.setTextColor(RenaTheme.accentText());
            button.setTypeface(
                    loadAppFont(
                            android.graphics.Typeface.BOLD
                    )
            );
        } catch (Throwable ignored) {
        }
    }

    private android.graphics.Typeface loadAppFont(int style) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                int fontId = getResources().getIdentifier(
                        "font", "font", getPackageName());
                if (fontId != 0) {
                    return android.graphics.Typeface.create(
                            getResources().getFont(fontId),
                            style
                    );
                }
            }
        } catch (Throwable ignored) {
        }
        return android.graphics.Typeface.create("sans-serif", style);
    }

    private LinearLayout buildStorageCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(10), dp(16), dp(10));
        card.setBackground(
                round(
                        RenaTheme.fg(28),
                        dp(18)
                )
        );

        LinearLayout cacheBox = buildStorageItem(
                getString(R.string.storage_cache_label),
                true
        );
        LinearLayout dataBox = buildStorageItem(
                getString(R.string.storage_data_label),
                false
        );

        LinearLayout.LayoutParams itemLp =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1f
                );
        cacheBox.setLayoutParams(itemLp);

        dataBox.setLayoutParams(
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1f
                )
        );

        card.addView(cacheBox);
        card.addView(dataBox);
        return card;
    }

    private LinearLayout buildStorageItem(
            String title,
            boolean cache
    ) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setPadding(0, 0, dp(8), 0);

        TextView label = text(title, 12);
        label.setTextColor(RenaTheme.fg(160));

        TextView value = text(getString(R.string.storage_calculating_text), 17);
        value.setTypeface(
                loadAppFont(
                        android.graphics.Typeface.BOLD
                )
        );
        value.setSingleLine(true);

        if (cache) {
            cacheSizeView = value;
        } else {
            dataSizeView = value;
        }

        box.addView(label);
        box.addView(value);
        return box;
    }

    private void refreshStorageSizes() {
        if (!storageCalculationActive.compareAndSet(false, true)) {
            return;
        }
        final File dataDirectory;
        final File cacheDirectory;
        final File codeCacheDirectory;

        try {
            dataDirectory = getApplicationInfo().dataDir != null
                    ? new File(getApplicationInfo().dataDir)
                    : null;
            cacheDirectory = getCacheDir();
            codeCacheDirectory = android.os.Build.VERSION.SDK_INT >= 21
                    ? getCodeCacheDir()
                    : null;
        } catch (Throwable ignored) {
            storageCalculationActive.set(false);
            return;
        }

        try {
            cleanupExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    long cacheBytes = directorySize(cacheDirectory);
                    long dataBytes = directorySize(dataDirectory);
                    dataBytes -= cacheBytes;
                    if (codeCacheDirectory != null) {
                        dataBytes -= directorySize(codeCacheDirectory);
                    }
                    dataBytes = Math.max(0L, dataBytes);
                    final long finalCacheBytes = cacheBytes;
                    final long finalDataBytes = dataBytes;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            storageCalculationActive.set(false);
                            if (isFinishing() || isDestroyed()) {
                                return;
                            }
                            if (cacheSizeView != null) {
                                cacheSizeView.setText(formatBytes(finalCacheBytes));
                            }
                            if (dataSizeView != null) {
                                dataSizeView.setText(formatBytes(finalDataBytes));
                            }
                        }
                    });
                }
            });
        } catch (Throwable rejected) {
            storageCalculationActive.set(false);
        }
    }

    private void confirmClearDownloads() {
        final RenaDialog dialog = new RenaDialog.Builder(this)
                .setTitle(getString(R.string.clear_downloads_confirm_title))
                .setMessage(getString(R.string.clear_downloads_confirm_message))
                .setNegativeButton(getString(R.string.cancel_text), null)
                .setPositiveButton(getString(R.string.clear_text), new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface value, int which) {
                        try {
                            cleanupExecutor.execute(new Runnable() {
                                @Override
                                public void run() {
                                    RenaDownloadManager.clearAll(true);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (isFinishing() || isDestroyed()) {
                                                return;
                                            }
                                            Toast.makeText(DataManagerActivity.this,
                                                    getString(R.string.downloads_cleared_text), Toast.LENGTH_SHORT).show();
                                            refreshStorageSizes();
                                        }
                                    });
                                }
                            });
                        } catch (Throwable ignored) {
                        }
                    }
                })
                .create();
        styleDialog(dialog);
        dialog.show();
        styleDialog(dialog);
    }

    private long directorySize(File file) {
        if (file == null || !file.exists()) {
            return 0L;
        }

        if (file.isFile()) {
            return Math.max(0L, file.length());
        }

        long total = 0L;
        File[] children = file.listFiles();
        if (children == null) {
            return 0L;
        }

        for (File child : children) {
            try {
                total += directorySize(child);
                if (total < 0L) {
                    return Long.MAX_VALUE;
                }
            } catch (Throwable ignored) {
            }
        }

        return total;
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }

        double kb = bytes / 1024.0;
        if (kb < 1024.0) {
            return String.format(java.util.Locale.US, "%.1f KB", kb);
        }

        double mb = kb / 1024.0;
        if (mb < 1024.0) {
            return String.format(java.util.Locale.US, "%.1f MB", mb);
        }

        double gb = mb / 1024.0;
        return String.format(java.util.Locale.US, "%.2f GB", gb);
    }

    private TextView action(String value) {
        TextView v = text(
                value,
                16
        );

        v.setGravity(
                Gravity.CENTER
        );
        v.setTextAlignment(
                View.TEXT_ALIGNMENT_CENTER
        );
        v.setPadding(
                dp(18),
                0,
                dp(18),
                0
        );
        v.setMinHeight(dp(62));

        v.setTypeface(
                loadAppFont(
                        android.graphics.Typeface.BOLD
                )
        );

        int background =
                RenaTheme.fg(30);

        if (value.equals(
                getString(R.string.data_all_web_text)
        )) {
            background =
                    RenaTheme.dangerSoft();
        } else if (value.equals(
                getString(R.string.data_cache_text)
        )) {
            background =
                    Color.argb(38, 66, 185, 255);
        } else if (value.equals(
                getString(R.string.data_cookies_text)
        )) {
            background =
                    RenaTheme.accentSoft();
        }

        v.setBackground(
                round(
                        background,
                        dp(18)
                )
        );

        return v;
    }

    private void bind(TextView view, final Runnable action) {
        view.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (action != null) {
                            action.run();
                        }
                    }
                }
        );
    }

    private TextView text(String value, float size) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(RenaTheme.text());
        t.setTextSize(size);
        try {
            int fontId = getResources().getIdentifier("font", "font", getPackageName());
            if (fontId != 0 && android.os.Build.VERSION.SDK_INT >= 26) {
                t.setTypeface(getResources().getFont(fontId));
            } else {
                t.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL));
            }
        } catch (Throwable ignored) {
            t.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL));
        }
        return t;
    }

    private LinearLayout.LayoutParams row() {
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(58)
                );
        lp.topMargin = dp(8);
        return lp;
    }

    private TextView description(String value) {
        TextView view = text(value, 12);
        view.setTextColor(RenaTheme.fg(155));
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(8), 0, dp(8), 0);
        return view;
    }

    private LinearLayout.LayoutParams descriptionRow() {
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(50)
                );
        lp.topMargin = dp(-2);
        lp.bottomMargin = dp(6);
        return lp;
    }

    private LinearLayout.LayoutParams exact(int h) {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, h
        );
    }

    private android.graphics.drawable.GradientDrawable round(int color, int radius) {
        android.graphics.drawable.GradientDrawable d =
                new android.graphics.drawable.GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        if (RenaTheme.strokeWidthDp() > 0 && Color.alpha(color) < 60) {
            d.setStroke((int) (getResources().getDisplayMetrics().density), RenaTheme.outline());
        }
        return d;
    }

    @Override
    public void onBackPressed() {
        if (closing) {
            return;
        }

        closing = true;
        finish();

        if (!RenaSettingsStore.getBoolean(
                this,
                "reduce_animation",
                false
        )) {
            overridePendingTransition(
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
            );
        } else {
            overridePendingTransition(0, 0);
        }
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
