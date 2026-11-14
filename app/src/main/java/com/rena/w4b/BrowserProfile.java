package com.rena.w4b;

import android.content.Context;
import android.webkit.CookieManager;
import android.webkit.WebStorage;
import android.webkit.WebView;

import androidx.webkit.Profile;
import androidx.webkit.ProfileStore;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

public final class BrowserProfile {
    public static final String NAME = "rena_browser";

    private BrowserProfile() {
    }

    public static boolean supported() {
        try {
            return WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static ProfileStore store() throws Exception {
        java.lang.reflect.Method method = ProfileStore.class.getMethod("getInstance");
        Object value = method.invoke(null);
        if (value instanceof ProfileStore) {
            return (ProfileStore) value;
        }
        throw new IllegalStateException("profile store");
    }

    public static Profile profile() {
        if (!supported()) {
            return null;
        }
        try {
            return store().getOrCreateProfile(NAME);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static CookieManager prepare(WebView view) {
        if (supported()) {
            try {
                Profile profile = profile();
                WebViewCompat.setProfile(view, NAME);
                if (profile != null) {
                    return profile.getCookieManager();
                }
            } catch (Throwable ignored) {
            }
        }
        return CookieManager.getInstance();
    }

    public static CookieManager cookies() {
        Profile profile = profile();
        if (profile != null) {
            try {
                return profile.getCookieManager();
            } catch (Throwable ignored) {
            }
        }
        return CookieManager.getInstance();
    }

    public static void clearSiteData(Context context, boolean cookies, boolean cache) {
        try {
            if (cookies) {
                CookieManager manager = cookies();
                manager.removeAllCookies(null);
                manager.flush();
                Profile profile = profile();
                WebStorage storage = profile == null ? WebStorage.getInstance() : profile.getWebStorage();
                storage.deleteAllData();
            }
            if (cache) {
                WebView temp = new WebView(context);
                try {
                    prepare(temp);
                    temp.clearCache(true);
                    temp.clearFormData();
                } finally {
                    temp.destroy();
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
