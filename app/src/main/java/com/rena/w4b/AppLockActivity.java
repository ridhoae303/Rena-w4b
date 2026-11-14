package com.rena.w4b;

import android.app.Activity;
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

    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(RenaLocale.wrap(base));
    }
    private Switch lockSwitch;
    private Switch biometricSwitch;
    private TextView timeoutValue;
    private boolean updatingSwitch;
    private boolean fingerprintAuthenticationInProgress;
    private Object biometricPrompt;
    private RenaDialog legacyFingerprintDialog;
    private boolean activityDestroyed;

    private static final int REQUEST_PIN_SETUP = 4101;
    private static final int REQUEST_PIN_CHANGE = 4102;

    @Override
    protected void onCreate(Bundle state) {
        RenaTheme.apply(this);
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
        window.setNavigationBarColor(RenaTheme.bg());
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
        root.setBackgroundColor(RenaTheme.bg());

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
                RenaTheme.text()
        );
        back.setGravity(Gravity.CENTER);
        back.setContentDescription(getString(R.string.app_lock_back_text));
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishWithAnimation();
            }
        });
        header.addView(back, new LinearLayout.LayoutParams(dp(52), dp(52)));

        TextView title = text(
                getString(R.string.app_lock_title),
                22,
                RenaTheme.text()
        );
        title.setTypeface(loadFont(android.graphics.Typeface.BOLD));
        header.addView(title, new LinearLayout.LayoutParams(0, dp(52), 1f));

        root.addView(header, exact(dp(56)));

        TextView heroTitle = text(
                getString(R.string.app_lock_hero_title),
                25,
                RenaTheme.text()
        );
        heroTitle.setTypeface(loadFont(android.graphics.Typeface.BOLD));
        root.addView(heroTitle, wrap(dp(56)));

        TextView heroMessage = text(
                getString(R.string.app_lock_hero_message),
                14,
                RenaTheme.fg(180)
        );
        heroMessage.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(heroMessage, wrap(dp(58)));

        LinearLayout lockCard = card();
        TextView lockLabel = text(
                getString(R.string.app_lock_switch_text),
                16,
                RenaTheme.text()
        );
        lockLabel.setTypeface(loadFont(android.graphics.Typeface.BOLD));
        lockCard.addView(lockLabel, new LinearLayout.LayoutParams(0, dp(58), 1f));

        lockSwitch = new Switch(this);
        lockSwitch.setContentDescription(
                getString(R.string.app_lock_switch_text)
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
                                        getString(R.string.pin_save_failed_text),
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
                                    getString(R.string.pin_save_failed_text),
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
                getString(R.string.change_pin_label),
                15,
                RenaTheme.fg(180)
        );
        changePinLabel.setTypeface(loadFont(android.graphics.Typeface.BOLD));
        changePinLabel.setPadding(dp(4), dp(18), 0, 0);
        root.addView(changePinLabel, wrap(dp(42)));

        LinearLayout changePinCard = card();
        TextView changePinLeft = text(
                getString(R.string.change_pin_text),
                15,
                RenaTheme.text()
        );
        changePinCard.addView(
                changePinLeft,
                new LinearLayout.LayoutParams(0, dp(58), 1f)
        );
        TextView changePinArrow = text(
                NativeConfig.changePinArrow(),
                24,
                RenaTheme.accentText()
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
                            getString(R.string.app_lock_enable_first_text),
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
                getString(R.string.app_lock_timeout_label),
                15,
                RenaTheme.fg(180)
        );
        timeoutLabel.setTypeface(loadFont(android.graphics.Typeface.BOLD));
        timeoutLabel.setPadding(dp(4), dp(18), 0, 0);
        root.addView(timeoutLabel, wrap(dp(42)));

        LinearLayout timeoutCard = card();
        TextView timeoutLeft = text(
                getString(R.string.app_lock_timeout_text),
                15,
                RenaTheme.text()
        );
        timeoutCard.addView(timeoutLeft, new LinearLayout.LayoutParams(0, dp(58), 1f));

        timeoutValue = text("", 14, RenaTheme.accentText());
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
                getString(R.string.app_lock_fingerprint_text),
                15,
                RenaTheme.fg(180)
        );
        biometricLabel.setTypeface(loadFont(android.graphics.Typeface.BOLD));
        biometricLabel.setPadding(dp(4), dp(18), 0, 0);
        root.addView(biometricLabel, wrap(dp(42)));

        LinearLayout biometricCard = card();
        boolean fingerprintAvailable =
                AppLockManager.canUseFingerprint(this);
        TextView biometricLeft = text(
                fingerprintAvailable
                        ? getString(R.string.app_lock_fingerprint_text)
                        : getString(R.string.app_lock_fingerprint_unavailable),
                15,
                RenaTheme.text()
        );
        biometricCard.addView(biometricLeft, new LinearLayout.LayoutParams(0, dp(58), 1f));

        biometricSwitch = new Switch(this);
        biometricSwitch.setContentDescription(
                getString(R.string.app_lock_fingerprint_text)
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
                getString(R.string.app_lock_security_note),
                12,
                RenaTheme.fg(150)
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
                    getString(R.string.app_lock_enable_first_text),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (!AppLockManager.canUseFingerprint(this)) {
            Toast.makeText(
                    this,
                    getString(R.string.app_lock_fingerprint_unavailable),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (state.biometricEnabled) {
            state.biometricEnabled = false;
            if (!SecureAppLockStore.write(this, state)) {
                Toast.makeText(
                        this,
                        getString(R.string.pin_save_failed_text),
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
                    getString(R.string.app_lock_fingerprint_unavailable),
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
                            .setTitle(getString(R.string.biometric_enable_title))
                            .setSubtitle(getString(R.string.biometric_enable_subtitle))
                            .setNegativeButton(
                                    getString(R.string.cancel_text),
                                    createMainThreadExecutor(),
                                    new android.content.DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(
                                                android.content.DialogInterface dialog,
                                                int which
                                        ) {
                                            finishFingerprintAuthentication(
                                                    getString(R.string.fingerprint_canceled_text)
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
                                    getString(R.string.fingerprint_failed_text),
                                    Toast.LENGTH_SHORT
                            ).show();
                        }

                        @Override
                        public void onAuthenticationError(
                                int errorCode,
                                CharSequence errString
                        ) {
                            finishFingerprintAuthentication(
                                    getString(R.string.fingerprint_canceled_text)
                            );
                        }
                    }
            );
        } catch (Throwable ignored) {
            finishFingerprintAuthentication(
                    getString(R.string.fingerprint_failed_text)
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
                    getString(R.string.pin_save_failed_text)
            );
            return;
        }

        finishFingerprintAuthentication(null);
    }

    private void finishFingerprintAuthentication(String message) {
        fingerprintAuthenticationInProgress = false;
        biometricPrompt = null;

        if (legacyFingerprintDialog != null) {
            RenaDialog dialog = legacyFingerprintDialog;
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
                        getString(R.string.app_lock_fingerprint_unavailable)
                );
                return;
            }

            legacyFingerprintDialog =
                    new RenaDialog.Builder(this)
                            .setTitle(getString(R.string.biometric_enable_title))
                            .setMessage(getString(R.string.fingerprint_touch_unlock_text))
                            .setNegativeButton(
                                    getString(R.string.cancel_text),
                                    new android.content.DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(
                                                android.content.DialogInterface dialog,
                                                int which
                                        ) {
                                            finishFingerprintAuthentication(
                                                    getString(R.string.fingerprint_canceled_text)
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
                                    getString(R.string.fingerprint_failed_text),
                                    Toast.LENGTH_SHORT
                            ).show();
                        }

                        @Override
                        public void onAuthenticationError(
                                int errorCode,
                                CharSequence errString
                        ) {
                            finishFingerprintAuthentication(
                                    getString(R.string.fingerprint_canceled_text)
                            );
                            cleanupLegacyFingerprint();
                        }
                    },
                    null
            );
        } catch (Throwable ignored) {
            finishFingerprintAuthentication(
                    getString(R.string.fingerprint_failed_text)
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
            RenaDialog dialog = legacyFingerprintDialog;
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
                    getString(R.string.app_lock_enable_first_text),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        final String[] labels = new String[]{
                getString(R.string.timeout_immediate),
                getString(R.string.timeout_one_minute),
                getString(R.string.timeout_five_minutes),
                getString(R.string.timeout_fifteen_minutes),
                getString(R.string.timeout_thirty_minutes),
                getString(R.string.timeout_one_hour),
                getString(R.string.timeout_five_hours),
                getString(R.string.timeout_ten_hours)
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

        new RenaDialog.Builder(this)
                .setTitle(getString(R.string.app_lock_timeout_dialog_title))
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
                                            getString(R.string.pin_save_failed_text),
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
                        getString(R.string.cancel_text),
                        null
                )
                .show();
    }

    private String timeoutText(int seconds) {
        if (seconds <= 0) {
            return getString(R.string.timeout_immediate);
        }
        if (seconds == 60) {
            return getString(R.string.timeout_one_minute);
        }
        if (seconds == 300) {
            return getString(R.string.timeout_five_minutes);
        }
        if (seconds == 900) {
            return getString(R.string.timeout_fifteen_minutes);
        }
        if (seconds == 1800) {
            return getString(R.string.timeout_thirty_minutes);
        }
        if (seconds == 3600) {
            return getString(R.string.timeout_one_hour);
        }
        if (seconds == 18000) {
            return getString(R.string.timeout_five_hours);
        }
        if (seconds == 36000) {
            return getString(R.string.timeout_ten_hours);
        }
        return getString(R.string.timeout_immediate);
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(12), 0, dp(8), 0);
        card.setBackground(round(RenaTheme.fg(26), dp(18)));
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
        if (RenaTheme.strokeWidthDp() > 0 && Color.alpha(color) < 60) {
            drawable.setStroke((int) (getResources().getDisplayMetrics().density), RenaTheme.outline());
        }
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
