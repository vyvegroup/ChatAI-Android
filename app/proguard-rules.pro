# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.chatai.app.data.remote.dto.** { *; }

# Markwon
-keep class io.noties.markwon.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Coil
-keep class coil.** { *; }
