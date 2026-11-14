package com.rena.w4b;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.webkit.JavascriptInterface;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;

public final class AntiDeleteStore {
    private static final int MAX_RECORDS = 4000;
    private static final int MAX_THUMBS = 150;
    private static final HashMap<String, AntiDeleteStore> INSTANCES = new HashMap<String, AntiDeleteStore>();

    private final Context context;
    private final File file;
    private final LinkedHashMap<String, JSONObject> records = new LinkedHashMap<String, JSONObject>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean loaded;
    private boolean scheduled;

    private AntiDeleteStore(Context context, String profile) {
        this.context = context.getApplicationContext();
        this.file = new File(this.context.getFilesDir(), "antidelete_" + safe(profile) + ".dat");
    }

    private static String safe(String value) {
        return value == null ? "default" : value.replaceAll("[^A-Za-z0-9_\\-]", "_");
    }

    public static synchronized AntiDeleteStore forProfile(Context context, String profile) {
        String key = safe(profile);
        AntiDeleteStore store = INSTANCES.get(key);
        if (store == null) {
            store = new AntiDeleteStore(context, profile);
            INSTANCES.put(key, store);
        }
        return store;
    }

    private synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        String raw = RenaCrypto.readText(context, file);
        if (raw == null) {
            return;
        }
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object != null && object.has("id")) {
                    records.put(object.getString("id"), object);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public synchronized JSONArray all() {
        ensureLoaded();
        JSONArray array = new JSONArray();
        for (JSONObject object : records.values()) {
            array.put(object);
        }
        return array;
    }

    public synchronized ArrayList<JSONObject> deleted() {
        ensureLoaded();
        ArrayList<JSONObject> out = new ArrayList<JSONObject>();
        for (JSONObject object : records.values()) {
            if (object.optBoolean("deleted", false)) {
                out.add(object);
            }
        }
        Collections.sort(out, new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject a, JSONObject b) {
                long left = a.optLong("deletedAt", 0L);
                long right = b.optLong("deletedAt", 0L);
                return left == right ? 0 : left < right ? 1 : -1;
            }
        });
        return out;
    }

    public synchronized void put(JSONObject record) {
        ensureLoaded();
        try {
            String id = record.getString("id");
            records.remove(id);
            records.put(id, record);
            prune();
        } catch (Throwable ignored) {
        }
        schedule();
    }

    public synchronized void clear() {
        ensureLoaded();
        records.clear();
        writeNow();
    }

    public static void clearAll(Context context) {
        try {
            File[] files = context.getFilesDir().listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.getName().startsWith("antidelete_")) {
                        f.delete();
                    }
                }
            }
            synchronized (AntiDeleteStore.class) {
                INSTANCES.clear();
            }
        } catch (Throwable ignored) {
        }
    }

    public static ArrayList<JSONObject> deletedAcrossProfiles(Context context) {
        ArrayList<JSONObject> out = new ArrayList<JSONObject>();
        try {
            File[] files = context.getFilesDir().listFiles();
            if (files != null) {
                for (File f : files) {
                    String name = f.getName();
                    if (name.startsWith("antidelete_") && name.endsWith(".dat")) {
                        String profile = name.substring("antidelete_".length(), name.length() - 4);
                        out.addAll(forProfile(context, profile).deleted());
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        Collections.sort(out, new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject a, JSONObject b) {
                long left = a.optLong("deletedAt", 0L);
                long right = b.optLong("deletedAt", 0L);
                return left == right ? 0 : left < right ? 1 : -1;
            }
        });
        return out;
    }

    public static long sizeOnDisk(Context context) {
        long total = 0L;
        try {
            File[] files = context.getFilesDir().listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.getName().startsWith("antidelete_")) {
                        total += f.length();
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return total;
    }

    private void prune() {
        if (records.size() > MAX_RECORDS) {
            ArrayList<String> remove = new ArrayList<String>();
            int excess = records.size() - MAX_RECORDS;
            for (java.util.Map.Entry<String, JSONObject> entry : records.entrySet()) {
                if (excess <= 0) {
                    break;
                }
                if (!entry.getValue().optBoolean("deleted", false)) {
                    remove.add(entry.getKey());
                    excess--;
                }
            }
            for (String key : remove) {
                records.remove(key);
            }
        }
        int thumbs = 0;
        ArrayList<JSONObject> list = new ArrayList<JSONObject>(records.values());
        for (int i = list.size() - 1; i >= 0; i--) {
            JSONObject object = list.get(i);
            if (object.has("thumb") && !TextUtils.isEmpty(object.optString("thumb"))) {
                thumbs++;
                if (thumbs > MAX_THUMBS && !object.optBoolean("deleted", false)) {
                    object.remove("thumb");
                }
            }
        }
    }

    private void schedule() {
        if (scheduled) {
            return;
        }
        scheduled = true;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                scheduled = false;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (AntiDeleteStore.this) {
                            writeNow();
                        }
                    }
                }).start();
            }
        }, 2500L);
    }

    private void writeNow() {
        try {
            JSONArray array = new JSONArray();
            for (JSONObject object : records.values()) {
                array.put(object);
            }
            RenaCrypto.writeText(context, file, array.toString());
        } catch (Throwable ignored) {
        }
    }

    public static final class Bridge {
        private final AntiDeleteStore store;

        public Bridge(AntiDeleteStore store) {
            this.store = store;
        }

        @JavascriptInterface
        public String load() {
            return store.all().toString();
        }

        @JavascriptInterface
        public void putAll(String json) {
            try {
                JSONArray array = new JSONArray(json);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject object = array.optJSONObject(i);
                    if (object != null) {
                        store.put(object);
                    }
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
