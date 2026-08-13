# Gson 反序列化需要保留 model 类
-keep class com.example.meetingroomapp.data.model.** { *; }

# 保留 API 回调接口
-keep interface com.example.meetingroomapp.data.model.ApiCallback { *; }

# 保留 Android 组件
-keep class com.example.meetingroomapp.PlatformSelectActivity { *; }
-keep class com.example.meetingroomapp.MainActivity { *; }
-keep class com.example.meetingroomapp.ConfigActivity { *; }
-keep class com.example.meetingroomapp.receiver.** { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class sun.misc.Unsafe { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# 保留泛型签名（Gson 解析需要）
-keepattributes EnclosingMethod, InnerClasses
