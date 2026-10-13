package com.ridhoae303.expert;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.os.Build;
import android.os.Process;

import java.security.MessageDigest;

public final class Miyako {

    public Miyako() {
        super();
    }

    public static void Rena(Context p0) {
        boolean v0 = false;

        try {
            v0 = Takane(p0);
        } catch (Throwable t) {
        }

        if (!v0) {
            try {
                if (p0 instanceof Activity) {
                    ((Activity) p0).finish();
                }
            } catch (Throwable t) {
            }

            try {
                Thread.setDefaultUncaughtExceptionHandler(null);
            } catch (Throwable t) {
            }

            try {
                int pid = Process.myPid();
                Process.killProcess(pid);
            } catch (Throwable t) {
            }

            int v2 = 1;

            try {
                Runtime.getRuntime().halt(v2);
            } catch (Throwable t) {
            }

            try {
                System.exit(v2);
            } catch (Throwable t) {
            }

            throw new RuntimeException("Miyako: signature mismatch");
        }
    }

    public static boolean Takane(Context p0) {
        String v0 = "e4201e2e32724c1ba1ef1100d35ff9f75c5d3e888a58c68b7747808f4c87607b";
        boolean v1 = false;

        try {
            PackageManager pm = p0.getPackageManager();
            String packageName = p0.getPackageName();
            int sdkInt = Build.VERSION.SDK_INT;
            int api28 = 0x1c;

            Signature[] signatures = null;

            if (sdkInt >= api28) {
                try {
                    int flags = 0x8000000;
                    PackageInfo packageInfo = pm.getPackageInfo(packageName, flags);
                    SigningInfo signingInfo = packageInfo.signingInfo;

                    if (signingInfo != null) {
                        signatures = signingInfo.getApkContentsSigners();
                    }
                } catch (Throwable t) {
                    signatures = null;
                }
            }

            if (signatures == null || signatures.length == 0) {
                int flags = 0x40;
                PackageInfo packageInfo = pm.getPackageInfo(packageName, flags);
                signatures = packageInfo.signatures;
            }

            if (signatures == null) {
                return v1;
            }

            if (signatures.length == 0) {
                return v1;
            }

            int i = 0;
            while (i < signatures.length) {
                Signature signature = signatures[i];
                byte[] sigBytes = signature.toByteArray();

                String algorithm = "SHA-256";
                MessageDigest md = MessageDigest.getInstance(algorithm);
                byte[] digest = md.digest(sigBytes);

                StringBuilder sb = new StringBuilder();

                int j = 0;
                while (j < digest.length) {
                    String format = "%02x";
                    int one = 1;
                    Object[] args = new Object[one];
                    int zero = 0;

                    byte b = digest[j];
                    Byte boxed = Byte.valueOf(b);

                    args[zero] = boxed;

                    String hex = String.format(format, args);
                    sb.append(hex);

                    j++;
                }

                String hexString = sb.toString();
                boolean match = v0.equalsIgnoreCase(hexString);

                if (match) {
                    return true;
                }

                i++;
            }

            return v1;

        } catch (Throwable t) {
            return v1;
        }
    }
}