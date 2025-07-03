package dev.isxander.modstitch.base.loom

/**
 * The extension for configuring Fabric-specific settings.
 */

import dev.isxander.modstitch.PlatformExtension
import dev.isxander.modstitch.base.extensions.modstitch
import dev.isxander.modstitch.util.ExtensionGetter
import dev.isxander.modstitch.util.NotExistsDelegate
import dev.isxander.modstitch.util.propConvention
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.*
import javax.inject.Inject

interface BaseLoomExtension : PlatformExtension<BaseLoomExtension> {
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
    fun configureLoom(action: Action<LoomGradleExtensionAPI>) = action.execute(loomExtension)
}

open class BaseLoomExtensionImpl @Inject constructor(
    providers: ProviderFactory,
    objects: ObjectFactory,
    @Transient private val project: Project
) : BaseLoomExtension {
    override val fabricLoaderVersion: Property<String> = objects.property<String>()
        .propConvention(providers.prop("fabricLoaderVersion"))

    override val loomExtension: LoomGradleExtensionAPI by ExtensionGetter(project)
    override fun configureLoom(action: Action<LoomGradleExtensionAPI>) =
        if (project.modstitch.isLoom) action.execute(loomExtension) else {}

    override fun applyIfCurrent(configure: Action<BaseLoomExtension>) =
        configure.execute(this)

    private fun ProviderFactory.prop(suffix: String) = gradleProperty("modstitch.loom.${suffix}")
}

open class BaseLoomExtensionDummy : BaseLoomExtension {
    override val fabricLoaderVersion: Property<String> by NotExistsDelegate()
    override val loomExtension: LoomGradleExtensionAPI by NotExistsDelegate()

    override fun applyIfCurrent(configure: Action<BaseLoomExtension>) {}
}
