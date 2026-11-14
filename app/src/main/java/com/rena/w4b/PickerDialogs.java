package com.rena.w4b;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public final class PickerDialogs {
    private PickerDialogs() {
    }

    public static View iconChip(Activity activity, String glyph, int iconRes) {
        FrameLayout chip = new FrameLayout(activity);
        if (iconRes != 0) {
            chip.setBackground(Ui.round(Color.WHITE, Ui.dp(activity, 12)));
            ImageView icon = new ImageView(activity);
            icon.setImageResource(iconRes);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            chip.addView(icon, new FrameLayout.LayoutParams(Ui.dp(activity, 22), Ui.dp(activity, 22), Gravity.CENTER));
        } else {
            chip.setBackground(Ui.round(RenaTheme.accentSoft(), Ui.dp(activity, 12)));
            TextView text = Ui.text(activity, glyph, 17f, RenaTheme.accentText(), Typeface.NORMAL);
            text.setGravity(Gravity.CENTER);
            chip.addView(text, new FrameLayout.LayoutParams(-1, -1));
        }
        return chip;
    }

    public static void engine(final Activity activity, final boolean first, final Runnable onChanged) {
        final SearchEngines.Engine[] engines = SearchEngines.all();
        final String currentId = SearchEngines.current(activity).id;
        final boolean[] chosen = new boolean[]{false};
        final RenaDialog[] holder = new RenaDialog[1];
        LinearLayout list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);
        for (final SearchEngines.Engine engine : engines) {
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(Ui.dp(activity, 8), 0, Ui.dp(activity, 12), 0);
            boolean selected = engine.id.equals(currentId) && SearchEngines.isChosen(activity);
            row.setBackground(Ui.ripple(activity, Ui.round(selected ? RenaTheme.accentSoft() : Color.TRANSPARENT, Ui.dp(activity, 14)), 14));
            row.addView(iconChip(activity, "", engine.icon), new LinearLayout.LayoutParams(Ui.dp(activity, 38), Ui.dp(activity, 38)));
            TextView name = Ui.text(activity, engine.name, 15f, selected ? RenaTheme.accentText() : RenaTheme.text(),
                    selected ? Typeface.BOLD : Typeface.NORMAL);
            LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0, -2, 1f);
            nameLp.leftMargin = Ui.dp(activity, 14);
            row.addView(name, nameLp);
            row.addView(Ui.text(activity, selected ? "\u25CF" : "\u25CB", 16f, selected ? RenaTheme.accentText() : RenaTheme.faint(),
                    Typeface.NORMAL), new LinearLayout.LayoutParams(-2, -2));
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    chosen[0] = true;
                    SearchEngines.choose(activity, engine.id);
                    if (holder[0] != null) {
                        holder[0].dismiss();
                    }
                    if (onChanged != null) {
                        onChanged.run();
                    }
                }
            });
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, Ui.dp(activity, 52));
            rowLp.topMargin = Ui.dp(activity, 3);
            list.addView(row, rowLp);
        }
        RenaDialog.Builder builder = new RenaDialog.Builder(activity)
                .setTitle(first ? R.string.browser_engine_first : R.string.browser_engine)
                .setView(list)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if (!chosen[0] && first && !SearchEngines.isChosen(activity)) {
                            SearchEngines.choose(activity, "google");
                            if (onChanged != null) {
                                onChanged.run();
                            }
                        }
                    }
                });
        if (first) {
            builder.setMessage(R.string.browser_engine_first_message);
        }
        holder[0] = builder.create();
        holder[0].show();
    }

    public static void dns(final Activity activity, final Runnable onChanged) {
        final DnsManager.Provider[] providers = DnsManager.providers();
        String[] labels = new String[providers.length];
        int checked = 0;
        String currentId = DnsManager.current(activity).id;
        for (int i = 0; i < providers.length; i++) {
            labels[i] = DnsManager.describe(activity, providers[i]);
            if (providers[i].id.equals(currentId)) {
                checked = i;
            }
        }
        new RenaDialog.Builder(activity)
                .setTitle(R.string.browser_dns)
                .setMessage(DnsManager.supported() ? activity.getString(R.string.browser_dns_message)
                        : activity.getString(R.string.browser_dns_unsupported))
                .setSingleChoiceItems(labels, checked, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        boolean ok = DnsManager.choose(activity, providers[which].id);
                        if (!ok) {
                            Toast.makeText(activity, R.string.browser_dns_unsupported, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(activity, activity.getString(R.string.browser_dns_applied, providers[which].label),
                                    Toast.LENGTH_SHORT).show();
                        }
                        if (onChanged != null) {
                            onChanged.run();
                        }
                    }
                })
                .show();
    }
}
