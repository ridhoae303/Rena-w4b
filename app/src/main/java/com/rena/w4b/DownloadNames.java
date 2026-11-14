package com.rena.w4b;

import android.net.Uri;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class DownloadNames {
    private DownloadNames() {
    }

    public static String fromContentDisposition(String header) {
        if (TextUtils.isEmpty(header)) {
            return null;
        }
        try {
            String lower = header.toLowerCase(Locale.US);
            int star = lower.indexOf("filename*=");
            if (star >= 0) {
                String value = header.substring(star + 10).trim();
                int end = value.indexOf(';');
                if (end >= 0) {
                    value = value.substring(0, end).trim();
                }
                int quote = value.indexOf("''");
                if (quote >= 0) {
                    String charset = value.substring(0, quote);
                    String encoded = value.substring(quote + 2);
                    if (encoded.startsWith("\"") && encoded.endsWith("\"") && encoded.length() > 1) {
                        encoded = encoded.substring(1, encoded.length() - 1);
                    }
                    String decoded = java.net.URLDecoder.decode(encoded.replace("+", "%2B"),
                            TextUtils.isEmpty(charset) ? "UTF-8" : charset);
                    if (!TextUtils.isEmpty(decoded)) {
                        return decoded;
                    }
                }
            }
            int plain = lower.indexOf("filename=");
            if (plain >= 0) {
                String value = header.substring(plain + 9).trim();
                if (value.startsWith("\"")) {
                    int close = value.indexOf('"', 1);
                    if (close > 0) {
                        return value.substring(1, close);
                    }
                    return value.substring(1);
                }
                int end = value.indexOf(';');
                if (end >= 0) {
                    value = value.substring(0, end);
                }
                return value.trim();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static String fromUrl(String url) {
        if (TextUtils.isEmpty(url) || url.startsWith("blob:") || url.startsWith("data:")) {
            return null;
        }
        try {
            Uri uri = Uri.parse(url);
            String last = uri.getLastPathSegment();
            if (TextUtils.isEmpty(last)) {
                return null;
            }
            String decoded = Uri.decode(last);
            return TextUtils.isEmpty(decoded) ? null : decoded;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static String sanitize(String name) {
        if (name == null) {
            return "download";
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c < 32 || c == '/' || c == '\\' || c == ':' || c == '*' || c == '?' || c == '"' ||
                    c == '<' || c == '>' || c == '|') {
                out.append('_');
            } else {
                out.append(c);
            }
        }
        String clean = out.toString().trim();
        while (clean.startsWith(".")) {
            clean = clean.substring(1);
        }
        while (clean.endsWith(".") || clean.endsWith(" ")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        if (clean.length() > 120) {
            int dot = clean.lastIndexOf('.');
            String ext = dot > 0 && clean.length() - dot <= 12 ? clean.substring(dot) : "";
            clean = clean.substring(0, 120 - ext.length()) + ext;
        }
        return TextUtils.isEmpty(clean) ? "download" : clean;
    }

    public static String extensionOf(String name) {
        if (name == null) {
            return "";
        }
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1 || name.length() - dot > 10) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.US);
    }

    public static boolean isGenericMime(String mime) {
        if (TextUtils.isEmpty(mime)) {
            return true;
        }
        String m = mime.toLowerCase(Locale.US);
        return m.startsWith("application/octet-stream") || m.startsWith("binary/octet-stream") ||
                m.startsWith("application/force-download") || m.startsWith("application/download") ||
                m.startsWith("application/x-download") || m.equals("*/*");
    }

    public static String cleanMime(String mime) {
        if (TextUtils.isEmpty(mime)) {
            return "";
        }
        int semi = mime.indexOf(';');
        return (semi >= 0 ? mime.substring(0, semi) : mime).trim().toLowerCase(Locale.US);
    }

    public static String extensionForMime(String mime) {
        String clean = cleanMime(mime);
        if (TextUtils.isEmpty(clean) || isGenericMime(clean)) {
            return null;
        }
        if ("application/vnd.android.package-archive".equals(clean)) {
            return "apk";
        }
        if ("audio/ogg".equals(clean) || "audio/opus".equals(clean)) {
            return "ogg";
        }
        if ("audio/mpeg".equals(clean)) {
            return "mp3";
        }
        if ("image/jpeg".equals(clean)) {
            return "jpg";
        }
        if ("text/plain".equals(clean)) {
            return "txt";
        }
        try {
            return MimeTypeMap.getSingleton().getExtensionFromMimeType(clean);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static String mimeForName(String name) {
        String ext = extensionOf(name);
        if (ext.length() == 0) {
            return "application/octet-stream";
        }
        if ("apk".equals(ext)) {
            return "application/vnd.android.package-archive";
        }
        try {
            String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
            return TextUtils.isEmpty(type) ? "application/octet-stream" : type;
        } catch (Throwable ignored) {
            return "application/octet-stream";
        }
    }

    public static String resolve(String url, String contentDisposition, String mimeType, String hint) {
        String name = fromContentDisposition(contentDisposition);
        if (TextUtils.isEmpty(name) && !TextUtils.isEmpty(hint) && !"download".equalsIgnoreCase(hint)) {
            name = hint;
        }
        if (TextUtils.isEmpty(name)) {
            name = fromUrl(url);
        }
        if (TextUtils.isEmpty(name) || looksLikeUuid(stripExt(name)) && TextUtils.isEmpty(extensionOf(name))) {
            String base = TextUtils.isEmpty(name) ? "download" : name;
            name = base;
        }
        name = sanitize(name);
        return fixExtension(name, mimeType);
    }

    public static String fixExtension(String name, String mimeType) {
        String ext = extensionOf(name);
        String mimeExt = extensionForMime(mimeType);
        if (ext.length() == 0 && mimeExt != null) {
            return name + "." + mimeExt;
        }
        if (("bin".equals(ext) || "dat".equals(ext) || "download".equals(ext)) && mimeExt != null) {
            return name.substring(0, name.length() - ext.length()) + mimeExt;
        }
        return name;
    }

    public static boolean looksLikeUuid(String value) {
        return value != null && value.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    }

    private static String stripExt(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    public static String sniffExtension(File file) {
        if (file == null || !file.isFile() || file.length() < 4) {
            return null;
        }
        InputStream input = null;
        try {
            input = new FileInputStream(file);
            byte[] head = new byte[16];
            int read = input.read(head);
            if (read < 4) {
                return null;
            }
            if (head[0] == 'P' && head[1] == 'K' && head[2] == 3 && head[3] == 4) {
                input.close();
                input = null;
                ZipFile zip = null;
                try {
                    zip = new ZipFile(file);
                    if (zip.getEntry("AndroidManifest.xml") != null && zip.getEntry("classes.dex") != null) {
                        return "apk";
                    }
                    if (zip.getEntry("[Content_Types].xml") != null) {
                        if (zip.getEntry("word/document.xml") != null) {
                            return "docx";
                        }
                        if (zip.getEntry("xl/workbook.xml") != null) {
                            return "xlsx";
                        }
                        if (zip.getEntry("ppt/presentation.xml") != null) {
                            return "pptx";
                        }
                    }
                    ZipEntry apks = zip.getEntry("base.apk");
                    if (apks != null || zip.getEntry("manifest.json") != null && zip.getEntry("icon.png") != null) {
                        return "xapk";
                    }
                    return "zip";
                } finally {
                    if (zip != null) {
                        try {
                            zip.close();
                        } catch (Throwable ignored) {
                        }
                    }
                }
            }
            if (head[0] == '%' && head[1] == 'P' && head[2] == 'D' && head[3] == 'F') {
                return "pdf";
            }
            if ((head[0] & 0xff) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G') {
                return "png";
            }
            if ((head[0] & 0xff) == 0xff && (head[1] & 0xff) == 0xd8) {
                return "jpg";
            }
            if (head[0] == 'G' && head[1] == 'I' && head[2] == 'F') {
                return "gif";
            }
            if (head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F' && read >= 12 &&
                    head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P') {
                return "webp";
            }
            if (read >= 8 && head[4] == 'f' && head[5] == 't' && head[6] == 'y' && head[7] == 'p') {
                return "mp4";
            }
            if (head[0] == 'O' && head[1] == 'g' && head[2] == 'g' && head[3] == 'S') {
                return "ogg";
            }
            if (head[0] == 'I' && head[1] == 'D' && head[2] == '3') {
                return "mp3";
            }
            if (head[0] == '7' && head[1] == 'z' && (head[2] & 0xff) == 0xbc) {
                return "7z";
            }
            if (head[0] == 'R' && head[1] == 'a' && head[2] == 'r' && head[3] == '!') {
                return "rar";
            }
            if (head[0] == 0x1a && (head[1] & 0xff) == 0x45 && (head[2] & 0xff) == 0xdf) {
                return "mkv";
            }
        } catch (Throwable ignored) {
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }
}
