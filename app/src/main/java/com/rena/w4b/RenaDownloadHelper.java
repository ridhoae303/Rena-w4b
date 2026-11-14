package com.rena.w4b;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.lang.ref.WeakReference;

public final class RenaDownloadHelper {
    private static Pending pending;
    private static String lastUrl = "";
    private static long lastAt;
    private static boolean dialogShowing;

    private RenaDownloadHelper() {
    }

    private static final class Pending {
        String sourceKey;
        int tabId;
        String url;
        String userAgent;
        String contentDisposition;
        String mime;
        long length;
        String cookie;
        String referer;
        WeakReference<WebView> source;
    }

    private static final class Meta {
        String name = "";
        String type = "";
        long size = -1L;
    }

    public static final class Bridge {
        @JavascriptInterface
        public boolean begin(String id, long size, String mime) {
            return RenaDownloadManager.beginBlob(id, size, mime);
        }

        @JavascriptInterface
        public boolean isPaused(String id) {
            return RenaDownloadManager.isPaused(id);
        }

        @JavascriptInterface
        public boolean isCancelled(String id) {
            return RenaDownloadManager.isCancelled(id);
        }

        @JavascriptInterface
        public boolean write(String id, String data) {
            return RenaDownloadManager.appendBlobChunk(id, data);
        }

        @JavascriptInterface
        public void finish(String id, boolean success) {
            RenaDownloadManager.finishBlob(id, success);
        }
    }

    public static void attach(WebView webView) {
        try {
            webView.addJavascriptInterface(new Bridge(), "RenaDownloads");
        } catch (Throwable ignored) {
        }
    }

    public static void handle(Activity activity, WebView source, String sourceKey, int tabId, String url,
                              String userAgent, String contentDisposition, String mime, long length,
                              String cookie, String referer) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        if (TextUtils.isEmpty(url)) {
            toast(activity, activity.getString(R.string.download_failed_text));
            return;
        }
        boolean blob = isBlob(url);
        if (!blob) {
            String scheme = "";
            try {
                scheme = Uri.parse(url).getScheme();
            } catch (Throwable ignored) {
            }
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                toast(activity, activity.getString(R.string.download_failed_text));
                return;
            }
        }
        long now = System.currentTimeMillis();
        if (url.equals(lastUrl) && now - lastAt < 2500L) {
            return;
        }
        lastUrl = url;
        lastAt = now;
        Pending request = new Pending();
        request.sourceKey = sourceKey;
        request.tabId = tabId;
        request.url = url;
        request.userAgent = userAgent;
        request.contentDisposition = contentDisposition;
        request.mime = mime;
        request.length = length;
        request.cookie = cookie;
        request.referer = referer;
        request.source = new WeakReference<WebView>(source);
        proceed(activity, request);
    }

    public static void resumePending(Activity activity) {
        Pending request = pending;
        if (request == null || activity == null || activity.isFinishing()) {
            return;
        }
        if (RenaPaths.canWrite()) {
            pending = null;
            proceed(activity, request);
        }
    }

    private static boolean isBlob(String url) {
        return url.startsWith("blob:") || url.startsWith("data:");
    }

    private static void proceed(final Activity activity, final Pending request) {
        if (!RenaPaths.canWrite()) {
            pending = request;
            requestStorageAccess(activity);
            return;
        }
        RenaPaths.ensureDirs();
        if (isBlob(request.url)) {
            WebView view = request.source == null ? null : request.source.get();
            if (view == null) {
                toast(activity, activity.getString(R.string.download_failed_text));
                return;
            }
            fetchMeta(view, request.url, new MetaCallback() {
                @Override
                public void done(Meta meta) {
                    showSaveDialog(activity, request, meta);
                }
            });
        } else {
            showSaveDialog(activity, request, null);
        }
    }

    private interface MetaCallback {
        void done(Meta meta);
    }

    private static void fetchMeta(WebView view, String url, final MetaCallback callback) {
        final Meta meta = new Meta();
        if (url.startsWith("data:")) {
            int comma = url.indexOf(',');
            int semi = url.indexOf(';');
            int end = semi > 0 && (comma < 0 || semi < comma) ? semi : comma;
            if (end > 5) {
                meta.type = url.substring(5, end);
            }
        }
        final boolean[] answered = new boolean[]{false};
        final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        final Runnable timeout = new Runnable() {
            @Override
            public void run() {
                if (!answered[0]) {
                    answered[0] = true;
                    callback.done(meta);
                }
            }
        };
        handler.postDelayed(timeout, 1800L);
        try {
            String script = "(function(u){try{return window.__renaBlobMeta?JSON.stringify(window.__renaBlobMeta(u)):null;}catch(e){return null;}})("
                    + PageScripts.quote(url) + ")";
            view.evaluateJavascript(script, new android.webkit.ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    if (answered[0]) {
                        return;
                    }
                    answered[0] = true;
                    handler.removeCallbacks(timeout);
                    try {
                        if (value != null && !"null".equals(value)) {
                            Object parsed = new JSONTokener(value).nextValue();
                            if (parsed instanceof String) {
                                JSONObject object = new JSONObject((String) parsed);
                                meta.name = object.optString("name", "");
                                String type = object.optString("type", "");
                                if (!TextUtils.isEmpty(type)) {
                                    meta.type = type;
                                }
                                meta.size = object.optLong("size", -1L);
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                    callback.done(meta);
                }
            });
        } catch (Throwable error) {
            if (!answered[0]) {
                answered[0] = true;
                handler.removeCallbacks(timeout);
                callback.done(meta);
            }
        }
    }

    public static void askStorageAccess(final Activity activity, final Runnable onCancel) {
        askStorage(activity, onCancel);
    }

    private static void requestStorageAccess(final Activity activity) {
        askStorage(activity, new Runnable() {
            @Override
            public void run() {
                cancelPending(activity);
            }
        });
    }

    private static void askStorage(final Activity activity, final Runnable onCancel) {
        if (Build.VERSION.SDK_INT < 30) {
            try {
                if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                        android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    activity.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE}, 7160);
                    return;
                }
            } catch (Throwable ignored) {
            }
            toast(activity, activity.getString(R.string.download_denied_text));
            if (onCancel != null) {
                onCancel.run();
            }
            return;
        }
        new RenaDialog.Builder(activity)
                .setTitle(R.string.storage_access_title)
                .setMessage(R.string.storage_access_message)
                .setNegativeButton(R.string.cancel_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (onCancel != null) {
                            onCancel.run();
                        }
                    }
                })
                .setPositiveButton(R.string.storage_settings_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            activity.startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                    Uri.parse("package:" + activity.getPackageName())));
                        } catch (Throwable first) {
                            try {
                                activity.startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
                            } catch (Throwable second) {
                                if (onCancel != null) {
                            onCancel.run();
                        }
                            }
                        }
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        if (onCancel != null) {
                            onCancel.run();
                        }
                    }
                })
                .show();
    }

    private static void cancelPending(Activity activity) {
        Pending request = pending;
        pending = null;
        if (request != null && isBlob(request.url) && request.source != null) {
            release(request.source.get(), request.url);
        }
    }

    public static void release(WebView view, String url) {
        if (view == null || TextUtils.isEmpty(url)) {
            return;
        }
        try {
            view.evaluateJavascript("(function(u){try{if(window.__renaReleaseBlob){window.__renaReleaseBlob(u);}}catch(e){}})("
                    + PageScripts.quote(url) + ")", null);
        } catch (Throwable ignored) {
        }
    }

    private static void showSaveDialog(final Activity activity, final Pending request, final Meta meta) {
        if (activity.isFinishing() || dialogShowing) {
            return;
        }
        final boolean blob = isBlob(request.url);
        String mimeType = request.mime;
        long size = request.length;
        String hint = null;
        if (meta != null) {
            if (!TextUtils.isEmpty(meta.type) && (DownloadNames.isGenericMime(mimeType) || !DownloadNames.isGenericMime(meta.type))) {
                mimeType = meta.type;
            }
            if (meta.size >= 0) {
                size = meta.size;
            }
            if (!TextUtils.isEmpty(meta.name)) {
                hint = meta.name;
            }
        }
        final String finalMime = TextUtils.isEmpty(mimeType) ? "application/octet-stream" : mimeType;
        final long finalSize = size;
        final String suggested = DownloadNames.resolve(request.url, request.contentDisposition, finalMime, hint);
        final boolean hasRealName = !TextUtils.isEmpty(hint) || !TextUtils.isEmpty(DownloadNames.fromContentDisposition(request.contentDisposition))
                || !TextUtils.isEmpty(DownloadNames.fromUrl(request.url)) && !blob;

        final String[] folders = RenaPaths.folders();
        final String[] labels = new String[folders.length];
        for (int i = 0; i < folders.length; i++) {
            labels[i] = RenaPaths.label(activity, folders[i]);
        }
        String initialFolder = RenaPaths.folderFor(finalMime, suggested);
        final int[] selected = new int[]{0};
        for (int i = 0; i < folders.length; i++) {
            if (folders[i].equals(initialFolder)) {
                selected[0] = i;
            }
        }

        final LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(activity, 2);
        content.setPadding(pad, 0, pad, 0);

        TextView nameLabel = Ui.text(activity, activity.getString(R.string.download_file_name_hint), 12f, RenaTheme.muted(), Typeface.BOLD);
        content.addView(nameLabel, new LinearLayout.LayoutParams(-2, -2));

        final EditText input = new EditText(activity);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setText(suggested);
        input.setTextColor(RenaTheme.text());
        input.setHintTextColor(RenaTheme.faint());
        input.setHint(R.string.download_file_name_hint);
        input.setTextSize(15f);
        input.setTypeface(Ui.font(activity, Typeface.NORMAL));
        input.setBackground(Ui.roundStroke(RenaTheme.surface2(), Ui.dp(activity, 14), RenaTheme.outline(), Ui.dp(activity, 1)));
        input.setPadding(Ui.dp(activity, 14), 0, Ui.dp(activity, 14), 0);
        input.setHighlightColor(RenaTheme.accentSoft());
        int dot = suggested.lastIndexOf('.');
        input.setSelection(0, dot > 0 ? dot : suggested.length());
        LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(-1, Ui.dp(activity, 50));
        inputLp.topMargin = Ui.dp(activity, 6);
        content.addView(input, inputLp);

        TextView folderLabel = Ui.text(activity, activity.getString(R.string.download_save_to_label), 12f, RenaTheme.muted(), Typeface.BOLD);
        LinearLayout.LayoutParams folderLabelLp = new LinearLayout.LayoutParams(-2, -2);
        folderLabelLp.topMargin = Ui.dp(activity, 14);
        content.addView(folderLabel, folderLabelLp);

        final LinearLayout picker = new LinearLayout(activity);
        picker.setOrientation(LinearLayout.HORIZONTAL);
        picker.setGravity(Gravity.CENTER_VERTICAL);
        picker.setPadding(Ui.dp(activity, 14), 0, Ui.dp(activity, 10), 0);
        picker.setBackground(Ui.ripple(activity, Ui.roundStroke(RenaTheme.surface2(), Ui.dp(activity, 14), RenaTheme.outline(), Ui.dp(activity, 1)), 14));
        final TextView pickerLabel = Ui.text(activity, labels[selected[0]], 15f, RenaTheme.text(), Typeface.NORMAL);
        picker.addView(pickerLabel, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView chevron = Ui.text(activity, "\u25BE", 18f, RenaTheme.muted(), Typeface.NORMAL);
        picker.addView(chevron, new LinearLayout.LayoutParams(-2, -2));
        LinearLayout.LayoutParams pickerLp = new LinearLayout.LayoutParams(-1, Ui.dp(activity, 50));
        pickerLp.topMargin = Ui.dp(activity, 6);
        content.addView(picker, pickerLp);

        final TextView pathView = Ui.text(activity, RenaPaths.displayPath(folders[selected[0]]), 12f, RenaTheme.muted(), Typeface.NORMAL);
        LinearLayout.LayoutParams pathLp = new LinearLayout.LayoutParams(-2, -2);
        pathLp.topMargin = Ui.dp(activity, 10);
        content.addView(pathView, pathLp);

        StringBuilder info = new StringBuilder();
        if (finalSize > 0) {
            info.append(DownloadFormat.size(finalSize));
        }
        if (!DownloadNames.isGenericMime(finalMime)) {
            if (info.length() > 0) {
                info.append("  \u00B7  ");
            }
            info.append(DownloadNames.cleanMime(finalMime));
        }
        if (info.length() > 0) {
            TextView infoView = Ui.text(activity, info.toString(), 12f, RenaTheme.faint(), Typeface.NORMAL);
            LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(-2, -2);
            infoLp.topMargin = Ui.dp(activity, 4);
            content.addView(infoView, infoLp);
        }

        picker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new RenaDialog.Builder(activity)
                        .setTitle(R.string.download_save_to_label)
                        .setSingleChoiceItems(labels, selected[0], new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                selected[0] = which;
                                pickerLabel.setText(labels[which]);
                                pathView.setText(RenaPaths.displayPath(folders[which]));
                            }
                        })
                        .show();
            }
        });

        final boolean[] confirmed = new boolean[]{false};
        dialogShowing = true;
        final RenaDialog dialog = new RenaDialog.Builder(activity)
                .setTitle(R.string.download_save_title)
                .setView(content)
                .setNegativeButton(R.string.cancel_text, null)
                .setPositiveButton(R.string.download_save_button_text, null)
                .create();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface d) {
                dialogShowing = false;
                if (!confirmed[0] && blob && request.source != null) {
                    release(request.source.get(), request.url);
                }
            }
        });
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface d) {
                dialog.getButton(RenaDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String typed = input.getText().toString();
                        String clean = DownloadNames.sanitize(typed);
                        if (TextUtils.isEmpty(typed.trim())) {
                            input.setError(activity.getString(R.string.download_file_name_hint));
                            return;
                        }
                        clean = DownloadNames.fixExtension(clean, finalMime);
                        confirmed[0] = true;
                        dialog.dismiss();
                        boolean edited = !typed.trim().equals(suggested);
                        begin(activity, request, clean, folders[selected[0]], finalMime, finalSize, !edited && !hasRealName);
                    }
                });
            }
        });
        dialog.show();
    }

    private static void begin(Activity activity, Pending request, String fileName, String folder, String mime,
                              long size, boolean autoName) {
        if (isBlob(request.url)) {
            WebView view = request.source == null ? null : request.source.get();
            final RenaDownloadManager.DownloadItem item = RenaDownloadManager.createBlob(activity, request.url,
                    fileName, folder, mime, size, request.tabId, request.sourceKey);
            if (item == null || view == null) {
                if (item != null) {
                    RenaDownloadManager.finishBlob(item.id, false);
                }
                release(view, request.url);
                toast(activity, activity.getString(R.string.download_failed_text));
                return;
            }
            final WebView target = view;
            try {
                String script = "(function(id,u){try{return window.__renaReadBlob?window.__renaReadBlob(id,u):false;}catch(e){return false;}})("
                        + PageScripts.quote(item.id) + "," + PageScripts.quote(request.url) + ")";
                final Activity host = activity;
                target.evaluateJavascript(script, new android.webkit.ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        if (!"true".equals(value)) {
                            RenaDownloadManager.finishBlob(item.id, false);
                            toast(host, host.getString(R.string.download_failed_text));
                        }
                    }
                });
            } catch (Throwable error) {
                RenaDownloadManager.finishBlob(item.id, false);
                toast(activity, activity.getString(R.string.download_failed_text));
                return;
            }
            toast(activity, activity.getString(R.string.download_started_text, fileName));
            return;
        }
        RenaDownloadManager.DownloadItem item = RenaDownloadManager.createHttp(activity, request.url, fileName,
                folder, mime, request.userAgent, request.cookie, request.referer, size, autoName);
        if (item == null) {
            toast(activity, activity.getString(R.string.download_failed_text));
            return;
        }
        toast(activity, activity.getString(R.string.download_started_text, fileName));
    }

    public static void saveSilently(final Activity activity, final WebView view, final String sourceKey, final int tabId,
                                    final String url, final String guessMime) {
        if (activity == null || view == null || TextUtils.isEmpty(url)) {
            return;
        }
        if (!RenaPaths.canWrite()) {
            toast(activity, activity.getString(R.string.download_denied_text));
            return;
        }
        RenaPaths.ensureDirs();
        fetchMeta(view, url, new MetaCallback() {
            @Override
            public void done(Meta meta) {
                String mime = !TextUtils.isEmpty(meta.type) ? meta.type : guessMime;
                String ext = DownloadNames.extensionForMime(mime);
                if (ext == null) {
                    ext = guessMime != null && guessMime.startsWith("video") ? "mp4" : "jpg";
                }
                String stamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(new java.util.Date());
                String name = "Status_" + stamp + "." + ext;
                final RenaDownloadManager.DownloadItem item = RenaDownloadManager.createBlob(activity, url, name,
                        RenaPaths.STATUS, mime, meta.size, tabId, sourceKey);
                if (item == null) {
                    return;
                }
                try {
                    String script = "(function(id,u){try{return window.__renaReadBlob?window.__renaReadBlob(id,u):false;}catch(e){return false;}})("
                            + PageScripts.quote(item.id) + "," + PageScripts.quote(url) + ")";
                    view.evaluateJavascript(script, new android.webkit.ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {
                            if (!"true".equals(value)) {
                                RenaDownloadManager.finishBlob(item.id, false);
                            }
                        }
                    });
                    toast(activity, activity.getString(R.string.status_saved_to, RenaPaths.displayPath(RenaPaths.STATUS)));
                } catch (Throwable error) {
                    RenaDownloadManager.finishBlob(item.id, false);
                }
            }
        });
    }

    public static void toast(Activity activity, String message) {
        try {
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
        } catch (Throwable ignored) {
        }
    }

    public static File fileOf(RenaDownloadManager.DownloadItem item) {
        return item == null || TextUtils.isEmpty(item.path) ? null : new File(item.path);
    }
}
