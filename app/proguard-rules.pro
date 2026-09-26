# -dontoptimize
# -dontobfuscate

# -keep class com.rena.w4b.** { *; }
# -keep class com.ridhoae303.expert.** { *; }

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

-keepclassmembers class com.ridhoae303.expert.Takane {
    public static native <methods>;
}

-dontwarn com.rena.w4b.**
-dontwarn com.ridhoae303.expert.**

-dontwarn com.google.vending.licensing.**
-dontwarn com.android.vending.licensing.**
-dontwarn android.support.annotation.**
-dontwarn androidx.annotation.**
-dontwarn androidx.core.**
-dontwarn android.support.v4.**
-dontwarn androidx.versionedparcelable.**

-keep interface androidx.webkit.ProfileStore { *; }
