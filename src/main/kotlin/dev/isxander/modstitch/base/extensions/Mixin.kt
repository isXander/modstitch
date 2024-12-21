package dev.isxander.modstitch.base.extensions

import dev.isxander.modstitch.util.Side
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.logging.Logger
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.*
import javax.inject.Inject

interface MixinBlock {
    val serializer: Property<MixinSettingsSerializer>

    val configs: NamedDomainObjectContainer<MixinConfigurationSettings>
    fun configs(action: Action<NamedDomainObjectContainer<MixinConfigurationSettings>>) = action.execute(configs)
}
open class MixinBlockImpl @Inject constructor(objects: ObjectFactory) : MixinBlock {
    override val serializer = objects.property<MixinSettingsSerializer>()
    override val configs = objects.domainObjectContainer(MixinConfigurationSettings::class)
}

typealias MixinSettingsSerializer = (configs: List<MixinConfigurationSettings>, logger: Logger) -> String

open class MixinConfigurationSettings @Inject constructor(private val namekt: String, objects: ObjectFactory) : Named {
    // Removes the need to import in the buildscript
    val BOTH = Side.Both
    val CLIENT = Side.Client
    val SERVER = Side.Server

    val config: Property<String> = objects.property<String>().convention("$namekt.mixins.json")
    val refmap: Property<String> = objects.property<String>().convention("$namekt.mixins.refmap.json")
    val side: Property<Side> = objects.property<Side>().convention(Side.Both)

    override fun getName(): String = namekt
}