package dev.isxander.modstitch.base.loom

/**
 * The extension for configuring Fabric-specific settings.
 */

import dev.isxander.modstitch.util.NotExistsDelegate
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.*
import javax.inject.Inject

interface BaseLoomExtension {
    /**
     * The version of Fabric Loader to use.
     */
    val fabricLoaderVersion: Property<String>

    /**
     * The underlying platform-specific extension: `loom`
     */
    val loomExtension: LoomGradleExtensionAPI

    /**
     * Configures the Loom extension.
     * This action will only be executed if the current platform is Loom.
     */
    fun configureLoom(action: Action<LoomGradleExtensionAPI>)
}

open class BaseLoomExtensionImpl @Inject constructor(
    objects: ObjectFactory,
    @Transient private val project: Project
) : BaseLoomExtension {
    override val fabricLoaderVersion: Property<String> = objects.property<String>()

    override val loomExtension: LoomGradleExtensionAPI
        get() = project.extensions.getByType<LoomGradleExtensionAPI>()

    override fun configureLoom(action: Action<LoomGradleExtensionAPI>) =
        action.execute(loomExtension)
}

open class BaseLoomExtensionDummy : BaseLoomExtension {
    override val fabricLoaderVersion: Property<String> by NotExistsDelegate()
    override val loomExtension: LoomGradleExtensionAPI by NotExistsDelegate()
    override fun configureLoom(action: Action<LoomGradleExtensionAPI>) {}
}
