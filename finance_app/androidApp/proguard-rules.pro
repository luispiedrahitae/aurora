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

# Koin
-keep class org.koin.** { *; }

# Coroutines
-dontwarn kotlinx.coroutines.**
