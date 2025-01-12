package dev.isxander.modstitch.base.extensions

import dev.isxander.modstitch.util.Side
import org.gradle.api.Action
import org.gradle.api.DomainObjectSet
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.logging.Logger
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
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
     */
    val addMixinsToModManifest: Property<Boolean>

    /**
     * Responsible for creating the `${mod_mixins}` expansion in your mod manifest files,
     * this is the alternative to [addMixinsToModManifest], if you'd like to expand traditionally.
     *
     * This property is already set by default by the platform of modstitch on this project.
     * You probably shouldn't touch this.
     */
    val serializer: Property<MixinSettingsSerializer>

    /**
     * Registers additional source sets to be processed by Mixin AP.
     */
    fun registerSourceSet(sourceSet: SourceSet, refmapName: String)

    @get:ApiStatus.Internal
    val mixinSourceSets: DomainObjectSet<MixinSourceSet>
}
open class MixinBlockImpl @Inject constructor(private val objects: ObjectFactory) : MixinBlock {
    override val serializer = objects.property<MixinSettingsSerializer>()
    override val configs = objects.domainObjectContainer(MixinConfigurationSettings::class)
    override val addMixinsToModManifest = objects.property<Boolean>().convention(false)
    override val mixinSourceSets = objects.domainObjectSet(MixinSourceSet::class)
    override fun registerSourceSet(sourceSet: SourceSet, refmapName: String) {
        objects.newInstance<MixinSourceSet>().apply {
            this.sourceSet = sourceSet
            this.refmapName.set(refmapName)
        }.also { mixinSourceSets.add(it) }
    }
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
}

open class MixinSourceSet @Inject constructor(objects: ObjectFactory) {
    val sourceSetName = objects.property<String>()
    val refmapName = objects.property<String>()

    var sourceSet: SourceSet
        get() = throw UnsupportedOperationException()
        set(value) { sourceSetName.set(value.name) }
}