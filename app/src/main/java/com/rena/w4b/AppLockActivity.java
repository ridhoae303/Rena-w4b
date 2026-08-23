package com.rena.w4b;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.drawable.GradientDrawable;



public class AppLockActivity extends Activity {
    private Switch lockSwitch;
    private Switch biometricSwitch;
    private TextView timeoutValue;
    private boolean updatingSwitch;
    private boolean fingerprintAuthenticationInProgress;
    private Object biometricPrompt;
    private android.app.AlertDialog legacyFingerprintDialog;
    private boolean activityDestroyed;

    private static final int REQUEST_PIN_SETUP = 4101;
    private static final int REQUEST_PIN_CHANGE = 4102;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        configureWindow();
        buildUi();
        refreshUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        configureWindow();
        refreshUi();
    }

    private void configureWindow() {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.BLACK);
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
        root.setPadding(dp(18), dp(12), dp(18), dp(20));
        root.setBackgroundColor(Color.rgb(17, 27, 33));

        if (android.os.Build.VERSION.SDK_INT >= 23) {
            root.setOnApplyWindowInsetsListener(
                    new View.OnApplyWindowInsetsListener() {
                        @Override
                        public WindowInsets onApplyWindowInsets(
                                View v,
                                WindowInsets insets
                        ) {
                            v.setPadding(
                                    dp(18),
                                    dp(12) + insets.getSystemWindowInsetTop(),
                                    dp(18),
                                    dp(20) + insets.getSystemWindowInsetBottom()
                            );
                            return insets;
                        }
                    }
            );
        }

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView back = text(
                NativeConfig.appLockBackSymbol(),
                38,
                Color.WHITE
        );
        back.setGravity(Gravity.CENTER);
        back.setContentDescription(NativeConfig.appLockBackText());
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishWithAnimation();
            }
        });
        header.addView(back, new LinearLayout.LayoutParams(dp(52), dp(52)));

        TextView title = text(
                NativeConfig.appLockTitle(),
                22,
                Color.WHITE
        );
        title.setTypeface(loadFont(android.graphics.Typeface.BOLD));
        header.addView(title, new LinearLayout.LayoutParams(0, dp(52), 1f));

        root.addView(header, exact(dp(56)));

        TextView heroTitle = text(
                NativeConfig.appLockHeroTitle(),
                25,
                Color.WHITE
        );
        heroTitle.setTypeface(loadFont(android.graphics.Typeface.BOLD));
        root.addView(heroTitle, wrap(dp(56)));

        TextView heroMessage = text(
                NativeConfig.appLockHeroMessage(),
                14,
                Color.argb(180, 255, 255, 255)
        );
        heroMessage.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(heroMessage, wrap(dp(58)));

        LinearLayout lockCard = card();
        TextView lockLabel = text(
                NativeConfig.appLockSwitchText(),
                16,
                Color.WHITE
        );
        lockLabel.setTypeface(loadFont(android.graphics.Typeface.BOLD));
        lockCard.addView(lockLabel, new LinearLayout.LayoutParams(0, dp(58), 1f));

        lockSwitch = new Switch(this);
        lockSwitch.setContentDescription(
                NativeConfig.appLockSwitchText()
        );
        lockSwitch.setMinWidth(dp(56));
        lockCard.addView(lockSwitch, new LinearLayout.LayoutParams(dp(56), dp(56)));

        lockSwitch.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(
                            CompoundButton button,
                            boolean checked
                    ) {
                        if (updatingSwitch) {
                            return;
                        }

                        if (!checked) {
                            SecureAppLockStore.State state =
                                    SecureAppLockStore.read(AppLockActivity.this);
                            state.enabled = false;
                            state.biometricEnabled = false;
                            state.lastBackgroundAtMillis = 0L;
                            if (!SecureAppLockStore.write(
                                    AppLockActivity.this,
                                    state
                            )) {
                                Toast.makeText(
                                        AppLockActivity.this,
                                        NativeConfig.pinSaveFailedText(),
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                            refreshUi();
                            return;
                        }

                        SecureAppLockStore.State state =
                                SecureAppLockStore.read(AppLockActivity.this);
                        if (!SecureAppLockStore.hasCredentials(state)) {
                            updatingSwitch = true;
                            lockSwitch.setChecked(false);
                            updatingSwitch = false;
                            startActivityForResult(
                                    new android.content.Intent(
                                            AppLockActivity.this,
                                            PinSetupActivity.class
                                    ),
                                    REQUEST_PIN_SETUP
                            );
                            return;
                        }

                        state.enabled = true;
                        state.lastBackgroundAtMillis = 0L;
                        if (!SecureAppLockStore.write(
                                AppLockActivity.this,
                                state
                        )) {
                            updatingSwitch = true;
                            lockSwitch.setChecked(false);
                            updatingSwitch = false;
                            Toast.makeText(
                                    AppLockActivity.this,
                                    NativeConfig.pinSaveFailedText(),
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }
                        refreshUi();
                    }
                }
        );

        lockCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (lockSwitch != null) {
                    lockSwitch.setChecked(!lockSwitch.isChecked());
                }
            }
        });

        root.addView(lockCard, wrap(dp(64)));

        TextView changePinLabel = text(
                NativeConfig.changePinLabel(),
                15,
                Color.argb(180, 255, 255, 255)
        );
        changePinLabel.setTypeface(loadFont(android.graphics.Typeface.BOLD));
        changePinLabel.setPadding(dp(4), dp(18), 0, 0);
        root.addView(changePinLabel, wrap(dp(42)));

        LinearLayout changePinCard = card();
        TextView changePinLeft = text(
                NativeConfig.changePinText(),
                15,
                Color.WHITE
        );
        changePinCard.addView(
                changePinLeft,
                new LinearLayout.LayoutParams(0, dp(58), 1f)
        );
        TextView changePinArrow = text(
                NativeConfig.changePinArrow(),
                24,
                Color.rgb(0, 220, 172)
        );
        changePinArrow.setGravity(Gravity.CENTER);
        changePinCard.addView(
                changePinArrow,
                new LinearLayout.LayoutParams(dp(44), dp(58))
        );
        changePinCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SecureAppLockStore.State state =
                        SecureAppLockStore.read(AppLockActivity.this);
                if (!state.enabled || !SecureAppLockStore.hasCredentials(state)) {
                    Toast.makeText(
                            AppLockActivity.this,
                            NativeConfig.appLockEnableFirstText(),
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                startActivityForResult(
                        new android.content.Intent(
                                AppLockActivity.this,
                                ChangePinActivity.class
                        ),
                        REQUEST_PIN_CHANGE
                );
            }
        });
        root.addView(changePinCard, wrap(dp(64)));

        TextView timeoutLabel = text(
                NativeConfig.appLockTimeoutLabel(),
                15,
                Color.argb(180, 255, 255, 255)
        );
        timeoutLabel.setTypeface(loadFont(android.graphics.Typeface.BOLD));
        timeoutLabel.setPadding(dp(4), dp(18), 0, 0);
        root.addView(timeoutLabel, wrap(dp(42)));

        LinearLayout timeoutCard = card();
        TextView timeoutLeft = text(
                NativeConfig.appLockTimeoutText(),
                15,
                Color.WHITE
        );
        timeoutCard.addView(timeoutLeft, new LinearLayout.LayoutParams(0, dp(58), 1f));

        timeoutValue = text("", 14, Color.rgb(0, 220, 172));
        timeoutValue.setGravity(Gravity.CENTER);
        timeoutCard.addView(timeoutValue, new LinearLayout.LayoutParams(dp(120), dp(58)));

        timeoutCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseTimeout();
            }
        });
        root.addView(timeoutCard, wrap(dp(64)));

        TextView biometricLabel = text(
                NativeConfig.appLockFingerprintText(),
                15,
                Color.argb(180, 255, 255, 255)
        );
        biometricLabel.setTypeface(loadFont(android.graphics.Typeface.BOLD));
        biometricLabel.setPadding(dp(4), dp(18), 0, 0);
        root.addView(biometricLabel, wrap(dp(42)));

        LinearLayout biometricCard = card();
        boolean fingerprintAvailable =
                AppLockManager.canUseFingerprint(this);
        TextView biometricLeft = text(
                fingerprintAvailable
                        ? NativeConfig.appLockFingerprintText()
                        : NativeConfig.appLockFingerprintUnavailable(),
                15,
                Color.WHITE
        );
        biometricCard.addView(biometricLeft, new LinearLayout.LayoutParams(0, dp(58), 1f));

        biometricSwitch = new Switch(this);
        biometricSwitch.setContentDescription(
                NativeConfig.appLockFingerprintText()
        );
        biometricSwitch.setMinWidth(dp(56));
        biometricCard.addView(biometricSwitch, new LinearLayout.LayoutParams(dp(56), dp(56)));
        biometricCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFingerprintEnrollment();
            }
        });

        biometricSwitch.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(
                            CompoundButton button,
                            boolean checked
                    ) {
                        if (updatingSwitch) {
                            return;
                        }

                        updatingSwitch = true;
                        button.setChecked(false);
                        updatingSwitch = false;
                        toggleFingerprintEnrollment();
                    }
                }
        );

        root.addView(biometricCard, wrap(dp(64)));

        TextView note = text(
                NativeConfig.appLockSecurityNote(),
                12,
                Color.argb(150, 255, 255, 255)
        );
        note.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(note, wrap(dp(74)));

        scroll.addView(root);
        setContentView(scroll);
    }

    private void toggleFingerprintEnrollment() {
        SecureAppLockStore.State state =
                SecureAppLockStore.read(this);

        if (!state.enabled) {
            Toast.makeText(
                    this,
                    NativeConfig.appLockEnableFirstText(),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (!AppLockManager.canUseFingerprint(this)) {
            Toast.makeText(
                    this,
                    NativeConfig.appLockFingerprintUnavailable(),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (state.biometricEnabled) {
            state.biometricEnabled = false;
            if (!SecureAppLockStore.write(this, state)) {
                Toast.makeText(
                        this,
                        NativeConfig.pinSaveFailedText(),
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }
            refreshUi();
            return;
        }

        beginFingerprintEnrollment();
    }

    private void beginFingerprintEnrollment() {
        if (fingerprintAuthenticationInProgress || activityDestroyed) {
            return;
        }

        if (!AppLockManager.canUseFingerprint(this)) {
            Toast.makeText(
                    this,
                    NativeConfig.appLockFingerprintUnavailable(),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        fingerprintAuthenticationInProgress = true;
        if (biometricSwitch != null) {
            biometricSwitch.setEnabled(false);
        }

        if (android.os.Build.VERSION.SDK_INT >= 28) {
            startPlatformBiometricEnrollment();
        } else {
            startLegacyFingerprintEnrollment();
        }
    }

    private void startPlatformBiometricEnrollment() {
        try {
            android.hardware.biometrics.BiometricPrompt prompt =
                    new android.hardware.biometrics.BiometricPrompt.Builder(this)
                            .setTitle("Enable Fingerprint Unlock")
                            .setSubtitle("Authenticate to enable fingerprint unlock")
                            .setNegativeButton(
                                    "Cancel",
                                    createMainThreadExecutor(),
                                    new android.content.DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(
                                                android.content.DialogInterface dialog,
                                                int which
                                        ) {
                                            finishFingerprintAuthentication(
                                                    NativeConfig.fingerprintCanceledText()
                                            );
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
                            saveBiometricEnrollment();
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            if (activityDestroyed || isFinishing()) {
                                return;
                            }

                            Toast.makeText(
                                    AppLockActivity.this,
                                    NativeConfig.fingerprintFailedText(),
                                    Toast.LENGTH_SHORT
                            ).show();
                        }

                        @Override
                        public void onAuthenticationError(
                                int errorCode,
                                CharSequence errString
                        ) {
                            finishFingerprintAuthentication(
                                    NativeConfig.fingerprintCanceledText()
                            );
                        }
                    }
            );
        } catch (Throwable ignored) {
            finishFingerprintAuthentication(
                    NativeConfig.fingerprintFailedText()
            );
        }
    }

    private void saveBiometricEnrollment() {
        if (activityDestroyed || isFinishing()) {
            return;
        }

        SecureAppLockStore.State state =
                SecureAppLockStore.read(this);
        state.biometricEnabled = true;

        if (!SecureAppLockStore.write(this, state)) {
            finishFingerprintAuthentication(
                    NativeConfig.pinSaveFailedText()
            );
            return;
        }

        finishFingerprintAuthentication(null);
    }

    private void finishFingerprintAuthentication(String message) {
        fingerprintAuthenticationInProgress = false;
        biometricPrompt = null;

        if (legacyFingerprintDialog != null) {
            android.app.AlertDialog dialog = legacyFingerprintDialog;
            legacyFingerprintDialog = null;
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }

        if (biometricSwitch != null) {
            biometricSwitch.setEnabled(true);
        }

        if (!TextUtils.isEmpty(message) && !activityDestroyed && !isFinishing()) {
            Toast.makeText(
                    this,
                    message,
                    Toast.LENGTH_SHORT
            ).show();
        }

        if (!activityDestroyed && !isFinishing()) {
            refreshUi();
        }
    }

    private void startLegacyFingerprintEnrollment() {
        try {
            android.hardware.fingerprint.FingerprintManager manager =
                    (android.hardware.fingerprint.FingerprintManager)
                            getSystemService(FINGERPRINT_SERVICE);

            if (manager == null ||
                    !manager.isHardwareDetected() ||
                    !manager.hasEnrolledFingerprints()) {
                finishFingerprintAuthentication(
                        NativeConfig.appLockFingerprintUnavailable()
                );
                return;
            }

            legacyFingerprintDialog =
                    new android.app.AlertDialog.Builder(this)
                            .setTitle("Enable Fingerprint Unlock")
                            .setMessage("Touch the fingerprint sensor to continue.")
                            .setNegativeButton(
                                    "Cancel",
                                    new android.content.DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(
                                                android.content.DialogInterface dialog,
                                                int which
                                        ) {
                                            finishFingerprintAuthentication(
                                                    NativeConfig.fingerprintCanceledText()
                                            );
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
                            saveBiometricEnrollment();
                            cleanupLegacyFingerprint();
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            if (activityDestroyed || isFinishing()) {
                                return;
                            }

                            Toast.makeText(
                                    AppLockActivity.this,
                                    NativeConfig.fingerprintFailedText(),
                                    Toast.LENGTH_SHORT
                            ).show();
                        }

                        @Override
                        public void onAuthenticationError(
                                int errorCode,
                                CharSequence errString
                        ) {
                            finishFingerprintAuthentication(
                                    NativeConfig.fingerprintCanceledText()
                            );
                            cleanupLegacyFingerprint();
                        }
                    },
                    null
            );
        } catch (Throwable ignored) {
            finishFingerprintAuthentication(
                    NativeConfig.fingerprintFailedText()
            );
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

    private void cleanupLegacyFingerprint() {
        if (legacyFingerprintDialog != null) {
            android.app.AlertDialog dialog = legacyFingerprintDialog;
            legacyFingerprintDialog = null;
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }
        biometricPrompt = null;
    }

    private void refreshUi() {
        SecureAppLockStore.State state =
                SecureAppLockStore.read(this);

        if (lockSwitch != null) {
            updatingSwitch = true;
            lockSwitch.setChecked(state.enabled);
            updatingSwitch = false;
        }

        if (timeoutValue != null) {
            timeoutValue.setText(timeoutText(state.timeoutSeconds));
        }

        if (biometricSwitch != null) {
            boolean available = AppLockManager.canUseFingerprint(this);
            updatingSwitch = true;
            biometricSwitch.setEnabled(
                    available && state.enabled &&
                            !fingerprintAuthenticationInProgress
            );
            biometricSwitch.setChecked(
                    available && state.biometricEnabled && state.enabled
            );
            updatingSwitch = false;
        }
    }

    private void chooseTimeout() {
        if (!SecureAppLockStore.read(this).enabled) {
            Toast.makeText(
                    this,
                    NativeConfig.appLockEnableFirstText(),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        // Keep this as a simple single-choice dialog: six practical values fit well
        // on a phone and it avoids another nested control inside App Lock.
        final String[] labels = new String[]{
                NativeConfig.timeoutImmediate(),
                NativeConfig.timeoutOneMinute(),
                NativeConfig.timeoutFiveMinutes(),
                NativeConfig.timeoutFifteenMinutes(),
                NativeConfig.timeoutThirtyMinutes(),
                NativeConfig.timeoutOneHour(),
                NativeConfig.timeoutFiveHours(),
                NativeConfig.timeoutTenHours()
        };
        final int[] values = new int[]{0, 60, 300, 900, 1800, 3600, 18000, 36000};

        SecureAppLockStore.State current =
                SecureAppLockStore.read(this);
        int selected = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current.timeoutSeconds) {
                selected = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(NativeConfig.appLockTimeoutDialogTitle())
                .setSingleChoiceItems(
                        labels,
                        selected,
                        new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(
                                    android.content.DialogInterface dialog,
                                    int which
                            ) {
                                SecureAppLockStore.State state =
                                        SecureAppLockStore.read(
                                                AppLockActivity.this
                                        );
                                state.timeoutSeconds = values[which];
                                state.lastBackgroundAtMillis = 0L;
                                if (!SecureAppLockStore.write(
                                        AppLockActivity.this,
                                        state
                                )) {
                                    Toast.makeText(
                                            AppLockActivity.this,
                                            NativeConfig.pinSaveFailedText(),
                                            Toast.LENGTH_SHORT
                                    ).show();
                                    return;
                                }
                                dialog.dismiss();
                                refreshUi();
                            }
                        }
                )
                .setNegativeButton(
                        NativeConfig.cancelText(),
                        null
                )
                .show();
    }

    private String timeoutText(int seconds) {
        if (seconds <= 0) {
            return NativeConfig.timeoutImmediate();
        }
        if (seconds == 60) {
            return NativeConfig.timeoutOneMinute();
        }
        if (seconds == 300) {
            return NativeConfig.timeoutFiveMinutes();
        }
        if (seconds == 900) {
            return NativeConfig.timeoutFifteenMinutes();
        }
        if (seconds == 1800) {
            return NativeConfig.timeoutThirtyMinutes();
        }
        if (seconds == 3600) {
            return NativeConfig.timeoutOneHour();
        }
        if (seconds == 18000) {
            return NativeConfig.timeoutFiveHours();
        }
        if (seconds == 36000) {
            return NativeConfig.timeoutTenHours();
        }
        return NativeConfig.timeoutImmediate();
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(12), 0, dp(8), 0);
        card.setBackground(round(Color.argb(26, 255, 255, 255), dp(18)));
        return card;
    }

    private TextView text(String value, float size, int color) {
        TextView view = new TextView(this);
        view.setText(TextUtils.isEmpty(value) ? "" : value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private LinearLayout.LayoutParams exact(int height) {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                height
        );
    }

    private LinearLayout.LayoutParams wrap(int height) {
        return exact(height);
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
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

    private int dp(int value) {
        return Math.round(
                value * getResources().getDisplayMetrics().density
        );
    }

    private void finishWithAnimation() {
        finish();
        overridePendingTransition(
                android.R.anim.fade_in,
                android.R.anim.fade_out
        );
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            android.content.Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PIN_SETUP || requestCode == REQUEST_PIN_CHANGE) {
            refreshUi();
        }
    }
    @Override
    protected void onDestroy() {
        activityDestroyed = true;
        fingerprintAuthenticationInProgress = false;
        biometricPrompt = null;
        super.onDestroy();
    }

}
