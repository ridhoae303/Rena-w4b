package com.rena.w4b;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class ProfileActivity extends Activity {
    private static final int REQ_ACCOUNT = 7301;
    private static final int REQ_IMAGE = 7302;
    private LinearLayout column;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(RenaLocale.wrap(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        RenaTheme.apply(this);
        super.onCreate(savedInstanceState);
        build();
        RenaTheme.applyWindow(this);
    }

    private int dp(float value) {
        return Ui.dp(this, value);
    }

    private void build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(RenaTheme.bg());
        root.setFitsSystemWindows(true);
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(8), dp(6), dp(8), dp(6));
        TextView back = Ui.text(this, NativeConfig.downloadBackSymbol(), 24f, RenaTheme.text(), Typeface.NORMAL);
        back.setGravity(Gravity.CENTER);
        back.setBackground(Ui.ripple(this, Ui.round(0, dp(24)), 24));
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        header.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));
        header.addView(Ui.text(this, getString(R.string.profile_title), 21f, RenaTheme.text(), Typeface.BOLD),
                new LinearLayout.LayoutParams(0, -2, 1f));
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));
        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(16), dp(8), dp(16), dp(32));
        scroll.addView(column, new FrameLayout.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);
        populate();
    }

    private View avatarView() {
        FrameLayout box = new FrameLayout(this);
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackground(Ui.round(RenaTheme.accentSoft(), dp(60)));
        image.setClipToOutline(true);
        image.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(View view, android.graphics.Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });
        Bitmap bitmap = UserProfileStore.avatar(this);
        if (bitmap != null) {
            image.setImageBitmap(bitmap);
        }
        box.addView(image, new FrameLayout.LayoutParams(dp(112), dp(112), Gravity.CENTER));
        if (bitmap == null) {
            String name = UserProfileStore.displayName(this);
            TextView letter = Ui.text(this, TextUtils.isEmpty(name) ? "R" : name.substring(0, 1).toUpperCase(),
                    44f, RenaTheme.accentText(), Typeface.BOLD);
            letter.setGravity(Gravity.CENTER);
            box.addView(letter, new FrameLayout.LayoutParams(dp(112), dp(112), Gravity.CENTER));
        }
        TextView edit = Ui.text(this, "\u270E", 16f, Color.WHITE, Typeface.NORMAL);
        edit.setGravity(Gravity.CENTER);
        edit.setBackground(Ui.round(RenaTheme.accent(), dp(18)));
        box.addView(edit, new FrameLayout.LayoutParams(dp(36), dp(36), Gravity.BOTTOM | Gravity.END));
        ((FrameLayout.LayoutParams) edit.getLayoutParams()).rightMargin = dp(4);
        ((FrameLayout.LayoutParams) edit.getLayoutParams()).bottomMargin = dp(2);
        box.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImage();
            }
        });
        return box;
    }

    private EditText field(String hint, String value, boolean multiline) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setHintTextColor(RenaTheme.faint());
        input.setTextColor(RenaTheme.text());
        input.setTextSize(15f);
        input.setTypeface(Ui.font(this, Typeface.NORMAL));
        input.setText(value);
        input.setInputType(multiline ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                : InputType.TYPE_CLASS_TEXT);
        input.setSingleLine(!multiline);
        if (multiline) {
            input.setMinLines(3);
            input.setGravity(Gravity.TOP | Gravity.START);
        }
        input.setBackground(Ui.roundStroke(RenaTheme.surface(), dp(16), RenaTheme.outline(), dp(1)));
        input.setPadding(dp(16), multiline ? dp(12) : 0, dp(16), multiline ? dp(12) : 0);
        return input;
    }

    private TextView caption(String text) {
        TextView view = Ui.text(this, text, 12f, RenaTheme.muted(), Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.topMargin = dp(16);
        lp.bottomMargin = dp(6);
        view.setLayoutParams(lp);
        return view;
    }

    private TextView button(String label, boolean primary, boolean danger) {
        TextView button = Ui.text(this, label, 14.5f, primary ? Color.WHITE : (danger ? RenaTheme.danger() : RenaTheme.accentText()), Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        int fill = primary ? RenaTheme.accent() : (danger ? RenaTheme.dangerSoft() : RenaTheme.accentSoft());
        button.setBackground(Ui.ripple(this, Ui.round(fill, dp(24)), 24));
        return button;
    }

    private void populate() {
        column.removeAllViews();
        LinearLayout.LayoutParams avatarLp = new LinearLayout.LayoutParams(dp(112), dp(112));
        avatarLp.gravity = Gravity.CENTER_HORIZONTAL;
        avatarLp.topMargin = dp(6);
        column.addView(avatarView(), avatarLp);

        if (!UserProfileStore.isSignedIn(this)) {
            TextView title = Ui.text(this, getString(R.string.profile_signin_title), 18f, RenaTheme.text(), Typeface.BOLD);
            title.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(-1, -2);
            titleLp.topMargin = dp(18);
            column.addView(title, titleLp);
            TextView info = Ui.text(this, getString(R.string.profile_signin_info), 13.5f, RenaTheme.muted(), Typeface.NORMAL);
            info.setGravity(Gravity.CENTER);
            info.setLineSpacing(0f, 1.25f);
            LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(-1, -2);
            infoLp.topMargin = dp(8);
            column.addView(info, infoLp);
            TextView google = button("G   " + getString(R.string.profile_choose_google), true, false);
            google.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    chooseGoogleAccount();
                }
            });
            LinearLayout.LayoutParams googleLp = new LinearLayout.LayoutParams(-1, dp(50));
            googleLp.topMargin = dp(22);
            column.addView(google, googleLp);
            TextView email = button(getString(R.string.profile_use_email), false, false);
            email.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    promptEmail();
                }
            });
            LinearLayout.LayoutParams emailLp = new LinearLayout.LayoutParams(-1, dp(50));
            emailLp.topMargin = dp(10);
            column.addView(email, emailLp);
            TextView note = Ui.text(this, getString(R.string.profile_optional_note), 12f, RenaTheme.faint(), Typeface.NORMAL);
            note.setGravity(Gravity.CENTER);
            note.setLineSpacing(0f, 1.2f);
            LinearLayout.LayoutParams noteLp = new LinearLayout.LayoutParams(-1, -2);
            noteLp.topMargin = dp(18);
            column.addView(note, noteLp);
            return;
        }

        TextView emailView = Ui.text(this, UserProfileStore.email(this), 14f, RenaTheme.muted(), Typeface.NORMAL);
        emailView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams emailLp = new LinearLayout.LayoutParams(-1, -2);
        emailLp.topMargin = dp(14);
        column.addView(emailView, emailLp);
        String providerLabel = "google".equals(UserProfileStore.provider(this))
                ? getString(R.string.profile_provider_google) : getString(R.string.profile_provider_email);
        TextView provider = Ui.text(this, providerLabel, 11.5f, RenaTheme.accentText(), Typeface.BOLD);
        provider.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams providerLp = new LinearLayout.LayoutParams(-1, -2);
        providerLp.topMargin = dp(4);
        column.addView(provider, providerLp);

        column.addView(caption(getString(R.string.profile_username)));
        final EditText username = field(getString(R.string.profile_username), UserProfileStore.username(this), false);
        column.addView(username, new LinearLayout.LayoutParams(-1, dp(52)));
        column.addView(caption(getString(R.string.profile_display_name)));
        final EditText name = field(getString(R.string.profile_display_name), UserProfileStore.displayName(this), false);
        column.addView(name, new LinearLayout.LayoutParams(-1, dp(52)));
        column.addView(caption(getString(R.string.profile_bio)));
        final EditText bio = field(getString(R.string.profile_bio), UserProfileStore.bio(this), true);
        column.addView(bio, new LinearLayout.LayoutParams(-1, -2));

        TextView save = button(getString(R.string.profile_save), true, false);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UserProfileStore.saveFields(ProfileActivity.this, username.getText().toString().trim(),
                        name.getText().toString().trim(), bio.getText().toString().trim());
                Toast.makeText(ProfileActivity.this, R.string.profile_saved, Toast.LENGTH_SHORT).show();
                populate();
            }
        });
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(-1, dp(50));
        saveLp.topMargin = dp(22);
        column.addView(save, saveLp);

        if (UserProfileStore.avatar(this) != null) {
            TextView removePhoto = button(getString(R.string.profile_remove_photo), false, false);
            removePhoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    UserProfileStore.removeAvatar(ProfileActivity.this);
                    populate();
                }
            });
            LinearLayout.LayoutParams removeLp = new LinearLayout.LayoutParams(-1, dp(46));
            removeLp.topMargin = dp(10);
            column.addView(removePhoto, removeLp);
        }

        TextView signOut = button(getString(R.string.profile_sign_out), false, true);
        signOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new RenaDialog.Builder(ProfileActivity.this)
                        .setTitle(R.string.profile_sign_out)
                        .setMessage(R.string.profile_sign_out_message)
                        .setNegativeButton(R.string.cancel_text, null)
                        .setPositiveButton(R.string.profile_sign_out, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                UserProfileStore.signOut(ProfileActivity.this);
                                populate();
                            }
                        })
                        .show();
            }
        });
        LinearLayout.LayoutParams outLp = new LinearLayout.LayoutParams(-1, dp(46));
        outLp.topMargin = dp(10);
        column.addView(signOut, outLp);

        TextView note = Ui.text(this, getString(R.string.profile_storage_note), 12f, RenaTheme.faint(), Typeface.NORMAL);
        note.setGravity(Gravity.CENTER);
        note.setLineSpacing(0f, 1.2f);
        LinearLayout.LayoutParams noteLp = new LinearLayout.LayoutParams(-1, -2);
        noteLp.topMargin = dp(18);
        column.addView(note, noteLp);
    }

    private void chooseGoogleAccount() {
        try {
            Intent intent = AccountManager.newChooseAccountIntent(null, null, new String[]{"com.google"}, null, null, null, null);
            startActivityForResult(intent, REQ_ACCOUNT);
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, R.string.profile_no_chooser, Toast.LENGTH_LONG).show();
        } catch (Throwable error) {
            Toast.makeText(this, R.string.profile_no_chooser, Toast.LENGTH_LONG).show();
        }
    }

    private void promptEmail() {
        final EditText input = field(getString(R.string.profile_email_hint), "", false);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        new RenaDialog.Builder(this)
                .setTitle(R.string.profile_use_email)
                .setView(input)
                .setNegativeButton(R.string.cancel_text, null)
                .setPositiveButton(R.string.okay_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String value = input.getText().toString().trim();
                        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
                            Toast.makeText(ProfileActivity.this, R.string.profile_email_invalid, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        UserProfileStore.signIn(ProfileActivity.this, value, "email");
                        populate();
                    }
                })
                .show();
    }

    private void chooseImage() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, REQ_IMAGE);
        } catch (Throwable error) {
            Toast.makeText(this, R.string.download_open_failed_text, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) {
            return;
        }
        if (requestCode == REQ_ACCOUNT) {
            String account = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            if (!TextUtils.isEmpty(account)) {
                UserProfileStore.signIn(this, account, "google");
                populate();
            }
        } else if (requestCode == REQ_IMAGE) {
            Uri uri = data.getData();
            if (uri != null) {
                if (UserProfileStore.saveAvatar(this, uri)) {
                    populate();
                } else {
                    Toast.makeText(this, R.string.profile_photo_failed, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
