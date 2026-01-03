package dev.isxander.modstitch.util

import org.gradle.api.Project

enum class Platform(val friendlyName: String, val modManifest: String) {
    Loom("fabric-loom", "fabric.mod.json"),
    LoomRemap("fabric-loom-remap", "fabric.mod.json"),
    MDG("moddevgradle", "META-INF/neoforge.mods.toml"),
    MDGLegacy("moddevgradle-legacy", "META-INF/mods.toml");

    val isModDevGradle: Boolean
        get() = this in listOf(MDG, MDGLegacy)
    val isModDevGradleRegular: Boolean
        get() = this == MDG
    val isModDevGradleLegacy: Boolean
        get() = this == MDGLegacy
    val isLoom: Boolean
        get() = this in listOf(Loom, LoomRemap)
    val isLoomRemap: Boolean
        get() = this == LoomRemap
    val isLoomNoRemap: Boolean
        get() = this == Loom

    companion object {
        val allModManifests = entries.map { it.modManifest }.toSet()

        fun fromFriendlyName(name: String): Platform? {
            return entries.firstOrNull { it.friendlyName == name }
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
