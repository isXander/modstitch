package dev.isxander.modstitch.util

import dev.isxander.modstitch.ModstitchExtensionPlugin
import org.gradle.api.Project

val MODSTITCH_VERSION: String = ModstitchExtensionPlugin::class.java.`package`.implementationVersion ?: "unknown"

fun printVersion(suffix: String, project: Project) {
    project.logger.lifecycle("Modstitch/$suffix $MODSTITCH_VERSION")
}
