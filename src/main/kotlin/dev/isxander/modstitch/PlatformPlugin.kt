package dev.isxander.modstitch

import dev.isxander.modstitch.util.PlatformExtensionInfo
import org.gradle.api.Plugin
import org.gradle.api.Project

abstract class PlatformPlugin<T : Any> : Plugin<Project> {
    open val platformExtensionInfo: PlatformExtensionInfo<T>? = null

    protected fun createRealPlatformExtension(target: Project, vararg constructionArguments: Any): T? {
        return platformExtensionInfo?.let {
            target.extensions.create(it.api.java, it.name, it.realImpl.java, *constructionArguments)
        }
    }
}