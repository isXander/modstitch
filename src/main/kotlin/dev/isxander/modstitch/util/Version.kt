package dev.isxander.modstitch.util

import dev.isxander.modstitch.ModstitchExtensionPlugin
import org.gradle.api.Project

val MODSTITCH_VERSION: String = ModstitchExtensionPlugin::class.java.`package`.implementationVersion

fun printVersion(suffix: String, project: Project) {
    val prop = "modstitch$suffix-versionprinted"
    if (project.rootProject.hasProperty(prop)) return
    project.rootProject.extensions.extraProperties[prop] = true

    project.logger.lifecycle("Modstitch/$suffix $MODSTITCH_VERSION")
}
