package dev.isxander.modstitch

import dev.isxander.modstitch.util.*
import org.gradle.api.Plugin
import org.gradle.api.Project

typealias ExtensionPlatforms = Map<Platform, PlatformPlugin<*>>

/**
 * A plugin that handles applying its platform-specific logic to a project
 */
open class ModstitchExtensionPlugin(
    private val name: String,
    private val platforms: ExtensionPlatforms
) : Plugin<Project> {
    override fun apply(target: Project) {
        val platformStr = getPlatformStrFromProperty(target)

        if (platformStr == "parent") {
            applyToChildren(target)
        } else {
            apply(target, parsePlatformStr(platformStr, target))
        }
    }

    /**
     * Allow applying the plugin to a parent project and having it apply to all children
     * This is not recommended because it does not generate type safe kotlin dsls
     */
    private fun applyToChildren(target: Project) {
        target.childProjects.forEach { (_, child) ->
            apply(child, parsePlatformStr(getPlatformStrFromProperty(child), child))
        }
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
        // to generate type safety so even when the platform is not applied, the script can be compiled
        unselectedPlatforms.forEach { unselectedPlatform ->
            unselectedPlatform.platformExtensionInfo?.let {
                createDummyExtension(target, it)
            }
        }
    }

    private fun <T : Any> createDummyExtension(target: Project, extension: PlatformExtensionInfo<T>) {
        // multiple platforms may use the same extension, so only create a dummy if it doesn't already exist
        // the real platform is always applied first
        val alreadyExists = target.extensions.extensionsSchema
            .find { it.name == extension.name } != null

        if (!alreadyExists) {
            target.extensions.create(extension.api, extension.name, extension.dummyImpl)
        }
    }

    private fun getPlatformStrFromProperty(target: Project): String {
        return target.findProperty("modstitch.platform")?.toString()
            ?: error("Project `${target.name}` is missing 'modstitch.platform' property. Cannot apply `$name`")
    }

    private fun parsePlatformStr(platform: String, project: Project): Platform {
        return Platform.fromFriendlyName(platform)
            ?: error("Unknown platform on project `${project.name}`: '$platform'. Options are: ${platforms.keys.joinToString(", ") { it.friendlyName }}")
    }
}
