package com.rena.w4b;

import android.content.Context;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class RenaCrypto {
    private static final String ALIAS = "rena_master_key_v1";
    private static final String PROVIDER = "AndroidKeyStore";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final byte[] MAGIC = new byte[]{'R', 'E', 'N', 'A', 1};
    private static SecretKey key;
    private static final SecureRandom RANDOM = new SecureRandom();

    private RenaCrypto() {
    }

    private static synchronized SecretKey key(Context context) throws Exception {
        if (key != null) {
            return key;
        }
        try {
            KeyStore store = KeyStore.getInstance(PROVIDER);
            store.load(null);
            if (!store.containsAlias(ALIAS)) {
                KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER);
                generator.init(new KeyGenParameterSpec.Builder(
                        ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build());
                generator.generateKey();
            }
            SecretKey candidate = (SecretKey) store.getKey(ALIAS, null);
            Cipher probe = Cipher.getInstance(TRANSFORMATION);
            probe.init(Cipher.ENCRYPT_MODE, candidate);
            probe.doFinal(new byte[]{1});
            key = candidate;
            return key;
        } catch (Throwable keystoreError) {
            File seed = new File(context.getFilesDir(), ".rena_seed");
            byte[] material;
            if (seed.isFile() && seed.length() >= 32) {
                material = readAll(seed);
            } else {
                material = new byte[48];
                RANDOM.nextBytes(material);
                FileOutputStream output = new FileOutputStream(seed);
                try {
                    output.write(material);
                    output.flush();
                } finally {
                    output.close();
                }
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(material);
            digest.update(context.getPackageName().getBytes("UTF-8"));
            key = new SecretKeySpec(digest.digest(), "AES");
            return key;
        }
    }

    public static byte[] encrypt(Context context, byte[] plain) {
        if (plain == null) {
            return null;
        }
        try {
            SecretKey secret = key(context);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            if (isKeystoreKey(secret)) {
                cipher.init(Cipher.ENCRYPT_MODE, secret);
            } else {
                byte[] iv = new byte[IV_LENGTH];
                RANDOM.nextBytes(iv);
                cipher.init(Cipher.ENCRYPT_MODE, secret, new GCMParameterSpec(TAG_BITS, iv));
            }
            byte[] iv = cipher.getIV();
            byte[] encrypted = cipher.doFinal(plain);
            byte[] out = new byte[MAGIC.length + 1 + iv.length + encrypted.length];
            System.arraycopy(MAGIC, 0, out, 0, MAGIC.length);
            out[MAGIC.length] = (byte) iv.length;
            System.arraycopy(iv, 0, out, MAGIC.length + 1, iv.length);
            System.arraycopy(encrypted, 0, out, MAGIC.length + 1 + iv.length, encrypted.length);
            return out;
        } catch (Throwable error) {
            return null;
        }
    }

    public static byte[] decrypt(Context context, byte[] blob) {
        if (blob == null || blob.length <= MAGIC.length + 1 + IV_LENGTH) {
            return null;
        }
        try {
            for (int i = 0; i < MAGIC.length; i++) {
                if (blob[i] != MAGIC[i]) {
                    return null;
                }
            }
            int ivLength = blob[MAGIC.length] & 0xff;
            byte[] iv = new byte[ivLength];
            System.arraycopy(blob, MAGIC.length + 1, iv, 0, ivLength);
            int offset = MAGIC.length + 1 + ivLength;
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key(context), new GCMParameterSpec(TAG_BITS, iv));
            return cipher.doFinal(blob, offset, blob.length - offset);
        } catch (Throwable error) {
            return null;
        }
    }

    public static String encryptString(Context context, String plain) {
        if (plain == null) {
            return null;
        }
        try {
            byte[] encrypted = encrypt(context, plain.getBytes("UTF-8"));
            return encrypted == null ? null : Base64.encodeToString(encrypted, Base64.NO_WRAP);
        } catch (Throwable error) {
            return null;
        }
    }

    public static String decryptString(Context context, String encoded) {
        if (encoded == null || encoded.length() == 0) {
            return null;
        }
        try {
            byte[] plain = decrypt(context, Base64.decode(encoded, Base64.NO_WRAP));
            return plain == null ? null : new String(plain, "UTF-8");
        } catch (Throwable error) {
            return null;
        }
    }

    public static boolean writeFile(Context context, File file, byte[] plain) {
        byte[] encrypted = encrypt(context, plain);
        if (encrypted == null || file == null) {
            return false;
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        File temp = new File(file.getPath() + ".tmp");
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(temp, false);
            output.write(encrypted);
            output.flush();
            output.getFD().sync();
            output.close();
            output = null;
            if (file.exists() && !file.delete()) {
                return false;
            }
            return temp.renameTo(file);
        } catch (Throwable error) {
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

    public static byte[] readFile(Context context, File file) {
        if (file == null || !file.isFile()) {
            return null;
        }
        try {
            return decrypt(context, readAll(file));
        } catch (Throwable error) {
            return null;
        }
    }

    public static boolean writeText(Context context, File file, String text) {
        try {
            return writeFile(context, file, text.getBytes("UTF-8"));
        } catch (Throwable error) {
            return false;
        }
    }

    public static String readText(Context context, File file) {
        byte[] data = readFile(context, file);
        if (data == null) {
            return null;
        }
        try {
            return new String(data, "UTF-8");
        } catch (Throwable error) {
            return null;
        }
    }

    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes("UTF-8"));
            StringBuilder builder = new StringBuilder();
            for (byte b : bytes) {
                builder.append(String.format(java.util.Locale.US, "%02x", b & 0xff));
            }
            return builder.toString();
        } catch (Throwable error) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private static boolean isKeystoreKey(SecretKey secret) {
        return secret != null && !(secret instanceof SecretKeySpec);
    }

    private static byte[] readAll(File file) throws Exception {
        FileInputStream input = new FileInputStream(file);
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream((int) Math.max(32, file.length()));
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        } finally {
            input.close();
        }
    }
}
