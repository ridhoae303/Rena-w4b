package com.rena.w4b;

import android.content.Context;
import android.content.SharedPreferences;

import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * App Lock credential store.
 *
 * The stored credential contains only the PIN verifier (PBKDF2 hash), random
 * salt, enabled state, timeout, and biometric preference. The PIN itself is
 * never stored. No Android Keystore or AES-GCM dependency is used here.
 * PIN policy is enforced by the activities only as exactly four decimal digits;
 * no weak/common PIN blacklist or strength check is applied.
 */
public final class SecureAppLockStore {
    private static final String ACTIVE_PREFS = "rena_app_lock_secure_v4";
    private static final String ACTIVE_VALUE = "credential";
    private static final String PAYLOAD_VERSION = "5";
    private static final int PBKDF2_ITERATIONS = 120000;
    private static final int HASH_BITS = 256;
    private static final int SALT_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int PAYLOAD_V4_PARTS = 6;
    private static final int PAYLOAD_V5_PARTS = 7;

    private SecureAppLockStore() {
    }

    public static synchronized State read(Context context) {
        if (context == null) {
            return new State();
        }
        try {
            SharedPreferences prefs = context.getSharedPreferences(
                    ACTIVE_PREFS,
                    Context.MODE_PRIVATE
            );
            String encoded = prefs.getString(ACTIVE_VALUE, null);
            if (encoded == null || encoded.length() == 0) {
                return new State();
            }
            State state = decode(encoded);
            return state == null ? new State() : state;
        } catch (Throwable ignored) {
            return new State();
        }
    }

    public static synchronized boolean write(Context context, State state) {
        if (context == null || state == null || !hasCredentials(state)) {
            return false;
        }

        final String payload;
        try {
            payload = encode(state);
        } catch (Throwable ignored) {
            return false;
        }

        try {
            SharedPreferences prefs = context.getSharedPreferences(
                    ACTIVE_PREFS,
                    Context.MODE_PRIVATE
            );
            return prefs.edit()
                    .putString(ACTIVE_VALUE, payload)
                    .commit();
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static synchronized void clear(Context context) {
        if (context == null) {
            return;
        }
        try {
            context.getSharedPreferences(
                    ACTIVE_PREFS,
                    Context.MODE_PRIVATE
            ).edit().remove(ACTIVE_VALUE).commit();
        } catch (Throwable ignored) {
        }
    }

    public static boolean hasCredentials(State state) {
        return state != null
                && state.salt != null
                && state.salt.length >= SALT_BYTES
                && state.pinHash != null
                && state.pinHash.length >= 16;
    }

    public static byte[] hashPin(String pin, byte[] salt) {
        if (pin == null || salt == null || salt.length < SALT_BYTES) {
            return null;
        }

        PBEKeySpec spec = null;
        try {
            SecretKeyFactory factory;
            try {
                factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            } catch (Throwable unavailable) {
                factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            }

            spec = new PBEKeySpec(
                    pin.toCharArray(),
                    salt,
                    PBKDF2_ITERATIONS,
                    HASH_BITS
            );
            return factory.generateSecret(spec).getEncoded();
        } catch (Throwable ignored) {
            return null;
        } finally {
            if (spec != null) {
                spec.clearPassword();
            }
        }
    }

    public static boolean verifyPin(String pin, State state) {
        if (pin == null || state == null || !hasCredentials(state)) {
            return false;
        }
        byte[] candidate = hashPin(pin, state.salt);
        return candidate != null && MessageDigest.isEqual(candidate, state.pinHash);
    }

    public static byte[] randomBytes(int count) {
        if (count <= 0) {
            return new byte[0];
        }
        byte[] value = new byte[count];
        RANDOM.nextBytes(value);
        return value;
    }

    private static String encode(State state) {
        return PAYLOAD_VERSION + "|"
                + state.enabled + "|"
                + state.biometricEnabled + "|"
                + state.timeoutSeconds + "|"
                + Math.max(0L, state.lastBackgroundAtMillis) + "|"
                + encodeBytes(state.salt) + "|"
                + encodeBytes(state.pinHash);
    }

    private static State decode(String payload) {
        if (payload == null || payload.length() == 0) {
            return null;
        }

        String[] parts = payload.split("\\|", -1);
        if (parts.length != PAYLOAD_V5_PARTS && parts.length != PAYLOAD_V4_PARTS) {
            return null;
        }

        boolean versionFive = PAYLOAD_VERSION.equals(parts[0]);
        boolean versionFour = "4".equals(parts[0]);
        if (!versionFive && !versionFour) {
            return null;
        }

        State state = new State();
        state.enabled = Boolean.parseBoolean(parts[1]);
        state.biometricEnabled = Boolean.parseBoolean(parts[2]);

        try {
            state.timeoutSeconds = Integer.parseInt(parts[3]);
        } catch (Throwable ignored) {
            return null;
        }

        if (versionFive) {
            try {
                state.lastBackgroundAtMillis = Long.parseLong(parts[4]);
            } catch (Throwable ignored) {
                return null;
            }
            state.salt = decodeBytes(parts[5]);
            state.pinHash = decodeBytes(parts[6]);
        } else {
            state.lastBackgroundAtMillis = 0L;
            state.salt = decodeBytes(parts[4]);
            state.pinHash = decodeBytes(parts[5]);
        }

        return hasCredentials(state) ? state : null;
    }

    private static String encodeBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "-";
        }
        return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP);
    }

    private static byte[] decodeBytes(String value) {
        if (value == null || "-".equals(value) || value.length() == 0) {
            return null;
        }
        try {
            return android.util.Base64.decode(value, android.util.Base64.NO_WRAP);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static final class State {
        public boolean enabled;
        public boolean biometricEnabled;
        public int timeoutSeconds;
        public long lastBackgroundAtMillis;
        public byte[] salt;
        public byte[] pinHash;

        public State copy() {
            State out = new State();
            out.enabled = enabled;
            out.biometricEnabled = biometricEnabled;
            out.timeoutSeconds = timeoutSeconds;
            out.lastBackgroundAtMillis = lastBackgroundAtMillis;
            out.salt = salt == null ? null : salt.clone();
            out.pinHash = pinHash == null ? null : pinHash.clone();
            return out;
        }
    }
}
