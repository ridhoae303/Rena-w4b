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
    public static native String donateText();
    public static native String donateUrl();
    public static native String developerAvatarUrl();
    public static native String githubRequestFailedText();

    public static native String easterEggFoundText();
    public static native String teamDeveloperText();
    public static native String officialTeamText();
    public static native String contributorText();
    public static native String teamLoadingText();
    public static native String teamRequestFailedText();
    public static native String teamNoContributorsText();
    public static native String teamGithubContentDescription();
    public static native String teamBackContentDescription();
    public static native String appLockText();
    public static native String appLockTitle();
    public static native String appLockBackText();
    public static native String appLockBackSymbol();
    public static native String appLockFingerprintUnavailable();
    public static native String appLockHeroTitle();
    public static native String appLockHeroMessage();
    public static native String appLockSwitchText();
    public static native String appLockTimeoutLabel();
    public static native String appLockTimeoutText();
    public static native String appLockFingerprintText();
    public static native String appLockEnableFirstText();
    public static native String appLockSecurityNote();
    public static native String changePinLabel();
    public static native String changePinText();
    public static native String changePinArrow();
    public static native String currentPinTitle();
    public static native String currentPinHint();
    public static native String newPinTitle();
    public static native String newPinMessage();
    public static native String pinChangedText();
    public static native String currentPinInvalidText();
    public static native String appLockTimeoutDialogTitle();
    public static native String timeoutImmediate();
    public static native String timeoutOneMinute();
    public static native String timeoutFiveMinutes();
    public static native String timeoutFifteenMinutes();
    public static native String timeoutThirtyMinutes();
    public static native String timeoutOneHour();
    public static native String timeoutFiveHours();
    public static native String timeoutTenHours();
    public static native String createPinTitle();
    public static native String createPinMessage();
    public static native String pinHint();
    public static native String confirmPinHint();
    public static native String savePinText();
    public static native String pinLengthText();
    public static native String pinMismatchText();
    public static native String pinSaveFailedText();
    public static native String lockScreenTitle();
    public static native String lockScreenMessage();
    public static native String lockPinHint();
    public static native String unlockText();
    public static native String fingerprintUnlockText();
    public static native String lockScreenFooter();
    public static native String incorrectPinText();
    public static native String fingerprintFailedText();
    public static native String fingerprintCanceledText();
    public static native String fingerprintEnabledText();
    public static native String hideNotificationLabel();
    public static native String hideNotificationEnabledText();
    public static native String hideNotificationDisabledText();
    public static native String reduceAnimationLabel();
    public static native String reduceAnimationEnabledText();
    public static native String reduceAnimationDisabledText();
    public static native String notificationSilentChannelName();
    public static native String notificationSilentChannelDescription();
    public static native String contributorsApiUrl();
    public static native String githubAvatarUrl(String username);
    public static native String githubProfileUrl(String username);

    public static native String officialTeamName1();
    public static native String officialTeamUsername1();
    public static native String officialTeamRole1();
    public static native String officialTeamName2();
    public static native String officialTeamUsername2();
    public static native String officialTeamRole2();
    public static native String officialTeamName3();
    public static native String officialTeamUsername3();
    public static native String officialTeamRole3();

    public static native boolean verifyIntegrity(android.content.Context context);
    public static native boolean verifyApkSigner(android.content.Context context, String apkPath);

    public static native String menuDeveloperTitle();
    public static native String menuGithub();
    public static native String menuTelegram();
    public static native String menuCommunity();

    public static native String menuContentDescription();
    public static native String loadingText();
    public static native String zoomLabel();
    public static native String zoomEnabledText();
    public static native String zoomDisabledText();
    public static native String switchTogglesText();
    public static native String refreshText();
    public static native String refreshContentDescription();
    public static native String exitToastText();
    public static native String socialMediaText();
    public static native String checkingUpdatesText();
    public static native String hideMenuToastText();
    public static native String storageWarningText();
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
    public static native String integrityFailedTitle();
    public static native String integrityCloseText();

    public static native String hideMenuLabel();
    public static native String hideMenuMessage();
    public static native String tooManyTabsTitle();
    public static native String newTabText();
    public static native String closeAllTabsText();
    public static native String tabLimitText();
    public static native String tabProfilesUnavailableText();
    public static native String closeAllTabsMessage();
    public static native String noJokesText();
    public static native String cancelText();
    public static native String notNowText();
    public static native String ignoreText();
    public static native String clearText();
    public static native String dataStorageText();
    public static native String checkUpdatesText();
    public static native String updateAvailableTitle();
    public static native String upToDateText();
    public static native String updateInstallPermissionTitle();
    public static native String updateInstallPermissionMessage();
    public static native String downloadUpdateText();
    public static native String updateDigestMissingText();
    public static native String updateDownloadFailedText();
    public static native String supportDevelopersText();
    public static native String starRepositoryText();
    public static native String githubRepositoryUrl();
    public static native String updaterApiUrl();
    public static native String applicationIntegrityTitle();
    public static native String dataCookiesText();
    public static native String dataCacheText();
    public static native String dataAllWebText();
    public static native String dataClearedText();
    public static native String updateNotAvailableText();
    public static native String clearCookiesMessage();
    public static native String clearAllWebMessage();
    public static native String cacheClearedText();
    public static native String accountsTabsText();
    public static native String toolsText();
    public static native String tabPrefix();
    public static native String storageAccessTitle();
    public static native String storageAccessMessage();
    public static native String storageSettingsButton();
    public static native String downloadDeniedText();
    public static native String closeTabSymbol();
    public static native String okayText();
    public static native String backgroundAccessTitle();
    public static native String backgroundAccessMessage();
    public static native String batteryOptimizationTitle();
    public static native String batteryOptimizationMessage();
    public static native String storageAlreadyHandledText();
    public static native String noInternetTitle();
    public static native String noInternetMessage();
    public static native String noInternetConnectionText();
    public static native String poorInternetConnectionText();
    public static native String exitText();

    public static native String notificationOpenText();
    public static native String notificationReplyText();
    public static native String notificationReadText();
    public static native String notificationIgnoreText();
    public static native String notificationChannelName();
    public static native String notificationChannelDescription();
}
