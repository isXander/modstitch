package dev.isxander.modstitch.base.extensions

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.mapProperty
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

interface MetadataBlock {
    val modId: Property<String>
    val modName: Property<String>
    val modVersion: Property<String>
    val modDescription: Property<String>
    val modLicense: Property<String>
    val modGroup: Property<String>
    val modAuthor: Property<String>
    val modCredits: Property<String>
    val replacementProperties: MapProperty<String, String>
}
open class MetadataBlockImpl @Inject constructor(objects: ObjectFactory) : MetadataBlock {
    /** Mods should use a `lower_snake_case` mod-id to obey the conventions of both mod loaders. */
    override val modId = objects.property<String>().convention("unnamed_mod")
    override val modName = objects.property<String>().convention("Unnamed Mod")
    override val modVersion = objects.property<String>().convention("1.0.0")
    override val modDescription = objects.property<String>().convention("")
    override val modLicense = objects.property<String>().convention("All Rights Reserved")
    override val modGroup = objects.property<String>().convention("com.example")
    override val modAuthor = objects.property<String>().convention("")
    override val modCredits = objects.property<String>().convention("")
    override val replacementProperties = objects.mapProperty<String, String>().convention(emptyMap())
}