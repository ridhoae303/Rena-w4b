package com.rena.w4b;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Objects;

public class UpdateFileProvider extends ContentProvider {
    public static final String AUTHORITY = "com.rena.w4b.updates";

    public static Uri buildUri(Context context, File file) {
        return new Uri.Builder()
                .scheme("content")
                .authority(AUTHORITY)
                .appendPath(file.getName())
                .build();
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public String getType(Uri uri) {
        return "application/vnd.android.package-archive";
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode)
            throws FileNotFoundException {
        File file = resolve(uri);
        if (!file.exists()) {
            throw new FileNotFoundException(uri.toString());
        }
        return ParcelFileDescriptor.open(
                file,
                ParcelFileDescriptor.MODE_READ_ONLY
        );
    }

    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder
    ) {
        File file = resolve(uri);

        String[] columns = projection;
        if (columns == null || columns.length == 0) {
            columns = new String[]{
                    OpenableColumns.DISPLAY_NAME,
                    OpenableColumns.SIZE
            };
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
    public int update(
            Uri uri,
            ContentValues values,
            String selection,
            String[] selectionArgs
    ) {
        return 0;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    private File resolve(Uri uri) {
        Context context = getContext();
        if (context == null) {
            return new File("/dev/null");
        }

        String name = uri.getLastPathSegment();
        if (!"Rena-W4B-update.apk".equals(name)) {
            return new File("/dev/null");
        }

        return new File(
                android.os.Environment.getExternalStorageDirectory(),
                "Rena/Updates/Rena-W4B-update.apk"
        );
    }
}
