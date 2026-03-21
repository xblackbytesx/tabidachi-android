-keep class com.example.tabidachi.network.** { *; }
-keepattributes *Annotation*
-dontwarn io.ktor.**
-dontwarn coil3.**
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}

# Tink / security-crypto missing annotations
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn javax.annotation.concurrent.**
