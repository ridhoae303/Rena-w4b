package com.rena.w4b;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SplashActivity extends Activity {
    private static final long SPLASH_DELAY_MS = 350L;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        if (android.os.Build.VERSION.SDK_INT < 26) {
            new AlertDialog.Builder(this)
                    .setTitle("Unsupported Android Version")
                    .setMessage("Rena W4B requires Android 8.0 (API 26) or later.")
                    .setPositiveButton(
                            "OK",
                            new android.content.DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(
                                        android.content.DialogInterface dialog,
                                        int which
                                ) {
                                    closeTaskSafely();
                                }
                            }
                    )
                    .setOnDismissListener(
                            new android.content.DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(
                                        android.content.DialogInterface dialog
                                ) {
                                    if (!isFinishing()) {
                                        closeTaskSafely();
                                    }
                                }
                            }
                    )
                    .show();
            return;
        }

        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false);
        }
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            window.setStatusBarColor(Color.WHITE);
            window.setNavigationBarColor(Color.WHITE);
            int flags =
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR |
                    View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            window.getDecorView().setSystemUiVisibility(flags);
        }

        if (!NativeConfig.isNativeAvailable()) {
            closeTaskSafely();
            return;
        }

        if (!integrityGate()) {
            showIntegrityFailure();
            return;
        }

        buildSplash();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isFinishing()) {
                    return;
                }

                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                overridePendingTransition(0, R.anim.splash_exit);
                finish();
            }
        }, SPLASH_DELAY_MS);
    }

    private void buildSplash() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.WHITE);

        ImageView icon = new ImageView(this);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        try {
            ApplicationInfo info = getPackageManager()
                    .getApplicationInfo(getPackageName(), 0);
            Drawable appIcon =
                    getPackageManager().getApplicationIcon(info);

            if (appIcon != null) {
                icon.setImageDrawable(appIcon);
            } else {
                icon.setImageResource(android.R.drawable.sym_def_app_icon);
            }
        } catch (Throwable ignored) {
            try {
                icon.setImageResource(android.R.drawable.sym_def_app_icon);
            } catch (Throwable ignoredAgain) {
                icon.setImageDrawable(null);
            }
        }

        root.addView(icon, new LinearLayout.LayoutParams(dp(112), dp(112)));

        TextView name = new TextView(this);
        name.setText(getApplicationName());
        name.setTextColor(Color.rgb(30, 30, 30));
        name.setTextSize(18);
        try {
            int fontId = getResources().getIdentifier("font", "font", getPackageName());
            name.setTypeface(
                    fontId != 0 && android.os.Build.VERSION.SDK_INT >= 26
                            ? getResources().getFont(fontId)
                            : Typeface.create("sans-serif", Typeface.BOLD)
            );
        } catch (Throwable ignored) {
            name.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        }
        name.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams nameLp =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        dp(44)
                );
        nameLp.topMargin = dp(12);
        root.addView(name, nameLp);

        setContentView(root);
    }

    private void showIntegrityFailure() {
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(NativeConfig.applicationIntegrityTitle())
                .setMessage(NativeConfig.integrityErrorMessage())
                .setPositiveButton(NativeConfig.okayText(), null)
                .create();
        dialog.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(android.content.DialogInterface d) {
                closeTaskSafely();
            }
        });
        dialog.show();
    }

    private void closeTaskSafely() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                finishAndRemoveTask();
            } else {
                finish();
            }
        } catch (Throwable ignored) {
            finish();
        }
    }

    private String getApplicationName() {
        try {
            ApplicationInfo info = getPackageManager()
                    .getApplicationInfo(getPackageName(), 0);
            CharSequence label = getPackageManager().getApplicationLabel(info);
            if (label != null && label.length() > 0) {
                return label.toString();
            }
        } catch (Throwable ignored) {
        }
        return NativeConfig.appFallbackName();
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



    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
