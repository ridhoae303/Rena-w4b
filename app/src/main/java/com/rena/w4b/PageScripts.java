package com.rena.w4b;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;

public final class PageScripts {
    private static final HashMap<String, String> CACHE = new HashMap<String, String>();

    private PageScripts() {
    }

    public static synchronized String load(Context context, String name) {
        String cached = CACHE.get(name);
        if (cached != null) {
            return cached;
        }
        InputStream input = null;
        try {
            input = context.getAssets().open("js/" + name + ".js");
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            String text = output.toString("UTF-8");
            CACHE.put(name, text);
            return text;
        } catch (Throwable error) {
            return "";
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    public static String quote(String value) {
        return org.json.JSONObject.quote(value == null ? "" : value);
    }
}
