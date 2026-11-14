package com.rena.w4b;

import android.webkit.JavascriptInterface;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public final class InspectorHub {
    private static final HashMap<String, InspectorHub> HUBS = new HashMap<String, InspectorHub>();
    private static final int LIMIT = 600;

    public static final class Entry {
        public String level = "log";
        public String text = "";
        public long time;
        public long id;
        public String method = "GET";
        public String url = "";
        public int status = -1;
        public long ms = -1;
        public String type = "";
        public long size = -1;
        public String via = "";
        public String headers = "";
        public boolean fromNative;
    }

    private final ArrayList<Entry> console = new ArrayList<Entry>();
    private final ArrayList<Entry> network = new ArrayList<Entry>();
    private long counter;

    public static synchronized InspectorHub forKey(String key) {
        InspectorHub hub = HUBS.get(key);
        if (hub == null) {
            hub = new InspectorHub();
            HUBS.put(key, hub);
        }
        return hub;
    }

    public static synchronized void drop(String key) {
        HUBS.remove(key);
    }

    public synchronized ArrayList<Entry> console() {
        return new ArrayList<Entry>(console);
    }

    public synchronized ArrayList<Entry> network() {
        return new ArrayList<Entry>(network);
    }

    public synchronized void clearConsole() {
        console.clear();
    }

    public synchronized void clearNetwork() {
        network.clear();
    }

    public synchronized void addConsole(String level, String text) {
        Entry entry = new Entry();
        entry.level = level;
        entry.text = text;
        entry.time = System.currentTimeMillis();
        console.add(entry);
        while (console.size() > LIMIT) {
            console.remove(0);
        }
    }

    public synchronized void addNative(String method, String url, String type, String headers) {
        Entry entry = new Entry();
        entry.id = ++counter;
        entry.method = method;
        entry.url = url;
        entry.type = type;
        entry.headers = headers;
        entry.time = System.currentTimeMillis();
        entry.fromNative = true;
        network.add(entry);
        while (network.size() > LIMIT) {
            network.remove(0);
        }
    }

    public synchronized void enrich(String url, int status, long ms, String type, long size, String via) {
        for (int i = network.size() - 1; i >= 0; i--) {
            Entry entry = network.get(i);
            if (entry.url.equals(url) && entry.status < 0) {
                entry.status = status;
                entry.ms = ms;
                if (type != null && type.length() > 0) {
                    entry.type = type;
                }
                entry.size = size;
                entry.via = via;
                return;
            }
        }
        Entry entry = new Entry();
        entry.id = ++counter;
        entry.url = url;
        entry.status = status;
        entry.ms = ms;
        entry.type = type == null ? "" : type;
        entry.size = size;
        entry.via = via;
        entry.time = System.currentTimeMillis();
        network.add(entry);
        while (network.size() > LIMIT) {
            network.remove(0);
        }
    }

    public static final class Bridge {
        private final InspectorHub hub;

        public Bridge(InspectorHub hub) {
            this.hub = hub;
        }

        @JavascriptInterface
        public void push(String kind, String json) {
            try {
                JSONObject object = new JSONObject(json);
                if ("console".equals(kind)) {
                    hub.addConsole(object.optString("level", "log"), object.optString("text", ""));
                } else if ("net".equals(kind)) {
                    String url = object.optString("url", "");
                    try {
                        url = new java.net.URL(new java.net.URL("http://localhost/"), url).toString();
                    } catch (Throwable ignored) {
                    }
                    hub.enrich(url, object.optInt("status", 0), object.optLong("ms", -1L),
                            object.optString("type", ""), object.optLong("size", -1L), object.optString("via", ""));
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
