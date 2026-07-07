# Compose / Kotlin reflection
-keep class kotlin.Metadata { *; }
-keepclasseswithmembers class * { @androidx.compose.runtime.Composable <methods>; }

# SQLDelight
-keep class app.cash.sqldelight.** { *; }
-keep class com.finanzen.db.** { *; }

# kotlinx-serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
# Serializers generados de nuestras @Serializable (EncryptedEnvelope, BackupSnapshot, etc.)
-keepclassmembers @kotlinx.serialization.Serializable class com.finanzen.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.finanzen.**$$serializer { *; }

# Koin
-keep class org.koin.** { *; }

# Coroutines
-dontwarn kotlinx.coroutines.**

# kotlinx-datetime
-dontwarn kotlinx.datetime.**
