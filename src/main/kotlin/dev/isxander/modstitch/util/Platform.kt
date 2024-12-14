package dev.isxander.modstitch.util

import org.gradle.api.Project

enum class Platform(val friendlyName: String, val modManifest: String) {
    Loom("loom", "fabric.mod.json"),
    ModDevGradle("moddevgradle", "META-INF/neoforge.mods.toml");

    companion object {
        val allModManifests = values().map { it.modManifest }

        fun fromSerialName(name: String): Platform? {
            return values().firstOrNull { it.friendlyName == name }
        }
    }
}

val Project.platformOrNull: Platform?
    get() = project.extensions.extraProperties.has("appliedPlatform")
        .let { if (it) Platform.fromSerialName(project.extensions.extraProperties["appliedPlatform"].toString()) else null }
var Project.platform: Platform
    get() = platformOrNull ?: throw IllegalStateException("Loader not set")
    internal set(value) {
        project.extensions.extraProperties["appliedPlatform"] = value.friendlyName
    }

val Project.isLoom: Boolean
    get() = platform == Platform.Loom
val Project.isModDevGradle: Boolean
    get() = platform == Platform.ModDevGradle
