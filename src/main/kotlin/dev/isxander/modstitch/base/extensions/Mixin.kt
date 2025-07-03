package dev.isxander.modstitch.base.extensions

import dev.isxander.modstitch.util.Side
import dev.isxander.modstitch.util.propConvention
import org.gradle.api.Action
import org.gradle.api.DomainObjectSet
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.logging.Logger
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.*
import org.jetbrains.annotations.ApiStatus
import javax.inject.Inject

interface MixinBlock {
    /**
     * A container for all your mixin configs.
     * Modstitch will automatically configure the underlying platform plugin to respect these
     * mixin configurations, including adding it to your mod manifest if [addMixinsToModManifest] is true.
     *
     * The name of the objects should be the prefix to your mixin config file, unless you configure the object otherwise.
     * `configs.register("mymod")` will be associated to `mymod.mixins.json`, for example.
     */
    val configs: NamedDomainObjectContainer<MixinConfigurationSettings>
    fun configs(action: Action<NamedDomainObjectContainer<MixinConfigurationSettings>>) = action.execute(configs)

    /**
     * Automatically appends your configured mixins into your mod manifest, like
     * `fabric.mod.json` or `neoforge.mods.toml`.
     * Defaults to false for backwards compatibility reasons.
     *
     * Ensure that your mod manifest is in the `templates` directory in order for this to work.
     */
    val addMixinsToModManifest: Property<Boolean>

    /**
     * Registers additional source sets to be processed by Mixin AP.
     */
    fun registerSourceSet(sourceSet: SourceSet, refmapName: Provider<String>)

    /**
     * Registers additional source sets to be processed by Mixin AP.
     */
    fun registerSourceSet(sourceSet: SourceSet, refmapName: String)

    @get:ApiStatus.Internal
    val mixinSourceSets: DomainObjectSet<MixinSourceSet>
}
open class MixinBlockImpl @Inject constructor(providers: ProviderFactory, private val objects: ObjectFactory) : MixinBlock {
    override val configs = objects.domainObjectContainer(MixinConfigurationSettings::class)
    override val addMixinsToModManifest = objects.property<Boolean>()
        .propConvention(providers.prop("addMixinsToModManifest")) { it.toBoolean() }
        .convention(false)
    override val mixinSourceSets = objects.domainObjectSet(MixinSourceSet::class)
    override fun registerSourceSet(sourceSet: SourceSet, refmapName: Provider<String>) {
        objects.newInstance<MixinSourceSet>().apply {
            this.sourceSet = sourceSet
            this.refmapName.set(refmapName)
        }.also { mixinSourceSets.add(it) }
    }
    override fun registerSourceSet(sourceSet: SourceSet, refmapName: String) {
        objects.newInstance<MixinSourceSet>().apply {
            this.sourceSet = sourceSet
            this.refmapName.set(refmapName)
        }.also { mixinSourceSets.add(it) }
    }

    private fun ProviderFactory.prop(suffix: String) = gradleProperty("modstitch.mixin.${suffix}")
}

typealias MixinSettingsSerializer = (configs: List<MixinConfigurationSettings>, logger: Logger) -> String

open class MixinConfigurationSettings @Inject constructor(private val namekt: String, objects: ObjectFactory) : Named {
    // Removes the need to import in the buildscript
    val BOTH = Side.Both
    val CLIENT = Side.Client
    val SERVER = Side.Server

    /**
     * The name of the mixin configuration file.
     * E.g. `mymod.mixins.json`.
     * If unset, defaults to `${name}.mixins.json`.
     */
    val config: Property<String> = objects.property<String>().convention("$namekt.mixins.json")

    /**
     * The side of the game this mixin configuration is for.
     * Defaults to [Side.Both].
     *
     * This is not valid for moddevgradle platforms and will produce a warning.
     */
    val side: Property<Side> = objects.property<Side>().convention(Side.Both)

    override fun getName(): String = namekt

    internal fun resolved(): FinalMixinConfigurationSettings {
        return FinalMixinConfigurationSettings(config.get(), side.getOrElse(Side.Both))
    }
}

open class MixinSourceSet @Inject constructor(objects: ObjectFactory) {
    val sourceSetName = objects.property<String>()
    val refmapName = objects.property<String>()

    var sourceSet: SourceSet
        get() = throw UnsupportedOperationException()
        set(value) { sourceSetName.set(value.name) }
}

data class FinalMixinConfigurationSettings(val config: String, val side: Side)
