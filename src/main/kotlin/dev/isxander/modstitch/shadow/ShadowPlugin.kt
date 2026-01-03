package dev.isxander.modstitch.shadow

import dev.isxander.modstitch.ModstitchExtensionPlugin
import dev.isxander.modstitch.PlatformPlugin
import dev.isxander.modstitch.base.moddevgradle.MDGType
import dev.isxander.modstitch.shadow.loom.ShadowLoomImpl
import dev.isxander.modstitch.shadow.moddevgradle.ShadowModdevgradleImpl
import dev.isxander.modstitch.util.Platform
import dev.isxander.modstitch.util.RegisteredGradlePlugin

@RegisteredGradlePlugin
class ShadowPlugin : ModstitchExtensionPlugin("shadow", platforms) {
    companion object {
        val platforms = mapOf<Platform, PlatformPlugin<*>>(
            Platform.LoomRemap to ShadowLoomImpl(),
            Platform.MDG to ShadowModdevgradleImpl(MDGType.Regular),
            Platform.MDGLegacy to ShadowModdevgradleImpl(MDGType.Legacy),
        )
    }
}