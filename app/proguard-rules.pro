# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.chatai.app.data.remote.dto.** { *; }
-keep class com.chatai.app.data.remote.ImageGenerateRequest { *; }
-keep class com.chatai.app.data.remote.ImageTaskResponse { *; }
-keep class com.chatai.app.data.remote.ImageTaskData { *; }
-keep class com.chatai.app.data.remote.ImageTaskInfo { *; }
-keep class com.chatai.app.ui.screens.chat.ParsedContent { *; }
-keep class com.chatai.app.ui.screens.chat.ImageTag { *; }

# Markwon
-keep class io.noties.markwon.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Coil
-keep class coil.** { *; }
