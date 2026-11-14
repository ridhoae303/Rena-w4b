package com.rena.w4b;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.HttpAuthHandler;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

final class BrowserController implements TranslateBridge.Host {
    private static final int MAX_LIVE_VIEWS = 3;
    private static final String START_URL = "about:blank";

    private final MainActivity host;
    private final FrameLayout rootLayout;
    private final BrowserStore store;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ArrayList<BTab> tabs = new ArrayList<BTab>();
    private final HashSet<String> httpAllowedHosts = new HashSet<String>();
    private final FrameLayout container;
    private final FrameLayout contentHolder;
    private final FrameLayout overlayHolder;
    private final FrameLayout fullscreenHolder;
    private final LinearLayout topBar;
    private final LinearLayout pill;
    private final ImageView leadingIcon;
    private final TextView urlLabel;
    private final TextView reloadButton;
    private final ProgressBar progressBar;
    private final FrameLayout drawer;
    private final View scrim;
    private final LinearLayout drawerBody;
    private BTab active;
    private int nextId = 1;
    private boolean visible;
    private boolean drawerOpen;
    private boolean drawerAnimating;
    private ValueAnimator drawerAnimator;
    private float drawerProgress;
    private int drawerWidth;
    private int insetTop;
    private int insetBottom;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private InspectorPanel inspector;
    private View omniboxView;
    private View historyView;
    private final Runnable thumbRunnable = new Runnable() {
        @Override
        public void run() {
            if (active != null) {
                captureThumb(active);
            }
        }
    };

    static final class BTab {
        int id;
        String url = "";
        String title = "";
        WebView view;
        Bitmap thumb;
        long lastUsed;
        int progress;
        boolean loading;
        boolean start;
        BTab opener;
        String upgradedFrom;
        CookieManager cookies;
        boolean thumbLoaded;
    }

    BrowserController(MainActivity host, FrameLayout rootLayout) {
        this.host = host;
        this.rootLayout = rootLayout;
        this.store = new BrowserStore(host);

        container = new FrameLayout(host);
        container.setBackgroundColor(RenaTheme.bg());
        container.setVisibility(View.GONE);

        LinearLayout column = new LinearLayout(host);
        column.setOrientation(LinearLayout.VERTICAL);
        container.addView(column, new FrameLayout.LayoutParams(-1, -1));

        topBar = new LinearLayout(host);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setBackgroundColor(RenaTheme.bg());
        column.addView(topBar, new LinearLayout.LayoutParams(-1, -2));

        pill = new LinearLayout(host);
        pill.setOrientation(LinearLayout.HORIZONTAL);
        pill.setGravity(Gravity.CENTER_VERTICAL);
        pill.setPadding(dp(12), 0, dp(4), 0);
        pill.setBackground(Ui.ripple(host, Ui.roundStroke(RenaTheme.surface2(), dp(24), RenaTheme.outline(), dp(1)), 24));
        leadingIcon = new ImageView(host);
        leadingIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        pill.addView(leadingIcon, new LinearLayout.LayoutParams(dp(20), dp(20)));
        urlLabel = Ui.text(host, "", 14.5f, RenaTheme.text(), Typeface.NORMAL);
        urlLabel.setSingleLine(true);
        urlLabel.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(0, -2, 1f);
        labelLp.leftMargin = dp(10);
        pill.addView(urlLabel, labelLp);
        reloadButton = Ui.text(host, "\u27F3", 20f, RenaTheme.text(), Typeface.NORMAL);
        reloadButton.setGravity(Gravity.CENTER);
        pill.addView(reloadButton, new LinearLayout.LayoutParams(dp(44), dp(44)));
        pill.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOmnibox();
            }
        });
        reloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reloadOrStop();
            }
        });
        LinearLayout.LayoutParams pillLp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        pillLp.leftMargin = dp(12);
        pillLp.rightMargin = dp(66);
        topBar.addView(pill, pillLp);

        progressBar = new ProgressBar(host, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgressTintList(ColorStateList.valueOf(RenaTheme.accent()));
        progressBar.setProgressBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        progressBar.setVisibility(View.INVISIBLE);
        column.addView(progressBar, new LinearLayout.LayoutParams(-1, dp(3)));

        contentHolder = new FrameLayout(host);
        column.addView(contentHolder, new LinearLayout.LayoutParams(-1, 0, 1f));

        overlayHolder = new FrameLayout(host);
        container.addView(overlayHolder, new FrameLayout.LayoutParams(-1, -1));

        fullscreenHolder = new FrameLayout(host);
        fullscreenHolder.setBackgroundColor(Color.BLACK);
        fullscreenHolder.setVisibility(View.GONE);
        container.addView(fullscreenHolder, new FrameLayout.LayoutParams(-1, -1));

        container.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                insetTop = insets.getSystemWindowInsetTop();
                insetBottom = insets.getSystemWindowInsetBottom();
                applyInsets();
                return insets;
            }
        });

        drawerWidth = Math.min((int) (host.getResources().getDisplayMetrics().widthPixels * 0.88f), dp(344));
        drawer = new FrameLayout(host);
        drawer.setBackgroundColor(RenaTheme.sheet());
        drawer.setVisibility(View.GONE);
        drawer.setClickable(true);
        ScrollView scroll = new ScrollView(host);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        drawerBody = new LinearLayout(host);
        drawerBody.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(drawerBody, new FrameLayout.LayoutParams(-1, -2));
        drawer.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        scrim = new View(host);
        scrim.setBackgroundColor(Color.BLACK);
        scrim.setAlpha(0f);
        scrim.setVisibility(View.GONE);
        scrim.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setDrawerOpen(false);
            }
        });
        applyInsets();
        restore();
    }

    View container() {
        return container;
    }

    View drawerView() {
        return drawer;
    }

    View scrimView() {
        return scrim;
    }

    int drawerWidth() {
        return drawerWidth;
    }

    private int dp(float value) {
        return Ui.dp(host, value);
    }

    private void applyInsets() {
        topBar.setPadding(0, insetTop + dp(8), 0, dp(8));
        contentHolder.setPadding(0, 0, 0, insetBottom);
        drawer.setPadding(0, insetTop, 0, insetBottom);
    }

    void applyTheme() {
        container.setBackgroundColor(RenaTheme.bg());
        topBar.setBackgroundColor(RenaTheme.bg());
        pill.setBackground(Ui.ripple(host, Ui.roundStroke(RenaTheme.surface2(), dp(24), RenaTheme.outline(), dp(1)), 24));
        urlLabel.setTextColor(RenaTheme.text());
        reloadButton.setTextColor(RenaTheme.text());
        drawer.setBackgroundColor(RenaTheme.sheet());
        for (BTab tab : tabs) {
            if (tab.view != null) {
                tab.view.setBackgroundColor(RenaTheme.bg());
                WebEngine.refresh(host, tab.view, true);
                WebEngine.switchTheme(host, tab.view);
                if (tab.start) {
                    loadStartPage(tab);
                }
            }
        }
        updateBar();
    }

    private String hubKey(BTab tab) {
        return "br:" + tab.id;
    }

    private void restore() {
        BrowserStore.Snapshot snapshot = store.readTabs();
        nextId = Math.max(1, snapshot.nextId);
        for (BrowserStore.TabRecord record : snapshot.tabs) {
            BTab tab = new BTab();
            tab.id = record.id;
            tab.url = record.url;
            tab.title = record.title;
            tab.lastUsed = record.lastUsed;
            tab.start = TextUtils.isEmpty(record.url) || START_URL.equals(record.url);
            tabs.add(tab);
            if (record.id == snapshot.activeId) {
                active = tab;
            }
        }
        if (tabs.isEmpty()) {
            active = createTab("", null);
        } else if (active == null) {
            active = tabs.get(tabs.size() - 1);
        }
    }

    private BTab createTab(String url, BTab opener) {
        BTab tab = new BTab();
        tab.id = nextId++;
        tab.url = url == null ? "" : url;
        tab.start = TextUtils.isEmpty(tab.url);
        tab.opener = opener;
        tab.lastUsed = System.currentTimeMillis();
        tabs.add(tab);
        return tab;
    }

    boolean isVisible() {
        return visible;
    }

    int tabCount() {
        return tabs.size();
    }

    void show() {
        visible = true;
        container.setVisibility(View.VISIBLE);
        attachActive();
        if (!SearchEngines.isChosen(host)) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (visible) {
                        showEngineChooser(true);
                    }
                }
            });
        }
    }

    void hide() {
        visible = false;
        closeOverlays();
        setDrawerOpen(false);
        if (active != null && active.view != null) {
            captureThumb(active);
            saveState(active);
            try {
                active.view.onPause();
            } catch (Throwable ignored) {
            }
        }
        container.setVisibility(View.GONE);
        persist();
    }

    private void attachActive() {
        if (active == null) {
            return;
        }
        ensureView(active);
        contentHolder.removeAllViews();
        if (active.view != null) {
            ViewGroup parent = active.view.getParent() instanceof ViewGroup ? (ViewGroup) active.view.getParent() : null;
            if (parent != null) {
                parent.removeView(active.view);
            }
            contentHolder.addView(active.view, new FrameLayout.LayoutParams(-1, -1));
            try {
                active.view.onResume();
            } catch (Throwable ignored) {
            }
        }
        active.lastUsed = System.currentTimeMillis();
        updateBar();
        trimViews();
    }

    private void trimViews() {
        ArrayList<BTab> live = new ArrayList<BTab>();
        for (BTab tab : tabs) {
            if (tab.view != null && tab != active) {
                live.add(tab);
            }
        }
        java.util.Collections.sort(live, new java.util.Comparator<BTab>() {
            @Override
            public int compare(BTab a, BTab b) {
                return a.lastUsed < b.lastUsed ? -1 : a.lastUsed > b.lastUsed ? 1 : 0;
            }
        });
        while (live.size() > MAX_LIVE_VIEWS - 1) {
            BTab victim = live.remove(0);
            releaseView(victim, true);
        }
    }

    private void releaseView(BTab tab, boolean keepState) {
        if (tab.view == null) {
            return;
        }
        if (keepState) {
            captureThumb(tab);
            saveState(tab);
        }
        try {
            ViewGroup parent = tab.view.getParent() instanceof ViewGroup ? (ViewGroup) tab.view.getParent() : null;
            if (parent != null) {
                parent.removeView(tab.view);
            }
            WebEngine.release(tab.view);
            tab.view.stopLoading();
            tab.view.destroy();
        } catch (Throwable ignored) {
        }
        tab.view = null;
    }

    private void ensureView(BTab tab) {
        if (tab.view != null) {
            return;
        }
        WebView view = createWebView(tab);
        tab.view = view;
        Bundle state = tab.start ? null : store.loadState(tab.id);
        boolean restored = false;
        if (state != null) {
            try {
                restored = view.restoreState(state) != null;
            } catch (Throwable ignored) {
                restored = false;
            }
        }
        if (!restored) {
            if (tab.start) {
                loadStartPage(tab);
            } else {
                view.loadUrl(tab.url);
            }
        }
    }

    private WebView createWebView(final BTab tab) {
        final WebView view = new WebView(host);
        tab.cookies = BrowserProfile.prepare(view);
        view.setBackgroundColor(RenaTheme.bg());
        WebEngine.applyBase(host, view, true);
        WebEngine.applyDevice(host, view, RenaPrefs.desktop(host));
        try {
            tab.cookies.setAcceptCookie(true);
            tab.cookies.setAcceptThirdPartyCookies(view, !RenaPrefs.tracking(host));
        } catch (Throwable ignored) {
        }
        RenaDownloadHelper.attach(view);
        view.addJavascriptInterface(new TranslateBridge(host, view, this), "RenaTranslate");
        view.addJavascriptInterface(new InspectorHub.Bridge(InspectorHub.forKey(hubKey(tab))), "RenaInspector");
        WebEngine.install(host, view, true);
        view.setWebViewClient(new Client(tab));
        view.setWebChromeClient(new Chrome(tab));
        view.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                String cookie = null;
                try {
                    cookie = tab.cookies == null ? null : tab.cookies.getCookie(url);
                } catch (Throwable ignored) {
                }
                RenaDownloadHelper.handle(host, view, "br:" + tab.id, tab.id, url, userAgent, contentDisposition,
                        mimeType, contentLength, cookie, view.getUrl());
            }
        });
        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return showContextMenu(tab, view);
            }
        });
        return view;
    }

    private BTab tabFor(WebView view) {
        for (BTab tab : tabs) {
            if (tab.view == view) {
                return tab;
            }
        }
        return null;
    }

    BTab newTab(String url, boolean activate, BTab opener) {
        BTab tab = createTab(url, opener);
        if (activate) {
            switchTo(tab);
        }
        persist();
        return tab;
    }

    void newTabFromUi() {
        BTab tab = newTab("", true, null);
        setDrawerOpen(false);
        if (tab != null) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    showOmnibox();
                }
            }, 220L);
        }
    }

    void switchTo(BTab tab) {
        if (tab == null) {
            return;
        }
        if (active != null && active != tab && active.view != null) {
            captureThumb(active);
            saveState(active);
            try {
                active.view.onPause();
            } catch (Throwable ignored) {
            }
        }
        active = tab;
        if (visible) {
            attachActive();
        }
        persist();
    }

    void closeTab(BTab tab) {
        if (tab == null) {
            return;
        }
        int index = tabs.indexOf(tab);
        boolean wasActive = tab == active;
        releaseView(tab, false);
        tabs.remove(tab);
        store.deleteTabFiles(tab.id);
        InspectorHub.drop(hubKey(tab));
        RenaDownloadManager.cancelBlobDownloadsForSource("br:" + tab.id);
        for (BTab other : tabs) {
            if (other.opener == tab) {
                other.opener = null;
            }
        }
        if (tabs.isEmpty()) {
            active = createTab("", null);
            if (visible) {
                attachActive();
            }
        } else if (wasActive) {
            BTab next = tab.opener != null && tabs.contains(tab.opener) ? tab.opener : tabs.get(Math.min(index, tabs.size() - 1));
            active = next;
            if (visible) {
                attachActive();
            }
        }
        persist();
        refreshDrawerIfOpen();
    }

    void closeAllTabs() {
        for (BTab tab : new ArrayList<BTab>(tabs)) {
            releaseView(tab, false);
            store.deleteTabFiles(tab.id);
            InspectorHub.drop(hubKey(tab));
        }
        tabs.clear();
        active = createTab("", null);
        if (visible) {
            attachActive();
        }
        persist();
        refreshDrawerIfOpen();
    }

    private void saveState(BTab tab) {
        if (tab.view == null || tab.start) {
            return;
        }
        try {
            Bundle bundle = new Bundle();
            tab.view.saveState(bundle);
            store.saveState(tab.id, bundle);
        } catch (Throwable ignored) {
        }
    }

    void persist() {
        BrowserStore.Snapshot snapshot = new BrowserStore.Snapshot();
        snapshot.activeId = active == null ? -1 : active.id;
        snapshot.nextId = nextId;
        for (BTab tab : tabs) {
            BrowserStore.TabRecord record = new BrowserStore.TabRecord();
            record.id = tab.id;
            record.url = tab.start ? "" : tab.url;
            record.title = tab.title;
            record.lastUsed = tab.lastUsed;
            snapshot.tabs.add(record);
        }
        store.writeTabs(snapshot);
    }

    void onPause() {
        if (active != null && active.view != null) {
            saveState(active);
            try {
                CookieManager manager = active.cookies != null ? active.cookies : CookieManager.getInstance();
                manager.flush();
            } catch (Throwable ignored) {
            }
        }
        persist();
    }

    void onResume() {
        if (visible && active != null && active.view != null) {
            try {
                active.view.onResume();
            } catch (Throwable ignored) {
            }
        }
    }

    void onDestroy() {
        persist();
        for (BTab tab : new ArrayList<BTab>(tabs)) {
            if (tab.view != null) {
                saveState(tab);
                releaseView(tab, false);
            }
        }
    }

    private void captureThumb(final BTab tab) {
        final WebView view = tab.view;
        if (view == null || view.getWidth() <= 0 || view.getHeight() <= 0 || tab.start) {
            return;
        }
        try {
            int width = 260;
            int height = Math.min(360, Math.max(120, (int) (width * (float) view.getHeight() / view.getWidth())));
            final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(bitmap);
            canvas.scale(width / (float) view.getWidth(), height / (float) view.getHeight());
            view.draw(canvas);
            tab.thumb = bitmap;
            tab.thumbLoaded = true;
            final int id = tab.id;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    store.saveThumb(id, bitmap);
                }
            }).start();
        } catch (Throwable ignored) {
        }
    }

    private void scheduleThumb() {
        handler.removeCallbacks(thumbRunnable);
        handler.postDelayed(thumbRunnable, 1600L);
    }

    BTab activeTab() {
        return active;
    }

    WebView activeWebView() {
        return active == null ? null : active.view;
    }

    private void updateBar() {
        if (active == null) {
            return;
        }
        if (active.start || TextUtils.isEmpty(active.url) || START_URL.equals(active.url)) {
            urlLabel.setText(host.getString(R.string.browser_search_hint, SearchEngines.current(host).name));
            urlLabel.setTextColor(RenaTheme.muted());
            leadingIcon.setImageResource(SearchEngines.current(host).icon);
            leadingIcon.clearColorFilter();
        } else {
            String host0 = BrowserStore.hostOf(active.url);
            urlLabel.setText(host0.length() == 0 ? active.url : host0);
            urlLabel.setTextColor(RenaTheme.text());
            boolean secure = active.url.startsWith("https://");
            leadingIcon.setImageResource(secure ? R.drawable.ic_lock : R.drawable.ic_lock_open);
            leadingIcon.setColorFilter(secure ? RenaTheme.accentText() : RenaTheme.danger());
        }
        reloadButton.setText(active.loading ? "\u2715" : "\u27F3");
        if (active.loading && active.progress < 100) {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(Math.max(6, active.progress));
        } else {
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    private void reloadOrStop() {
        if (active == null || active.view == null) {
            return;
        }
        if (active.loading) {
            active.view.stopLoading();
        } else if (active.start) {
            loadStartPage(active);
        } else {
            active.view.reload();
        }
    }

    void goBackStep() {
        if (active != null && active.view != null && active.view.canGoBack()) {
            active.view.goBack();
        }
    }

    void goForwardStep() {
        if (active != null && active.view != null && active.view.canGoForward()) {
            active.view.goForward();
        }
    }

    void goHome() {
        if (active == null) {
            return;
        }
        active.start = true;
        active.url = "";
        ensureView(active);
        loadStartPage(active);
        updateBar();
    }

    private String httpsFirst(BTab tab, String url) {
        if (url != null && url.startsWith("http://") && RenaPrefs.httpsOnly(host)) {
            String h = BrowserStore.hostOf(url);
            if (!httpAllowedHosts.contains(h) && !h.equals("localhost") && !h.matches("^\\d{1,3}(\\.\\d{1,3}){3}$")) {
                tab.upgradedFrom = url;
                return "https://" + url.substring(7);
            }
        }
        return url;
    }

    void openUrl(String raw, boolean inNewTab) {
        String target = SearchEngines.resolve(host, raw);
        if (target.length() == 0) {
            return;
        }
        if (SearchEngines.looksLikeWhatsAppWebQuery(raw) || SearchEngines.isWhatsAppWebUrl(target)) {
            askSwitchToW4b(target);
            return;
        }
        if (inNewTab || active == null) {
            BTab created = createTab("", null);
            created.start = false;
            created.url = httpsFirst(created, target);
            switchTo(created);
            persist();
            return;
        }
        BTab tab = active;
        tab.start = false;
        tab.url = httpsFirst(tab, target);
        ensureView(tab);
        if (tab.view != null) {
            tab.view.loadUrl(tab.url);
        }
        updateBar();
    }

    private void loadStartPage(BTab tab) {
        if (tab.view == null) {
            return;
        }
        tab.start = true;
        tab.url = "";
        tab.view.loadDataWithBaseURL(START_URL, startPageHtml(), "text/html", "utf-8", null);
    }

    private String startPageHtml() {
        SearchEngines.Engine engine = SearchEngines.current(host);
        StringBuilder tiles = new StringBuilder();
        for (BrowserStore.HistoryEntry entry : store.topSites(8)) {
            String hostName = BrowserStore.hostOf(entry.url);
            if (hostName.length() == 0) {
                continue;
            }
            String letter = hostName.substring(0, 1).toUpperCase(Locale.US);
            tiles.append("<a class='t' href='").append(escapeHtml(entry.url)).append("'><i>").append(escapeHtml(letter))
                    .append("</i><span>").append(escapeHtml(hostName)).append("</span></a>");
        }
        String hint = host.getString(R.string.browser_search_hint, engine.name);
        return "<!doctype html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<style>:root{color-scheme:" + (RenaTheme.isDark() ? "dark" : "light") + "}"
                + "*{box-sizing:border-box;-webkit-tap-highlight-color:transparent}"
                + "body{margin:0;min-height:100vh;background:" + RenaTheme.cssBackground() + ";color:" + RenaTheme.cssText()
                + ";font-family:sans-serif;display:flex;flex-direction:column;align-items:center;padding:12vh 20px 32px}"
                + ".logo{font-size:34px;font-weight:800;letter-spacing:.5px}.logo b{color:#00a884}.sub{margin-top:6px;color:"
                + RenaTheme.cssMuted() + ";font-size:13px}"
                + "form{margin-top:28px;width:100%;max-width:520px;display:flex;gap:8px}"
                + "input{flex:1;height:50px;border-radius:26px;border:1px solid " + RenaTheme.cssOutline() + ";background:"
                + RenaTheme.cssSurface() + ";color:" + RenaTheme.cssText() + ";padding:0 20px;font-size:16px;outline:none}"
                + "button{width:50px;height:50px;border-radius:50%;border:0;background:#00a884;color:#fff;font-size:20px}"
                + ".g{margin-top:34px;display:grid;grid-template-columns:repeat(4,1fr);gap:14px;width:100%;max-width:520px}"
                + ".t{display:flex;flex-direction:column;align-items:center;text-decoration:none;color:" + RenaTheme.cssText() + ";font-size:11px}"
                + ".t i{width:52px;height:52px;border-radius:16px;background:" + RenaTheme.cssSurface() + ";border:1px solid "
                + RenaTheme.cssOutline() + ";display:flex;align-items:center;justify-content:center;font-style:normal;font-size:21px;font-weight:700;color:#00a884}"
                + ".t span{margin-top:7px;max-width:70px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;color:" + RenaTheme.cssMuted() + "}"
                + "</style></head><body><div class='logo'>Rena<b>Browser</b></div><div class='sub'>" + escapeHtml(engine.name) + "</div>"
                + "<form onsubmit=\"var q=document.getElementById('q').value;if(q){location.href='rena://search?q='+encodeURIComponent(q);}return false;\">"
                + "<input id='q' type='search' placeholder='" + escapeHtml(hint) + "' autocomplete='off' autocapitalize='off'><button type='submit'>\u2192</button></form>"
                + "<div class='g'>" + tiles + "</div></body></html>";
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("'", "&#39;").replace("\"", "&quot;");
    }

    private String errorPage(String title, String message, String url) {
        String safeUrl = escapeHtml(url);
        return "<!doctype html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<style>body{margin:0;background:" + RenaTheme.cssBackground() + ";color:" + RenaTheme.cssText()
                + ";font-family:sans-serif;display:flex;min-height:100vh;align-items:center;justify-content:center;padding:24px}"
                + ".c{max-width:420px;text-align:center}h1{font-size:22px;margin:0 0 10px}p{color:" + RenaTheme.cssMuted()
                + ";line-height:1.5;font-size:14px;word-break:break-word}a{display:inline-block;margin-top:22px;padding:13px 28px;background:#00a884;color:#fff;border-radius:26px;text-decoration:none;font-weight:700}"
                + ".e{font-size:46px;margin-bottom:10px}</style></head><body><div class='c'><div class='e'>\u26A0\uFE0F</div><h1>"
                + escapeHtml(title) + "</h1><p>" + escapeHtml(message) + "</p><p>" + safeUrl + "</p><a href='" + safeUrl + "'>"
                + escapeHtml(host.getString(R.string.browser_retry)) + "</a></div></body></html>";
    }

    private void askSwitchToW4b(final String url) {
        new RenaDialog.Builder(host)
                .setTitle(R.string.browser_wa_title)
                .setMessage(R.string.browser_wa_message)
                .setNegativeButton(R.string.cancel_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (active != null && active.view != null && active.view.canGoBack()) {
                            active.view.goBack();
                        }
                    }
                })
                .setPositiveButton(R.string.yes_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (active != null && !active.start && active.url.length() > 0 && SearchEngines.isWhatsAppWebUrl(active.url) &&
                                active.view != null && !active.view.canGoBack()) {
                            closeTab(active);
                        }
                        host.browserSwitchToW4b();
                    }
                })
                .show();
    }

    private final class Client extends WebViewClient {
        private final BTab tab;

        Client(BTab tab) {
            this.tab = tab;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            String url = uri.toString();
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.US);
            if ("rena".equals(scheme)) {
                String query = uri.getQueryParameter("q");
                if (query != null) {
                    openUrl(query, false);
                }
                return true;
            }
            if ("http".equals(scheme) || "https".equals(scheme)) {
                if (!request.isForMainFrame()) {
                    return false;
                }
                if (SearchEngines.isWhatsAppWebUrl(url)) {
                    askSwitchToW4b(url);
                    return true;
                }
                if ("http".equals(scheme) && RenaPrefs.httpsOnly(host)) {
                    String h = uri.getHost() == null ? "" : uri.getHost();
                    if (!httpAllowedHosts.contains(h) && !h.equals("localhost") && !h.matches("^\\d{1,3}(\\.\\d{1,3}){3}$")) {
                        tab.upgradedFrom = url;
                        view.loadUrl("https://" + url.substring(7));
                        return true;
                    }
                }
                if (RenaPrefs.tracking(host)) {
                    String stripped = AdBlocker.stripTracking(url);
                    if (!stripped.equals(url)) {
                        view.loadUrl(stripped);
                        return true;
                    }
                }
                if (RenaPrefs.adblock(host) && !request.hasGesture() && AdBlocker.isAd(uri)) {
                    return true;
                }
                return false;
            }
            if ("about".equals(scheme) || "data".equals(scheme) || "blob".equals(scheme) || "javascript".equals(scheme) ||
                    "file".equals(scheme) || "content".equals(scheme)) {
                return false;
            }
            if ("intent".equals(scheme)) {
                try {
                    Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                    String fallback = intent.getStringExtra("browser_fallback_url");
                    if (fallback != null && fallback.startsWith("http")) {
                        view.loadUrl(fallback);
                    }
                } catch (Throwable ignored) {
                }
                return true;
            }
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                host.startActivity(intent);
            } catch (Throwable ignored) {
            }
            return true;
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            String scheme = uri.getScheme();
            if (scheme == null || !(scheme.equals("http") || scheme.equals("https"))) {
                return null;
            }
            String url = uri.toString();
            try {
                StringBuilder headers = new StringBuilder();
                Map<String, String> map = request.getRequestHeaders();
                if (map != null) {
                    for (Map.Entry<String, String> entry : map.entrySet()) {
                        headers.append(entry.getKey()).append(": ").append(entry.getValue()).append('\n');
                    }
                }
                String type = request.isForMainFrame() ? "document" : guessType(url, map);
                InspectorHub.forKey(hubKey(tab)).addNative(request.getMethod(), url, type, headers.toString());
            } catch (Throwable ignored) {
            }
            if (request.isForMainFrame()) {
                return null;
            }
            boolean blocked = (RenaPrefs.adblock(host) && AdBlocker.isAd(uri)) ||
                    (RenaPrefs.tracking(host) && AdBlocker.isTracker(uri)) ||
                    (RenaPrefs.detector(host) && AdBlocker.isDetector(uri));
            if (blocked) {
                InspectorHub.forKey(hubKey(tab)).enrich(url, 0, 0L, "blocked", 0L, "rena");
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Access-Control-Allow-Origin", "*");
                return new WebResourceResponse("text/plain", "utf-8", 204, "Blocked", headers, new ByteArrayInputStream(new byte[0]));
            }
            return null;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            tab.loading = true;
            tab.progress = 8;
            if (url != null && !START_URL.equals(url)) {
                tab.url = url;
                tab.start = false;
                InspectorHub.forKey(hubKey(tab)).clearNetwork();
                InspectorHub.forKey(hubKey(tab)).addConsole("tip", "\u2014 " + url);
            }
            WebEngine.injectFallback(view);
            if (tab == active) {
                updateBar();
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            tab.loading = false;
            tab.progress = 100;
            if (url != null && !START_URL.equals(url)) {
                tab.url = url;
                tab.start = false;
                String title = view.getTitle();
                if (!TextUtils.isEmpty(title)) {
                    tab.title = title;
                }
                store.addHistory(url, tab.title);
                if (tab == active) {
                    scheduleThumb();
                }
            } else if (START_URL.equals(url) && tab.start) {
                tab.url = "";
            }
            try {
                CookieManager manager = tab.cookies != null ? tab.cookies : CookieManager.getInstance();
                manager.flush();
            } catch (Throwable ignored) {
            }
            if (!WebEngine.hasDocStart(view)) {
                if (RenaPrefs.bool(host, RenaPrefs.TRANSLATE_BROWSER, false)) {
                    WebEngine.setTranslate(host, view, true);
                }
                WebEngine.switchTheme(host, view);
            }
            if (tab == active) {
                updateBar();
            }
            persistLater();
        }

        @Override
        public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
            if (url != null && !START_URL.equals(url) && !url.startsWith("data:")) {
                tab.url = url;
                tab.start = false;
                if (tab == active) {
                    updateBar();
                }
            }
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            if (!request.isForMainFrame()) {
                return;
            }
            String failing = request.getUrl().toString();
            String description = error.getDescription() == null ? "" : error.getDescription().toString();
            if (failing.startsWith("https://") && tab.upgradedFrom != null && failing.equals("https://" + tab.upgradedFrom.substring(7))) {
                final String original = tab.upgradedFrom;
                tab.upgradedFrom = null;
                new RenaDialog.Builder(host)
                        .setTitle(R.string.browser_https_failed_title)
                        .setMessage(host.getString(R.string.browser_https_failed_message, BrowserStore.hostOf(original)))
                        .setNegativeButton(R.string.cancel_text, null)
                        .setPositiveButton(R.string.browser_continue_http, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                httpAllowedHosts.add(BrowserStore.hostOf(original));
                                if (tab.view != null) {
                                    tab.view.loadUrl(original);
                                }
                            }
                        })
                        .show();
                return;
            }
            String title = host.getString(R.string.browser_error_title);
            String message = RenaPrefs.offlineMode(host) && !WebEngine.hasNetwork(host)
                    ? host.getString(R.string.browser_error_offline) : description;
            view.loadDataWithBaseURL(failing, errorPage(title, message, failing), "text/html", "utf-8", failing);
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, android.net.http.SslError error) {
            handler.cancel();
            String failing = error.getUrl();
            view.loadDataWithBaseURL(failing, errorPage(host.getString(R.string.browser_ssl_title),
                    host.getString(R.string.browser_ssl_message), failing), "text/html", "utf-8", failing);
        }

        @Override
        public void onReceivedHttpAuthRequest(WebView view, final HttpAuthHandler authHandler, String hostName, String realm) {
            LinearLayout form = new LinearLayout(host);
            form.setOrientation(LinearLayout.VERTICAL);
            final EditText user = styledInput(host.getString(R.string.browser_auth_user), false);
            final EditText pass = styledInput(host.getString(R.string.browser_auth_pass), true);
            form.addView(user, new LinearLayout.LayoutParams(-1, dp(48)));
            LinearLayout.LayoutParams passLp = new LinearLayout.LayoutParams(-1, dp(48));
            passLp.topMargin = dp(10);
            form.addView(pass, passLp);
            new RenaDialog.Builder(host)
                    .setTitle(hostName)
                    .setMessage(realm)
                    .setView(form)
                    .setNegativeButton(R.string.cancel_text, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            authHandler.cancel();
                        }
                    })
                    .setPositiveButton(R.string.okay_text, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            authHandler.proceed(user.getText().toString(), pass.getText().toString());
                        }
                    })
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            authHandler.cancel();
                        }
                    })
                    .show();
        }

        @Override
        public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
            final BTab dead = tab;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    releaseView(dead, false);
                    if (dead == active && visible) {
                        attachActive();
                    }
                }
            });
            return true;
        }
    }

    private String guessType(String url, Map<String, String> headers) {
        String lower = url.toLowerCase(Locale.US);
        int q = lower.indexOf('?');
        if (q > 0) {
            lower = lower.substring(0, q);
        }
        if (lower.endsWith(".js") || lower.endsWith(".mjs")) {
            return "script";
        }
        if (lower.endsWith(".css")) {
            return "stylesheet";
        }
        if (lower.matches(".*\\.(png|jpg|jpeg|gif|webp|svg|ico|avif|bmp)$")) {
            return "image";
        }
        if (lower.matches(".*\\.(woff2?|ttf|otf|eot)$")) {
            return "font";
        }
        if (lower.matches(".*\\.(mp4|webm|m3u8|mpd|mp3|ogg|ts|m4s)$")) {
            return "media";
        }
        if (lower.endsWith(".json")) {
            return "json";
        }
        if (headers != null) {
            String accept = headers.get("Accept");
            if (accept != null && accept.contains("application/json")) {
                return "xhr";
            }
        }
        return "other";
    }

    private void persistLater() {
        handler.removeCallbacks(persistRunnable);
        handler.postDelayed(persistRunnable, 1200L);
    }

    private final Runnable persistRunnable = new Runnable() {
        @Override
        public void run() {
            persist();
        }
    };

    private final class Chrome extends WebChromeClient {
        private final BTab tab;

        Chrome(BTab tab) {
            this.tab = tab;
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            tab.progress = newProgress;
            tab.loading = newProgress < 100;
            if (tab == active) {
                updateBar();
            }
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            if (!TextUtils.isEmpty(title) && !title.startsWith("about:")) {
                tab.title = title;
                store.updateTitle(tab.url, title);
                if (tab == active) {
                    refreshDrawerIfOpen();
                }
            }
        }

        @Override
        public boolean onConsoleMessage(ConsoleMessage message) {
            String level = "log";
            if (message.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                level = "error";
            } else if (message.messageLevel() == ConsoleMessage.MessageLevel.WARNING) {
                level = "warn";
            } else if (message.messageLevel() == ConsoleMessage.MessageLevel.DEBUG) {
                level = "debug";
            }
            String source = message.sourceId() == null ? "" : message.sourceId();
            int slash = source.lastIndexOf('/');
            if (slash >= 0 && slash < source.length() - 1) {
                source = source.substring(slash + 1);
            }
            InspectorHub.forKey(hubKey(tab)).addConsole(level, message.message() +
                    (source.length() > 0 ? "  (" + source + ":" + message.lineNumber() + ")" : ""));
            return true;
        }

        @Override
        public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
            boolean multiple = false;
            try {
                multiple = params.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE;
            } catch (Throwable ignored) {
            }
            return host.browserFileChooser(callback, multiple);
        }

        @Override
        public void onPermissionRequest(final PermissionRequest request) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    String origin = request.getOrigin() == null ? "" : request.getOrigin().getHost();
                    new RenaDialog.Builder(host)
                            .setTitle(R.string.browser_permission_title)
                            .setMessage(host.getString(R.string.browser_permission_message, origin))
                            .setNegativeButton(R.string.permission_deny_button, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    request.deny();
                                }
                            })
                            .setPositiveButton(R.string.permission_allow_button, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    host.browserPermission(request);
                                }
                            })
                            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    request.deny();
                                }
                            })
                            .show();
                }
            });
        }

        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
            if (!isUserGesture && RenaPrefs.adblock(host)) {
                return false;
            }
            BTab created = createTab("", tab);
            created.start = false;
            ensureView(created);
            try {
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                // Resolve the public Android API at runtime for SDK stubs that declare a wrong signature.
                WebView.WebViewTransport.class.getMethod("setWebView", WebView.class).invoke(transport, created.view);
                resultMsg.sendToTarget();
            } catch (Throwable error) {
                closeTab(created);
                return false;
            }
            switchTo(created);
            persist();
            return true;
        }

        @Override
        public void onCloseWindow(WebView window) {
            BTab victim = tabFor(window);
            if (victim != null) {
                closeTab(victim);
            }
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
            new RenaDialog.Builder(host)
                    .setTitle(BrowserStore.hostOf(url))
                    .setMessage(message)
                    .setPositiveButton(R.string.okay_text, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            result.confirm();
                        }
                    })
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            result.cancel();
                        }
                    })
                    .show();
            return true;
        }

        @Override
        public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
            new RenaDialog.Builder(host)
                    .setTitle(BrowserStore.hostOf(url))
                    .setMessage(message)
                    .setNegativeButton(R.string.cancel_text, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            result.cancel();
                        }
                    })
                    .setPositiveButton(R.string.okay_text, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            result.confirm();
                        }
                    })
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            result.cancel();
                        }
                    })
                    .show();
            return true;
        }

        @Override
        public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, final JsPromptResult result) {
            final EditText input = styledInput("", false);
            input.setText(defaultValue);
            new RenaDialog.Builder(host)
                    .setTitle(BrowserStore.hostOf(url))
                    .setMessage(message)
                    .setView(input)
                    .setNegativeButton(R.string.cancel_text, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            result.cancel();
                        }
                    })
                    .setPositiveButton(R.string.okay_text, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            result.confirm(input.getText().toString());
                        }
                    })
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            result.cancel();
                        }
                    })
                    .show();
            return true;
        }

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            if (customView != null) {
                callback.onCustomViewHidden();
                return;
            }
            customView = view;
            customViewCallback = callback;
            fullscreenHolder.removeAllViews();
            fullscreenHolder.addView(view, new FrameLayout.LayoutParams(-1, -1));
            fullscreenHolder.setVisibility(View.VISIBLE);
            host.browserSetFullscreen(true);
        }

        @Override
        public void onHideCustomView() {
            hideCustomView();
        }

        @Override
        public Bitmap getDefaultVideoPoster() {
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        }
    }

    private void hideCustomView() {
        if (customView == null) {
            return;
        }
        fullscreenHolder.removeAllViews();
        fullscreenHolder.setVisibility(View.GONE);
        customView = null;
        host.browserSetFullscreen(false);
        try {
            if (customViewCallback != null) {
                customViewCallback.onCustomViewHidden();
            }
        } catch (Throwable ignored) {
        }
        customViewCallback = null;
    }

    private EditText styledInput(String hint, boolean password) {
        EditText input = new EditText(host);
        input.setHint(hint);
        input.setHintTextColor(RenaTheme.faint());
        input.setTextColor(RenaTheme.text());
        input.setTextSize(15f);
        input.setSingleLine(true);
        input.setTypeface(Ui.font(host, Typeface.NORMAL));
        input.setInputType(password ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD : InputType.TYPE_CLASS_TEXT);
        input.setBackground(Ui.roundStroke(RenaTheme.surface2(), dp(14), RenaTheme.outline(), dp(1)));
        input.setPadding(dp(14), 0, dp(14), 0);
        return input;
    }

    private boolean showContextMenu(final BTab tab, final WebView view) {
        WebView.HitTestResult result = view.getHitTestResult();
        if (result == null) {
            return false;
        }
        final int type = result.getType();
        final String extra = result.getExtra();
        if (TextUtils.isEmpty(extra)) {
            return false;
        }
        final ArrayList<String> labels = new ArrayList<String>();
        final ArrayList<Integer> actions = new ArrayList<Integer>();
        boolean image = type == WebView.HitTestResult.IMAGE_TYPE || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE;
        boolean link = type == WebView.HitTestResult.SRC_ANCHOR_TYPE || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE;
        if (type == WebView.HitTestResult.PHONE_TYPE || type == WebView.HitTestResult.EMAIL_TYPE || type == WebView.HitTestResult.GEO_TYPE) {
            return false;
        }
        if (link) {
            labels.add(host.getString(R.string.browser_ctx_new_tab));
            actions.add(0);
            labels.add(host.getString(R.string.browser_ctx_copy_link));
            actions.add(1);
            labels.add(host.getString(R.string.browser_ctx_share));
            actions.add(2);
            labels.add(host.getString(R.string.browser_ctx_download_link));
            actions.add(3);
        }
        if (image) {
            labels.add(host.getString(R.string.browser_ctx_image_tab));
            actions.add(4);
            labels.add(host.getString(R.string.browser_ctx_save_image));
            actions.add(5);
            labels.add(host.getString(R.string.browser_ctx_copy_image));
            actions.add(6);
        }
        if (labels.isEmpty()) {
            return false;
        }
        final Message message = handler.obtainMessage();
        view.requestFocusNodeHref(message);
        Bundle data = message.getData();
        String hrefValue = data == null ? null : data.getString("url");
        String srcValue = data == null ? null : data.getString("src");
        final String linkUrl = !TextUtils.isEmpty(hrefValue) ? hrefValue : extra;
        final String imageUrl = !TextUtils.isEmpty(srcValue) ? srcValue : extra;
        new RenaDialog.Builder(host)
                .setTitle(Ui.ellipsize(link ? linkUrl : imageUrl, 60))
                .setItems(labels.toArray(new String[labels.size()]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int action = actions.get(which);
                        switch (action) {
                            case 0:
                                openUrl(linkUrl, true);
                                break;
                            case 1:
                                copyToClipboard(linkUrl);
                                break;
                            case 2:
                                shareText(linkUrl);
                                break;
                            case 3:
                                startDownload(tab, view, linkUrl);
                                break;
                            case 4:
                                openUrl(imageUrl, true);
                                break;
                            case 5:
                                startDownload(tab, view, imageUrl);
                                break;
                            default:
                                copyToClipboard(imageUrl);
                                break;
                        }
                    }
                })
                .show();
        return true;
    }

    private void startDownload(BTab tab, WebView view, String url) {
        String cookie = null;
        try {
            cookie = tab.cookies == null ? null : tab.cookies.getCookie(url);
        } catch (Throwable ignored) {
        }
        RenaDownloadHelper.handle(host, view, "br:" + tab.id, tab.id, url, view.getSettings().getUserAgentString(), null, null, -1L,
                cookie, view.getUrl());
    }

    private void copyToClipboard(String text) {
        try {
            ClipboardManager manager = (ClipboardManager) host.getSystemService(Context.CLIPBOARD_SERVICE);
            manager.setPrimaryClip(ClipData.newPlainText("Rena", text));
            Toast.makeText(host, R.string.copied_text, Toast.LENGTH_SHORT).show();
        } catch (Throwable ignored) {
        }
    }

    private void shareText(String text) {
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, text);
            host.startActivity(Intent.createChooser(intent, host.getString(R.string.browser_share)));
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onTranslateClosed() {
        RenaPrefs.setBool(host, RenaPrefs.TRANSLATE_BROWSER, false);
        for (BTab tab : tabs) {
            if (tab.view != null) {
                WebEngine.refresh(host, tab.view, true);
                if (tab == active) {
                    WebEngine.setTranslate(host, tab.view, false);
                }
            }
        }
        refreshDrawerIfOpen();
    }

    void closeOverlays() {
        hideKeyboard();
        if (omniboxView != null) {
            overlayHolder.removeView(omniboxView);
            omniboxView = null;
        }
        if (historyView != null) {
            overlayHolder.removeView(historyView);
            historyView = null;
        }
        if (inspector != null) {
            inspector.stopRefresh();
            overlayHolder.removeView(inspector);
            inspector = null;
        }
    }

    private void hideKeyboard() {
        try {
            InputMethodManager manager = (InputMethodManager) host.getSystemService(Context.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(container.getWindowToken(), 0);
        } catch (Throwable ignored) {
        }
    }

    private static final class Suggestion {
        static final int GO = 0;
        static final int SEARCH = 1;
        static final int HISTORY = 2;
        int type;
        String title;
        String subtitle;
        String value;
    }

    void showOmnibox() {
        if (omniboxView != null) {
            return;
        }
        closeOverlays();
        setDrawerOpen(false);
        final FrameLayout overlay = new FrameLayout(host);
        overlay.setBackgroundColor(RenaTheme.bg());
        overlay.setClickable(true);
        LinearLayout column = new LinearLayout(host);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(0, insetTop + dp(8), 0, insetBottom);
        overlay.addView(column, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), 0, dp(12), dp(8));
        TextView back = Ui.text(host, "\u2190", 22f, RenaTheme.text(), Typeface.NORMAL);
        back.setGravity(Gravity.CENTER);
        back.setBackground(Ui.ripple(host, Ui.round(0, dp(24)), 24));
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeOverlays();
            }
        });
        row.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));
        final EditText input = new EditText(host);
        input.setHint(host.getString(R.string.browser_search_hint, SearchEngines.current(host).name));
        input.setHintTextColor(RenaTheme.faint());
        input.setTextColor(RenaTheme.text());
        input.setTextSize(15.5f);
        input.setSingleLine(true);
        input.setTypeface(Ui.font(host, Typeface.NORMAL));
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setImeOptions(EditorInfo.IME_ACTION_GO);
        input.setSelectAllOnFocus(true);
        input.setBackground(Ui.roundStroke(RenaTheme.surface2(), dp(24), RenaTheme.outline(), dp(1)));
        input.setPadding(dp(18), 0, dp(40), 0);
        FrameLayout inputWrap = new FrameLayout(host);
        inputWrap.addView(input, new FrameLayout.LayoutParams(-1, dp(48)));
        TextView clear = Ui.text(host, "\u2715", 15f, RenaTheme.muted(), Typeface.NORMAL);
        clear.setGravity(Gravity.CENTER);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                input.setText("");
            }
        });
        inputWrap.addView(clear, new FrameLayout.LayoutParams(dp(40), dp(48), Gravity.END));
        row.addView(inputWrap, new LinearLayout.LayoutParams(0, dp(48), 1f));
        column.addView(row, new LinearLayout.LayoutParams(-1, -2));

        final ListView list = new ListView(host);
        list.setDivider(null);
        list.setSelector(android.R.color.transparent);
        final ArrayList<Suggestion> items = new ArrayList<Suggestion>();
        final BaseAdapter adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return items.size();
            }

            @Override
            public Object getItem(int position) {
                return items.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                LinearLayout line;
                if (convertView instanceof LinearLayout) {
                    line = (LinearLayout) convertView;
                    line.removeAllViews();
                } else {
                    line = new LinearLayout(host);
                    line.setOrientation(LinearLayout.HORIZONTAL);
                    line.setGravity(Gravity.CENTER_VERTICAL);
                }
                line.setPadding(dp(18), dp(10), dp(18), dp(10));
                Suggestion item = items.get(position);
                TextView icon = Ui.text(host, item.type == Suggestion.SEARCH ? "\uD83D\uDD0D" : item.type == Suggestion.GO ? "\u2192" : "\uD83D\uDD52",
                        16f, RenaTheme.accentText(), Typeface.NORMAL);
                icon.setGravity(Gravity.CENTER);
                icon.setBackground(Ui.round(RenaTheme.accentSoft(), dp(18)));
                line.addView(icon, new LinearLayout.LayoutParams(dp(36), dp(36)));
                LinearLayout text = new LinearLayout(host);
                text.setOrientation(LinearLayout.VERTICAL);
                TextView title = Ui.text(host, item.title, 14.5f, RenaTheme.text(), Typeface.NORMAL);
                title.setSingleLine(true);
                title.setEllipsize(TextUtils.TruncateAt.END);
                text.addView(title, new LinearLayout.LayoutParams(-2, -2));
                if (!TextUtils.isEmpty(item.subtitle)) {
                    TextView sub = Ui.text(host, item.subtitle, 12f, RenaTheme.muted(), Typeface.NORMAL);
                    sub.setSingleLine(true);
                    sub.setEllipsize(TextUtils.TruncateAt.MIDDLE);
                    LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-2, -2);
                    subLp.topMargin = dp(3);
                    text.addView(sub, subLp);
                }
                LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, -2, 1f);
                textLp.leftMargin = dp(14);
                line.addView(text, textLp);
                return line;
            }
        };
        list.setAdapter(adapter);
        column.addView(list, new LinearLayout.LayoutParams(-1, 0, 1f));

        final Runnable rebuild = new Runnable() {
            @Override
            public void run() {
                String typed = input.getText().toString().trim();
                items.clear();
                if (typed.length() > 0) {
                    Suggestion search = new Suggestion();
                    search.type = Suggestion.SEARCH;
                    search.title = typed;
                    search.subtitle = host.getString(R.string.browser_search_with, SearchEngines.current(host).name);
                    search.value = typed;
                    String resolved = SearchEngines.resolve(host, typed);
                    if (resolved.length() > 0 && !resolved.equals(SearchEngines.searchUrl(host, typed))) {
                        Suggestion go = new Suggestion();
                        go.type = Suggestion.GO;
                        go.title = resolved;
                        go.subtitle = host.getString(R.string.browser_go_to);
                        go.value = typed;
                        items.add(go);
                    }
                    items.add(search);
                }
                String needle = typed.toLowerCase(Locale.US);
                HashSet<String> seen = new HashSet<String>();
                ArrayList<BrowserStore.HistoryEntry> pool = needle.length() == 0 ? store.topSites(8) : store.history();
                for (BrowserStore.HistoryEntry entry : pool) {
                    if (items.size() >= 9) {
                        break;
                    }
                    if (needle.length() > 0 && !entry.url.toLowerCase(Locale.US).contains(needle) &&
                            !entry.title.toLowerCase(Locale.US).contains(needle)) {
                        continue;
                    }
                    if (!seen.add(entry.url)) {
                        continue;
                    }
                    Suggestion h = new Suggestion();
                    h.type = Suggestion.HISTORY;
                    h.title = TextUtils.isEmpty(entry.title) ? BrowserStore.hostOf(entry.url) : entry.title;
                    h.subtitle = entry.url;
                    h.value = entry.url;
                    items.add(h);
                }
                adapter.notifyDataSetChanged();
            }
        };
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                rebuild.run();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        list.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(android.widget.AdapterView<?> parent, View view, int position, long id) {
                Suggestion item = items.get(position);
                closeOverlays();
                openUrl(item.value, false);
            }
        });
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {
                String typed = input.getText().toString().trim();
                if (typed.length() > 0) {
                    closeOverlays();
                    openUrl(typed, false);
                }
                return true;
            }
        });
        overlayHolder.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
        omniboxView = overlay;
        if (active != null && !active.start && !TextUtils.isEmpty(active.url)) {
            input.setText(active.url);
        }
        rebuild.run();
        input.requestFocus();
        input.selectAll();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    InputMethodManager manager = (InputMethodManager) host.getSystemService(Context.INPUT_METHOD_SERVICE);
                    manager.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
                } catch (Throwable ignored) {
                }
            }
        }, 120L);
    }

    void showHistory() {
        closeOverlays();
        setDrawerOpen(false);
        final FrameLayout overlay = new FrameLayout(host);
        overlay.setBackgroundColor(RenaTheme.bg());
        overlay.setClickable(true);
        LinearLayout column = new LinearLayout(host);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(0, insetTop + dp(6), 0, insetBottom);
        overlay.addView(column, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout header = new LinearLayout(host);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(8), 0, dp(8), dp(4));
        TextView back = Ui.text(host, "\u2190", 22f, RenaTheme.text(), Typeface.NORMAL);
        back.setGravity(Gravity.CENTER);
        back.setBackground(Ui.ripple(host, Ui.round(0, dp(24)), 24));
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeOverlays();
            }
        });
        header.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));
        header.addView(Ui.text(host, host.getString(R.string.browser_history), 21f, RenaTheme.text(), Typeface.BOLD),
                new LinearLayout.LayoutParams(0, -2, 1f));
        TextView clearAll = Ui.text(host, host.getString(R.string.clear_text), 14f, RenaTheme.accentText(), Typeface.BOLD);
        clearAll.setGravity(Gravity.CENTER);
        clearAll.setPadding(dp(14), 0, dp(14), 0);
        header.addView(clearAll, new LinearLayout.LayoutParams(-2, dp(44)));
        column.addView(header, new LinearLayout.LayoutParams(-1, -2));

        final EditText search = styledInput(host.getString(R.string.browser_history_search), false);
        search.setBackground(Ui.roundStroke(RenaTheme.surface(), dp(22), RenaTheme.outline(), dp(1)));
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(-1, dp(44));
        searchLp.setMargins(dp(14), dp(2), dp(14), dp(8));
        column.addView(search, searchLp);

        final ListView list = new ListView(host);
        list.setDivider(null);
        list.setSelector(android.R.color.transparent);
        final ArrayList<Object> rows = new ArrayList<Object>();
        final BaseAdapter adapter = new BaseAdapter() {
            @Override
            public int getViewTypeCount() {
                return 2;
            }

            @Override
            public int getItemViewType(int position) {
                return rows.get(position) instanceof String ? 0 : 1;
            }

            @Override
            public int getCount() {
                return rows.size();
            }

            @Override
            public Object getItem(int position) {
                return rows.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                Object row = rows.get(position);
                if (row instanceof String) {
                    TextView headerView = convertView instanceof TextView ? (TextView) convertView : Ui.sectionTitle(host, "");
                    headerView.setText((String) row);
                    headerView.setPadding(dp(20), dp(16), dp(20), dp(6));
                    return headerView;
                }
                BrowserStore.HistoryEntry entry = (BrowserStore.HistoryEntry) row;
                LinearLayout line;
                if (convertView instanceof LinearLayout) {
                    line = (LinearLayout) convertView;
                    line.removeAllViews();
                } else {
                    line = new LinearLayout(host);
                    line.setOrientation(LinearLayout.VERTICAL);
                }
                line.setPadding(dp(20), dp(9), dp(20), dp(9));
                TextView title = Ui.text(host, TextUtils.isEmpty(entry.title) ? BrowserStore.hostOf(entry.url) : entry.title,
                        14.5f, RenaTheme.text(), Typeface.NORMAL);
                title.setSingleLine(true);
                title.setEllipsize(TextUtils.TruncateAt.END);
                line.addView(title, new LinearLayout.LayoutParams(-2, -2));
                TextView sub = Ui.text(host, entry.url, 12f, RenaTheme.muted(), Typeface.NORMAL);
                sub.setSingleLine(true);
                sub.setEllipsize(TextUtils.TruncateAt.MIDDLE);
                LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-2, -2);
                subLp.topMargin = dp(3);
                line.addView(sub, subLp);
                return line;
            }
        };
        list.setAdapter(adapter);
        column.addView(list, new LinearLayout.LayoutParams(-1, 0, 1f));

        final Runnable rebuild = new Runnable() {
            @Override
            public void run() {
                String needle = search.getText().toString().trim().toLowerCase(Locale.US);
                rows.clear();
                String lastGroup = "";
                DateFormat format = DateFormat.getDateInstance(DateFormat.MEDIUM, RenaLocale.localeFor(host));
                long now = System.currentTimeMillis();
                int count = 0;
                for (BrowserStore.HistoryEntry entry : store.history()) {
                    if (needle.length() > 0 && !entry.url.toLowerCase(Locale.US).contains(needle) &&
                            !entry.title.toLowerCase(Locale.US).contains(needle)) {
                        continue;
                    }
                    long days = (now - entry.time) / 86400000L;
                    String group = days <= 0 ? host.getString(R.string.date_today) : days == 1 ? host.getString(R.string.date_yesterday)
                            : format.format(new Date(entry.time));
                    if (!group.equals(lastGroup)) {
                        rows.add(group);
                        lastGroup = group;
                    }
                    rows.add(entry);
                    if (++count >= 600) {
                        break;
                    }
                }
                adapter.notifyDataSetChanged();
            }
        };
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                rebuild.run();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        list.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(android.widget.AdapterView<?> parent, View view, int position, long id) {
                Object row = rows.get(position);
                if (row instanceof BrowserStore.HistoryEntry) {
                    closeOverlays();
                    openUrl(((BrowserStore.HistoryEntry) row).url, false);
                }
            }
        });
        list.setOnItemLongClickListener(new android.widget.AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(android.widget.AdapterView<?> parent, View view, int position, long id) {
                Object row = rows.get(position);
                if (row instanceof BrowserStore.HistoryEntry) {
                    final BrowserStore.HistoryEntry entry = (BrowserStore.HistoryEntry) row;
                    new RenaDialog.Builder(host)
                            .setTitle(Ui.ellipsize(entry.url, 60))
                            .setNegativeButton(R.string.cancel_text, null)
                            .setPositiveButton(R.string.download_delete_text, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    store.removeHistory(entry.url);
                                    rebuild.run();
                                }
                            })
                            .show();
                }
                return true;
            }
        });
        clearAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showClearDialog(new Runnable() {
                    @Override
                    public void run() {
                        rebuild.run();
                    }
                });
            }
        });
        overlayHolder.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
        historyView = overlay;
        rebuild.run();
    }

    void openInspector() {
        if (active == null || active.view == null) {
            return;
        }
        closeOverlays();
        setDrawerOpen(false);
        final BTab tab = active;
        inspector = new InspectorPanel(host, new InspectorPanel.Target() {
            @Override
            public WebView webView() {
                return tab.view;
            }

            @Override
            public String hubKey() {
                return BrowserController.this.hubKey(tab);
            }

            @Override
            public CookieManager cookieManager() {
                return tab.cookies;
            }

            @Override
            public String userAgent() {
                return tab.view == null ? "" : tab.view.getSettings().getUserAgentString();
            }
        }, new Runnable() {
            @Override
            public void run() {
                closeOverlays();
            }
        });
        overlayHolder.addView(inspector, new FrameLayout.LayoutParams(-1, -1));
    }

    boolean onBack() {
        if (customView != null) {
            hideCustomView();
            return true;
        }
        if (inspector != null || omniboxView != null || historyView != null) {
            closeOverlays();
            return true;
        }
        if (drawerOpen) {
            setDrawerOpen(false);
            return true;
        }
        if (active != null && active.view != null && active.view.canGoBack()) {
            active.view.goBack();
            return true;
        }
        if (tabs.size() > 1 && active != null && active.opener != null) {
            closeTab(active);
            return true;
        }
        return false;
    }

    boolean isDrawerOpen() {
        return drawerOpen;
    }

    void toggleDrawer() {
        setDrawerOpen(!drawerOpen);
    }

    void setDrawerOpen(final boolean open) {
        if (open == drawerOpen && !drawerAnimating) {
            return;
        }
        if (open) {
            rebuildDrawer();
            hideKeyboard();
            if (active != null && active.view != null) {
                captureThumb(active);
            }
        }
        drawerOpen = open;
        drawerAnimating = true;
        if (drawerAnimator != null) {
            drawerAnimator.cancel();
        }
        drawer.setVisibility(View.VISIBLE);
        scrim.setVisibility(View.VISIBLE);
        drawerAnimator = ValueAnimator.ofFloat(drawerProgress, open ? 1f : 0f);
        drawerAnimator.setDuration(RenaPrefs.bool(host, "reduce_animations", false) ? 1L : 260L);
        drawerAnimator.setInterpolator(new android.view.animation.DecelerateInterpolator(1.6f));
        drawerAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                drawerProgress = (Float) animation.getAnimatedValue();
                drawer.setTranslationX((1f - drawerProgress) * drawerWidth);
                scrim.setAlpha(drawerProgress * 0.5f);
            }
        });
        drawerAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                drawerAnimating = false;
                if (!drawerOpen) {
                    drawer.setVisibility(View.GONE);
                    scrim.setVisibility(View.GONE);
                }
            }
        });
        drawerAnimator.start();
    }

    private void refreshDrawerIfOpen() {
        if (drawerOpen) {
            rebuildDrawer();
        }
    }

    private View chip(String glyph, int iconRes) {
        FrameLayout chip = new FrameLayout(host);
        if (iconRes != 0) {
            chip.setBackground(Ui.round(Color.WHITE, dp(12)));
            ImageView icon = new ImageView(host);
            icon.setImageResource(iconRes);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            chip.addView(icon, new FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER));
        } else {
            chip.setBackground(Ui.round(RenaTheme.accentSoft(), dp(12)));
            TextView text = Ui.text(host, glyph, 17f, RenaTheme.accentText(), Typeface.NORMAL);
            text.setGravity(Gravity.CENTER);
            chip.addView(text, new FrameLayout.LayoutParams(-1, -1));
        }
        return chip;
    }

    private View settingRow(String glyph, int iconRes, String title, String subtitle, View.OnClickListener click) {
        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(8), dp(10), dp(8));
        row.setBackground(Ui.pressable(host, RenaTheme.surface(), 16));
        row.addView(chip(glyph, iconRes), new LinearLayout.LayoutParams(dp(38), dp(38)));
        LinearLayout text = new LinearLayout(host);
        text.setOrientation(LinearLayout.VERTICAL);
        TextView t = Ui.text(host, title, 14.5f, RenaTheme.text(), Typeface.BOLD);
        t.setSingleLine(true);
        text.addView(t, new LinearLayout.LayoutParams(-2, -2));
        if (!TextUtils.isEmpty(subtitle)) {
            TextView s = Ui.text(host, subtitle, 12f, RenaTheme.muted(), Typeface.NORMAL);
            s.setSingleLine(true);
            s.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(-2, -2);
            sLp.topMargin = dp(2);
            text.addView(s, sLp);
        }
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, -2, 1f);
        textLp.leftMargin = dp(12);
        row.addView(text, textLp);
        if (click != null) {
            row.addView(Ui.text(host, "\u203A", 22f, RenaTheme.faint(), Typeface.NORMAL), new LinearLayout.LayoutParams(-2, -2));
            row.setOnClickListener(click);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(8);
        drawerBody.addView(row, lp);
        return row;
    }

    private void toggleRow(String glyph, String title, String subtitle, boolean checked, final View.OnClickListener onToggle,
                           final String key) {
        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(8), dp(8), dp(8));
        row.setBackground(Ui.pressable(host, RenaTheme.surface(), 16));
        row.addView(chip(glyph, 0), new LinearLayout.LayoutParams(dp(38), dp(38)));
        LinearLayout text = new LinearLayout(host);
        text.setOrientation(LinearLayout.VERTICAL);
        text.addView(Ui.text(host, title, 14.5f, RenaTheme.text(), Typeface.BOLD), new LinearLayout.LayoutParams(-2, -2));
        if (!TextUtils.isEmpty(subtitle)) {
            TextView s = Ui.text(host, subtitle, 12f, RenaTheme.muted(), Typeface.NORMAL);
            LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(-2, -2);
            sLp.topMargin = dp(2);
            text.addView(s, sLp);
        }
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, -2, 1f);
        textLp.leftMargin = dp(12);
        row.addView(text, textLp);
        final Switch toggle = Ui.toggle(host, checked);
        toggle.setClickable(false);
        row.addView(toggle, new LinearLayout.LayoutParams(-2, -2));
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean next = !RenaPrefs.bool(host, key, key.equals(RenaPrefs.BR_ADBLOCK) || key.equals(RenaPrefs.BR_TRACKING) ||
                        key.equals(RenaPrefs.BR_DETECTOR) || key.equals(RenaPrefs.BR_HTTPS_ONLY));
                RenaPrefs.setBool(host, key, next);
                toggle.setChecked(next);
                onToggle.onClick(v);
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(8);
        drawerBody.addView(row, lp);
    }

    private void section(String title) {
        TextView view = Ui.sectionTitle(host, title);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.topMargin = dp(18);
        lp.bottomMargin = dp(8);
        drawerBody.addView(view, lp);
    }

    private TextView pillButton(String label, boolean primary) {
        TextView button = Ui.text(host, label, 14f, primary ? Color.WHITE : RenaTheme.accentText(), Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(16), 0, dp(16), 0);
        button.setBackground(Ui.ripple(host, primary ? Ui.round(RenaTheme.accent(), dp(22)) :
                Ui.roundStroke(RenaTheme.accentSoft(), dp(22), RenaTheme.accent(), dp(1)), 22));
        return button;
    }

    private void rebuildDrawer() {
        drawerBody.removeAllViews();
        drawerBody.setPadding(dp(16), dp(18), dp(16), dp(32));

        LinearLayout head = new LinearLayout(host);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView logo = Ui.text(host, "R", 22f, Color.WHITE, Typeface.BOLD);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(Ui.round(RenaTheme.accent(), dp(16)));
        head.addView(logo, new LinearLayout.LayoutParams(dp(46), dp(46)));
        LinearLayout titles = new LinearLayout(host);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.addView(Ui.text(host, host.getString(R.string.browser_name), 21f, RenaTheme.text(), Typeface.BOLD), new LinearLayout.LayoutParams(-2, -2));
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-2, -2);
        subLp.topMargin = dp(3);
        titles.addView(Ui.text(host, host.getString(R.string.browser_tabs_count, tabs.size()), 12.5f, RenaTheme.muted(), Typeface.NORMAL), subLp);
        LinearLayout.LayoutParams titlesLp = new LinearLayout.LayoutParams(0, -2, 1f);
        titlesLp.leftMargin = dp(12);
        head.addView(titles, titlesLp);
        drawerBody.addView(head, new LinearLayout.LayoutParams(-1, -2));

        TextView backToW4b = pillButton("\u2190  " + host.getString(R.string.browser_back_to_w4b), true);
        backToW4b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setDrawerOpen(false);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        host.browserSwitchToW4b(true);
                    }
                }, 260L);
            }
        });
        LinearLayout.LayoutParams backLp = new LinearLayout.LayoutParams(-1, dp(46));
        backLp.topMargin = dp(16);
        drawerBody.addView(backToW4b, backLp);

        LinearLayout nav = new LinearLayout(host);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        String[] glyphs = new String[]{"\u2190", "\u2192", "\u27F3", "\u2302", "\u22EE"};
        for (int i = 0; i < glyphs.length; i++) {
            final int index = i;
            TextView button = Ui.text(host, glyphs[i], 20f, RenaTheme.text(), Typeface.NORMAL);
            button.setGravity(Gravity.CENTER);
            button.setBackground(Ui.pressable(host, RenaTheme.surface(), 14));
            boolean enabled = true;
            if (i == 0) {
                enabled = active != null && active.view != null && active.view.canGoBack();
            } else if (i == 1) {
                enabled = active != null && active.view != null && active.view.canGoForward();
            }
            button.setAlpha(enabled ? 1f : 0.35f);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switch (index) {
                        case 0:
                            goBackStep();
                            break;
                        case 1:
                            goForwardStep();
                            break;
                        case 2:
                            reloadOrStop();
                            break;
                        case 3:
                            goHome();
                            break;
                        default:
                            showMoreActions();
                            return;
                    }
                    setDrawerOpen(false);
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(46), 1f);
            lp.leftMargin = i == 0 ? 0 : dp(8);
            nav.addView(button, lp);
        }
        LinearLayout.LayoutParams navLp = new LinearLayout.LayoutParams(-1, -2);
        navLp.topMargin = dp(14);
        drawerBody.addView(nav, navLp);

        section(host.getString(R.string.browser_tabs_section));
        LinearLayout tabActions = new LinearLayout(host);
        tabActions.setOrientation(LinearLayout.HORIZONTAL);
        TextView newTab = pillButton("\uFF0B  " + host.getString(R.string.browser_new_tab), false);
        newTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newTabFromUi();
            }
        });
        tabActions.addView(newTab, new LinearLayout.LayoutParams(0, dp(42), 1f));
        TextView closeAll = pillButton(host.getString(R.string.browser_close_all), false);
        closeAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new RenaDialog.Builder(host)
                        .setTitle(R.string.browser_close_all)
                        .setMessage(R.string.browser_close_all_message)
                        .setNegativeButton(R.string.cancel_text, null)
                        .setPositiveButton(R.string.yes_text, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                closeAllTabs();
                            }
                        })
                        .show();
            }
        });
        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(0, dp(42), 1f);
        closeLp.leftMargin = dp(8);
        tabActions.addView(closeAll, closeLp);
        LinearLayout.LayoutParams tabActionsLp = new LinearLayout.LayoutParams(-1, -2);
        tabActionsLp.bottomMargin = dp(10);
        drawerBody.addView(tabActions, tabActionsLp);
        for (int i = tabs.size() - 1; i >= 0; i--) {
            drawerBody.addView(tabRow(tabs.get(i)), tabRowParams());
        }

        section(host.getString(R.string.browser_tools_section));
        settingRow("\uD83D\uDD52", 0, host.getString(R.string.browser_history), host.getString(R.string.browser_history_sub), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHistory();
            }
        });
        settingRow("\u2B07", 0, host.getString(R.string.downloads_title), host.getString(R.string.browser_downloads_sub), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setDrawerOpen(false);
                host.startActivity(new Intent(host, DownloadActivity.class));
            }
        });
        settingRow("\uD83D\uDD0D", 0, host.getString(R.string.inspect_title), host.getString(R.string.browser_inspect_sub), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openInspector();
            }
        });

        section(host.getString(R.string.switch_toggles_text));
        toggleRow("\uD83C\uDF10", host.getString(R.string.translate_title), host.getString(R.string.browser_translate_sub),
                RenaPrefs.bool(host, RenaPrefs.TRANSLATE_BROWSER, false), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean on = RenaPrefs.bool(host, RenaPrefs.TRANSLATE_BROWSER, false);
                        applyConfigAll();
                        WebEngine.setTranslate(host, activeWebView(), on);
                    }
                }, RenaPrefs.TRANSLATE_BROWSER);
        toggleRow("\uD83D\uDEE1", host.getString(R.string.browser_adblock), host.getString(R.string.browser_adblock_sub),
                RenaPrefs.adblock(host), toggleApply(true), RenaPrefs.BR_ADBLOCK);
        toggleRow("\uD83D\uDC41", host.getString(R.string.browser_tracking), host.getString(R.string.browser_tracking_sub),
                RenaPrefs.tracking(host), toggleApply(true), RenaPrefs.BR_TRACKING);
        toggleRow("\uD83D\uDEAB", host.getString(R.string.browser_detector), host.getString(R.string.browser_detector_sub),
                RenaPrefs.detector(host), toggleApply(true), RenaPrefs.BR_DETECTOR);
        toggleRow("\uD83D\uDDA5", host.getString(R.string.browser_desktop), host.getString(R.string.browser_desktop_sub),
                RenaPrefs.desktop(host), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        applyDeviceChanges();
                    }
                }, RenaPrefs.BR_DESKTOP);
        toggleRow("\uD83D\uDD12", host.getString(R.string.browser_https_only), host.getString(R.string.browser_https_only_sub),
                RenaPrefs.httpsOnly(host), toggleApply(false), RenaPrefs.BR_HTTPS_ONLY);
        toggleRow("\uD83D\uDCF4", host.getString(R.string.offline_mode_title), host.getString(R.string.offline_mode_sub),
                RenaPrefs.offlineMode(host), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        for (BTab tab : tabs) {
                            if (tab.view != null) {
                                WebEngine.applyCache(host, tab.view);
                            }
                        }
                        host.applyOfflineMode();
                    }
                }, RenaPrefs.OFFLINE_MODE);

        section(host.getString(R.string.browser_network_section));
        SearchEngines.Engine engine = SearchEngines.current(host);
        settingRow("", engine.icon, host.getString(R.string.browser_engine), engine.name, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEngineChooser(false);
            }
        });
        settingRow("\uD83E\uDDED", 0, host.getString(R.string.browser_dns), DnsManager.describe(host, DnsManager.current(host)), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDnsChooser();
            }
        });
        DeviceProfiles.Device device = DeviceProfiles.current(host);
        DeviceProfiles.Ratio ratio = DeviceProfiles.currentRatio(host);
        settingRow("\uD83D\uDCF1", 0, host.getString(R.string.device_title), device.label + "  \u00B7  " + ratio.label, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DeviceDialog.show(host, new Runnable() {
                    @Override
                    public void run() {
                        applyDeviceChanges();
                        host.refreshW4bDevice();
                        refreshDrawerIfOpen();
                    }
                });
            }
        });

        section(host.getString(R.string.browser_data_section));
        settingRow("\uD83E\uDDF9", 0, host.getString(R.string.browser_clear_data), host.getString(R.string.browser_clear_data_sub), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showClearDialog(null);
            }
        });
        settingRow("\u2699", 0, host.getString(R.string.settings_title), host.getString(R.string.browser_settings_sub), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setDrawerOpen(false);
                host.openSettings();
            }
        });

        TextView second = pillButton("\u2190  " + host.getString(R.string.browser_back_to_w4b), true);
        second.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setDrawerOpen(false);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        host.browserSwitchToW4b(true);
                    }
                }, 260L);
            }
        });
        LinearLayout.LayoutParams secondLp = new LinearLayout.LayoutParams(-1, dp(46));
        secondLp.topMargin = dp(20);
        drawerBody.addView(second, secondLp);
    }

    private View.OnClickListener toggleApply(final boolean reload) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyConfigAll();
                if (reload && active != null && active.view != null && !active.start) {
                    active.view.reload();
                }
            }
        };
    }

    private LinearLayout.LayoutParams tabRowParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(8);
        return lp;
    }

    private View tabRow(final BTab tab) {
        boolean isActive = tab == active;
        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), dp(8), dp(4), dp(8));
        row.setBackground(Ui.ripple(host, Ui.roundStroke(isActive ? RenaTheme.accentSoft() : RenaTheme.surface(), dp(16),
                isActive ? RenaTheme.accent() : RenaTheme.outline(), isActive ? dp(2) : dp(RenaTheme.strokeWidthDp())), 16));
        FrameLayout thumbBox = new FrameLayout(host);
        thumbBox.setBackground(Ui.round(RenaTheme.surface2(), dp(10)));
        thumbBox.setClipToOutline(true);
        thumbBox.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(View view, android.graphics.Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(10));
            }
        });
        if (!tab.thumbLoaded && tab.thumb == null) {
            tab.thumb = store.loadThumb(tab.id);
            tab.thumbLoaded = true;
        }
        if (tab.thumb != null) {
            ImageView image = new ImageView(host);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setImageBitmap(tab.thumb);
            thumbBox.addView(image, new FrameLayout.LayoutParams(-1, -1));
        } else {
            String name = tab.start || TextUtils.isEmpty(tab.url) ? "R" : BrowserStore.hostOf(tab.url);
            TextView letter = Ui.text(host, name.length() == 0 ? "?" : name.substring(0, 1).toUpperCase(Locale.US), 20f,
                    RenaTheme.accentText(), Typeface.BOLD);
            letter.setGravity(Gravity.CENTER);
            thumbBox.addView(letter, new FrameLayout.LayoutParams(-1, -1));
        }
        row.addView(thumbBox, new LinearLayout.LayoutParams(dp(72), dp(54)));
        LinearLayout text = new LinearLayout(host);
        text.setOrientation(LinearLayout.VERTICAL);
        String title = tab.start || TextUtils.isEmpty(tab.url) ? host.getString(R.string.browser_new_tab_title)
                : (TextUtils.isEmpty(tab.title) ? BrowserStore.hostOf(tab.url) : tab.title);
        TextView titleView = Ui.text(host, title, 13.5f, RenaTheme.text(), Typeface.BOLD);
        titleView.setMaxLines(2);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        text.addView(titleView, new LinearLayout.LayoutParams(-2, -2));
        TextView sub = Ui.text(host, tab.start || TextUtils.isEmpty(tab.url) ? "" : BrowserStore.hostOf(tab.url), 11.5f, RenaTheme.muted(), Typeface.NORMAL);
        sub.setSingleLine(true);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-2, -2);
        subLp.topMargin = dp(3);
        text.addView(sub, subLp);
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, -2, 1f);
        textLp.leftMargin = dp(10);
        row.addView(text, textLp);
        TextView close = Ui.text(host, "\u2715", 15f, RenaTheme.muted(), Typeface.NORMAL);
        close.setGravity(Gravity.CENTER);
        close.setBackground(Ui.ripple(host, Ui.round(0, dp(20)), 20));
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeTab(tab);
            }
        });
        row.addView(close, new LinearLayout.LayoutParams(dp(40), dp(40)));
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTo(tab);
                setDrawerOpen(false);
            }
        });
        return row;
    }

    private void applyConfigAll() {
        for (BTab tab : tabs) {
            if (tab.view == null) {
                continue;
            }
            try {
                if (tab.cookies != null) {
                    tab.cookies.setAcceptThirdPartyCookies(tab.view, !RenaPrefs.tracking(host));
                }
            } catch (Throwable ignored) {
            }
            WebEngine.refresh(host, tab.view, true);
        }
        WebView view = activeWebView();
        if (view != null) {
            try {
                view.evaluateJavascript("(function(){try{window.__RENA_CFG=window.__RENA_CFG||{};window.__RENA_CFG.adblock="
                        + RenaPrefs.adblock(host) + ";window.__RENA_CFG.antiDetector=" + RenaPrefs.detector(host)
                        + ";if(window.__renaAdblockRefresh){window.__renaAdblockRefresh(window.__RENA_CFG);}}catch(e){}})()", null);
            } catch (Throwable ignored) {
            }
        }
    }

    void applyDeviceChanges() {
        boolean emulate = RenaPrefs.desktop(host);
        for (BTab tab : tabs) {
            if (tab.view != null) {
                WebEngine.applyDevice(host, tab.view, emulate);
                WebEngine.refresh(host, tab.view, true);
            }
        }
        if (active != null && active.view != null && !active.start) {
            active.view.reload();
        }
    }

    private void showMoreActions() {
        final String url = active == null || active.start ? null : active.url;
        if (url == null || url.length() == 0) {
            return;
        }
        String[] labels = new String[]{host.getString(R.string.browser_share), host.getString(R.string.browser_ctx_copy_link),
                host.getString(R.string.browser_open_external), host.getString(R.string.browser_find)};
        new RenaDialog.Builder(host)
                .setItems(labels, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            shareText(url);
                        } else if (which == 1) {
                            copyToClipboard(url);
                        } else if (which == 2) {
                            try {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.putExtra("rena_skip", true);
                                host.startActivity(Intent.createChooser(intent, url));
                            } catch (Throwable ignored) {
                            }
                        } else {
                            setDrawerOpen(false);
                            promptFind();
                        }
                    }
                })
                .show();
    }

    private void promptFind() {
        final EditText input = styledInput(host.getString(R.string.browser_find), false);
        new RenaDialog.Builder(host)
                .setTitle(R.string.browser_find)
                .setView(input)
                .setNegativeButton(R.string.cancel_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (active != null && active.view != null) {
                            active.view.clearMatches();
                        }
                    }
                })
                .setPositiveButton(R.string.browser_find_next, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (active != null && active.view != null) {
                            active.view.findAllAsync(input.getText().toString());
                        }
                    }
                })
                .show();
    }
    void showEngineChooser(final boolean first) {
        PickerDialogs.engine(host, first, new Runnable() {
            @Override
            public void run() {
                for (BTab tab : tabs) {
                    if (tab.start && tab.view != null) {
                        loadStartPage(tab);
                    }
                }
                updateBar();
                refreshDrawerIfOpen();
            }
        });
    }
    void showDnsChooser() {
        PickerDialogs.dns(host, new Runnable() {
            @Override
            public void run() {
                refreshDrawerIfOpen();
            }
        });
    }

    private CheckBox checkbox(String label, boolean checked) {
        CheckBox box = new CheckBox(host);
        box.setText(label);
        box.setChecked(checked);
        box.setTextColor(RenaTheme.text());
        box.setTextSize(15f);
        box.setTypeface(Ui.font(host, Typeface.NORMAL));
        box.setButtonTintList(ColorStateList.valueOf(RenaTheme.accent()));
        box.setPadding(dp(8), 0, 0, 0);
        return box;
    }

    void showClearDialog(final Runnable done) {
        LinearLayout form = new LinearLayout(host);
        form.setOrientation(LinearLayout.VERTICAL);
        final CheckBox history = checkbox(host.getString(R.string.browser_clear_history_option, store.historyCount()), true);
        final CheckBox cookies = checkbox(host.getString(R.string.browser_clear_cookies_option), false);
        final CheckBox cache = checkbox(host.getString(R.string.browser_clear_cache_option), false);
        form.addView(history, new LinearLayout.LayoutParams(-1, dp(46)));
        form.addView(cookies, new LinearLayout.LayoutParams(-1, dp(46)));
        form.addView(cache, new LinearLayout.LayoutParams(-1, dp(46)));
        new RenaDialog.Builder(host)
                .setTitle(R.string.browser_clear_data)
                .setMessage(R.string.browser_clear_data_message)
                .setView(form)
                .setNegativeButton(R.string.cancel_text, null)
                .setPositiveButton(R.string.clear_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (history.isChecked()) {
                            store.clearHistory();
                        }
                        if (cookies.isChecked() || cache.isChecked()) {
                            BrowserProfile.clearSiteData(host, cookies.isChecked(), cache.isChecked());
                        }
                        if (cookies.isChecked() && active != null && active.view != null && !active.start) {
                            active.view.reload();
                        }
                        Toast.makeText(host, R.string.browser_data_cleared, Toast.LENGTH_SHORT).show();
                        if (done != null) {
                            done.run();
                        }
                    }
                })
                .show();
    }
}
