package com.rena.w4b;

import android.content.Context;

import java.util.HashMap;

public final class RenaPrefs {
    public static final String AUTO_UPDATE = "auto_update_check";
    public static final String LAST_SEEN_VERSION = "last_seen_version_code";
    public static final String APP_ICON = "app_icon_alias";
    public static final String OFFLINE_MODE = "offline_mode";
    public static final String DEVICE_ID = "device_profile_id";
    public static final String RATIO_ID = "device_ratio_id";
    public static final String TRANSLATE_W4B = "translate_w4b";
    public static final String TRANSLATE_BROWSER = "translate_browser";
    public static final String TRANSLATE_LANG = "translate_lang";
    public static final String ANTI_DELETE = "anti_delete";
    public static final String STATUS_SAVER = "status_saver";
    public static final String STATUS_AUTO = "status_auto";
    public static final String BR_ENGINE = "browser_engine";
    public static final String BR_ADBLOCK = "browser_adblock";
    public static final String BR_TRACKING = "browser_tracking";
    public static final String BR_DETECTOR = "browser_adblock_detector";
    public static final String BR_DESKTOP = "browser_desktop_mode";
    public static final String BR_DNS = "browser_dns";
    public static final String BR_HTTPS_ONLY = "browser_https_only";
    public static final String MODE_BROWSER = "mode_browser_active";
    public static final String UPDATE_NOTES_PENDING = "update_installed_pending";

    private static final HashMap<String, String> CACHE = new HashMap<String, String>();

    private RenaPrefs() {
    }

    public static synchronized String str(Context context, String key, String fallback) {
        String cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        String value = RenaSettingsStore.getString(context, key, null);
        if (value == null) {
            return fallback;
        }
        CACHE.put(key, value);
        return value;
    }

    public static synchronized void setStr(Context context, String key, String value) {
        CACHE.put(key, value);
        RenaSettingsStore.putString(context, key, value);
    }

    public static boolean bool(Context context, String key, boolean fallback) {
        String value = str(context, key, null);
        return value == null ? fallback : "true".equalsIgnoreCase(value);
    }

    public static void setBool(Context context, String key, boolean value) {
        setStr(context, key, value ? "true" : "false");
    }

    public static long lng(Context context, String key, long fallback) {
        String value = str(context, key, null);
        if (value == null) {
            return fallback;
        }
        try {
            return Long.parseLong(value);
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    public static void setLng(Context context, String key, long value) {
        setStr(context, key, String.valueOf(value));
    }

    public static boolean autoUpdate(Context context) {
        return bool(context, AUTO_UPDATE, true);
    }

    public static boolean offlineMode(Context context) {
        return bool(context, OFFLINE_MODE, false);
    }

    public static boolean adblock(Context context) {
        return bool(context, BR_ADBLOCK, true);
    }

    public static boolean tracking(Context context) {
        return bool(context, BR_TRACKING, true);
    }

    public static boolean detector(Context context) {
        return bool(context, BR_DETECTOR, true);
    }

    public static boolean desktop(Context context) {
        return bool(context, BR_DESKTOP, false);
    }

    public static boolean httpsOnly(Context context) {
        return bool(context, BR_HTTPS_ONLY, true);
    }

    public static String translateLang(Context context) {
        String stored = str(context, TRANSLATE_LANG, null);
        if (stored != null) {
            return stored;
        }
        return RenaLocale.isIndonesian(context) ? "id" : "en";
    }
}
