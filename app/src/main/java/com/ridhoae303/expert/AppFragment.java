package com.ridhoae303.expert;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.os.Build;
import android.os.Process;

import java.security.MessageDigest;
import java.util.Locale;

public final class AppFragment {
    private static volatile boolean zzf;

    public AppFragment() {
        super();
    }

    public static synchronized void onCreate(Context context) {
        if (!zzf) {
            zzf = true;

            Context applicationContext = context;
            if (context != null) {
                Context candidate = context.getApplicationContext();
                if (candidate != null) {
                    applicationContext = candidate;
                }
            }

            try {
                CrashLog.Rena(applicationContext, false);
            } catch (Throwable ignored) {
            }

            boolean valid = false;
            try {
                valid = zza(applicationContext);
            } catch (Throwable ignored) {
            }

            if (!valid) {
                try {
                    if (context instanceof Activity) {
                        ((Activity) context).finish();
                    }
                } catch (Throwable ignored) {
                }

                try {
                    Thread.setDefaultUncaughtExceptionHandler(null);
                } catch (Throwable ignored) {
                }

                try {
                    int pid = Process.myPid();
                    Process.killProcess(pid);
                } catch (Throwable ignored) {
                }

                int exitCode = 1;
                try {
                    Runtime.getRuntime().halt(exitCode);
                } catch (Throwable ignored) {
                }

                try {
                    System.exit(exitCode);
                } catch (Throwable ignored) {
                }

                throw new RuntimeException("Takane: Violation detected");
            }

            RuntimeGuard guard = new RuntimeGuard(applicationContext);
            Thread guardThread = new Thread(guard, "Takane-RuntimeGuard");
            guardThread.setDaemon(true);
            guardThread.start();
        }
    }

    public static void setTelemetryConsent(Context context, boolean consent) {
        if (context != null) {
            SharedPreferences preferences = context.getSharedPreferences(
                    "ridhoae303_diagnostics",
                    Context.MODE_PRIVATE
            );
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("telemetry_consent", consent).apply();
        }
    }

    public static boolean zza(Context context) {
        String expectedHash = "e4201e2e32724c1ba1ef1100d35ff9f75c5d3e888a58c68b7747808f4c87607b";
        boolean invalid = false;

        try {
            PackageManager packageManager = context.getPackageManager();
            if (packageManager != null) {
                String packageName = context.getPackageName();
                if (packageName != null) {
                    ApplicationInfo applicationInfo = context.getApplicationInfo();
                    if (applicationInfo != null && applicationInfo.sourceDir != null) {
                        int flags;
                        if (Build.VERSION.SDK_INT >= 28) {
                            flags = 0x08000000;
                        } else {
                            flags = 0x40;
                        }

                        PackageInfo archiveInfo = packageManager.getPackageArchiveInfo(
                                applicationInfo.sourceDir,
                                flags
                        );

                        if (archiveInfo != null) {
                            int archiveResult = zzh(archiveInfo, expectedHash);
                            if (archiveResult != -1) {
                                if (archiveResult == 1) {
                                    return true;
                                }
                                zzc(context, "SIGNATURE_MISMATCH");
                                return invalid;
                            }
                        }

                        int packageFlags;
                        if (Build.VERSION.SDK_INT >= 28) {
                            packageFlags = 0x08000000;
                        } else {
                            packageFlags = 0x40;
                        }

                        PackageInfo packageInfo = packageManager.getPackageInfo(
                                packageName,
                                packageFlags
                        );
                        if (packageInfo != null) {
                            int packageResult = zzh(packageInfo, expectedHash);
                            if (packageResult != -1) {
                                if (packageResult == 1) {
                                    return true;
                                }
                                zzc(context, "SIGNATURE_MISMATCH");
                                return invalid;
                            }
                        }
                    }
                }
            }

            zzc(context, "INTEGRITY_UNREADABLE");
            return invalid;
        } catch (Throwable ignored) {
            zzc(context, "INTEGRITY_UNREADABLE");
            return invalid;
        }
    }

    private static void zzc(Context context, String reason) {
        Thread reporterThread;
        try {
            Reporter reporter = new Reporter(context, reason);
            reporterThread = new Thread(reporter, "Takane-Report");
            reporterThread.setDaemon(true);
            reporterThread.start();
        } catch (Throwable ignored) {
            return;
        }

        while (true) {
            try {
                reporterThread.join();
                return;
            } catch (InterruptedException ignored) {
            }
        }
    }

    private static int zzd(Signature[] signatures, String expectedHash, boolean requireAll) {
        if (signatures == null || expectedHash == null) {
            return -1;
        }

        try {
            int count = signatures.length;
            if (count <= 0) {
                return -1;
            }

            boolean matched = false;
            for (int index = 0; index < count; index++) {
                Signature signature = signatures[index];
                if (signature == null) {
                    return -1;
                }

                String signatureHash = zze(signature);
                if (signatureHash == null) {
                    return -1;
                }

                boolean equals = expectedHash.equalsIgnoreCase(signatureHash);
                if (equals) {
                    matched = true;
                    if (!requireAll) {
                        return 1;
                    }
                } else if (requireAll) {
                    return 0;
                }
            }

            return matched ? 1 : 0;
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private static String zze(Signature signature) {
        if (signature != null) {
            try {
                byte[] signatureBytes = signature.toByteArray();
                if (signatureBytes != null) {
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    byte[] hashBytes = digest.digest(signatureBytes);
                    StringBuilder builder = new StringBuilder();
                    String hexDigits = "0123456789abcdef";

                    for (int index = 0; index < hashBytes.length; index++) {
                        int value = hashBytes[index] & 0xff;
                        int high = (value >>> 4) & 0x0f;
                        int low = value & 0x0f;
                        builder.append(hexDigits.charAt(high));
                        builder.append(hexDigits.charAt(low));
                    }

                    return builder.toString();
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    public static void zzg() {
        try {
            Thread.setDefaultUncaughtExceptionHandler(null);
        } catch (Throwable ignored) {
        }

        try {
            int pid = Process.myPid();
            Process.killProcess(pid);
        } catch (Throwable ignored) {
        }

        int exitCode = 1;
        try {
            Runtime.getRuntime().halt(exitCode);
        } catch (Throwable ignored) {
        }

        try {
            System.exit(exitCode);
        } catch (Throwable ignored) {
        }

        throw new RuntimeException("Takane: Violation detected");
    }

    private static int zzh(PackageInfo packageInfo, String expectedHash) {
        if (packageInfo == null || expectedHash == null) {
            return -1;
        }

        try {
            if (Build.VERSION.SDK_INT >= 28) {
                SigningInfo signingInfo = packageInfo.signingInfo;
                if (signingInfo != null) {
                    Signature[] currentSigners = signingInfo.getApkContentsSigners();
                    int signingResult = zzd(currentSigners, expectedHash, true);
                    if (signingResult != -1) {
                        return signingResult;
                    }
                }
            }

            Signature[] legacySignatures = packageInfo.signatures;
            if (legacySignatures != null && legacySignatures.length > 0) {
                boolean multipleSigners = legacySignatures.length > 1;
                return zzd(legacySignatures, expectedHash, multipleSigners);
            }
        } catch (Throwable ignored) {
        }

        return -1;
    }

    static final class Reporter implements Runnable {
        private final Context context;
        private final String details;
        private final String reason;
        private final String status;

        public Reporter(Context context, String reason) {
            this(context, reason, "KILL", "");
        }

        public Reporter(Context context, String reason, String status, String details) {
            this.context = context;
            this.reason = reason;
            this.status = status;
            this.details = details;
        }

        @Override
        public void run() {
            java.net.HttpURLConnection connection = null;
            try {
                Context currentContext = context;
                if (currentContext == null) {
                    return;
                }

                SharedPreferences preferences = currentContext.getSharedPreferences(
                        "ridhoae303_diagnostics",
                        Context.MODE_PRIVATE
                );
                boolean consent = preferences.getBoolean("telemetry_consent", false);
                if (!consent) {
                    return;
                }

                java.net.URL url = new java.net.URL(
                        "https://warden-sooty.vercel.app/api/verify"
                );
                javax.net.ssl.HttpsURLConnection httpsConnection =
                        (javax.net.ssl.HttpsURLConnection) url.openConnection();
                connection = httpsConnection;
                java.net.HttpURLConnection httpConnection = httpsConnection;
                httpConnection.setConnectTimeout(3500);
                httpConnection.setReadTimeout(3500);
                httpConnection.setDoOutput(true);
                httpConnection.setUseCaches(false);
                httpConnection.setRequestMethod("POST");
                httpConnection.setRequestProperty(
                        "Content-Type",
                        "application/json; charset=UTF-8"
                );
                httpConnection.setRequestProperty("Accept", "application/json");

                org.json.JSONObject payload = new org.json.JSONObject();
                String packageName = currentContext.getPackageName();
                payload.put("packageName", packageName);

                PackageManager packageManager = currentContext.getPackageManager();
                ApplicationInfo applicationInfo = packageManager.getApplicationInfo(
                        packageName,
                        0
                );
                CharSequence label = packageManager.getApplicationLabel(applicationInfo);
                String appName = label.toString();
                payload.put("appName", appName);

                PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
                String appVersion = packageInfo.versionName;
                payload.put("appVersion", appVersion);
                int appVersionCode = packageInfo.versionCode;
                payload.put("appVersionCode", appVersionCode);

                java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                        Locale.US
                );
                java.util.Date eventDate = new java.util.Date();
                String eventTime = formatter.format(eventDate);
                payload.put("eventTime", eventTime);

                String manufacturer = Build.MANUFACTURER;
                payload.put("deviceManufacturer", manufacturer);
                String model = Build.MODEL;
                payload.put("deviceModel", model);
                String deviceName = Build.DEVICE;
                payload.put("deviceName", deviceName);
                String androidVersion = Build.VERSION.RELEASE;
                payload.put("androidVersion", androidVersion);
                int androidSdk = Build.VERSION.SDK_INT;
                payload.put("androidSdk", androidSdk);
                payload.put("reason", reason);
                payload.put("status", status);

                String currentDetails = details;
                if (currentDetails != null && currentDetails.length() > 0) {
                    payload.put("details", currentDetails);
                }

                byte[] body = payload.toString().getBytes("UTF-8");
                java.io.OutputStream outputStream = httpConnection.getOutputStream();
                outputStream.write(body);
                outputStream.flush();
                outputStream.close();
                httpConnection.getResponseCode();
                httpConnection.disconnect();
                connection = null;
            } catch (Throwable ignored) {
            }

            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    static final class RuntimeGuard implements Runnable {
        private final Context context;
        private final java.security.SecureRandom random;

        public RuntimeGuard(Context context) {
            this.context = context;
            this.random = new java.security.SecureRandom();
        }

        @Override
        public void run() {
            while (true) {
                try {
                    int delay = random.nextInt(0x493e1) + 0x1d4c0;
                    Thread.sleep((long) delay);
                } catch (InterruptedException ignored) {
                }

                Context currentContext = context;
                boolean valid = AppFragment.zza(currentContext);
                if (!valid) {
                    AppFragment.zzg();
                    return;
                }
            }
        }
    }
}
