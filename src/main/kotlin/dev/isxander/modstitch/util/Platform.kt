package dev.isxander.modstitch.util

import org.gradle.api.Project

enum class Platform(val friendlyName: String, val modManifest: String?) {
    Loom("loom", "fabric.mod.json"),
    MDG("moddevgradle", "META-INF/neoforge.mods.toml"),
    MDGLegacy("moddevgradle-legacy", "META-INF/mods.toml");

    val isModDevGradle: Boolean
        get() = this in listOf(MDG, MDGLegacy)
    val isModDevGradleRegular: Boolean
        get() = this == MDG
    val isModDevGradleLegacy: Boolean
        get() = this == MDGLegacy
    val isLoom: Boolean
        get() = this == Loom

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
