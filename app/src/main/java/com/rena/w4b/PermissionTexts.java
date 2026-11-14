package com.rena.w4b;

import android.content.Context;

public final class PermissionTexts {
    private PermissionTexts() {
    }

    private static int kind(String permission) {
        String p = permission == null ? "" : permission;
        if (p.contains("CAMERA")) {
            return 1;
        }
        if (p.contains("RECORD_AUDIO")) {
            return 2;
        }
        if (p.contains("POST_NOTIFICATIONS")) {
            return 3;
        }
        if (p.contains("READ_EXTERNAL_STORAGE") || p.contains("READ_MEDIA") || p.contains("WRITE_EXTERNAL_STORAGE")) {
            return 4;
        }
        return 0;
    }

    public static String title(Context context, String permission) {
        switch (kind(permission)) {
            case 1:
                return context.getString(R.string.perm_camera_title);
            case 2:
                return context.getString(R.string.perm_mic_title);
            case 3:
                return context.getString(R.string.perm_notif_title);
            case 4:
                return context.getString(R.string.perm_file_title);
            default:
                return context.getString(R.string.perm_other_title);
        }
    }

    public static String message(Context context, String permission) {
        switch (kind(permission)) {
            case 1:
                return context.getString(R.string.perm_camera_message);
            case 2:
                return context.getString(R.string.perm_mic_message);
            case 3:
                return context.getString(R.string.perm_notif_message);
            case 4:
                return context.getString(R.string.perm_file_message);
            default:
                return context.getString(R.string.perm_other_message);
        }
    }

    public static String denied(Context context, String permission) {
        switch (kind(permission)) {
            case 1:
                return context.getString(R.string.perm_camera_denied);
            case 2:
                return context.getString(R.string.perm_mic_denied);
            case 3:
                return context.getString(R.string.perm_notif_denied);
            case 4:
                return context.getString(R.string.perm_file_denied);
            default:
                return context.getString(R.string.perm_other_denied);
        }
    }
}
