package com.rena.w4b;

public final class NativeConfig {
    private static boolean nativeAvailable;

    static {
        try {
            System.loadLibrary("rena");
            nativeAvailable = true;
        } catch (Throwable ignored) {
            nativeAvailable = false;
        }
    }

    public static boolean isNativeAvailable() {
        return nativeAvailable;
    }

    private NativeConfig() {
    }

    public static native String webUrl();
    public static native String desktopUserAgent();
    public static native String developerName();
    public static native String githubUrl();
    public static native String telegramUrl();
    public static native String communityUrl();
    public static native String donateUrl();
    public static native String developerAvatarUrl();
    public static native String githubRequestFailedText();

    public static native String appLockBackSymbol();
    public static native String changePinArrow();
    public static native String contributorsApiUrl();
    public static native String githubAvatarUrl(String username);
    public static native String githubProfileUrl(String username);

    public static native String officialTeamName1();
    public static native String officialTeamUsername1();
    public static native String officialTeamName2();
    public static native String officialTeamUsername2();
    public static native String officialTeamName3();
    public static native String officialTeamUsername3();

    public static native String authoritativeVersionName();
    public static native int authoritativeVersionCode();

    public static native boolean verifyIntegrity(android.content.Context context);
    public static native boolean verifyRuntimeBinding(android.content.Context context);
    public static native boolean verifyApkSigner(android.content.Context context, String apkPath);

    public static native String menuGithub();
    public static native String menuTelegram();

    public static native String appFallbackName();
    public static native String renaImageAssetName();

    public static native String unsupportedAndroidTitle();
    public static native String unsupportedAndroidMessage();

    public static native String integrityErrorMessage();
    public static native String integrityFailedTitle();
    public static native String integrityCloseText();

    public static native String githubRepositoryUrl();
    public static native String updaterApiUrl();
    public static native String applicationIntegrityTitle();
    public static native String closeTabSymbol();

    public static native String downloadBackSymbol();
    public static native String downloadProgressSeparator();
    public static native String downloadBytesSuffix();
    public static native String downloadKilobytesSuffix();
    public static native String downloadMegabytesSuffix();
    public static native String downloadGigabytesSuffix();
    public static native String downloadStorageRootName();
}
