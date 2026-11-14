package com.rena.w4b;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileNotFoundException;

public class UpdateFileProvider extends ContentProvider {
    public static final String AUTHORITY = "com.rena.w4b.updates";

    public static Uri buildUri(Context context, File file) {
        if (file != null) {
            try {
                File root = new File(Environment.getExternalStorageDirectory(), NativeConfig.downloadStorageRootName()).getCanonicalFile();
                File target = file.getCanonicalFile();
                String rootPath = root.getPath() + File.separator;
                if (target.getPath().startsWith(rootPath)) {
                    String relative = target.getPath().substring(rootPath.length());
                    Uri.Builder builder = new Uri.Builder()
                            .scheme("content")
                            .authority(AUTHORITY)
                            .appendPath("download");
                    String[] segments = relative.split("/");
                    for (String segment : segments) {
                        if (segment.length() > 0) {
                            builder.appendPath(segment);
                        }
                    }
                    return builder.build();
                }
            } catch (Throwable ignored) {
            }
        }
        return new Uri.Builder()
                .scheme("content")
                .authority(AUTHORITY)
                .appendPath(file == null ? "" : file.getName())
                .build();
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public String getType(Uri uri) {
        File file = resolve(uri);
        String extension = MimeTypeMap.getFileExtensionFromUrl(file.getName());
        String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                extension == null ? "" : extension.toLowerCase(java.util.Locale.US)
        );
        if (file.getName().toLowerCase(java.util.Locale.US).endsWith(".apk")) {
            return "application/vnd.android.package-archive";
        }
        return type == null ? "application/octet-stream" : type;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File file = resolve(uri);
        if (!file.exists() || !file.isFile()) {
            throw new FileNotFoundException(uri.toString());
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        File file = resolve(uri);
        String[] columns = projection;
        if (columns == null || columns.length == 0) {
            columns = new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
        }
        MatrixCursor cursor = new MatrixCursor(columns, 1);
        Object[] row = new Object[columns.length];
        for (int i = 0; i < columns.length; i++) {
            if (OpenableColumns.DISPLAY_NAME.equals(columns[i])) {
                row[i] = file.getName();
            } else if (OpenableColumns.SIZE.equals(columns[i])) {
                row[i] = file.length();
            }
        }
        cursor.addRow(row);
        return cursor;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        File file = resolve(uri);
        return file.delete() ? 1 : 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    private File resolve(Uri uri) {
        Context context = getContext();
        if (context == null || uri == null) {
            return new File("/dev/null");
        }
        if (uri.getPathSegments().size() >= 3 && "download".equals(uri.getPathSegments().get(0))) {
            try {
                File root = new File(Environment.getExternalStorageDirectory(), NativeConfig.downloadStorageRootName()).getCanonicalFile();
                File target = root;
                for (int i = 1; i < uri.getPathSegments().size(); i++) {
                    String segment = uri.getPathSegments().get(i);
                    if (segment.indexOf('/') >= 0 || segment.indexOf('\\') >= 0 || ".".equals(segment) || "..".equals(segment)) {
                        return new File("/dev/null");
                    }
                    target = new File(target, segment);
                }
                target = target.getCanonicalFile();
                String rootPath = root.getPath() + File.separator;
                if (!target.getPath().startsWith(rootPath)) {
                    return new File("/dev/null");
                }
                return target;
            } catch (Throwable ignored) {
                return new File("/dev/null");
            }
        }
        String name = uri.getLastPathSegment();
        if (!"Rena-W4B-update.apk".equals(name)) {
            return new File("/dev/null");
        }
        File root = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (root == null) {
            return new File("/dev/null");
        }
        return new File(root, "Rena/Updates/Rena-W4B-update.apk");
    }
}
