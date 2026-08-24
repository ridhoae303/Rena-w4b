package com.rena.w4b;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.graphics.drawable.GradientDrawable;



public class LockScreenActivity extends Activity {
    private EditText pinInput;
    private TextView unlockButton;
    private boolean unlocking;
    private boolean verifyingPin;
    private final ExecutorService pinExecutor = Executors.newSingleThreadExecutor();
    private Object biometricPrompt;
    private android.app.AlertDialog legacyFingerprintDialog;
    private boolean activityDestroyed;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        AppLockManager.markLockScreenShown();
        configureWindow();
        buildUi();
    }

    @Override
    protected void onDestroy() {
        activityDestroyed = true;
        biometricPrompt = null;
        dismissLegacyFingerprintDialog();
        pinExecutor.shutdownNow();
        super.onDestroy();
    }

    private void configureWindow() {
        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false);
        }
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(28), dp(42), dp(28), dp(36));
        root.setBackground(createGradient());

        TextView title = text(
                NativeConfig.lockScreenTitle(),
                30,
                Color.WHITE
        );
        title.setGravity(Gravity.CENTER);
        title.setTypeface(loadFont(android.graphics.Typeface.BOLD));
        root.addView(title, exact(dp(62)));

        TextView message = text(
                NativeConfig.lockScreenMessage(),
                15,
                Color.argb(230, 255, 255, 255)
        );
        message.setGravity(Gravity.CENTER);
        root.addView(message, exact(dp(58)));

        pinInput = new EditText(this);
        pinInput.setSingleLine(true);
        pinInput.setTextColor(Color.WHITE);
        pinInput.setHintTextColor(Color.argb(180, 255, 255, 255));
        pinInput.setHint(NativeConfig.lockPinHint());
        pinInput.setGravity(Gravity.CENTER);
        pinInput.setTextSize(20);
        pinInput.setInputType(
                InputType.TYPE_CLASS_NUMBER |
                        InputType.TYPE_NUMBER_VARIATION_PASSWORD
        );
        pinInput.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter(4)
        });
        pinInput.setBackground(round(
                Color.argb(45, 255, 255, 255),
                dp(18)
        ));
        root.addView(pinInput, exact(dp(58)));

        TextView unlock = text(
                NativeConfig.unlockText(),
                15,
                Color.WHITE
        );
        unlock.setGravity(Gravity.CENTER);
        unlock.setTypeface(loadFont(android.graphics.Typeface.BOLD));
        unlock.setBackground(round(
                Color.rgb(30, 106, 80),
                dp(18)
        ));
        unlockButton = unlock;
        unlock.setEnabled(false);
        unlock.setAlpha(0.45f);
        unlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unlockWithPin();
            }
        });
        LinearLayout.LayoutParams unlockLp = exact(dp(54));
        unlockLp.topMargin = dp(16);
        root.addView(unlock, unlockLp);

        if (AppLockManager.canUseFingerprint(this) &&
                SecureAppLockStore.read(this).biometricEnabled) {
            ImageButton fingerprint = new ImageButton(this);
            fingerprint.setImageResource(R.drawable.fingerprint);
            fingerprint.setBackground(round(
                    Color.argb(40, 255, 255, 255),
                    dp(18)
            ));
            fingerprint.setContentDescription(
                    NativeConfig.fingerprintUnlockText()
            );
            fingerprint.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            fingerprint.setPadding(
                    dp(12),
                    dp(12),
                    dp(12),
                    dp(12)
            );
            fingerprint.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startFingerprint();
                }
            });
            LinearLayout.LayoutParams fpLp = exact(dp(54));
            fpLp.topMargin = dp(12);
            root.addView(fingerprint, fpLp);
        }

        TextView footer = text(
                NativeConfig.lockScreenFooter(),
                12,
                Color.argb(165, 255, 255, 255)
        );
        footer.setGravity(Gravity.CENTER);
        root.addView(footer, exact(dp(72)));

        pinInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s,
                    int start,
                    int count,
                    int after
            ) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count
            ) {
                updateUnlockButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        updateUnlockButtonState();

        scroll.addView(root);
        setContentView(scroll);
    }

    private void updateUnlockButtonState() {
        boolean ready =
                pinInput != null &&
                pinInput.getText() != null &&
                pinInput.getText().length() == 4;

        if (unlockButton != null) {
            boolean enabled = ready && !verifyingPin;
            unlockButton.setEnabled(enabled);
            unlockButton.setAlpha(enabled ? 1.0f : 0.45f);
        }
    }

    private void unlockWithPin() {
        if (unlocking || verifyingPin) {
            return;
        }

        String pin = pinInput == null
                ? ""
                : pinInput.getText().toString();

        if (pin.length() != 4 || !pin.matches("\\d{4}")) {
            Toast.makeText(
                    this,
                    NativeConfig.incorrectPinText(),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        final SecureAppLockStore.State state =
                SecureAppLockStore.read(this);

        if (!SecureAppLockStore.hasCredentials(state)) {
            Toast.makeText(
                    this,
                    "App Lock data is unavailable. Please recreate your PIN in App Lock settings.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        final String attempt = pin;
        final SecureAppLockStore.State snapshot = state.copy();
        verifyingPin = true;
        updateUnlockButtonState();

        pinExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final boolean valid =
                        SecureAppLockStore.verifyPin(
                                attempt,
                                snapshot
                        );

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        verifyingPin = false;

                        if (activityDestroyed || isFinishing()) {
                            return;
                        }

                        if (valid) {
                            unlockAndFinish();
                            return;
                        }

                        updateUnlockButtonState();
                        Toast.makeText(
                                LockScreenActivity.this,
                                NativeConfig.incorrectPinText(),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
            }
        });
    }

    private void unlockAndFinish() {
        if (activityDestroyed || isFinishing() || unlocking) {
            return;
        }
        unlocking = true;
        biometricPrompt = null;
        AppLockManager.markLockScreenUnlocked();
        finish();
        overridePendingTransition(
                android.R.anim.fade_in,
                android.R.anim.fade_out
        );
    }

    private void startFingerprint() {
        if (activityDestroyed ||
                unlocking ||
                !AppLockManager.canUseFingerprint(this)) {
            return;
        }

        if (android.os.Build.VERSION.SDK_INT >= 28) {
            startPlatformBiometricUnlock();
        } else {
            startLegacyFingerprintUnlock();
        }
    }

    private void startPlatformBiometricUnlock() {
        try {
            android.hardware.biometrics.BiometricPrompt prompt =
                    new android.hardware.biometrics.BiometricPrompt.Builder(this)
                            .setTitle(NativeConfig.unlockDialogTitle())
                            .setSubtitle(NativeConfig.unlockDialogSubtitle())
                            .setNegativeButton(
                                    NativeConfig.cancelText(),
                                    createMainThreadExecutor(),
                                    new android.content.DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(
                                                android.content.DialogInterface dialog,
                                                int which
                                        ) {
                                            Toast.makeText(
                                                    LockScreenActivity.this,
                                                    NativeConfig.fingerprintCanceledText(),
                                                    Toast.LENGTH_SHORT
                                            ).show();
                                        }
                                    }
                            )
                            .build();

            biometricPrompt = prompt;

            prompt.authenticate(
                    new android.os.CancellationSignal(),
                    createMainThreadExecutor(),
                    new android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(
                                android.hardware.biometrics.BiometricPrompt.AuthenticationResult result
                        ) {
                            unlockAndFinish();
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            if (activityDestroyed || isFinishing()) {
                                return;
                            }

                            Toast.makeText(
                                    LockScreenActivity.this,
                                    NativeConfig.fingerprintFailedText(),
                                    Toast.LENGTH_SHORT
                            ).show();
                        }

                        @Override
                        public void onAuthenticationError(
                                int errorCode,
                                CharSequence errString
                        ) {
                            if (activityDestroyed || isFinishing()) {
                                return;
                            }

                            Toast.makeText(
                                    LockScreenActivity.this,
                                    NativeConfig.fingerprintCanceledText(),
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
            );
        } catch (Throwable ignored) {
            startLegacyFingerprintUnlock();
        }
    }

    private void startLegacyFingerprintUnlock() {
        try {
            android.hardware.fingerprint.FingerprintManager manager =
                    (android.hardware.fingerprint.FingerprintManager)
                            getSystemService(FINGERPRINT_SERVICE);

            if (manager == null ||
                    !manager.isHardwareDetected() ||
                    !manager.hasEnrolledFingerprints()) {
                return;
            }

            legacyFingerprintDialog =
                    new android.app.AlertDialog.Builder(this)
                            .setTitle(NativeConfig.unlockDialogTitle())
                            .setMessage(NativeConfig.fingerprintTouchUnlockText())
                            .setNegativeButton(
                                    NativeConfig.cancelText(),
                                    new android.content.DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(
                                                android.content.DialogInterface dialog,
                                                int which
                                        ) {
                                            Toast.makeText(
                                                    LockScreenActivity.this,
                                                    NativeConfig.fingerprintCanceledText(),
                                                    Toast.LENGTH_SHORT
                                            ).show();
                                        }
                                    }
                            )
                            .create();

            legacyFingerprintDialog.show();

            final android.os.CancellationSignal cancellationSignal =
                    new android.os.CancellationSignal();

            manager.authenticate(
                    null,
                    cancellationSignal,
                    0,
                    new android.hardware.fingerprint.FingerprintManager.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(
                                android.hardware.fingerprint.FingerprintManager.AuthenticationResult result
                        ) {
                            dismissLegacyFingerprintDialog();
                            unlockAndFinish();
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            if (activityDestroyed || isFinishing()) {
                                return;
                            }

                            Toast.makeText(
                                    LockScreenActivity.this,
                                    NativeConfig.fingerprintFailedText(),
                                    Toast.LENGTH_SHORT
                            ).show();
                        }

                        @Override
                        public void onAuthenticationError(
                                int errorCode,
                                CharSequence errString
                        ) {
                            dismissLegacyFingerprintDialog();
                        }
                    },
                    null
            );
        } catch (Throwable ignored) {
            dismissLegacyFingerprintDialog();
            Toast.makeText(
                    this,
                    NativeConfig.fingerprintFailedText(),
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private java.util.concurrent.Executor createMainThreadExecutor() {
        return new java.util.concurrent.Executor() {
            @Override
            public void execute(Runnable command) {
                runOnUiThread(command);
            }
        };
    }

    private void dismissLegacyFingerprintDialog() {
        if (legacyFingerprintDialog != null) {
            android.app.AlertDialog dialog = legacyFingerprintDialog;
            legacyFingerprintDialog = null;
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }
        biometricPrompt = null;
    }

    @Override
    public void onBackPressed() {
        // App Lock must not be bypassed with Back.
    }

    private TextView text(String value, float size, int color) {
        TextView view = new TextView(this);
        view.setText(TextUtils.isEmpty(value) ? "" : value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private android.graphics.Typeface loadFont(int style) {
        try {
            int id = getResources().getIdentifier(
                    "font",
                    "font",
                    getPackageName()
            );
            if (id != 0 && android.os.Build.VERSION.SDK_INT >= 26) {
                return getResources().getFont(id);
            }
        } catch (Throwable ignored) {
        }
        return android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT,
                style
        );
    }

    private LinearLayout.LayoutParams exact(int height) {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                height
        );
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private GradientDrawable createGradient() {
        return new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{
                        Color.rgb(25, 117, 82),
                        Color.rgb(20, 85, 65),
                        Color.rgb(8, 38, 30)
                }
        );
    }

    private int dp(int value) {
        return Math.round(
                value * getResources().getDisplayMetrics().density
        );
    }
}
