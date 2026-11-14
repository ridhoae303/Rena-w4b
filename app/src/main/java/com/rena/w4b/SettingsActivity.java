package com.rena.w4b;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.app.role.RoleManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {
    private LinearLayout column;
    private ScrollView scroll;

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

    @Override
    protected void onResume() {
        super.onResume();
        if (column != null) {
            populate();
        }
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
        header.addView(Ui.text(this, getString(R.string.settings_title), 22f, RenaTheme.text(), Typeface.BOLD),
                new LinearLayout.LayoutParams(0, -2, 1f));
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));
        scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(16), dp(4), dp(16), dp(36));
        scroll.addView(column, new FrameLayout.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);
        populate();
    }

    private void bumpEpoch() {
        RenaPrefs.setLng(this, "ui_epoch", System.currentTimeMillis());
    }

    private void section(String title) {
        TextView view = Ui.sectionTitle(this, title);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.topMargin = dp(20);
        lp.bottomMargin = dp(8);
        column.addView(view, lp);
    }

    private LinearLayout rowBase(String glyph, int iconRes, String title, String subtitle) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        row.setBackground(Ui.pressable(this, RenaTheme.surface(), 18));
        row.addView(PickerDialogs.iconChip(this, glyph, iconRes), new LinearLayout.LayoutParams(dp(40), dp(40)));
        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        TextView t = Ui.text(this, title, 15f, RenaTheme.text(), Typeface.BOLD);
        text.addView(t, new LinearLayout.LayoutParams(-2, -2));
        if (!TextUtils.isEmpty(subtitle)) {
            TextView s = Ui.text(this, subtitle, 12.5f, RenaTheme.muted(), Typeface.NORMAL);
            s.setLineSpacing(0f, 1.15f);
            LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(-2, -2);
            sLp.topMargin = dp(3);
            text.addView(s, sLp);
        }
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, -2, 1f);
        textLp.leftMargin = dp(14);
        row.addView(text, textLp);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(8);
        column.addView(row, lp);
        return row;
    }

    private void row(String glyph, int iconRes, String title, String subtitle, View.OnClickListener click) {
        LinearLayout row = rowBase(glyph, iconRes, title, subtitle);
        row.addView(Ui.text(this, "\u203A", 22f, RenaTheme.faint(), Typeface.NORMAL), new LinearLayout.LayoutParams(-2, -2));
        row.setOnClickListener(click);
    }

    private interface Toggle {
        void changed(boolean value);
    }

    private void toggle(String glyph, String title, String subtitle, boolean checked, final Toggle toggle) {
        final LinearLayout row = rowBase(glyph, 0, title, subtitle);
        final Switch control = Ui.toggle(this, checked);
        control.setClickable(false);
        row.addView(control, new LinearLayout.LayoutParams(-2, -2));
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean next = !control.isChecked();
                control.setChecked(next);
                toggle.changed(next);
            }
        });
    }

    private void populate() {
        final int scrollY = scroll == null ? 0 : scroll.getScrollY();
        column.removeAllViews();

        LinearLayout profile = new LinearLayout(this);
        profile.setOrientation(LinearLayout.HORIZONTAL);
        profile.setGravity(Gravity.CENTER_VERTICAL);
        profile.setPadding(dp(14), dp(14), dp(14), dp(14));
        profile.setBackground(Ui.pressable(this, RenaTheme.surface(), 20));
        ImageView avatar = new ImageView(this);
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatar.setBackground(Ui.round(RenaTheme.accentSoft(), dp(28)));
        avatar.setClipToOutline(true);
        avatar.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(View view, android.graphics.Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });
        Bitmap bitmap = UserProfileStore.avatar(this);
        FrameLayout avatarBox = new FrameLayout(this);
        if (bitmap != null) {
            avatar.setImageBitmap(bitmap);
        }
        avatarBox.addView(avatar, new FrameLayout.LayoutParams(dp(56), dp(56)));
        if (bitmap == null) {
            TextView letter = Ui.text(this, "R", 22f, RenaTheme.accentText(), Typeface.BOLD);
            letter.setGravity(Gravity.CENTER);
            avatarBox.addView(letter, new FrameLayout.LayoutParams(dp(56), dp(56)));
        }
        profile.addView(avatarBox, new LinearLayout.LayoutParams(dp(56), dp(56)));
        LinearLayout profileText = new LinearLayout(this);
        profileText.setOrientation(LinearLayout.VERTICAL);
        String name = UserProfileStore.displayName(this);
        profileText.addView(Ui.text(this, UserProfileStore.isSignedIn(this) && !TextUtils.isEmpty(name) ? name : getString(R.string.profile_title),
                17f, RenaTheme.text(), Typeface.BOLD), new LinearLayout.LayoutParams(-2, -2));
        TextView profileSub = Ui.text(this, UserProfileStore.isSignedIn(this) ? UserProfileStore.email(this)
                : getString(R.string.profile_not_signed_in), 12.5f, RenaTheme.muted(), Typeface.NORMAL);
        LinearLayout.LayoutParams profileSubLp = new LinearLayout.LayoutParams(-2, -2);
        profileSubLp.topMargin = dp(3);
        profileText.addView(profileSub, profileSubLp);
        LinearLayout.LayoutParams profileTextLp = new LinearLayout.LayoutParams(0, -2, 1f);
        profileTextLp.leftMargin = dp(14);
        profile.addView(profileText, profileTextLp);
        profile.addView(Ui.text(this, "\u203A", 22f, RenaTheme.faint(), Typeface.NORMAL), new LinearLayout.LayoutParams(-2, -2));
        profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SettingsActivity.this, ProfileActivity.class));
            }
        });
        column.addView(profile, new LinearLayout.LayoutParams(-1, -2));

        section(getString(R.string.settings_section_appearance));
        row("\uD83C\uDFA8", 0, getString(R.string.settings_theme), RenaTheme.name(this, RenaTheme.mode(this)), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseTheme();
            }
        });
        row("\uD83C\uDF0D", 0, getString(R.string.settings_language), RenaLocale.label(this, RenaLocale.getLanguage(this)), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseLanguage();
            }
        });
        row("\uD83D\uDDBC", 0, getString(R.string.settings_icon), IconSwitcher.label(this, IconSwitcher.current(this)), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseIcon();
            }
        });

        section(getString(R.string.browser_name));
        SearchEngines.Engine engine = SearchEngines.current(this);
        row("", engine.icon, getString(R.string.browser_engine), engine.name, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PickerDialogs.engine(SettingsActivity.this, false, new Runnable() {
                    @Override
                    public void run() {
                        populate();
                    }
                });
            }
        });
        row("\uD83C\uDF10", 0, getString(R.string.settings_default_browser), defaultBrowserStatus(), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestDefaultBrowser();
            }
        });
        toggle("\uD83D\uDDA5", getString(R.string.browser_desktop), getString(R.string.browser_desktop_sub), RenaPrefs.desktop(this), new Toggle() {
            @Override
            public void changed(boolean value) {
                RenaPrefs.setBool(SettingsActivity.this, RenaPrefs.BR_DESKTOP, value);
                bumpEpoch();
            }
        });
        toggle("\uD83D\uDEE1", getString(R.string.browser_adblock), getString(R.string.browser_adblock_sub), RenaPrefs.adblock(this), new Toggle() {
            @Override
            public void changed(boolean value) {
                RenaPrefs.setBool(SettingsActivity.this, RenaPrefs.BR_ADBLOCK, value);
            }
        });
        toggle("\uD83D\uDC41", getString(R.string.browser_tracking), getString(R.string.browser_tracking_sub), RenaPrefs.tracking(this), new Toggle() {
            @Override
            public void changed(boolean value) {
                RenaPrefs.setBool(SettingsActivity.this, RenaPrefs.BR_TRACKING, value);
            }
        });
        toggle("\uD83D\uDEAB", getString(R.string.browser_detector), getString(R.string.browser_detector_sub), RenaPrefs.detector(this), new Toggle() {
            @Override
            public void changed(boolean value) {
                RenaPrefs.setBool(SettingsActivity.this, RenaPrefs.BR_DETECTOR, value);
            }
        });
        toggle("\uD83D\uDD12", getString(R.string.browser_https_only), getString(R.string.browser_https_only_sub), RenaPrefs.httpsOnly(this), new Toggle() {
            @Override
            public void changed(boolean value) {
                RenaPrefs.setBool(SettingsActivity.this, RenaPrefs.BR_HTTPS_ONLY, value);
            }
        });
        row("\uD83E\uDDED", 0, getString(R.string.browser_dns), DnsManager.describe(this, DnsManager.current(this)), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PickerDialogs.dns(SettingsActivity.this, new Runnable() {
                    @Override
                    public void run() {
                        populate();
                    }
                });
            }
        });
        toggle("\uD83D\uDCF4", getString(R.string.offline_mode_title), getString(R.string.offline_mode_sub), RenaPrefs.offlineMode(this), new Toggle() {
            @Override
            public void changed(boolean value) {
                RenaPrefs.setBool(SettingsActivity.this, RenaPrefs.OFFLINE_MODE, value);
                bumpEpoch();
            }
        });

        section(getString(R.string.settings_section_whatsapp));
        row("\uD83D\uDCF1", 0, getString(R.string.device_title),
                DeviceProfiles.current(this).label + "  \u00B7  " + DeviceProfiles.currentRatio(this).label, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DeviceDialog.show(SettingsActivity.this, new Runnable() {
                            @Override
                            public void run() {
                                bumpEpoch();
                                populate();
                            }
                        });
                    }
                });
        toggle("\uD83D\uDDD1", getString(R.string.antidelete_toggle), getString(R.string.settings_antidelete_sub),
                RenaPrefs.bool(this, RenaPrefs.ANTI_DELETE, true), new Toggle() {
                    @Override
                    public void changed(boolean value) {
                        RenaPrefs.setBool(SettingsActivity.this, RenaPrefs.ANTI_DELETE, value);
                        bumpEpoch();
                    }
                });
        row("\uD83D\uDCDC", 0, getString(R.string.deleted_messages_title), getString(R.string.menu_deleted_sub), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SettingsActivity.this, DeletedMessagesActivity.class));
            }
        });
        toggle("\uD83D\uDCBE", getString(R.string.status_saver_toggle), getString(R.string.settings_status_sub),
                RenaPrefs.bool(this, RenaPrefs.STATUS_SAVER, false), new Toggle() {
                    @Override
                    public void changed(boolean value) {
                        RenaPrefs.setBool(SettingsActivity.this, RenaPrefs.STATUS_SAVER, value);
                        RenaPrefs.setBool(SettingsActivity.this, RenaPrefs.STATUS_AUTO, value);
                        bumpEpoch();
                    }
                });

        section(getString(R.string.settings_section_updates));
        toggle("\u21BB", getString(R.string.settings_auto_update), getString(R.string.settings_auto_update_sub), RenaPrefs.autoUpdate(this), new Toggle() {
            @Override
            public void changed(boolean value) {
                if (value) {
                    RenaPrefs.setBool(SettingsActivity.this, RenaPrefs.AUTO_UPDATE, true);
                } else {
                    confirmDisableAutoUpdate();
                }
            }
        });
        row("\u2B06", 0, getString(R.string.check_updates_text), versionLabel(), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                intent.putExtra("check_updates", true);
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });

        section(getString(R.string.settings_section_privacy));
        row("\uD83D\uDD10", 0, getString(R.string.app_lock_text), getString(R.string.settings_lock_sub), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SettingsActivity.this, AppLockActivity.class));
            }
        });
        row("\uD83D\uDDC2", 0, getString(R.string.data_storage_text), getString(R.string.settings_data_sub), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent dataIntent = new Intent(SettingsActivity.this, DataManagerActivity.class);
                try {
                    dataIntent.putExtra("tab_id", Integer.parseInt(RenaPrefs.str(SettingsActivity.this, "active_tab_id", "1")));
                } catch (Throwable ignored) {
                    dataIntent.putExtra("tab_id", 1);
                }
                String profile = RenaPrefs.str(SettingsActivity.this, "active_profile_id", "");
                if (profile.length() > 0) {
                    dataIntent.putExtra("profile_id", profile);
                }
                startActivity(dataIntent);
            }
        });
        row("\u2B07", 0, getString(R.string.downloads_title), getString(R.string.browser_downloads_sub), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SettingsActivity.this, DownloadActivity.class));
            }
        });

        section(getString(R.string.settings_section_about));
        row("\uD83D\uDC65", 0, getString(R.string.team_developer_text), NativeConfig.developerName(), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SettingsActivity.this, TeamDeveloperActivity.class));
            }
        });
        row("\u2B50", 0, NativeConfig.menuGithub(), NativeConfig.githubUrl(), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                open(NativeConfig.githubUrl());
            }
        });
        row("\u2708", 0, NativeConfig.menuTelegram(), NativeConfig.telegramUrl(), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                open(NativeConfig.telegramUrl());
            }
        });
        TextView footer = Ui.text(this, getString(R.string.footer_text), 12.5f, RenaTheme.faint(), Typeface.NORMAL);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerLp = new LinearLayout.LayoutParams(-1, -2);
        footerLp.topMargin = dp(20);
        column.addView(footer, footerLp);

        if (scroll != null) {
            scroll.post(new Runnable() {
                @Override
                public void run() {
                    scroll.scrollTo(0, scrollY);
                }
            });
        }
    }

    private String defaultBrowserStatus() {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                RoleManager manager = (RoleManager) getSystemService(Context.ROLE_SERVICE);
                if (manager != null && manager.isRoleAvailable(RoleManager.ROLE_BROWSER)) {
                    return manager.isRoleHeld(RoleManager.ROLE_BROWSER)
                            ? getString(R.string.settings_default_browser_yes) : getString(R.string.settings_default_browser_no);
                }
            }
        } catch (Throwable ignored) {
        }
        return getString(R.string.settings_default_browser_open);
    }

    private void requestDefaultBrowser() {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                RoleManager manager = (RoleManager) getSystemService(Context.ROLE_SERVICE);
                if (manager != null && manager.isRoleAvailable(RoleManager.ROLE_BROWSER)) {
                    if (manager.isRoleHeld(RoleManager.ROLE_BROWSER)) {
                        Toast.makeText(this, R.string.settings_default_browser_yes, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    startActivityForResult(manager.createRequestRoleIntent(RoleManager.ROLE_BROWSER), 7401);
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            startActivity(new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS));
        } catch (Throwable ignored) {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (Throwable ignoredAgain) {
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 7401 && column != null) {
            populate();
        }
    }

    private void open(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Throwable ignored) {
        }
    }

    private String versionLabel() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            return getString(R.string.settings_version, info.versionName);
        } catch (Throwable ignored) {
            return "";
        }
    }

    private void confirmDisableAutoUpdate() {
        new RenaDialog.Builder(this)
                .setTitle(R.string.settings_auto_update_off_title)
                .setMessage(R.string.settings_auto_update_off_message)
                .setNegativeButton(R.string.cancel_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        populate();
                    }
                })
                .setPositiveButton(R.string.settings_turn_off, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        RenaPrefs.setBool(SettingsActivity.this, RenaPrefs.AUTO_UPDATE, false);
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        populate();
                    }
                })
                .show();
    }

    private void chooseTheme() {
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        final RenaDialog[] holder = new RenaDialog[1];
        int current = RenaTheme.mode(this);
        int[] modes = new int[]{RenaTheme.LIGHT, RenaTheme.DARK, RenaTheme.AMOLED, RenaTheme.PURE_BLACK};
        int[] swatchBg = new int[]{Color.rgb(245, 247, 248), Color.rgb(17, 27, 33), Color.BLACK, Color.BLACK};
        int[] swatchSurface = new int[]{Color.WHITE, Color.rgb(29, 43, 51), Color.rgb(11, 15, 18), Color.BLACK};
        String[] hints = new String[]{getString(R.string.theme_light_sub), getString(R.string.theme_dark_sub),
                getString(R.string.theme_amoled_sub), getString(R.string.theme_pure_sub)};
        for (int i = 0; i < modes.length; i++) {
            final int mode = modes[i];
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(10), 0, dp(12), 0);
            boolean selected = mode == current;
            row.setBackground(Ui.ripple(this, Ui.round(selected ? RenaTheme.accentSoft() : Color.TRANSPARENT, dp(16)), 16));
            FrameLayout swatch = new FrameLayout(this);
            swatch.setBackground(Ui.roundStroke(swatchBg[i], dp(12), Color.argb(90, 128, 128, 128), dp(1)));
            View inner = new View(this);
            inner.setBackground(Ui.roundStroke(swatchSurface[i], dp(6), mode == RenaTheme.PURE_BLACK ? Color.rgb(70, 70, 70) : 0,
                    mode == RenaTheme.PURE_BLACK ? dp(1) : 0));
            swatch.addView(inner, new FrameLayout.LayoutParams(dp(22), dp(14), Gravity.CENTER));
            row.addView(swatch, new LinearLayout.LayoutParams(dp(44), dp(44)));
            LinearLayout text = new LinearLayout(this);
            text.setOrientation(LinearLayout.VERTICAL);
            text.addView(Ui.text(this, RenaTheme.name(this, mode), 15f, selected ? RenaTheme.accentText() : RenaTheme.text(),
                    selected ? Typeface.BOLD : Typeface.NORMAL), new LinearLayout.LayoutParams(-2, -2));
            LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(-2, -2);
            hintLp.topMargin = dp(2);
            text.addView(Ui.text(this, hints[i], 11.5f, RenaTheme.muted(), Typeface.NORMAL), hintLp);
            LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, -2, 1f);
            textLp.leftMargin = dp(14);
            row.addView(text, textLp);
            row.addView(Ui.text(this, selected ? "\u25CF" : "\u25CB", 16f, selected ? RenaTheme.accentText() : RenaTheme.faint(), Typeface.NORMAL),
                    new LinearLayout.LayoutParams(-2, -2));
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    holder[0].dismiss();
                    if (mode != RenaTheme.mode(SettingsActivity.this)) {
                        RenaTheme.setMode(SettingsActivity.this, mode);
                        bumpEpoch();
                        recreate();
                    }
                }
            });
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, dp(60));
            rowLp.topMargin = dp(4);
            list.addView(row, rowLp);
        }
        holder[0] = new RenaDialog.Builder(this).setTitle(R.string.settings_theme).setView(list).create();
        holder[0].show();
    }

    private void chooseLanguage() {
        final String[] codes = new String[]{RenaLocale.LANG_ID, RenaLocale.LANG_EN};
        String[] labels = new String[]{"Indonesia", "English"};
        int checked = RenaLocale.isIndonesian(this) ? 0 : 1;
        new RenaDialog.Builder(this)
                .setTitle(R.string.settings_language)
                .setSingleChoiceItems(labels, checked, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!codes[which].equals(RenaLocale.getLanguage(SettingsActivity.this))) {
                            RenaLocale.setLanguage(SettingsActivity.this, codes[which]);
                            bumpEpoch();
                            recreate();
                        }
                    }
                })
                .show();
    }

    private void chooseIcon() {
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        final RenaDialog[] holder = new RenaDialog[1];
        String current = IconSwitcher.current(this);
        String[] ids = IconSwitcher.IDS;
        for (int r = 0; r < 2; r++) {
            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);
            for (int c = 0; c < 3; c++) {
                final String id = ids[r * 3 + c];
                boolean selected = id.equals(current);
                LinearLayout cell = new LinearLayout(this);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.CENTER_HORIZONTAL);
                cell.setPadding(dp(4), dp(10), dp(4), dp(8));
                cell.setBackground(Ui.ripple(this, Ui.roundStroke(selected ? RenaTheme.accentSoft() : Color.TRANSPARENT, dp(16),
                        selected ? RenaTheme.accent() : 0, selected ? dp(2) : 0), 16));
                ImageView icon = new ImageView(this);
                Bitmap preview = IconSwitcher.preview(this, id, dp(64));
                if (preview != null) {
                    icon.setImageBitmap(preview);
                }
                cell.addView(icon, new LinearLayout.LayoutParams(dp(60), dp(60)));
                TextView label = Ui.text(this, IconSwitcher.label(this, id), 12.5f, selected ? RenaTheme.accentText() : RenaTheme.text(),
                        selected ? Typeface.BOLD : Typeface.NORMAL);
                label.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(-2, -2);
                labelLp.topMargin = dp(8);
                cell.addView(label, labelLp);
                cell.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        holder[0].dismiss();
                        IconSwitcher.apply(SettingsActivity.this, id);
                        Toast.makeText(SettingsActivity.this, R.string.settings_icon_changed, Toast.LENGTH_LONG).show();
                        populate();
                    }
                });
                LinearLayout.LayoutParams cellLp = new LinearLayout.LayoutParams(0, -2, 1f);
                cellLp.setMargins(dp(3), dp(3), dp(3), dp(3));
                line.addView(cell, cellLp);
            }
            grid.addView(line, new LinearLayout.LayoutParams(-1, -2));
        }
        holder[0] = new RenaDialog.Builder(this).setTitle(R.string.settings_icon).setView(grid).create();
        holder[0].show();
    }
}
