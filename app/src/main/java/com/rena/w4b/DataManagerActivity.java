package com.rena.w4b;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
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

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        configureWindow();
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        configureWindow();
    }

    private void configureWindow() {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false);
        }
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.BLACK);
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
        root.setBackgroundColor(Color.rgb(17, 27, 33));
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
        back.setTextColor(Color.WHITE);
        back.setTextSize(30);
        back.setGravity(Gravity.CENTER);
        back.setBackgroundColor(Color.TRANSPARENT);
        back.setContentDescription(NativeConfig.teamBackContentDescription());
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
                NativeConfig.dataStorageText(),
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

        TextView cookies =
                action(NativeConfig.dataCookiesText());
        root.addView(cookies, row());
        root.addView(description(NativeConfig.dataCookiesDescription()), descriptionRow());
        bind(cookies, new Runnable() {
            @Override
            public void run() {
                confirmClearCookies();
            }
        });

        TextView cache =
                action(NativeConfig.dataCacheText());
        root.addView(cache, row());
        root.addView(description(NativeConfig.dataCacheDescription()), descriptionRow());
        bind(cache, new Runnable() {
            @Override
            public void run() {
                clearCacheOnly();
            }
        });

        TextView all =
                action(NativeConfig.dataAllWebText());
        root.addView(all, row());
        root.addView(description(NativeConfig.dataAllWebDescription()), descriptionRow());
        bind(all, new Runnable() {
            @Override
            public void run() {
                confirmClearAll();
            }
        });

        TextView storage = text(
                Environment.getExternalStorageDirectory()
                        .getAbsolutePath() + "/Rena/",
                12
        );
        storage.setTextColor(
                Color.argb(145, 255, 255, 255)
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

    private void confirmClearCookies() {
        final AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(NativeConfig.dataCookiesText())
                        .setMessage(NativeConfig.clearCookiesMessage())
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
        final AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(NativeConfig.dataAllWebText())
                        .setMessage(NativeConfig.clearAllWebMessage())
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
                        ProfileStore
                                .getInstance()
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
                NativeConfig.dataClearedText(),
                Toast.LENGTH_SHORT
        ).show();

        finish();
    }

    @Override
    protected void onDestroy() {
        cleanupExecutor.shutdownNow();
        super.onDestroy();
    }

    private void styleDialog(AlertDialog dialog) {
        if (dialog == null) {
            return;
        }

        try {
            Window window = dialog.getWindow();
            if (window != null) {
                android.graphics.drawable.GradientDrawable background =
                        new android.graphics.drawable.GradientDrawable();
                background.setColor(Color.rgb(32, 44, 49));
                background.setCornerRadius(dp(24));
                window.setBackgroundDrawable(background);
                window.addFlags(
                        WindowManager.LayoutParams.FLAG_DIM_BEHIND
                );

                WindowManager.LayoutParams lp =
                        window.getAttributes();
                lp.dimAmount = 0.58f;
                window.setAttributes(lp);
            }
        } catch (Throwable ignored) {
        }

        try {
            TextView message =
                    dialog.findViewById(android.R.id.message);
            if (message != null) {
                message.setTextColor(Color.rgb(224, 232, 235));
            }

            int positive =
                    getResources().getIdentifier(
                            "button1",
                            "id",
                            "android"
                    );
            int negative =
                    getResources().getIdentifier(
                            "button2",
                            "id",
                            "android"
                    );

            TextView positiveButton = dialog.findViewById(positive);
            TextView negativeButton = dialog.findViewById(negative);
            ButtonLike(positiveButton);
            ButtonLike(negativeButton);
        } catch (Throwable ignored) {
        }
    }

    private void ButtonLike(TextView button) {
        if (button == null) {
            return;
        }

        try {
            button.setTextColor(Color.rgb(0, 220, 172));
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
                Color.argb(30, 255, 255, 255);

        if (value.equals(
                NativeConfig.dataAllWebText()
        )) {
            background =
                    Color.argb(40, 255, 80, 90);
        } else if (value.equals(
                NativeConfig.dataCacheText()
        )) {
            background =
                    Color.argb(38, 66, 185, 255);
        } else if (value.equals(
                NativeConfig.dataCookiesText()
        )) {
            background =
                    Color.argb(38, 0, 220, 172);
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
        t.setTextColor(Color.WHITE);
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
        view.setTextColor(Color.argb(155, 255, 255, 255));
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
