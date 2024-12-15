package dev.isxander.modstitch.publishing

import dev.isxander.modstitch.ExtensionPlatforms
import dev.isxander.modstitch.ModstitchExtensionPlugin
import dev.isxander.modstitch.base.moddevgradle.MDGType
import dev.isxander.modstitch.publishing.loom.PublishingLoomImpl
import dev.isxander.modstitch.publishing.moddevgradle.PublishingModdevgradleImpl
import dev.isxander.modstitch.util.Platform

class PublishingPlugin : ModstitchExtensionPlugin("publishing", platforms) {
    companion object {
        val platforms: ExtensionPlatforms = mapOf(
            Platform.Loom to PublishingLoomImpl(),
            Platform.MDG to PublishingModdevgradleImpl(MDGType.Regular),
            Platform.MDGLegacy to PublishingModdevgradleImpl(MDGType.Legacy),
            Platform.MDGVanilla to PublishingModdevgradleImpl(MDGType.Vanilla),
        )
    }
}