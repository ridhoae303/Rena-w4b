package com.rena.w4b;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Locale;

public final class RenaPaths {
    public static final String IMAGES = "Images";
    public static final String VIDEOS = "Videos";
    public static final String AUDIO = "Audio";
    public static final String VOICE = "Voice Notes";
    public static final String DOCUMENT = "Document";
    public static final String PACKAGES = "Packages";
    public static final String OTHER = "Other";
    public static final String STATUS = "Status";

    private RenaPaths() {
    }

    public static String[] folders() {
        return new String[]{IMAGES, VIDEOS, AUDIO, VOICE, DOCUMENT, PACKAGES, STATUS, OTHER};
    }

    public static File root() {
        return new File(Environment.getExternalStorageDirectory(), NativeConfig.downloadStorageRootName());
    }

    public static File dir(String folder) {
        return new File(root(), TextUtils.isEmpty(folder) ? OTHER : folder);
    }

    public static boolean hasAllFilesAccess() {
        if (Build.VERSION.SDK_INT < 30) {
            return true;
        }
        try {
            return Environment.isExternalStorageManager();
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean canWrite() {
        try {
            if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                return false;
            }
            if (Build.VERSION.SDK_INT >= 30 && !hasAllFilesAccess()) {
                return false;
            }
            File root = root();
            if (!root.exists() && !root.mkdirs()) {
                return false;
            }
            if (!root.isDirectory()) {
                return false;
            }
            File probe = new File(root, ".rena_write_test");
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(probe, false);
                output.write(0);
                output.flush();
            } finally {
                if (output != null) {
                    try {
                        output.close();
                    } catch (Throwable ignored) {
                    }
                }
                try {
                    probe.delete();
                } catch (Throwable ignored) {
                }
            }
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void ensureDirs() {
        try {
            File root = root();
            if (!root.exists()) {
                root.mkdirs();
            }
            for (String folder : folders()) {
                File dir = new File(root, folder);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public static String label(Context context, String folder) {
        if (IMAGES.equals(folder)) {
            return context.getString(R.string.download_category_images);
        }
        if (VIDEOS.equals(folder)) {
            return context.getString(R.string.download_category_videos);
        }
        if (AUDIO.equals(folder)) {
            return context.getString(R.string.download_category_audio);
        }
        if (VOICE.equals(folder)) {
            return context.getString(R.string.download_category_voice_notes);
        }
        if (DOCUMENT.equals(folder)) {
            return context.getString(R.string.download_category_documents);
        }
        if (PACKAGES.equals(folder)) {
            return context.getString(R.string.download_category_packages);
        }
        if (STATUS.equals(folder)) {
            return context.getString(R.string.download_category_status);
        }
        return context.getString(R.string.download_category_other);
    }

    public static String displayPath(String folder) {
        return "/" + NativeConfig.downloadStorageRootName() + "/" + (TextUtils.isEmpty(folder) ? OTHER : folder) + "/";
    }

    public static String folderFor(String mimeType, String fileName) {
        String name = fileName == null ? "" : fileName.toLowerCase(Locale.US);
        String mime = mimeType == null ? "" : mimeType.toLowerCase(Locale.US);
        if (name.endsWith(".apk") || name.endsWith(".apks") || name.endsWith(".xapk") || name.endsWith(".apkm") ||
                "application/vnd.android.package-archive".equals(mime)) {
            return PACKAGES;
        }
        if (isVoiceName(name) && (mime.startsWith("audio/") || hasExt(name, "ogg", "opus", "m4a", "mp3", "aac", "wav", "amr"))) {
            return VOICE;
        }
        if (mime.startsWith("image/") || hasExt(name, "png", "jpg", "jpeg", "gif", "webp", "bmp", "svg", "heic", "heif", "avif", "ico")) {
            return IMAGES;
        }
        if (mime.startsWith("video/") || hasExt(name, "mp4", "mkv", "webm", "mov", "avi", "3gp", "m4v", "flv", "wmv", "ts")) {
            return VIDEOS;
        }
        if (mime.startsWith("audio/") || hasExt(name, "mp3", "m4a", "aac", "ogg", "opus", "wav", "flac", "amr", "mid")) {
            return AUDIO;
        }
        if (hasExt(name, "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv", "rtf", "odt", "ods", "odp",
                "epub", "md", "json", "xml", "html", "htm") ||
                mime.startsWith("text/") || mime.contains("pdf") || mime.contains("msword") ||
                mime.contains("officedocument") || mime.contains("spreadsheet") || mime.contains("presentation")) {
            return DOCUMENT;
        }
        return OTHER;
    }

    private static boolean isVoiceName(String name) {
        return name.contains("voice") || name.contains("ptt-") || name.startsWith("ptt") || name.contains("-ptt");
    }

    private static boolean hasExt(String name, String... exts) {
        for (String ext : exts) {
            if (name.endsWith("." + ext)) {
                return true;
            }
        }
        return false;
    }
}
