-dontoptimize
-dontobfuscate

-keep class com.rena.w4b.** { *; }
-keepnames class com.rena.w4b.** { *; }

-keepattributes InnerClasses
-keepattributes EnclosingMethod

-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations
-keepattributes Signature
-keepattributes Exceptions

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

-keepclassmembers class com.rena.w4b.NativeConfig {
    public static native <methods>;
}

-dontwarn com.google.vending.licensing.**
-dontwarn com.android.vending.licensing.**
-dontwarn android.support.annotation.Keep

-dontwarn androidx.annotation.**

-dontwarn androidx.core.**
-dontwarn android.support.v4.**

-keep class com.rena.w4b.RenaApplication { *; }

-keep class com.rena.w4b.AppLockActivity { *; }
-keep class com.rena.w4b.PinSetupActivity { *; }
-keep class com.rena.w4b.ChangePinActivity { *; }
-keep class com.rena.w4b.LockScreenActivity { *; }
-keep class com.rena.w4b.AppLockManager { *; }
-keep class com.rena.w4b.SecureAppLockStore { *; }
