# Rena keeps Java entry points and JNI bridge classes intact.
-dontoptimize
-dontobfuscate

-keep class com.rena.w4b.** { *; }
-keep class com.ridhoae303.expert.** { *; }

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

# JNI entry points must retain their names/signatures.
-keepclassmembers class com.rena.w4b.NativeConfig {
    public static native <methods>;
}

-keepclassmembers class com.ridhoae303.expert.Takane {
    public static native <methods>;
}

# AIDE's legacy ProGuard input can temporarily omit incremental program
# classes even though the sources are present. Suppress unresolved warning
# noise for this application's own packages instead of treating it as a
# release-build failure. The classes themselves remain kept above.
-dontwarn com.rena.w4b.**
-dontwarn com.ridhoae303.expert.**

# Optional/legacy dependencies referenced by bundled AndroidX/old support code.
-dontwarn com.google.vending.licensing.**
-dontwarn com.android.vending.licensing.**
-dontwarn android.support.annotation.**
-dontwarn androidx.annotation.**
-dontwarn androidx.core.**
-dontwarn android.support.v4.**
-dontwarn androidx.versionedparcelable.**
