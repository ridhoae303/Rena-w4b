package com.rena.w4b;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public final class ContributorCache {
    private static final String CACHE_DIR = "contributor_cache";

    private ContributorCache() {
    }

    public static File avatarFile(Context context, String username) {
        String safe = username == null ? "unknown" : username;
        safe = safe.replaceAll("[^A-Za-z0-9._-]", "_");
        return new File(cacheDir(context), "avatar_" + safe + ".png");
    }

    private static File cacheDir(Context context) {
        Context app = context.getApplicationContext();
        File dir = new File(app.getFilesDir(), CACHE_DIR);

        if (!dir.exists()) {
            try {
                dir.mkdirs();
            } catch (Throwable ignored) {
            }
        }

        return dir;
    }

    public static boolean saveAvatar(
            Context context,
            String username,
            byte[] data
    ) {
        if (context == null || username == null || data == null || data.length == 0) {
            return false;
        }

        File target = avatarFile(context, username);
        File parent = target.getParentFile();

        if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.exists()) {
            return false;
        }

        File temp = new File(target.getParentFile(), target.getName() + ".tmp");

        FileOutputStream output = null;
        try {
            output = new FileOutputStream(temp);
            output.write(data);
            output.flush();
            output.close();
            output = null;

            if (!temp.renameTo(target)) {
                FileInputStream input = new FileInputStream(temp);
                output = new FileOutputStream(target);
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                }
                output.flush();
                input.close();
                output.close();
                output = null;
                temp.delete();
            }

            return true;
        } catch (Throwable ignored) {
            return false;
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (Throwable ignored) {
                }
            }
            if (temp.exists()) {
                temp.delete();
            }
        }
    }
}
