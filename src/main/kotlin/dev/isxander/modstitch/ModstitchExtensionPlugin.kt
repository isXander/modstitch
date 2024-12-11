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
        apply(target, platform ?: getDesiredLoaderFromProperty(target))
    }

    private fun getDesiredLoaderFromProperty(target: Project): Platform {
        val desiredLoaderStr = target.findProperty("modstitch.loader")?.toString()
            ?: error("Project `${target.name}` is missing 'modstitch.loader' property. Cannot apply ")
        return Platform.fromSerialName(desiredLoaderStr)
            ?: error("Unknown loader on project `${target.name}`: $desiredLoaderStr")
    }

    private fun apply(target: Project, selectedPlatform: Platform) {
        val platformPlugin = platforms[selectedPlatform]
            ?: error("No platform plugin found for loader $selectedPlatform")
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
