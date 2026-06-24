plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.spotless)
    alias(libs.plugins.detekt)
}

allprojects {
    apply(
        plugin =
            rootProject.libs.plugins.spotless
                .get()
                .pluginId,
    )
    apply(
        plugin =
            rootProject.libs.plugins.detekt
                .get()
                .pluginId,
    )

    extensions.configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        kotlin {
            target("src/**/*.kt")
            targetExclude("**/build/**", "**/generated/**")
            ktlint(
                rootProject.libs.versions.ktlint
                    .get(),
            ).editorConfigOverride(
                mapOf(
                    "android" to "true",
                    // ktlint 1.x quitó la excepción automática de @Composable en function-naming;
                    // hay que declararla o todos los Composables PascalCase fallan.
                    "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
                    // El proyecto agrupa tipos por concern (Repositories.kt, Destinations.kt);
                    // la regla filename (1 tipo = 1 archivo con su nombre) contradice ese diseño.
                    "ktlint_standard_filename" to "disabled",
                ),
            )
            trimTrailingWhitespace()
            endWithNewline()
        }
        kotlinGradle {
            target("*.gradle.kts")
            ktlint(
                rootProject.libs.versions.ktlint
                    .get(),
            )
        }
    }

    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        buildUponDefaultConfig = true
        allRules = false
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        autoCorrect = false
        parallel = true
    }

    dependencies {
        add("detektPlugins", rootProject.libs.detekt.formatting)
    }
}

// ── Tareas agregadas ─────────────────────────────────────────────────────────

tasks.register("checkQuality") {
    group = "verification"
    description = "Spotless check + Detekt en todos los módulos + tests del módulo shared"
    dependsOn(
        subprojects.map { "${it.path}:spotlessCheck" } +
            subprojects.map { "${it.path}:detekt" } +
            ":shared:allTests",
    )
}

tasks.register("format") {
    group = "formatting"
    description = "Aplica Spotless a todos los módulos"
    dependsOn(subprojects.map { "${it.path}:spotlessApply" })
}

tasks.register("prepareForCommit") {
    group = "verification"
    description = "format + checkQuality. Atajo previo a commitear."
    dependsOn("format")
    finalizedBy("checkQuality")
}

tasks.register("runDesktopPreview") {
    group = "application"
    description = "Atajo para abrir el preview de escritorio (útil en Windows sin Mac/emulador)"
    dependsOn(":desktopApp:run")
}
