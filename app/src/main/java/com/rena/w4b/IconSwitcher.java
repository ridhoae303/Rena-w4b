package com.rena.w4b;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.drawable.Drawable;

public final class IconSwitcher {
    public static final String[] IDS = new String[]{"default", "aurora", "sunset", "ocean", "sakura", "mono"};

    private IconSwitcher() {
    }

    private static String aliasName(String id) {
        String capital = Character.toUpperCase(id.charAt(0)) + id.substring(1);
        return "com.rena.w4b.Launcher" + capital;
    }

    public static String current(Context context) {
        PackageManager manager = context.getPackageManager();
        for (String id : IDS) {
            try {
                int state = manager.getComponentEnabledSetting(new ComponentName(context.getPackageName(), aliasName(id)));
                if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                    return id;
                }
            } catch (Throwable ignored) {
            }
        }
        return "default";
    }

    public static void apply(Context context, String target) {
        PackageManager manager = context.getPackageManager();
        for (String id : IDS) {
            int state = id.equals(target) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            try {
                manager.setComponentEnabledSetting(new ComponentName(context.getPackageName(), aliasName(id)), state,
                        PackageManager.DONT_KILL_APP);
            } catch (Throwable ignored) {
            }
        }
        RenaPrefs.setStr(context, RenaPrefs.APP_ICON, target);
    }

    public static int iconRes(String id) {
        switch (id) {
            case "aurora":
                return R.mipmap.ic_launcher_aurora;
            case "sunset":
                return R.mipmap.ic_launcher_sunset;
            case "ocean":
                return R.mipmap.ic_launcher_ocean;
            case "sakura":
                return R.mipmap.ic_launcher_sakura;
            case "mono":
                return R.mipmap.ic_launcher_mono;
            default:
                return R.mipmap.ic_launcher;
        }
    }

    public static String label(Context context, String id) {
        switch (id) {
            case "aurora":
                return context.getString(R.string.icon_aurora);
            case "sunset":
                return context.getString(R.string.icon_sunset);
            case "ocean":
                return context.getString(R.string.icon_ocean);
            case "sakura":
                return context.getString(R.string.icon_sakura);
            case "mono":
                return context.getString(R.string.icon_mono);
            default:
                return context.getString(R.string.icon_default);
        }
    }

    public static Bitmap preview(Context context, String id, int size) {
        try {
            Drawable drawable = context.getResources().getDrawable(iconRes(id), context.getTheme());
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            Path clip = new Path();
            clip.addCircle(size / 2f, size / 2f, size / 2f, Path.Direction.CW);
            canvas.clipPath(clip);
            drawable.setBounds(-size / 6, -size / 6, size + size / 6, size + size / 6);
            drawable.draw(canvas);
            return bitmap;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
