package dev.isxander.modstitch.base.retrofuturagradle

import com.gtnewhorizons.retrofuturagradle.MinecraftExtension
import dev.isxander.modstitch.base.extensions.ModstitchExtension
import dev.isxander.modstitch.util.MinecraftVersion
import dev.isxander.modstitch.util.NotExistsDelegate
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

interface BaseRetroFuturaGradleExtension {
    /**
     * The version of Forge to use
     *
     * This property is actually only a verification, RetroFuturaGradle only accepts
     * a single forge version for 1.7.10, and another specific one for 1.12.2
     */
    val forgeVersion: Property<String>

    /**
     * The version of MCP to use
     */
    val mcpVersion: Property<String>

    /**
     * The MCP channel to use
     */
    val mcpChannel: Property<String>

    /**
     * Sets the FMLCorePlugin manifest attribute
     */
    val coreModClassName: Property<String>

    /**
     * Sets the FMLCorePluginContainsFMLMod manifest attribute
     */
    val hasModAndCoreMod: Property<Boolean>

    /**
     * The required dependencies for mixins to work
     *
     * These will be UniMixins on 1.7.10, and MixinBooter on 1.12.2
     *
     * This property is provided to filter them out when shading, since they are mod dependencies
     * and will have to be provided at runtime
     */
    val mixinsDependencies: ListProperty<String>

    /**
     * The underlying platform-specific extension
     *
     * Attempting to access this property on another platform than `retrofuturagradle`
     * will throw an error
     */
    val minecraft: MinecraftExtension

    /**
     * Configurres the Minecraft extension
     *
     * This action will only be executed on the `retrofuturagradle` platform
     */
    fun minecraft(action: Action<MinecraftExtension>)

}

open class BaseRetroFuturaGradleExtensionImpl @Inject constructor(
    objects: ObjectFactory,
    @Transient private val project: Project,
) : BaseRetroFuturaGradleExtension {
    override val forgeVersion: Property<String> = objects.property()
    override val mcpVersion: Property<String> = objects.property()
    override val mcpChannel: Property<String> = objects.property()
    override val coreModClassName: Property<String> = objects.property()
    override val hasModAndCoreMod: Property<Boolean> = objects.property<Boolean>()
    override val minecraft: MinecraftExtension
        get() = project.extensions.getByType<MinecraftExtension>()
    override val mixinsDependencies: ListProperty<String> = objects.listProperty()

    override fun minecraft(action: Action<MinecraftExtension>) = action(minecraft)

    init {
        forgeVersion.finalizeValueOnRead()
        mcpVersion.finalizeValueOnRead()
        mcpChannel.finalizeValueOnRead()
        coreModClassName.finalizeValueOnRead()
        hasModAndCoreMod.finalizeValueOnRead()
        mixinsDependencies.finalizeValueOnRead()
        val mixinBooterDeps = listOf(
            "zone.rong:mixinbooter:10.7",
            "org.ow2.asm:asm-debug-all:5.2"
        )
        val unimixinsMixin = "com.github.LegacyModdingMC.UniMixins:unimixins-all-1.7.10:0.2.1"
        val mixinDependency = project.provider {
            project.extensions.getByType<ModstitchExtension>()
        }.flatMap {
            it.minecraftVersion
        }.map {
            MinecraftVersion.Companion.parseOrderableOrNull(it)?.let {
                if (it == MinecraftVersion.LegacyRelease(8, 9)) {
                    mixinBooterDeps
                } else if (it == MinecraftVersion.LegacyRelease(12, 2)) {
                    mixinBooterDeps
                } else if (it == MinecraftVersion.LegacyRelease(7, 10)) {
                    listOf(unimixinsMixin)
                } else null
            }
        }
        mixinsDependencies.convention(mixinDependency)
    }
}

open class BaseRetroFuturaGradleExtensionDummy : BaseRetroFuturaGradleExtension {
    override val forgeVersion: Property<String> by NotExistsDelegate()
    override val mcpVersion: Property<String> by NotExistsDelegate()
    override val mcpChannel: Property<String> by NotExistsDelegate()
    override val coreModClassName: Property<String> by NotExistsDelegate()
    override val hasModAndCoreMod: Property<Boolean> by NotExistsDelegate()
    override val minecraft: MinecraftExtension by NotExistsDelegate()
    override val mixinsDependencies: ListProperty<String> by NotExistsDelegate()
    override fun minecraft(action: Action<MinecraftExtension>) {}
}
