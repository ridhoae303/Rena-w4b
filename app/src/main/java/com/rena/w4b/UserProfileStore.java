package com.rena.w4b;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

public final class UserProfileStore {
    private static final String FILE = "profile.dat";
    private static final String AVATAR = "profile_avatar.dat";

    private UserProfileStore() {
    }

    private static JSONObject read(Context context) {
        String raw = RenaCrypto.readText(context, new File(context.getFilesDir(), FILE));
        if (raw == null) {
            return new JSONObject();
        }
        try {
            return new JSONObject(raw);
        } catch (Throwable ignored) {
            return new JSONObject();
        }
    }

    private static void write(Context context, JSONObject object) {
        RenaCrypto.writeText(context, new File(context.getFilesDir(), FILE), object.toString());
    }

    public static boolean isSignedIn(Context context) {
        return read(context).optBoolean("signed", false);
    }

    public static String email(Context context) {
        return read(context).optString("email", "");
    }

    public static String provider(Context context) {
        return read(context).optString("provider", "");
    }

    public static String username(Context context) {
        return read(context).optString("username", "");
    }

    public static String displayName(Context context) {
        return read(context).optString("name", "");
    }

    public static String bio(Context context) {
        return read(context).optString("bio", "");
    }

    public static void signIn(Context context, String email, String provider) {
        JSONObject object = read(context);
        try {
            object.put("signed", true);
            object.put("email", email);
            object.put("provider", provider);
            if (TextUtils.isEmpty(object.optString("username", ""))) {
                int at = email.indexOf('@');
                object.put("username", at > 0 ? email.substring(0, at) : email);
            }
            if (TextUtils.isEmpty(object.optString("name", ""))) {
                int at = email.indexOf('@');
                object.put("name", at > 0 ? email.substring(0, at) : email);
            }
            object.put("signedAt", System.currentTimeMillis());
        } catch (Throwable ignored) {
        }
        write(context, object);
    }

    public static void saveFields(Context context, String username, String name, String bio) {
        JSONObject object = read(context);
        try {
            object.put("username", username);
            object.put("name", name);
            object.put("bio", bio);
        } catch (Throwable ignored) {
        }
        write(context, object);
    }

    public static void signOut(Context context) {
        new File(context.getFilesDir(), FILE).delete();
        new File(context.getFilesDir(), AVATAR).delete();
    }

    public static Bitmap avatar(Context context) {
        byte[] data = RenaCrypto.readFile(context, new File(context.getFilesDir(), AVATAR));
        if (data == null) {
            return null;
        }
        try {
            return BitmapFactory.decodeByteArray(data, 0, data.length);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean saveAvatar(Context context, Uri uri) {
        InputStream input = null;
        try {
            input = context.getContentResolver().openInputStream(uri);
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, bounds);
            input.close();
            int sample = 1;
            while (bounds.outWidth / (sample * 2) >= 768 && bounds.outHeight / (sample * 2) >= 768) {
                sample *= 2;
            }
            input = context.getContentResolver().openInputStream(uri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sample;
            Bitmap decoded = BitmapFactory.decodeStream(input, null, options);
            if (decoded == null) {
                return false;
            }
            int side = Math.min(decoded.getWidth(), decoded.getHeight());
            int x = (decoded.getWidth() - side) / 2;
            int y = (decoded.getHeight() - side) / 2;
            Bitmap square = Bitmap.createBitmap(decoded, x, y, side, side);
            Bitmap scaled = Bitmap.createScaledBitmap(square, Math.min(512, side), Math.min(512, side), true);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            scaled.compress(Bitmap.CompressFormat.JPEG, 88, out);
            return RenaCrypto.writeFile(context, new File(context.getFilesDir(), AVATAR), out.toByteArray());
        } catch (Throwable ignored) {
            return false;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    public static void removeAvatar(Context context) {
        new File(context.getFilesDir(), AVATAR).delete();
    }

    public static String summary(Context context) {
        if (!isSignedIn(context)) {
            return context.getString(R.string.profile_not_signed_in);
        }
        String name = displayName(context);
        return TextUtils.isEmpty(name) ? email(context) : name + " \u00B7 " + email(context);
    }
}
