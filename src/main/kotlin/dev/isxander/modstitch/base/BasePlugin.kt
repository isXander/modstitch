package dev.isxander.modstitch.base

import dev.isxander.modstitch.ModstitchExtensionPlugin
import dev.isxander.modstitch.util.Platform
import dev.isxander.modstitch.PlatformPlugin
import dev.isxander.modstitch.base.fabric.BaseFabricImpl
import dev.isxander.modstitch.base.neoforge.BaseNeoforgeImpl

class BasePlugin : ModstitchExtensionPlugin("base", platforms) {
    companion object {
        val platforms = mapOf<Platform, PlatformPlugin<*>>(
            Platform.Fabric to BaseFabricImpl,
            Platform.NeoForge to BaseNeoforgeImpl,
        )
    }
}