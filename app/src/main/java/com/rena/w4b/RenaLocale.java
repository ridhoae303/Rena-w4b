package com.rena.w4b;

import android.content.Context;
import android.content.res.Configuration;

import java.util.Locale;

public final class RenaLocale {
    public static final String LANG_EN = "en";
    public static final String LANG_ID = "in";
    private static final String KEY = "app_language";
    private static volatile String cached;

    private RenaLocale() {
    }

    public static String getLanguage(Context context) {
        String value = cached;
        if (value != null) {
            return value;
        }
        String stored = RenaSettingsStore.getString(context, KEY, "");
        if (stored.length() == 0) {
            String system = Locale.getDefault().getLanguage();
            stored = "in".equals(system) || "id".equals(system) ? LANG_ID : LANG_EN;
        }
        cached = stored;
        return stored;
    }

    public static void setLanguage(Context context, String language) {
        cached = language;
        RenaSettingsStore.putString(context, KEY, language);
    }

    public static boolean isIndonesian(Context context) {
        return LANG_ID.equals(getLanguage(context));
    }

    public static Locale localeFor(Context context) {
        return isIndonesian(context) ? new Locale("in", "ID") : Locale.ENGLISH;
    }

    public static Context wrap(Context base) {
        if (base == null) {
            return null;
        }
        try {
            Locale locale = localeFor(base);
            Locale.setDefault(locale);
            Configuration configuration = new Configuration(base.getResources().getConfiguration());
            configuration.setLocale(locale);
            boolean dark = RenaTheme.isDark(base);
            configuration.uiMode = (configuration.uiMode & ~android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                    | (dark ? android.content.res.Configuration.UI_MODE_NIGHT_YES : android.content.res.Configuration.UI_MODE_NIGHT_NO);
            return base.createConfigurationContext(configuration);
        } catch (Throwable ignored) {
            return base;
        }
    }

    public static String label(Context context, String language) {
        return LANG_ID.equals(language) ? "Bahasa Indonesia" : "English";
    }
}
