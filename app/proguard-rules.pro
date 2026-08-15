# Rena W4B - AIDE / ProGuard 6.0.3 compatibility profile

-dontoptimize
-dontobfuscate

# Keep all application classes and all generated anonymous/inner classes.
-keep class com.rena.w4b.** { *; }
-keepnames class com.rena.w4b.** { *; }

# Preserve Java inner-class metadata.
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Preserve runtime annotations/signatures used by Android/framework code.
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations
-keepattributes Signature
-keepattributes Exceptions

# JNI bridge must never be removed/renamed.
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

-keepclassmembers class com.rena.w4b.NativeConfig {
    public static native <methods>;
}

# Android application entry point.
-keep class android.app.Application { <init>(...); }

# AIDE 6.0.3 sometimes injects/ships legacy configuration for classes that
# are not present in the local Android jar. These are configuration-only
# warnings, not app program classes.
-dontwarn com.google.vending.licensing.**
-dontwarn com.android.vending.licensing.**
-dontwarn android.support.annotation.Keep

# The uploaded log also contains unresolved synthetic accessor references
# inside com.rena.w4b.*. Suppress only those warnings so ProGuard 6.0.3 can
# finish processing the legacy compiler output without globally ignoring
# every warning.
#
# NOTE: this does NOT repair invalid bytecode. If the generated .class files
# are genuinely mismatched, the Java classes must still be rebuilt cleanly.
-dontwarn com.rena.w4b.**

# Do not use -ignorewarnings globally.
