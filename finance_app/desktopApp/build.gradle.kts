import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(libs.koin.core)

    // ponytail: forzamos el nativo Skiko a la versión del jar Kotlin (0.9.4.2). El catalog de Compose 1.7.3
    // arrastra un runtime-windows-x64 0.8.18 antiguo que no expone los símbolos JNI del jar más nuevo.
    // Cuando Compose MP suba a 1.8+ esto se podrá eliminar.
    val skikoNative =
        when {
            org.gradle.internal.os.OperatingSystem
                .current()
                .isWindows -> libs.skiko.awt.runtime.windows.x64
            org.gradle.internal.os.OperatingSystem
                .current()
                .isMacOsX -> {
                if (System.getProperty("os.arch") == "aarch64") {
                    libs.skiko.awt.runtime.macos.arm64
                } else {
                    libs.skiko.awt.runtime.macos.x64
                }
            }
            else -> libs.skiko.awt.runtime.linux.x64
        }
    runtimeOnly(skikoNative)
}

compose.desktop {
    application {
        mainClass = "com.finanzen.desktop.MainKt"

        nativeDistributions {
            // Preview de desarrollo en Windows. No se distribuye en stores.
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "Cauce Preview"
            packageVersion = "0.1.0"
            description = "Cauce — preview de UI (no para distribución)"
            vendor = "Cauce"
        }
    }
}
