package dev.isxander.modstitch.util

import org.gradle.api.Project

enum class Platform(val friendlyName: String, val modManifest: String?) {
    Loom("loom", "fabric.mod.json"),
    MDG("moddevgradle", "META-INF/neoforge.mods.toml"),
    MDGLegacy("moddevgradle-legacy", "META-INF/mods.toml"),
    MDGVanilla("moddevgradle-vanilla", null);

    companion object {
        val allModManifests = values().mapNotNull { it.modManifest }

        fun fromFriendlyName(name: String): Platform? {
            return values().firstOrNull { it.friendlyName == name }
        }
    }
}

val Project.platformOrNull: Platform?
    get() = project.extensions.extraProperties.has("appliedPlatform")
        .let { if (it) Platform.fromFriendlyName(project.extensions.extraProperties["appliedPlatform"].toString()) else null }
var Project.platform: Platform
    get() = platformOrNull ?: throw IllegalStateException("Loader not set")
    internal set(value) {
        project.extensions.extraProperties["appliedPlatform"] = value.friendlyName
    }

val Project.isLoom: Boolean
    get() = platform == Platform.Loom
val Project.isModDevGradle: Boolean
    get() = platform in listOf(Platform.MDG, Platform.MDGLegacy, Platform.MDGVanilla)
val Project.isModDevGradleRegular: Boolean
    get() = platform == Platform.MDG
val Project.isModDevGradleLegacy: Boolean
    get() = platform == Platform.MDGLegacy
val Project.isModDevGradleVanilla: Boolean
    get() = platform == Platform.MDGVanilla
