plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvmToolchain(17)

    androidTarget()

    jvm("desktop")

    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)

            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines.ext)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.navigation.compose)

            implementation(libs.koalaplot.core)

            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }

        androidMain.dependencies {
            implementation(libs.sqldelight.driver.android)
            implementation(libs.androidx.biometric)
            implementation(libs.androidx.work.runtime)
            implementation(libs.koin.android)
        }

        val desktopMain by getting {
            dependencies {
                implementation(libs.sqldelight.driver.sqlite)
                // Provee Dispatchers.Main = AWT EventQueue. Sin esto, viewModelScope/stateIn
                // fallan silenciosamente en JVM Desktop y los StateFlow no propagan a la UI.
                implementation(libs.kotlinx.coroutines.swing)
            }
        }

        iosMain.dependencies {
            implementation(libs.sqldelight.driver.native)
        }
    }
}

android {
    namespace = "com.finanzen.shared"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

sqldelight {
    databases {
        create("FinanzenDb") {
            packageName.set("com.finanzen.db")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
            verifyMigrations.set(true)
        }
    }
}
