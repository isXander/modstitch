package dev.isxander.modstitch.base.moddevgradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class EnabledMarkerPlugin : Plugin<Project> {
    override fun apply(target: Project) {
    }

    companion object {
        const val ID = "dev.isxander.modstitch.base.moddevgradle.enabled-marker"
    }
}