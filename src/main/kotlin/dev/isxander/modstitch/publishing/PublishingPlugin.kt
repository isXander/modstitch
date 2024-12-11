package dev.isxander.modstitch.publishing

import dev.isxander.modstitch.ExtensionPlatforms
import dev.isxander.modstitch.ModstitchExtensionPlugin
import dev.isxander.modstitch.publishing.fabric.PublishingFabricImpl
import dev.isxander.modstitch.publishing.neoforge.PublishingNeoforgeImpl
import dev.isxander.modstitch.util.Platform

class PublishingPlugin : ModstitchExtensionPlugin("publishing", platforms) {
    companion object {
        val platforms: ExtensionPlatforms = mapOf(
            Platform.Fabric to PublishingFabricImpl,
            Platform.NeoForge to PublishingNeoforgeImpl,
        )
    }
}