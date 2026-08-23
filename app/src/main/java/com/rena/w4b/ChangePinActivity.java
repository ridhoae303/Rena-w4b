package com.rena.w4b;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Small, self-contained PIN replacement screen. It never touches WebView state. */
public class ChangePinActivity extends Activity {
    private EditText currentPin;
    private EditText newPin;
    private EditText confirmPin;
    private boolean saving;
    private boolean activityDestroyed;
    private final ExecutorService saveExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        configureWindow();
        buildUi();
    }

    private void configureWindow() {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
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
        root.setPadding(dp(28), dp(36), dp(28), dp(36));
        root.setBackground(createGradient());

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
                finish();
            }
        });
        header.addView(back, new LinearLayout.LayoutParams(dp(52), dp(52)));

        TextView title = text(
                NativeConfig.changePinLabel(),
                27,
                Color.WHITE
        );
        title.setTypeface(TypefaceHelper.getAppFont(this, android.graphics.Typeface.BOLD));
        header.addView(title, new LinearLayout.LayoutParams(0, dp(52), 1f));
        root.addView(header, wrapHeight(dp(58)));

        TextView message = text(
                NativeConfig.newPinMessage(),
                14,
                Color.argb(230, 255, 255, 255)
        );
        message.setGravity(Gravity.CENTER);
        root.addView(message, wrapHeight(dp(68)));

        currentPin = pinField(NativeConfig.currentPinHint());
        root.addView(currentPin, wrapHeight(dp(56)));

        View gap1 = new View(this);
        root.addView(gap1, wrapHeight(dp(12)));

        newPin = pinField(NativeConfig.pinHint());
        root.addView(newPin, wrapHeight(dp(56)));

        View gap2 = new View(this);
        root.addView(gap2, wrapHeight(dp(12)));

        confirmPin = pinField(NativeConfig.confirmPinHint());
        root.addView(confirmPin, wrapHeight(dp(56)));

        TextView save = button(
                NativeConfig.savePinText(),
                Color.rgb(31, 108, 81)
        );
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changePin();
            }
        });
        LinearLayout.LayoutParams saveLp = wrapHeight(dp(54));
        saveLp.topMargin = dp(24);
        root.addView(save, saveLp);

        scroll.addView(root);
        setContentView(scroll);
    }

    private void changePin() {
        if (saving) {
            return;
        }

        final SecureAppLockStore.State state =
                SecureAppLockStore.read(this);
        if (!state.enabled || !SecureAppLockStore.hasCredentials(state)) {
            Toast.makeText(
                    this,
                    NativeConfig.appLockEnableFirstText(),
                    Toast.LENGTH_SHORT
            ).show();
            finish();
            return;
        }

        final String current = currentPin == null
                ? ""
                : currentPin.getText().toString();
        final String replacement = newPin == null
                ? ""
                : newPin.getText().toString();
        final String confirm = confirmPin == null
                ? ""
                : confirmPin.getText().toString();

        if (current.length() != 4 || !current.matches("\\d{4}")) {
            Toast.makeText(
                    this,
                    NativeConfig.currentPinInvalidText(),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (replacement.length() != 4 || !replacement.matches("\\d{4}")) {
            Toast.makeText(
                    this,
                    NativeConfig.pinLengthText(),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (!replacement.equals(confirm)) {
            Toast.makeText(
                    this,
                    NativeConfig.pinMismatchText(),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        saving = true;
        final SecureAppLockStore.State snapshot = state.copy();

        saveExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final int result;
                if (!SecureAppLockStore.verifyPin(current, snapshot)) {
                    result = 1;
                } else {
                    byte[] salt = SecureAppLockStore.randomBytes(16);
                    byte[] hash = SecureAppLockStore.hashPin(replacement, salt);
                    if (hash == null) {
                        result = 2;
                    } else {
                        snapshot.salt = salt;
                        snapshot.pinHash = hash;
                        snapshot.lastBackgroundAtMillis = 0L;
                        result = SecureAppLockStore.write(
                                ChangePinActivity.this.getApplicationContext(),
                                snapshot
                        ) ? 0 : 2;
                    }
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        saving = false;
                        if (activityDestroyed || isFinishing()) {
                            return;
                        }

                        if (result == 1) {
                            Toast.makeText(
                                    ChangePinActivity.this,
                                    NativeConfig.currentPinInvalidText(),
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }

                        if (result != 0) {
                            Toast.makeText(
                                    ChangePinActivity.this,
                                    NativeConfig.pinSaveFailedText(),
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }

                        Toast.makeText(
                                ChangePinActivity.this,
                                NativeConfig.pinChangedText(),
                                Toast.LENGTH_SHORT
                        ).show();
                        setResult(RESULT_OK);
                        finish();
                        overridePendingTransition(
                                android.R.anim.fade_in,
                                android.R.anim.fade_out
                        );
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        activityDestroyed = true;
        saveExecutor.shutdownNow();
        super.onDestroy();
    }

    private EditText pinField(String hint) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.argb(180, 255, 255, 255));
        input.setHint(hint);
        input.setGravity(Gravity.CENTER);
        input.setTextSize(18);
        input.setInputType(
                InputType.TYPE_CLASS_NUMBER |
                        InputType.TYPE_NUMBER_VARIATION_PASSWORD
        );
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        input.setPadding(dp(16), 0, dp(16), 0);
        input.setBackground(round(Color.argb(50, 255, 255, 255), dp(18)));
        return input;
    }

    private TextView button(String value, int backgroundColor) {
        TextView view = text(value, 15, Color.WHITE);
        view.setGravity(Gravity.CENTER);
        view.setTypeface(TypefaceHelper.getAppFont(this, android.graphics.Typeface.BOLD));
        view.setBackground(round(backgroundColor, dp(18)));
        return view;
    }

    private TextView text(String value, float size, int color) {
        TextView view = new TextView(this);
        view.setText(TextUtils.isEmpty(value) ? "" : value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private LinearLayout.LayoutParams wrapHeight(int height) {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                height
        );
    }

    private GradientDrawable createGradient() {
        return new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{
                        Color.rgb(135, 231, 183),
                        Color.rgb(60, 163, 124),
                        Color.rgb(23, 93, 72)
                }
        );
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class TypefaceHelper {
        static android.graphics.Typeface getAppFont(Activity activity, int style) {
            try {
                int id = activity.getResources().getIdentifier(
                        "font",
                        "font",
                        activity.getPackageName()
                );
                if (id != 0 && android.os.Build.VERSION.SDK_INT >= 26) {
                    return activity.getResources().getFont(id);
                }
            } catch (Throwable ignored) {
            }
            return android.graphics.Typeface.create(
                    android.graphics.Typeface.DEFAULT,
                    style
            );
        }
    }
}
