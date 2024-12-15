package dev.isxander.modstitch.base

import dev.isxander.modstitch.ModstitchExtensionPlugin
import dev.isxander.modstitch.util.Platform
import dev.isxander.modstitch.PlatformPlugin
import dev.isxander.modstitch.base.loom.BaseLoomImpl
import dev.isxander.modstitch.base.moddevgradle.BaseModdevgradleImpl
import dev.isxander.modstitch.base.moddevgradle.MDGType

class BasePlugin : ModstitchExtensionPlugin("base", platforms) {
    companion object {
        val platforms = mapOf<Platform, PlatformPlugin<*>>(
            Platform.Loom to BaseLoomImpl(),
            Platform.MDG to BaseModdevgradleImpl(MDGType.Regular),
            Platform.MDGLegacy to BaseModdevgradleImpl(MDGType.Legacy),
            Platform.MDGVanilla to BaseModdevgradleImpl(MDGType.Vanilla),
        )
    }
}