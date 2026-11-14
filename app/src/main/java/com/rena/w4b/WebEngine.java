package com.rena.w4b;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.webkit.WebSettings;
import android.webkit.WebView;

import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

public final class WebEngine {
    public static final String WA_ORIGIN = "https://web.whatsapp.com";
    private static final WeakHashMap<WebView, Object> HANDLES = new WeakHashMap<WebView, Object>();
    private static final WeakHashMap<WebView, String> LAST_SCRIPT = new WeakHashMap<WebView, String>();
    private static Boolean docStart;

    private WebEngine() {
    }

    public static boolean docStartSupported() {
        if (docStart == null) {
            try {
                Class<?> featureClass = Class.forName("androidx.webkit.WebViewFeature");
                Method method = featureClass.getMethod("isFeatureSupported", String.class);
                docStart = Boolean.TRUE.equals(method.invoke(null, "DOCUMENT_START_SCRIPT"));
            } catch (Throwable ignored) {
                docStart = Boolean.FALSE;
            }
        }
        return docStart;
    }

    public static String themeName(Context context) {
        switch (RenaTheme.mode(context)) {
            case RenaTheme.LIGHT:
                return "light";
            case RenaTheme.AMOLED:
                return "amoled";
            case RenaTheme.PURE_BLACK:
                return "pure";
            default:
                return "dark";
        }
    }

    public static boolean hasNetwork(Context context) {
        try {
            ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (manager == null) {
                return true;
            }
            NetworkInfo info = manager.getActiveNetworkInfo();
            return info != null && info.isConnected();
        } catch (Throwable ignored) {
            return true;
        }
    }

    public static void applyBase(Context context, WebView view, boolean browser) {
        WebSettings settings = view.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(browser);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccess(browser);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        if (browser) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }
        applyCache(context, view);
        applyDarkNeutral(settings);
    }

    public static void applyCache(Context context, WebView view) {
        WebSettings settings = view.getSettings();
        if (RenaPrefs.offlineMode(context)) {
            settings.setCacheMode(hasNetwork(context) ? WebSettings.LOAD_CACHE_ELSE_NETWORK : WebSettings.LOAD_CACHE_ONLY);
        } else {
            settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        }
    }

    private static void applyDarkNeutral(WebSettings settings) {
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                settings.setAlgorithmicDarkeningAllowed(false);
            } else if (Build.VERSION.SDK_INT >= 29) {
                settings.setForceDark(WebSettings.FORCE_DARK_OFF);
            }
        } catch (Throwable ignored) {
        }
    }

    public static void applyViewport(Context context, WebView view, boolean wide) {
        WebSettings settings = view.getSettings();
        settings.setUseWideViewPort(wide);
        settings.setLoadWithOverviewMode(wide);
    }

    public static void applyDevice(Context context, WebView view, boolean emulate) {
        WebSettings settings = view.getSettings();
        if (emulate) {
            DeviceProfiles.applyUserAgent(context, settings, DeviceProfiles.current(context));
        } else {
            DeviceProfiles.applyMobile(context, settings, WebSettings.getDefaultUserAgent(context));
        }
        applyViewport(context, view, emulate);
    }

    public static JSONObject config(Context context, boolean browser) {
        JSONObject cfg = new JSONObject();
        try {
            cfg.put("theme", themeName(context));
            boolean emulate = !browser || RenaPrefs.desktop(context);
            if (emulate) {
                DeviceProfiles.Device device = DeviceProfiles.current(context);
                DeviceProfiles.Ratio ratio = DeviceProfiles.currentRatio(context);
                cfg.put("device", DeviceProfiles.configFor(context, device, ratio));
                cfg.put("layoutWidth", ratio.layoutWidth);
            } else {
                cfg.put("forceZoom", true);
            }
            JSONObject translate = new JSONObject();
            translate.put("enabled", RenaPrefs.bool(context, browser ? RenaPrefs.TRANSLATE_BROWSER : RenaPrefs.TRANSLATE_W4B, false));
            translate.put("lang", RenaPrefs.translateLang(context));
            JSONObject labels = new JSONObject();
            Context localized = RenaLocale.wrap(context);
            labels.put("title", localized.getString(R.string.translate_title));
            labels.put("to", localized.getString(R.string.translate_to));
            labels.put("close", localized.getString(R.string.translate_close));
            translate.put("labels", labels);
            cfg.put("translate", translate);
            if (browser) {
                cfg.put("adblock", RenaPrefs.adblock(context));
                cfg.put("antiDetector", RenaPrefs.detector(context));
                cfg.put("tracking", RenaPrefs.tracking(context));
            } else {
                cfg.put("antiDelete", RenaPrefs.bool(context, RenaPrefs.ANTI_DELETE, true));
                cfg.put("statusSaver", RenaPrefs.bool(context, RenaPrefs.STATUS_SAVER, false));
                cfg.put("statusAuto", RenaPrefs.bool(context, RenaPrefs.STATUS_AUTO, false));
                JSONObject adLabels = new JSONObject();
                adLabels.put("deleted", localized.getString(R.string.antidelete_label));
                adLabels.put("media", localized.getString(R.string.antidelete_media));
                cfg.put("adLabels", adLabels);
                JSONObject statusLabels = new JSONObject();
                statusLabels.put("save", localized.getString(R.string.status_save));
                statusLabels.put("saved", localized.getString(R.string.status_saved));
                cfg.put("statusLabels", statusLabels);
            }
        } catch (Throwable ignored) {
        }
        return cfg;
    }

    public static String script(Context context, boolean browser) {
        StringBuilder script = new StringBuilder();
        script.append("window.__RENA_CFG=").append(config(context, browser).toString()).append(";\n");
        script.append(PageScripts.load(context, "device")).append('\n');
        script.append(PageScripts.load(context, "zoom")).append('\n');
        script.append(PageScripts.load(context, "theme")).append('\n');
        script.append(PageScripts.load(context, "download")).append('\n');
        script.append(PageScripts.load(context, "translate")).append('\n');
        if (browser) {
            script.append(PageScripts.load(context, "adblock")).append('\n');
            script.append(PageScripts.load(context, "inspector")).append('\n');
        } else {
            script.append(PageScripts.load(context, "wa")).append('\n');
            script.append(PageScripts.load(context, "antidelete")).append('\n');
            script.append(PageScripts.load(context, "status")).append('\n');
        }
        return script.toString();
    }

    public static void install(Context context, WebView view, boolean browser) {
        String script = script(context, browser);
        LAST_SCRIPT.put(view, script);
        removeHandle(view);
        if (!docStartSupported()) {
            return;
        }
        try {
            Class<?> compat = Class.forName("androidx.webkit.WebViewCompat");
            Method add = compat.getMethod("addDocumentStartJavaScript", WebView.class, String.class, Set.class);
            Set<String> rules = new HashSet<String>();
            rules.add(browser ? "*" : WA_ORIGIN);
            Object handle = add.invoke(null, view, script, rules);
            HANDLES.put(view, handle);
        } catch (Throwable ignored) {
        }
    }

    public static boolean hasDocStart(WebView view) {
        return view != null && HANDLES.containsKey(view);
    }

    public static void injectFallback(WebView view) {
        if (view == null || HANDLES.containsKey(view)) {
            return;
        }
        String script = LAST_SCRIPT.get(view);
        if (script != null) {
            try {
                view.evaluateJavascript(script, null);
            } catch (Throwable ignored) {
            }
        }
    }

    public static void refresh(Context context, WebView view, boolean browser) {
        install(context, view, browser);
    }

    private static void removeHandle(WebView view) {
        Object handle = HANDLES.remove(view);
        if (handle == null) {
            return;
        }
        try {
            handle.getClass().getMethod("remove").invoke(handle);
        } catch (Throwable ignored) {
            try {
                Class<?> handler = Class.forName("androidx.webkit.ScriptHandler");
                handler.getMethod("remove").invoke(handle);
            } catch (Throwable ignoredAgain) {
            }
        }
    }

    public static void release(WebView view) {
        removeHandle(view);
        LAST_SCRIPT.remove(view);
    }

    public static void switchTheme(Context context, WebView view) {
        if (view == null) {
            return;
        }
        try {
            view.evaluateJavascript("(function(){try{window.__RENA_CFG=window.__RENA_CFG||{};window.__RENA_CFG.theme="
                    + PageScripts.quote(themeName(context)) + ";if(window.__renaSetTheme){window.__renaSetTheme("
                    + PageScripts.quote(themeName(context)) + ");}}catch(e){}})()", null);
        } catch (Throwable ignored) {
        }
    }

    public static void setTranslate(Context context, WebView view, boolean on) {
        if (view == null) {
            return;
        }
        try {
            if (on) {
                String lang = RenaPrefs.translateLang(context);
                String script = PageScripts.load(context, "translate");
                String bootstrap = "(function(){window.__RENA_CFG=window.__RENA_CFG||{};window.__RENA_CFG.translate=window.__RENA_CFG.translate||{};"
                        + "window.__RENA_CFG.translate.lang=" + PageScripts.quote(lang) + ";"
                        + "window.__RENA_CFG.translate.labels=window.__RENA_CFG.translate.labels||{to:"
                        + PageScripts.quote(RenaLocale.wrap(context).getString(R.string.translate_to)) + ",close:"
                        + PageScripts.quote(RenaLocale.wrap(context).getString(R.string.translate_close)) + "};})();";
                view.evaluateJavascript(bootstrap + script + ";(function(){if(window.__renaTr){window.__renaTr.enable("
                        + PageScripts.quote(lang) + ");}})();", null);
            } else {
                view.evaluateJavascript("(function(){try{if(window.__renaTr){window.__renaTr.disable();}}catch(e){}})()", null);
            }
        } catch (Throwable ignored) {
        }
    }
}
