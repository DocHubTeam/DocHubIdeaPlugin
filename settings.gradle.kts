rootProject.name = "IDEAPlugin"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://www.jetbrains.com/intellij-repository/releases")
        maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
    }

    plugins {
        id("org.jetbrains.kotlin.jvm") version providers.gradleProperty("kotlinVersion").get()
        id("org.jetbrains.intellij.platform.settings") version providers.gradleProperty("gradleIntellijPluginVersion").get()
        id("org.jetbrains.intellij.platform") version providers.gradleProperty("gradleIntellijPluginVersion").get()
        id("org.jetbrains.grammarkit") version providers.gradleProperty("grammarKitVersion").get()
        id("org.jetbrains.intellij.deps.jflex") version providers.gradleProperty("jflexVersion").get()
    }
}

plugins {
    id("org.jetbrains.intellij.platform.settings") version providers.gradleProperty("gradleIntellijPluginVersion").get()
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// Этот блок подтянет всё остальное (включая JFlex для тестов/сборки)
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://www.jetbrains.com/intellij-repository/releases")
        maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
    }
}