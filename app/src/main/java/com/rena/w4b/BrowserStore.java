package com.rena.w4b;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Parcel;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public final class BrowserStore {
    public static final class TabRecord {
        public int id;
        public String url = "";
        public String title = "";
        public long lastUsed;
    }

    public static final class HistoryEntry {
        public String url = "";
        public String title = "";
        public long time;
    }

    private static final String TABS_FILE = "browser_tabs.dat";
    private static final String HISTORY_FILE = "browser_history.dat";
    private static final int HISTORY_LIMIT = 4000;

    private final Context context;
    private final ArrayList<HistoryEntry> history = new ArrayList<HistoryEntry>();
    private boolean historyLoaded;

    public BrowserStore(Context context) {
        this.context = context.getApplicationContext();
    }

    private File dir() {
        File dir = new File(context.getFilesDir(), "browser");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static final class Snapshot {
        public final ArrayList<TabRecord> tabs = new ArrayList<TabRecord>();
        public int activeId = -1;
        public int nextId = 1;
    }

    public Snapshot readTabs() {
        Snapshot snapshot = new Snapshot();
        String raw = RenaCrypto.readText(context, new File(dir(), TABS_FILE));
        if (raw == null) {
            return snapshot;
        }
        try {
            JSONObject root = new JSONObject(raw);
            snapshot.activeId = root.optInt("active", -1);
            snapshot.nextId = root.optInt("next", 1);
            JSONArray array = root.optJSONArray("tabs");
            if (array != null) {
                for (int i = 0; i < array.length(); i++) {
                    JSONObject object = array.optJSONObject(i);
                    if (object == null) {
                        continue;
                    }
                    TabRecord record = new TabRecord();
                    record.id = object.optInt("id");
                    record.url = object.optString("url", "");
                    record.title = object.optString("title", "");
                    record.lastUsed = object.optLong("used", 0L);
                    snapshot.tabs.add(record);
                    if (record.id >= snapshot.nextId) {
                        snapshot.nextId = record.id + 1;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return snapshot;
    }

    public synchronized void writeTabs(Snapshot snapshot) {
        try {
            JSONObject root = new JSONObject();
            root.put("active", snapshot.activeId);
            root.put("next", snapshot.nextId);
            JSONArray array = new JSONArray();
            for (TabRecord record : snapshot.tabs) {
                JSONObject object = new JSONObject();
                object.put("id", record.id);
                object.put("url", record.url);
                object.put("title", record.title);
                object.put("used", record.lastUsed);
                array.put(object);
            }
            root.put("tabs", array);
            RenaCrypto.writeText(context, new File(dir(), TABS_FILE), root.toString());
        } catch (Throwable ignored) {
        }
    }

    public void saveState(int tabId, Bundle bundle) {
        if (bundle == null) {
            return;
        }
        Parcel parcel = Parcel.obtain();
        try {
            parcel.writeBundle(bundle);
            RenaCrypto.writeFile(context, new File(dir(), "tab_" + tabId + ".state"), parcel.marshall());
        } catch (Throwable ignored) {
        } finally {
            parcel.recycle();
        }
    }

    public Bundle loadState(int tabId) {
        byte[] data = RenaCrypto.readFile(context, new File(dir(), "tab_" + tabId + ".state"));
        if (data == null) {
            return null;
        }
        Parcel parcel = Parcel.obtain();
        try {
            parcel.unmarshall(data, 0, data.length);
            parcel.setDataPosition(0);
            return parcel.readBundle(getClass().getClassLoader());
        } catch (Throwable ignored) {
            return null;
        } finally {
            parcel.recycle();
        }
    }

    public void saveThumb(int tabId, Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 62, out);
            RenaCrypto.writeFile(context, new File(dir(), "thumb_" + tabId + ".jpg.enc"), out.toByteArray());
        } catch (Throwable ignored) {
        }
    }

    public Bitmap loadThumb(int tabId) {
        byte[] data = RenaCrypto.readFile(context, new File(dir(), "thumb_" + tabId + ".jpg.enc"));
        if (data == null) {
            return null;
        }
        try {
            return BitmapFactory.decodeByteArray(data, 0, data.length);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public void deleteTabFiles(int tabId) {
        new File(dir(), "tab_" + tabId + ".state").delete();
        new File(dir(), "thumb_" + tabId + ".jpg.enc").delete();
    }

    private synchronized void ensureHistory() {
        if (historyLoaded) {
            return;
        }
        historyLoaded = true;
        String raw = RenaCrypto.readText(context, new File(dir(), HISTORY_FILE));
        if (raw == null) {
            return;
        }
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null) {
                    continue;
                }
                HistoryEntry entry = new HistoryEntry();
                entry.url = object.optString("u", "");
                entry.title = object.optString("t", "");
                entry.time = object.optLong("d", 0L);
                if (entry.url.length() > 0) {
                    history.add(entry);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public synchronized void addHistory(String url, String title) {
        if (TextUtils.isEmpty(url) || url.startsWith("about:") || url.startsWith("data:") || url.startsWith("blob:") ||
                url.startsWith("javascript:")) {
            return;
        }
        ensureHistory();
        long now = System.currentTimeMillis();
        if (!history.isEmpty()) {
            HistoryEntry last = history.get(0);
            if (last.url.equals(url)) {
                if (!TextUtils.isEmpty(title)) {
                    last.title = title;
                }
                last.time = now;
                scheduleWrite();
                return;
            }
        }
        HistoryEntry entry = new HistoryEntry();
        entry.url = url;
        entry.title = title == null ? "" : title;
        entry.time = now;
        history.add(0, entry);
        while (history.size() > HISTORY_LIMIT) {
            history.remove(history.size() - 1);
        }
        scheduleWrite();
    }

    public synchronized void updateTitle(String url, String title) {
        if (TextUtils.isEmpty(url) || TextUtils.isEmpty(title)) {
            return;
        }
        ensureHistory();
        for (int i = 0; i < Math.min(history.size(), 5); i++) {
            if (history.get(i).url.equals(url)) {
                history.get(i).title = title;
                scheduleWrite();
                return;
            }
        }
    }

    public synchronized ArrayList<HistoryEntry> history() {
        ensureHistory();
        return new ArrayList<HistoryEntry>(history);
    }

    public synchronized ArrayList<HistoryEntry> topSites(int limit) {
        ensureHistory();
        final HashMap<String, int[]> counts = new HashMap<String, int[]>();
        HashMap<String, HistoryEntry> latest = new HashMap<String, HistoryEntry>();
        for (HistoryEntry entry : history) {
            String host = hostOf(entry.url);
            if (host.length() == 0) {
                continue;
            }
            int[] c = counts.get(host);
            if (c == null) {
                c = new int[]{0};
                counts.put(host, c);
                latest.put(host, entry);
            }
            c[0]++;
        }
        ArrayList<String> hosts = new ArrayList<String>(counts.keySet());
        java.util.Collections.sort(hosts, new java.util.Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                return counts.get(b)[0] - counts.get(a)[0];
            }
        });
        ArrayList<HistoryEntry> out = new ArrayList<HistoryEntry>();
        for (String host : hosts) {
            if (out.size() >= limit) {
                break;
            }
            out.add(latest.get(host));
        }
        return out;
    }

    public static String hostOf(String url) {
        try {
            String host = android.net.Uri.parse(url).getHost();
            return host == null ? "" : host.replaceFirst("^www\\.", "");
        } catch (Throwable ignored) {
            return "";
        }
    }

    public synchronized void removeHistory(String url) {
        ensureHistory();
        for (int i = history.size() - 1; i >= 0; i--) {
            if (history.get(i).url.equals(url)) {
                history.remove(i);
            }
        }
        scheduleWrite();
    }

    public synchronized void clearHistory() {
        ensureHistory();
        history.clear();
        writeHistoryNow();
    }

    public synchronized int historyCount() {
        ensureHistory();
        return history.size();
    }

    private boolean writeScheduled;

    private void scheduleWrite() {
        if (writeScheduled) {
            return;
        }
        writeScheduled = true;
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (BrowserStore.this) {
                            writeScheduled = false;
                            writeHistoryNow();
                        }
                    }
                }).start();
            }
        }, 2000L);
    }

    private void writeHistoryNow() {
        try {
            JSONArray array = new JSONArray();
            for (HistoryEntry entry : history) {
                JSONObject object = new JSONObject();
                object.put("u", entry.url);
                object.put("t", entry.title);
                object.put("d", entry.time);
                array.put(object);
            }
            RenaCrypto.writeText(context, new File(dir(), HISTORY_FILE), array.toString());
        } catch (Throwable ignored) {
        }
    }

    public void deleteEverything() {
        clearHistory();
        File[] files = dir().listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
    }

    public long sizeOnDisk() {
        long total = 0L;
        File[] files = dir().listFiles();
        if (files != null) {
            for (File file : files) {
                total += file.length();
            }
        }
        return total;
    }
}
