package dev.isxander.modstitch

import dev.isxander.modstitch.util.*
import org.gradle.api.Plugin
import org.gradle.api.Project

typealias ExtensionPlatforms = Map<Platform, PlatformPlugin<*>>

open class ModstitchExtensionPlugin(
    private val name: String,
    private val platforms: ExtensionPlatforms,
    val platform: Platform? = null
) : Plugin<Project> {
    override fun apply(target: Project) {
        apply(target, platform ?: getDesiredPlatformFromProperty(target))
    }

    private fun getDesiredPlatformFromProperty(target: Project): Platform {
        val desiredPlatformStr = target.findProperty("modstitch.platform")?.toString()
            ?: error("Project `${target.name}` is missing 'modstitch.platform' property. Cannot apply ")
        return Platform.fromSerialName(desiredPlatformStr)
            ?: error("Unknown platform on project `${target.name}`: '$desiredPlatformStr'. Options are: ${Platform.values().joinToString(", ") { it.friendlyName }}")
    }

    private fun apply(target: Project, selectedPlatform: Platform) {
        val platformPlugin = platforms[selectedPlatform]
            ?: error("This plugin does not support the platform `$selectedPlatform`. Supported platforms: ${platforms.keys.joinToString(", ") { it.friendlyName }}")
        val unselectedPlatforms = platforms.values - platformPlugin

        if (target.platformOrNull != null && target.platformOrNull != selectedPlatform) {
            error("Modstitch: ${target.name} has already been assigned platform `${target.platformOrNull}` but extension `$name` is trying to assign platform `$selectedPlatform`")
        }
        target.platform = selectedPlatform

        // apply the real plugin for the correct platform
        platformPlugin.apply(target)

        // create dud extensions for all other platforms
        unselectedPlatforms.forEach { unselectedPlatform ->
            unselectedPlatform.platformExtensionInfo?.let {
                createDudExtension(target, it)
            }
        }
    }

    private fun <T : Any> createDudExtension(target: Project, extension: PlatformExtensionInfo<T>): T {
        return target.extensions.create(extension.api.java, extension.name, extension.dummyImpl.java)
    }
}
