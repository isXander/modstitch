package dev.isxander.modstitch.base

import dev.isxander.modstitch.ModstitchExtensionPlugin
import dev.isxander.modstitch.util.Platform
import dev.isxander.modstitch.PlatformPlugin
import dev.isxander.modstitch.base.loom.BaseLoomImpl
import dev.isxander.modstitch.base.moddevgradle.BaseModDevGradleImpl
import dev.isxander.modstitch.base.moddevgradle.MDGType
import dev.isxander.modstitch.util.RegisteredGradlePlugin

@RegisteredGradlePlugin
class BasePlugin : ModstitchExtensionPlugin("base", platforms) {
    companion object {
        val platforms = mapOf<Platform, PlatformPlugin<*>>(
            Platform.Loom to BaseLoomImpl(),
            Platform.MDG to BaseModDevGradleImpl(MDGType.Regular),
            Platform.MDGLegacy to BaseModDevGradleImpl(MDGType.Legacy),
        )
    }
}
