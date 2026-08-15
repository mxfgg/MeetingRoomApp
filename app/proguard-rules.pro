# Gson 反序列化需要保留 model 类
-keep class com.example.meetingroomapp.data.model.** { *; }

# 保留 API 回调接口
-keep interface com.example.meetingroomapp.data.model.ApiCallback { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# 保留泛型签名（Gson 解析需要）
-keepattributes EnclosingMethod, InnerClasses
