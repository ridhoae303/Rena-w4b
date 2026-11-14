package com.rena.w4b;

import android.app.Activity;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import java.lang.ref.WeakReference;

public final class StatusBridge {
    private final WeakReference<Activity> activity;
    private final WeakReference<WebView> view;
    private final String sourceKey;
    private final int tabId;

    public StatusBridge(Activity activity, WebView view, String sourceKey, int tabId) {
        this.activity = new WeakReference<Activity>(activity);
        this.view = new WeakReference<WebView>(view);
        this.sourceKey = sourceKey;
        this.tabId = tabId;
    }

    @JavascriptInterface
    public void save(final String url, final String mime) {
        final Activity host = activity.get();
        final WebView target = view.get();
        if (host == null || target == null) {
            return;
        }
        host.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                RenaDownloadHelper.saveSilently(host, target, sourceKey, tabId, url, mime);
            }
        });
    }
}
