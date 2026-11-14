-dontoptimize

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

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.rena.w4b.RenaDownloadHelper$Bridge { *; }
-keep class com.rena.w4b.TranslateBridge { *; }
-keep class com.rena.w4b.AntiDeleteStore$Bridge { *; }
-keep class com.rena.w4b.InspectorHub$Bridge { *; }
-keep class com.rena.w4b.StatusBridge { *; }
-keep class androidx.webkit.** { *; }
-dontwarn androidx.webkit.**
