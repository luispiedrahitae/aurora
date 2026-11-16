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
}

compose.desktop {
    application {
        mainClass = "com.finanzen.desktop.MainKt"

        nativeDistributions {
            // Preview de desarrollo en Windows. No se distribuye en stores.
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "Sage Preview"
            packageVersion = "0.1.0"
            description = "Sage — preview de UI (no para distribución)"
            vendor = "Sage"
        }
    }
}
