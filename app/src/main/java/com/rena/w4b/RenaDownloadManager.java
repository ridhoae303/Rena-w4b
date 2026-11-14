package com.rena.w4b;

import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class RenaDownloadManager {
    public static final int QUEUED = 0;
    public static final int RUNNING = 1;
    public static final int PAUSED = 2;
    public static final int COMPLETED = 3;
    public static final int FAILED = 4;
    public static final int CANCELLED = 5;

    private static final String STORE_NAME = "downloads_v3.dat";
    private static final String LEGACY_PREFS = "rena_downloads";
    private static final long BLOB_START_TIMEOUT_MS = 15000L;
    private static final int MAX_AUTO_RETRY = 3;
    private static final Object LOCK = new Object();
    private static final ArrayList<DownloadItem> ITEMS = new ArrayList<DownloadItem>();
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3);
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static Context appContext;
    private static boolean loaded;
    private static long lastPersist;
    private static boolean persistScheduled;

    private RenaDownloadManager() {
    }

    public static void initialize(Context context) {
        if (context == null) {
            return;
        }
        synchronized (LOCK) {
            if (appContext == null) {
                appContext = context.getApplicationContext() == null ? context : context.getApplicationContext();
            }
            if (loaded) {
                return;
            }
            loaded = true;
            boolean any = false;
            String raw = RenaCrypto.readText(appContext, new File(appContext.getFilesDir(), STORE_NAME));
            if (raw != null) {
                try {
                    JSONArray array = new JSONArray(raw);
                    for (int i = 0; i < array.length(); i++) {
                        DownloadItem item = DownloadItem.fromJson(array.optJSONObject(i));
                        if (item != null) {
                            normalizeLoaded(item);
                            ITEMS.add(item);
                            any = true;
                        }
                    }
                } catch (Throwable ignored) {
                }
            } else {
                any = importLegacy();
            }
            if (any) {
                writeNow();
            }
        }
    }

    private static boolean importLegacy() {
        try {
            String raw = appContext.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE).getString("items", "[]");
            JSONArray array = new JSONArray(raw);
            boolean any = false;
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null || !"Completed".equals(object.optString("status"))) {
                    continue;
                }
                DownloadItem item = new DownloadItem();
                item.id = object.optString("id", UUID.randomUUID().toString());
                item.url = object.optString("url", "");
                item.fileName = object.optString("fileName", "download");
                item.path = object.optString("path", "");
                item.mimeType = object.optString("mimeType", "application/octet-stream");
                item.folder = RenaPaths.folderFor(item.mimeType, item.fileName);
                item.createdAt = object.optLong("createdAt", System.currentTimeMillis());
                item.status = COMPLETED;
                item.total = object.optLong("total", -1L);
                item.downloaded = object.optLong("downloaded", 0L);
                if (!TextUtils.isEmpty(item.path) && new File(item.path).isFile()) {
                    ITEMS.add(item);
                    any = true;
                }
            }
            appContext.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE).edit().clear().apply();
            return any;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void normalizeLoaded(DownloadItem item) {
        File target = new File(item.path);
        File partial = new File(item.path + ".part");
        if (item.status == COMPLETED && !target.exists()) {
            item.status = FAILED;
            item.error = "missing";
        } else if (item.status == RUNNING || item.status == QUEUED) {
            if (item.blob) {
                item.status = FAILED;
                item.error = "interrupted";
            } else {
                item.status = PAUSED;
                item.downloaded = partial.exists() ? partial.length() : 0L;
            }
        }
        if (item.blob && item.status == PAUSED) {
            item.status = FAILED;
            item.error = "interrupted";
        }
    }

    public static DownloadItem createHttp(Context context, String url, String fileName, String folder, String mimeType,
                                          String userAgent, String cookie, String referer, long expectedLength,
                                          boolean autoName) {
        initialize(context);
        if (TextUtils.isEmpty(url)) {
            return null;
        }
        File dir = RenaPaths.dir(folder);
        if (!dir.exists() && !dir.mkdirs()) {
            return null;
        }
        DownloadItem item = new DownloadItem();
        item.id = UUID.randomUUID().toString();
        item.url = url;
        item.fileName = fileName;
        item.folder = folder;
        item.mimeType = mimeType;
        item.userAgent = userAgent;
        item.cookie = cookie;
        item.referer = referer;
        item.total = expectedLength > 0 ? expectedLength : -1L;
        item.autoName = autoName;
        item.path = uniqueFile(dir, fileName).getAbsolutePath();
        item.status = QUEUED;
        synchronized (LOCK) {
            ITEMS.add(0, item);
            persistLocked(true);
        }
        start(item);
        return item;
    }

    public static DownloadItem createBlob(Context context, String url, String fileName, String folder, String mimeType,
                                          long size, int tabId, String sourceKey) {
        initialize(context);
        if (TextUtils.isEmpty(url)) {
            return null;
        }
        File dir = RenaPaths.dir(folder);
        if (!dir.exists() && !dir.mkdirs()) {
            return null;
        }
        final DownloadItem item = new DownloadItem();
        item.id = UUID.randomUUID().toString();
        item.url = url;
        item.fileName = fileName;
        item.folder = folder;
        item.mimeType = mimeType;
        item.total = size > 0 ? size : -1L;
        item.path = uniqueFile(dir, fileName).getAbsolutePath();
        item.sourceTabId = tabId;
        item.sourceKey = sourceKey;
        item.status = QUEUED;
        item.blob = true;
        item.active = true;
        synchronized (LOCK) {
            ITEMS.add(0, item);
            persistLocked(true);
        }
        ensureService();
        MAIN.postDelayed(new Runnable() {
            @Override
            public void run() {
                synchronized (item.blobLock) {
                    if (item.status == QUEUED && item.blob && !item.cancelled) {
                        item.status = FAILED;
                        item.error = "blob-start-timeout";
                        item.active = false;
                        safeDelete(partialFile(item));
                        persist(true);
                    }
                }
            }
        }, BLOB_START_TIMEOUT_MS);
        return item;
    }

    public static File uniqueFile(File directory, String fileName) {
        File target = new File(directory, fileName);
        if (!target.exists() && !new File(directory, fileName + ".part").exists() && !pathTaken(target.getAbsolutePath())) {
            return target;
        }
        String base = fileName;
        String extension = "";
        int dot = fileName.lastIndexOf('.');
        if (dot > 0 && fileName.length() - dot <= 10) {
            base = fileName.substring(0, dot);
            extension = fileName.substring(dot);
        }
        int suffix = 2;
        do {
            target = new File(directory, base + " (" + suffix + ")" + extension);
            suffix++;
        } while (target.exists() || new File(target.getPath() + ".part").exists() || pathTaken(target.getAbsolutePath()));
        return target;
    }

    private static boolean pathTaken(String path) {
        synchronized (LOCK) {
            for (DownloadItem item : ITEMS) {
                if (path.equals(item.path) && item.status != CANCELLED && item.status != FAILED) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void start(final DownloadItem item) {
        if (item == null || appContext == null) {
            return;
        }
        synchronized (LOCK) {
            if (item.status == COMPLETED || item.blob) {
                return;
            }
            if (item.active) {
                return;
            }
            item.paused = false;
            item.cancelled = false;
            item.active = true;
            item.error = null;
            item.status = QUEUED;
            item.retries = 0;
            persistLocked(true);
        }
        ensureService();
        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                runHttp(item);
            }
        });
    }

    public static void pause(String id) {
        DownloadItem item = find(id);
        if (item == null || item.status == COMPLETED || item.status == CANCELLED) {
            return;
        }
        if (item.blob) {
            synchronized (item.blobLock) {
                item.paused = true;
                item.status = PAUSED;
            }
            persist(true);
            return;
        }
        item.paused = true;
        HttpURLConnection connection = item.connection;
        if (connection != null) {
            try {
                connection.disconnect();
            } catch (Throwable ignored) {
            }
        }
        if (!item.active) {
            item.status = PAUSED;
        }
        persist(true);
    }

    public static void resume(String id) {
        DownloadItem item = find(id);
        if (item == null || item.status == COMPLETED) {
            return;
        }
        if (item.blob) {
            synchronized (item.blobLock) {
                if (item.status == FAILED || item.cancelled) {
                    return;
                }
                item.paused = false;
                item.status = RUNNING;
            }
            persist(true);
            return;
        }
        if (item.active) {
            if (item.paused) {
                item.resumeRequested = true;
            }
            return;
        }
        start(item);
    }

    public static void retry(String id) {
        DownloadItem item = find(id);
        if (item == null || item.blob || item.status == COMPLETED || item.active) {
            return;
        }
        start(item);
    }

    public static void cancel(String id) {
        DownloadItem item = find(id);
        if (item == null) {
            return;
        }
        if (item.blob) {
            synchronized (item.blobLock) {
                if (item.status == COMPLETED) {
                    return;
                }
                item.cancelled = true;
                item.paused = false;
                item.active = false;
                item.status = CANCELLED;
                safeDelete(partialFile(item));
            }
            persist(true);
            return;
        }
        item.cancelled = true;
        HttpURLConnection connection = item.connection;
        if (connection != null) {
            try {
                connection.disconnect();
            } catch (Throwable ignored) {
            }
        }
        if (!item.active) {
            item.status = CANCELLED;
            safeDelete(partialFile(item));
        }
        persist(true);
    }

    public static void delete(String id, boolean deleteFile) {
        DownloadItem item = find(id);
        if (item == null) {
            return;
        }
        if (item.status != COMPLETED) {
            cancel(id);
        }
        safeDelete(partialFile(item));
        if (deleteFile) {
            safeDelete(new File(item.path));
        }
        synchronized (LOCK) {
            ITEMS.remove(item);
            persistLocked(true);
        }
    }

    public static int clearAll(boolean deleteFiles) {
        ArrayList<DownloadItem> copy = getItems();
        for (DownloadItem item : copy) {
            delete(item.id, deleteFiles);
        }
        if (deleteFiles) {
            clearDirectoryContents(RenaPaths.root());
            RenaPaths.ensureDirs();
        }
        return copy.size();
    }

    public static int clearFinishedRecords() {
        int count = 0;
        ArrayList<DownloadItem> copy = getItems();
        for (DownloadItem item : copy) {
            if (item.status == COMPLETED || item.status == FAILED || item.status == CANCELLED) {
                delete(item.id, false);
                count++;
            }
        }
        return count;
    }

    private static void clearDirectoryContents(File directory) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return;
        }
        File[] children = directory.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child == null) {
                continue;
            }
            try {
                if (child.isDirectory()) {
                    clearDirectoryContents(child);
                }
                child.delete();
            } catch (Throwable ignored) {
            }
        }
    }

    public static ArrayList<DownloadItem> getItems() {
        synchronized (LOCK) {
            ArrayList<DownloadItem> copy = new ArrayList<DownloadItem>(ITEMS);
            Collections.sort(copy, new Comparator<DownloadItem>() {
                @Override
                public int compare(DownloadItem left, DownloadItem right) {
                    return left.createdAt == right.createdAt ? 0 : left.createdAt < right.createdAt ? 1 : -1;
                }
            });
            return copy;
        }
    }

    public static DownloadItem find(String id) {
        if (TextUtils.isEmpty(id)) {
            return null;
        }
        synchronized (LOCK) {
            for (DownloadItem item : ITEMS) {
                if (id.equals(item.id)) {
                    return item;
                }
            }
        }
        return null;
    }

    public static int activeCount() {
        int count = 0;
        synchronized (LOCK) {
            for (DownloadItem item : ITEMS) {
                if (item.status == RUNNING || item.status == QUEUED) {
                    count++;
                }
            }
        }
        return count;
    }

    public static boolean isPaused(String id) {
        DownloadItem item = find(id);
        return item != null && item.paused;
    }

    public static boolean isCancelled(String id) {
        DownloadItem item = find(id);
        return item == null || item.cancelled || item.status == CANCELLED;
    }

    public static boolean hasPendingBlobDownloadForSource(String sourceKey) {
        synchronized (LOCK) {
            for (DownloadItem item : ITEMS) {
                if (item.blob && sourceKey != null && sourceKey.equals(item.sourceKey) &&
                        (item.status == RUNNING || item.status == QUEUED || item.status == PAUSED)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void cancelBlobDownloadsForSource(String sourceKey) {
        ArrayList<DownloadItem> candidates = new ArrayList<DownloadItem>();
        synchronized (LOCK) {
            for (DownloadItem item : ITEMS) {
                if (item.blob && sourceKey != null && sourceKey.equals(item.sourceKey)) {
                    candidates.add(item);
                }
            }
        }
        boolean changed = false;
        for (DownloadItem item : candidates) {
            synchronized (item.blobLock) {
                if (item.status == RUNNING || item.status == QUEUED || item.status == PAUSED) {
                    item.cancelled = true;
                    item.paused = false;
                    item.active = false;
                    item.status = CANCELLED;
                    safeDelete(partialFile(item));
                    changed = true;
                }
            }
        }
        if (changed) {
            persist(true);
        }
    }

    public static boolean beginBlob(String id, long totalBytes, String mime) {
        final DownloadItem item = find(id);
        if (item == null || TextUtils.isEmpty(item.path)) {
            return false;
        }
        synchronized (item.blobLock) {
            if (item.cancelled || item.status == CANCELLED) {
                return false;
            }
            File partial = partialFile(item);
            try {
                File parent = partial.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    throw new IOException("mkdir");
                }
                if (partial.exists() && !partial.delete()) {
                    throw new IOException("reset");
                }
                FileOutputStream empty = new FileOutputStream(partial, false);
                empty.close();
                item.downloaded = 0L;
                item.total = totalBytes >= 0L ? totalBytes : -1L;
                if (!TextUtils.isEmpty(mime) && DownloadNames.isGenericMime(item.mimeType)) {
                    item.mimeType = mime;
                }
                item.status = item.paused ? PAUSED : RUNNING;
                item.blob = true;
                item.active = true;
                item.blobWriteFailed = false;
                item.startedAt = System.currentTimeMillis();
                persist(true);
                return true;
            } catch (Throwable error) {
                item.blobWriteFailed = true;
                item.active = false;
                item.status = FAILED;
                item.error = "write";
                persist(true);
                safeDelete(partial);
                return false;
            }
        }
    }

    public static boolean appendBlobChunk(String id, String encodedChunk) {
        final DownloadItem item = find(id);
        if (item == null) {
            return false;
        }
        if (TextUtils.isEmpty(encodedChunk)) {
            return !item.blobWriteFailed && !item.cancelled;
        }
        synchronized (item.blobLock) {
            if (item.cancelled || item.blobWriteFailed || item.status == FAILED || item.status == CANCELLED) {
                return false;
            }
            if (item.paused || item.status != RUNNING) {
                return false;
            }
            File partial = partialFile(item);
            FileOutputStream output = null;
            try {
                byte[] bytes = android.util.Base64.decode(encodedChunk, android.util.Base64.DEFAULT);
                if (bytes.length == 0) {
                    return true;
                }
                output = new FileOutputStream(partial, true);
                output.write(bytes);
                output.flush();
                item.downloaded += bytes.length;
                updateSpeed(item, bytes.length);
                persistIfNeeded(item);
                return true;
            } catch (Throwable error) {
                item.blobWriteFailed = true;
                item.status = FAILED;
                item.error = "write";
                item.active = false;
                persist(true);
                safeDelete(partial);
                return false;
            } finally {
                if (output != null) {
                    try {
                        output.close();
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
    }

    public static boolean finishBlob(String id, boolean success) {
        final DownloadItem item = find(id);
        if (item == null) {
            return false;
        }
        boolean completed = false;
        synchronized (item.blobLock) {
            File partial = partialFile(item);
            File target = new File(item.path);
            if (item.cancelled || item.status == CANCELLED) {
                safeDelete(partial);
                item.active = false;
                persist(true);
                return false;
            }
            boolean sizeMatches = item.total < 0L || partial.length() == item.total;
            if (success && !item.blobWriteFailed && partial.isFile() && sizeMatches) {
                File finalTarget = finalizeName(item, partial, target);
                if (partial.renameTo(finalTarget)) {
                    item.path = finalTarget.getAbsolutePath();
                    item.fileName = finalTarget.getName();
                    item.status = COMPLETED;
                    item.downloaded = finalTarget.length();
                    item.total = item.downloaded;
                    item.active = false;
                    item.completedAt = System.currentTimeMillis();
                    completed = true;
                }
            }
            if (!completed) {
                item.status = FAILED;
                if (item.error == null) {
                    item.error = "incomplete";
                }
                item.active = false;
                safeDelete(partial);
            }
        }
        persist(true);
        if (completed) {
            onCompleted(item);
        }
        return completed;
    }

    private static File finalizeName(DownloadItem item, File partial, File target) {
        String ext = DownloadNames.extensionOf(target.getName());
        if (ext.length() == 0 || "bin".equals(ext) || "dat".equals(ext)) {
            String sniffed = DownloadNames.sniffExtension(partial);
            if (sniffed != null) {
                String name = target.getName();
                String base = ext.length() == 0 ? name : name.substring(0, name.length() - ext.length() - 1);
                File renamed = uniqueFile(target.getParentFile(), base + "." + sniffed);
                item.mimeType = DownloadNames.mimeForName(renamed.getName());
                String folder = RenaPaths.folderFor(item.mimeType, renamed.getName());
                if (!folder.equals(item.folder)) {
                    File dir = RenaPaths.dir(folder);
                    if (dir.exists() || dir.mkdirs()) {
                        item.folder = folder;
                        return uniqueFile(dir, renamed.getName());
                    }
                }
                return renamed;
            }
        }
        return target;
    }

    private static void runHttp(DownloadItem item) {
        HttpURLConnection connection = null;
        InputStream input = null;
        FileOutputStream output = null;
        boolean finished = false;
        boolean retryLater = false;
        File partial = partialFile(item);
        try {
            if (item.cancelled || item.paused) {
                return;
            }
            item.status = RUNNING;
            item.startedAt = System.currentTimeMillis();
            persist(true);
            File parent = partial.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IOException("mkdir");
            }
            long existing = partial.exists() ? partial.length() : 0L;
            String currentUrl = item.url;
            String originalHost = new URL(item.url).getHost();
            int responseCode = -1;
            for (int redirects = 0; redirects < 10; redirects++) {
                URL requestUrl = new URL(currentUrl);
                connection = (HttpURLConnection) requestUrl.openConnection();
                item.connection = connection;
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(45000);
                connection.setUseCaches(false);
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "*/*");
                connection.setRequestProperty("Accept-Encoding", "identity");
                if (!TextUtils.isEmpty(item.userAgent)) {
                    connection.setRequestProperty("User-Agent", item.userAgent);
                }
                if (!TextUtils.isEmpty(item.cookie) && originalHost != null && originalHost.equalsIgnoreCase(requestUrl.getHost())) {
                    connection.setRequestProperty("Cookie", item.cookie);
                }
                if (!TextUtils.isEmpty(item.referer)) {
                    connection.setRequestProperty("Referer", item.referer);
                }
                if (existing > 0L) {
                    connection.setRequestProperty("Range", "bytes=" + existing + "-");
                }
                connection.connect();
                responseCode = connection.getResponseCode();
                if (responseCode >= 300 && responseCode < 400 && responseCode != 304) {
                    String location = connection.getHeaderField("Location");
                    connection.disconnect();
                    connection = null;
                    if (TextUtils.isEmpty(location)) {
                        throw new IOException("redirect");
                    }
                    currentUrl = new URL(new URL(currentUrl), location).toString();
                    continue;
                }
                break;
            }
            if (connection == null) {
                throw new IOException("no-connection");
            }
            if (responseCode == 416 && existing > 0L) {
                existing = 0L;
                partial.delete();
                connection.disconnect();
                connection = null;
                throw new RetryException();
            }
            if (responseCode < 200 || responseCode >= 300) {
                item.error = "http-" + responseCode;
                throw new IOException("http " + responseCode);
            }
            if (existing > 0L && responseCode != HttpURLConnection.HTTP_PARTIAL) {
                existing = 0L;
            }
            long responseLength = connection.getContentLengthLong();
            item.total = responseLength > 0L ? existing + responseLength : (item.total > 0 ? item.total : -1L);
            item.downloaded = existing;
            adoptServerName(item, connection);
            partial = partialFile(item);
            if (existing == 0L && partial.exists()) {
                partial.delete();
            }
            persist(true);
            input = new BufferedInputStream(connection.getInputStream(), 65536);
            output = new FileOutputStream(partial, existing > 0L);
            byte[] buffer = new byte[65536];
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (item.paused || item.cancelled || Thread.currentThread().isInterrupted()) {
                    break;
                }
                output.write(buffer, 0, count);
                item.downloaded += count;
                updateSpeed(item, count);
                persistIfNeeded(item);
            }
            output.flush();
            output.close();
            output = null;
            input.close();
            input = null;
            if (item.cancelled) {
                item.status = CANCELLED;
                safeDelete(partial);
            } else if (item.paused) {
                item.status = PAUSED;
            } else if (partial.exists() && (item.total <= 0L || item.downloaded >= item.total)) {
                File target = new File(item.path);
                File finalTarget = finalizeName(item, partial, target);
                if (partial.renameTo(finalTarget)) {
                    item.path = finalTarget.getAbsolutePath();
                    item.fileName = finalTarget.getName();
                    item.status = COMPLETED;
                    item.downloaded = finalTarget.length();
                    item.total = item.downloaded;
                    item.completedAt = System.currentTimeMillis();
                    finished = true;
                } else {
                    item.status = FAILED;
                    item.error = "rename";
                }
            } else {
                throw new IOException("truncated");
            }
        } catch (RetryException retry) {
            retryLater = true;
        } catch (Throwable error) {
            if (item.cancelled) {
                item.status = CANCELLED;
                safeDelete(partial);
            } else if (item.paused) {
                item.status = PAUSED;
            } else if (item.retries < MAX_AUTO_RETRY && item.error == null && isRecoverable(error)) {
                item.retries++;
                retryLater = true;
            } else {
                item.status = FAILED;
                if (item.error == null) {
                    item.error = "network";
                }
            }
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Throwable ignored) {
                }
            }
            if (output != null) {
                try {
                    output.close();
                } catch (Throwable ignored) {
                }
            }
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Throwable ignored) {
                }
            }
            item.connection = null;
            item.active = false;
            item.speed = 0L;
            boolean resumeAfterPause = item.resumeRequested && !item.cancelled && !finished;
            item.resumeRequested = false;
            persist(true);
            if (finished) {
                onCompleted(item);
            } else if (retryLater && !item.cancelled && !item.paused) {
                item.active = true;
                item.status = QUEUED;
                final DownloadItem again = item;
                MAIN.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!again.cancelled && !again.paused) {
                            EXECUTOR.execute(new Runnable() {
                                @Override
                                public void run() {
                                    runHttp(again);
                                }
                            });
                        } else {
                            again.active = false;
                        }
                    }
                }, 1500L * Math.max(1, item.retries));
            } else if (resumeAfterPause) {
                item.paused = false;
                start(item);
            }
        }
    }

    private static boolean isRecoverable(Throwable error) {
        return error instanceof java.net.SocketTimeoutException || error instanceof java.net.SocketException ||
                error instanceof java.io.EOFException || error instanceof javax.net.ssl.SSLException ||
                error instanceof IOException && "truncated".equals(error.getMessage());
    }

    private static void adoptServerName(DownloadItem item, HttpURLConnection connection) {
        try {
            String serverMime = DownloadNames.cleanMime(connection.getContentType());
            String disposition = connection.getHeaderField("Content-Disposition");
            if (!item.autoName) {
                if (DownloadNames.isGenericMime(item.mimeType) && !TextUtils.isEmpty(serverMime)) {
                    item.mimeType = serverMime;
                }
                return;
            }
            String baseHint = item.fileName;
            String name = DownloadNames.resolve(item.url, disposition, TextUtils.isEmpty(serverMime) ? item.mimeType : serverMime,
                    TextUtils.isEmpty(disposition) ? baseHint : null);
            if (TextUtils.isEmpty(name) || name.equals(item.fileName)) {
                if (!TextUtils.isEmpty(serverMime) && DownloadNames.isGenericMime(item.mimeType)) {
                    item.mimeType = serverMime;
                }
                return;
            }
            File partialOld = partialFile(item);
            boolean hadPartial = partialOld.exists() && partialOld.length() > 0;
            String folder = RenaPaths.folderFor(TextUtils.isEmpty(serverMime) ? item.mimeType : serverMime, name);
            if (hadPartial) {
                return;
            }
            File dir = RenaPaths.dir(folder);
            if (!dir.exists() && !dir.mkdirs()) {
                return;
            }
            File target = uniqueFile(dir, name);
            item.fileName = target.getName();
            item.path = target.getAbsolutePath();
            item.folder = folder;
            if (!TextUtils.isEmpty(serverMime)) {
                item.mimeType = serverMime;
            }
        } catch (Throwable ignored) {
        }
    }

    private static void onCompleted(final DownloadItem item) {
        try {
            if (appContext != null) {
                MediaScannerConnection.scanFile(appContext, new String[]{item.path}, new String[]{item.mimeType}, null);
            }
        } catch (Throwable ignored) {
        }
        MAIN.post(new Runnable() {
            @Override
            public void run() {
                try {
                    DownloadService.notifyCompleted(appContext, item);
                } catch (Throwable ignored) {
                }
            }
        });
    }

    private static void ensureService() {
        final Context context = appContext;
        if (context == null) {
            return;
        }
        MAIN.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Intent intent = new Intent(context, DownloadService.class);
                    if (Build.VERSION.SDK_INT >= 26) {
                        context.startForegroundService(intent);
                    } else {
                        context.startService(intent);
                    }
                } catch (Throwable ignored) {
                }
            }
        });
    }

    private static void updateSpeed(DownloadItem item, int count) {
        long now = System.currentTimeMillis();
        if (item.speedMark == 0L) {
            item.speedMark = now;
            item.speedBytes = 0L;
        }
        item.speedBytes += count;
        long elapsed = now - item.speedMark;
        if (elapsed >= 700L) {
            long instant = item.speedBytes * 1000L / elapsed;
            item.speed = item.speed == 0L ? instant : (item.speed * 6L + instant * 4L) / 10L;
            item.speedMark = now;
            item.speedBytes = 0L;
        }
    }

    private static File partialFile(DownloadItem item) {
        return new File(item.path + ".part");
    }

    private static void persistIfNeeded(DownloadItem item) {
        long now = System.currentTimeMillis();
        if (now - item.lastPersistAt >= 1500L) {
            item.lastPersistAt = now;
            persist(false);
        }
    }

    private static void persist(boolean force) {
        synchronized (LOCK) {
            persistLocked(force);
        }
    }

    private static void persistLocked(boolean force) {
        if (appContext == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (!force && now - lastPersist < 1500L) {
            return;
        }
        lastPersist = now;
        if (persistScheduled) {
            return;
        }
        persistScheduled = true;
        IO.execute(new Runnable() {
            @Override
            public void run() {
                synchronized (LOCK) {
                    persistScheduled = false;
                    writeNow();
                }
            }
        });
    }

    private static void writeNow() {
        if (appContext == null) {
            return;
        }
        try {
            JSONArray array = new JSONArray();
            for (DownloadItem item : ITEMS) {
                array.put(item.toJson());
            }
            RenaCrypto.writeText(appContext, new File(appContext.getFilesDir(), STORE_NAME), array.toString());
        } catch (Throwable ignored) {
        }
    }

    private static void safeDelete(File file) {
        if (file != null && file.exists()) {
            try {
                file.delete();
            } catch (Throwable ignored) {
            }
        }
    }

    private static final class RetryException extends IOException {
    }

    public static final class DownloadItem {
        public String id;
        public String url;
        public String fileName;
        public String folder = RenaPaths.OTHER;
        public String mimeType;
        public String userAgent;
        public String cookie;
        public String referer;
        public String path;
        public volatile int status = QUEUED;
        public String error;
        public long createdAt = System.currentTimeMillis();
        public long startedAt;
        public long completedAt;
        public long lastPersistAt;
        public volatile long downloaded;
        public volatile long total = -1L;
        public volatile long speed;
        public long speedMark;
        public long speedBytes;
        public boolean blob;
        public boolean autoName;
        public int sourceTabId = -1;
        public String sourceKey;
        public int retries;
        public volatile boolean paused;
        public volatile boolean cancelled;
        public volatile boolean active;
        public volatile boolean resumeRequested;
        volatile boolean blobWriteFailed;
        final transient Object blobLock = new Object();
        transient volatile HttpURLConnection connection;

        JSONObject toJson() {
            JSONObject object = new JSONObject();
            try {
                object.put("id", id);
                object.put("url", blob ? "" : url);
                object.put("fileName", fileName);
                object.put("folder", folder);
                object.put("mimeType", mimeType);
                object.put("userAgent", userAgent == null ? "" : userAgent);
                object.put("referer", referer == null ? "" : referer);
                object.put("path", path);
                object.put("status", status);
                object.put("error", error == null ? "" : error);
                object.put("createdAt", createdAt);
                object.put("completedAt", completedAt);
                object.put("downloaded", downloaded);
                object.put("total", total);
                object.put("blob", blob);
                object.put("autoName", autoName);
                object.put("sourceTabId", sourceTabId);
                object.put("sourceKey", sourceKey == null ? "" : sourceKey);
            } catch (Throwable ignored) {
            }
            return object;
        }

        static DownloadItem fromJson(JSONObject object) {
            if (object == null) {
                return null;
            }
            DownloadItem item = new DownloadItem();
            item.id = object.optString("id", "");
            item.url = object.optString("url", "");
            item.fileName = object.optString("fileName", "download");
            item.folder = object.optString("folder", RenaPaths.OTHER);
            item.mimeType = object.optString("mimeType", "application/octet-stream");
            item.userAgent = object.optString("userAgent", "");
            item.referer = object.optString("referer", "");
            item.cookie = "";
            item.path = object.optString("path", "");
            item.status = object.optInt("status", FAILED);
            String error = object.optString("error", "");
            item.error = error.length() == 0 ? null : error;
            item.createdAt = object.optLong("createdAt", System.currentTimeMillis());
            item.completedAt = object.optLong("completedAt", 0L);
            item.downloaded = object.optLong("downloaded", 0L);
            item.total = object.optLong("total", -1L);
            item.blob = object.optBoolean("blob", false);
            item.autoName = object.optBoolean("autoName", false);
            item.sourceTabId = object.optInt("sourceTabId", -1);
            String key = object.optString("sourceKey", "");
            item.sourceKey = key.length() == 0 ? null : key;
            return TextUtils.isEmpty(item.id) || TextUtils.isEmpty(item.path) ? null : item;
        }

        public boolean isFinished() {
            return status == COMPLETED || status == FAILED || status == CANCELLED;
        }

        public int percent() {
            if (total > 0) {
                return (int) Math.min(100L, downloaded * 100L / total);
            }
            return status == COMPLETED ? 100 : 0;
        }
    }
}
