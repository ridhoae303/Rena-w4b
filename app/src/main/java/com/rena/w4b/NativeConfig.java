package com.rena.w4b;

public final class NativeConfig {
    static {
        System.loadLibrary("rena");
    }

    private NativeConfig() {
    }

    public static native String webUrl();
    public static native String desktopUserAgent();
    public static native String developerName();
    public static native String githubUrl();
    public static native String telegramUrl();
    public static native String communityUrl();
    public static native String donateText();
    public static native String donateUrl();

    public static native String expectedSignatureSha256();

    public static native String menuDeveloperTitle();
    public static native String menuGithub();
    public static native String menuTelegram();
    public static native String menuCommunity();

    public static native String menuContentDescription();
    public static native String loadingText();
    public static native String zoomLabel();
    public static native String refreshText();
    public static native String refreshContentDescription();
    public static native String footerText();
    public static native String appFallbackName();
    public static native String renaImageAssetName();

    public static native String permissionTitle(String permission);
    public static native String permissionMessage(String permission);
    public static native String permissionAllowButton();
    public static native String permissionDenyButton();
    public static native String permissionDeniedTitle();
    public static native String permissionDeniedMessage(String permission);
    public static native String permissionCloseButton();
    public static native String permissionSettingsButton();

    public static native String integrityErrorMessage();
    public static native String backPressToast();
}
