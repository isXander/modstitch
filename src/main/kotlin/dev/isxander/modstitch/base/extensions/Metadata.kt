package dev.isxander.modstitch.base.extensions

import dev.isxander.modstitch.util.propConvention
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.mapProperty
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

interface MetadataBlock {
    /**
     * Your mod's ID. Mods should use a `lower_snake_case` mod-id to obey the conventions of both mod loaders.
     * Resources in the `templates` directory will be expanded with this value via `${mod_id}`.
     * Other gradle configuration will also use this value.
     */
    val modId: Property<String>

    /**
     * The friendly name of your mod.
     * Resources in the `templates` directory will be expanded with this value via `${mod_name}`.
     * Other gradle configuration may also use this value.
     * Publications via modstitch/publishing may use this value.
     */
    val modName: Property<String>

    /**
     * The version of your mod.
     * Resources in the `templates` directory will be expanded with this value via `${mod_version}`.
     * Other gradle configuration may also use this value.
     * Publications via modstitch/publishing may use this value.
     */
    val modVersion: Property<String>

    /**
     * A description of your mod.
     * Resources in the `templates` directory will be expanded with this value via `${mod_description}`.
     */
    val modDescription: Property<String>

    /**
     * The license name of your mod, for example "MIT" or "All Rights Reserved".
     * Resources in the `templates` directory will be expanded with this value via `${mod_license}`.
     */
    val modLicense: Property<String>

    /**
     * The group of your mod, for example "com.example".
     * Resources in the `templates` directory will be expanded with this value via `${mod_group}`.
     * Other gradle configuration may also use this value.
     */
    val modGroup: Property<String>

    /**
     * The author of your mod.
     * Resources in the `templates` directory will be expanded with this value via `${mod_author}`.
     */
    val modAuthor: Property<String>

    /**
     * The credits for your mod.
     * Resources in the `templates` directory will be expanded with this value via `${mod_credits}`.
     */
    val modCredits: Property<String>

    /**
     * A map of additional properties to replace in the templates directory.
     * Resources in the `templates` directory will be expanded with these values.
     */
    val replacementProperties: MapProperty<String, String>
}
open class MetadataBlockImpl @Inject constructor(providers: ProviderFactory, objects: ObjectFactory) : MetadataBlock {
    /** Mods should use a `lower_snake_case` mod-id to obey the conventions of both mod loaders. */
    override val modId = objects.property<String>()
        .propConvention(providers.prop("modId"))
        .convention("unnamed_mod")
    override val modName = objects.property<String>()
        .propConvention(providers.prop("modName"))
        .convention("Unnamed Mod")
    override val modVersion = objects.property<String>()
        .propConvention(providers.prop("modVersion"))
        .convention("1.0.0")
    override val modDescription = objects.property<String>()
        .propConvention(providers.prop("modDescription"))
        .convention("")
    override val modLicense = objects.property<String>()
        .propConvention(providers.prop("modLicense"))
        .convention("All Rights Reserved")
    override val modGroup = objects.property<String>()
        .propConvention(providers.prop("modGroup"))
        .convention("com.example")
    override val modAuthor = objects.property<String>()
        .propConvention(providers.prop("modAuthor"))
        .convention("")
    override val modCredits = objects.property<String>()
        .propConvention(providers.prop("modCredits"))
        .convention("")
    override val replacementProperties = objects.mapProperty<String, String>()
        .convention(emptyMap())

    private fun ProviderFactory.prop(suffix: String) = gradleProperty("modstitch.metadata.${suffix}")
}