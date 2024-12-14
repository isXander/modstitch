package dev.isxander.modstitch.publishing

import dev.isxander.modstitch.ExtensionPlatforms
import dev.isxander.modstitch.ModstitchExtensionPlugin
import dev.isxander.modstitch.publishing.loom.PublishingLoomImpl
import dev.isxander.modstitch.publishing.moddevgradle.PublishingModdevgradleImpl
import dev.isxander.modstitch.util.Platform

class PublishingPlugin : ModstitchExtensionPlugin("publishing", platforms) {
    companion object {
        val platforms: ExtensionPlatforms = mapOf(
            Platform.Loom to PublishingLoomImpl,
            Platform.ModDevGradle to PublishingModdevgradleImpl,
        )
    }
}