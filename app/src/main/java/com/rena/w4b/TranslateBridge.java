package com.rena.w4b;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.LruCache;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class TranslateBridge {
    public interface Host {
        void onTranslateClosed();
    }

    private static final ExecutorService POOL = Executors.newFixedThreadPool(3);
    private static final LruCache<String, String> CACHE = new LruCache<String, String>(2500);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final String ENDPOINT = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&dt=t&tl=";

    private final WeakReference<Activity> activity;
    private final WeakReference<WebView> view;
    private final Host host;

    public TranslateBridge(Activity activity, WebView view, Host host) {
        this.activity = new WeakReference<Activity>(activity);
        this.view = new WeakReference<WebView>(view);
        this.host = host;
    }

    @JavascriptInterface
    public void setLang(String code) {
        Activity a = activity.get();
        if (a != null && !TextUtils.isEmpty(code)) {
            RenaPrefs.setStr(a, RenaPrefs.TRANSLATE_LANG, code);
        }
    }

    @JavascriptInterface
    public void close() {
        MAIN.post(new Runnable() {
            @Override
            public void run() {
                if (host != null) {
                    host.onTranslateClosed();
                }
            }
        });
    }

    @JavascriptInterface
    public void translate(final String id, final String jsonTexts, final String lang) {
        POOL.execute(new Runnable() {
            @Override
            public void run() {
                String result = doTranslate(jsonTexts, lang);
                deliver(id, result);
            }
        });
    }

    private void deliver(final String id, final String json) {
        MAIN.post(new Runnable() {
            @Override
            public void run() {
                WebView target = view.get();
                if (target == null) {
                    return;
                }
                try {
                    target.evaluateJavascript("(function(){try{if(window.__renaTr){window.__renaTr.cb("
                            + PageScripts.quote(id) + "," + PageScripts.quote(json) + ");}}catch(e){}})()", null);
                } catch (Throwable ignored) {
                }
            }
        });
    }

    private String doTranslate(String jsonTexts, String lang) {
        JSONArray output = new JSONArray();
        try {
            JSONArray input = new JSONArray(jsonTexts);
            int count = input.length();
            String[] texts = new String[count];
            String[] results = new String[count];
            int[] missing = new int[count];
            int missingCount = 0;
            for (int i = 0; i < count; i++) {
                texts[i] = input.optString(i, "").replace('\n', ' ').trim();
                String cached = CACHE.get(lang + "\u0001" + texts[i]);
                if (cached != null) {
                    results[i] = cached;
                } else if (texts[i].length() > 0) {
                    missing[missingCount++] = i;
                } else {
                    results[i] = "";
                }
            }
            if (missingCount > 0) {
                StringBuilder joined = new StringBuilder();
                for (int k = 0; k < missingCount; k++) {
                    if (k > 0) {
                        joined.append('\n');
                    }
                    joined.append(texts[missing[k]]);
                }
                String translated = request(joined.toString(), lang);
                boolean mapped = false;
                if (translated != null) {
                    String[] lines = translated.split("\n", -1);
                    if (lines.length == missingCount) {
                        for (int k = 0; k < lines.length; k++) {
                            results[missing[k]] = lines[k].trim();
                        }
                        mapped = true;
                    }
                }
                if (!mapped) {
                    for (int k = 0; k < missingCount; k++) {
                        int index = missing[k];
                        String single = request(texts[index], lang);
                        results[index] = single == null ? texts[index] : single.trim();
                    }
                }
                for (int k = 0; k < missingCount; k++) {
                    int index = missing[k];
                    if (results[index] != null) {
                        CACHE.put(lang + "\u0001" + texts[index], results[index]);
                    }
                }
            }
            for (int i = 0; i < count; i++) {
                output.put(results[i] == null ? texts[i] : results[i]);
            }
        } catch (Throwable ignored) {
        }
        return output.toString();
    }

    private static String request(String text, String lang) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(ENDPOINT + URLEncoder.encode(lang, "UTF-8"));
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(15000);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36");
            byte[] body = ("q=" + URLEncoder.encode(text, "UTF-8")).getBytes("UTF-8");
            OutputStream out = connection.getOutputStream();
            out.write(body);
            out.flush();
            out.close();
            if (connection.getResponseCode() != 200) {
                return null;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            reader.close();
            JSONArray root = new JSONArray(builder.toString());
            JSONArray segments = root.optJSONArray(0);
            if (segments == null) {
                return null;
            }
            StringBuilder translated = new StringBuilder();
            for (int i = 0; i < segments.length(); i++) {
                JSONArray segment = segments.optJSONArray(i);
                if (segment != null && !segment.isNull(0)) {
                    translated.append(segment.optString(0, ""));
                }
            }
            return translated.toString();
        } catch (Throwable error) {
            return null;
        } finally {
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Throwable ignored) {
                }
            }
        }
    }
}
