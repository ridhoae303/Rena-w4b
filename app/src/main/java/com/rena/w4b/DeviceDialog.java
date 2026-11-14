package com.rena.w4b;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class DeviceDialog {
    private DeviceDialog() {
    }

    private static void picker(final Activity activity, LinearLayout parent, String label, final TextView valueView,
                               final Runnable onClick) {
        TextView caption = Ui.text(activity, label, 12f, RenaTheme.muted(), Typeface.BOLD);
        LinearLayout.LayoutParams captionLp = new LinearLayout.LayoutParams(-2, -2);
        captionLp.topMargin = Ui.dp(activity, 10);
        parent.addView(caption, captionLp);
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Ui.dp(activity, 14), 0, Ui.dp(activity, 10), 0);
        row.setBackground(Ui.ripple(activity, Ui.roundStroke(RenaTheme.surface2(), Ui.dp(activity, 14),
                RenaTheme.outline(), Ui.dp(activity, 1)), 14));
        row.addView(valueView, new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(Ui.text(activity, "\u25BE", 18f, RenaTheme.muted(), Typeface.NORMAL), new LinearLayout.LayoutParams(-2, -2));
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClick.run();
            }
        });
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, Ui.dp(activity, 50));
        rowLp.topMargin = Ui.dp(activity, 6);
        parent.addView(row, rowLp);
    }

    public static void show(final Activity activity, final Runnable onChanged) {
        final LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        final DeviceProfiles.Device[] devices = DeviceProfiles.devices();
        final DeviceProfiles.Ratio[] ratios = DeviceProfiles.ratios();
        final DeviceProfiles.Device[] selectedDevice = new DeviceProfiles.Device[]{DeviceProfiles.current(activity)};
        final DeviceProfiles.Ratio[] selectedRatio = new DeviceProfiles.Ratio[]{DeviceProfiles.currentRatio(activity)};

        final TextView deviceValue = Ui.text(activity, selectedDevice[0].label, 15f, RenaTheme.text(), Typeface.NORMAL);
        picker(activity, content, activity.getString(R.string.device_os_label), deviceValue, new Runnable() {
            @Override
            public void run() {
                String[] labels = new String[devices.length];
                int checked = 0;
                for (int i = 0; i < devices.length; i++) {
                    labels[i] = devices[i].label;
                    if (devices[i].id.equals(selectedDevice[0].id)) {
                        checked = i;
                    }
                }
                new RenaDialog.Builder(activity)
                        .setTitle(R.string.device_os_label)
                        .setSingleChoiceItems(labels, checked, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                selectedDevice[0] = devices[which];
                                deviceValue.setText(devices[which].label);
                            }
                        })
                        .show();
            }
        });

        final TextView ratioValue = Ui.text(activity, ratioLabel(activity, selectedRatio[0]), 15f, RenaTheme.text(), Typeface.NORMAL);
        picker(activity, content, activity.getString(R.string.device_ratio_label), ratioValue, new Runnable() {
            @Override
            public void run() {
                String[] labels = new String[ratios.length];
                int checked = 0;
                for (int i = 0; i < ratios.length; i++) {
                    labels[i] = ratioLabel(activity, ratios[i]);
                    if (ratios[i].id.equals(selectedRatio[0].id)) {
                        checked = i;
                    }
                }
                new RenaDialog.Builder(activity)
                        .setTitle(R.string.device_ratio_label)
                        .setSingleChoiceItems(labels, checked, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                selectedRatio[0] = ratios[which];
                                ratioValue.setText(ratioLabel(activity, ratios[which]));
                            }
                        })
                        .show();
            }
        });

        TextView note = Ui.text(activity, activity.getString(R.string.device_note), 12f, RenaTheme.faint(), Typeface.NORMAL);
        note.setLineSpacing(0f, 1.2f);
        LinearLayout.LayoutParams noteLp = new LinearLayout.LayoutParams(-2, -2);
        noteLp.topMargin = Ui.dp(activity, 14);
        content.addView(note, noteLp);

        new RenaDialog.Builder(activity)
                .setTitle(R.string.device_title)
                .setView(content)
                .setNegativeButton(R.string.cancel_text, null)
                .setPositiveButton(R.string.device_apply, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        RenaPrefs.setStr(activity, RenaPrefs.DEVICE_ID, selectedDevice[0].id);
                        RenaPrefs.setStr(activity, RenaPrefs.RATIO_ID, selectedRatio[0].id);
                        if (onChanged != null) {
                            onChanged.run();
                        }
                    }
                })
                .show();
    }

    private static String ratioLabel(Activity activity, DeviceProfiles.Ratio ratio) {
        return "auto".equals(ratio.id) ? activity.getString(R.string.device_ratio_auto) : ratio.label;
    }
}
