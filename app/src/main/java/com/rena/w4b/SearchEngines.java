package com.rena.w4b;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import java.net.URLEncoder;
import java.util.Locale;

public final class SearchEngines {
    public static final class Engine {
        public final String id;
        public final String name;
        public final String searchUrl;
        public final String home;
        public final int icon;

        Engine(String id, String name, String searchUrl, String home, int icon) {
            this.id = id;
            this.name = name;
            this.searchUrl = searchUrl;
            this.home = home;
            this.icon = icon;
        }
    }

    private static final Engine[] ENGINES = new Engine[]{
            new Engine("google", "Google", "https://www.google.com/search?q=%s", "https://www.google.com/", R.drawable.ic_se_google),
            new Engine("bing", "Bing", "https://www.bing.com/search?q=%s", "https://www.bing.com/", R.drawable.ic_se_bing),
            new Engine("duckduckgo", "DuckDuckGo", "https://duckduckgo.com/?q=%s", "https://duckduckgo.com/", R.drawable.ic_se_duckduckgo),
            new Engine("yahoo", "Yahoo", "https://search.yahoo.com/search?p=%s", "https://search.yahoo.com/", R.drawable.ic_se_yahoo),
            new Engine("brave", "Brave Search", "https://search.brave.com/search?q=%s", "https://search.brave.com/", R.drawable.ic_se_brave),
            new Engine("ecosia", "Ecosia", "https://www.ecosia.org/search?q=%s", "https://www.ecosia.org/", R.drawable.ic_se_ecosia),
            new Engine("startpage", "Startpage", "https://www.startpage.com/do/search?q=%s", "https://www.startpage.com/", R.drawable.ic_se_startpage),
            new Engine("yandex", "Yandex", "https://yandex.com/search/?text=%s", "https://yandex.com/", R.drawable.ic_se_yandex),
            new Engine("baidu", "Baidu", "https://www.baidu.com/s?wd=%s", "https://www.baidu.com/", R.drawable.ic_se_baidu),
            new Engine("qwant", "Qwant", "https://www.qwant.com/?q=%s", "https://www.qwant.com/", R.drawable.ic_se_qwant)
    };

    private SearchEngines() {
    }

    public static Engine[] all() {
        return ENGINES;
    }

    public static boolean isChosen(Context context) {
        return !TextUtils.isEmpty(RenaPrefs.str(context, RenaPrefs.BR_ENGINE, ""));
    }

    public static Engine current(Context context) {
        String id = RenaPrefs.str(context, RenaPrefs.BR_ENGINE, "google");
        for (Engine engine : ENGINES) {
            if (engine.id.equals(id)) {
                return engine;
            }
        }
        return ENGINES[0];
    }

    public static void choose(Context context, String id) {
        RenaPrefs.setStr(context, RenaPrefs.BR_ENGINE, id);
    }

    public static String searchUrl(Context context, String query) {
        try {
            return current(context).searchUrl.replace("%s", URLEncoder.encode(query, "UTF-8"));
        } catch (Throwable ignored) {
            return current(context).home;
        }
    }

    public static boolean looksLikeWhatsAppWebQuery(String input) {
        if (input == null) {
            return false;
        }
        String q = input.trim().toLowerCase(Locale.US).replaceAll("\\s+", " ");
        return q.equals("whatsapp web") || q.equals("web whatsapp") || q.equals("whatsapp web login") ||
                q.equals("wa web") || q.equals("whatsapp web online") || q.equals("whatsapp web version") ||
                q.equals("login whatsapp web") || q.equals("whatsapp web download") || q.equals("whatsapp web apk");
    }

    public static boolean isWhatsAppWebUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }
        try {
            Uri uri = Uri.parse(url);
            String host = uri.getHost();
            return host != null && host.equalsIgnoreCase("web.whatsapp.com");
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static String resolve(Context context, String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.length() == 0) {
            return "";
        }
        String lower = text.toLowerCase(Locale.US);
        if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("file://") ||
                lower.startsWith("about:") || lower.startsWith("data:") || lower.startsWith("javascript:") ||
                lower.startsWith("content://") || lower.startsWith("blob:")) {
            return text;
        }
        boolean hasSpace = text.indexOf(' ') >= 0;
        boolean hasDot = text.indexOf('.') > 0 && !text.endsWith(".");
        boolean localhost = lower.startsWith("localhost") || lower.matches("^\\d{1,3}(\\.\\d{1,3}){3}(:\\d+)?(/.*)?$");
        if (!hasSpace && (hasDot || localhost) && !lower.startsWith("?")) {
            return (localhost ? "http://" : "https://") + text;
        }
        return searchUrl(context, text);
    }
}
