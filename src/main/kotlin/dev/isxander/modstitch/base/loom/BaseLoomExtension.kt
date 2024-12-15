package dev.isxander.modstitch.base.loom

/**
 * The extension for configuring Fabric-specific settings.
 */

import dev.isxander.modstitch.PlatformExtension
import dev.isxander.modstitch.util.ExtensionGetter
import dev.isxander.modstitch.util.NotExistsDelegate
import dev.isxander.modstitch.util.isLoom
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.*
import javax.inject.Inject

interface BaseLoomExtension : PlatformExtension<BaseLoomExtension> {
    val fabricLoaderVersion: Property<String>

    val loomExtension: LoomGradleExtensionAPI
    fun configureLoom(action: Action<LoomGradleExtensionAPI>) = action.execute(loomExtension)
}

open class BaseLoomExtensionImpl @Inject constructor(objects: ObjectFactory, private val project: Project) :
    BaseLoomExtension {
    override val fabricLoaderVersion: Property<String> = objects.property<String>()

    override val loomExtension: LoomGradleExtensionAPI by ExtensionGetter(project)
    override fun configureLoom(action: Action<LoomGradleExtensionAPI>) =
        if (project.isLoom) action.execute(loomExtension) else {}

    override fun applyIfCurrent(configure: Action<BaseLoomExtension>) =
        configure.execute(this)
}

open class BaseLoomExtensionDummy : BaseLoomExtension {
    override val fabricLoaderVersion: Property<String> by NotExistsDelegate()
    override val loomExtension: LoomGradleExtensionAPI by NotExistsDelegate()

    override fun applyIfCurrent(configure: Action<BaseLoomExtension>) {}
}
