package dev.isxander.modstitch.base.extensions

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

interface ParchmentBlock {
    /**
     * The Minecraft version to use for Parchment mappings.
     * By default, this is the same as the Minecraft version of the project.
     */
    val minecraftVersion: Property<String>

    /**
     * The mappings version to use for Parchment mappings.
     * E.g. 2024.12.29
     */
    val mappingsVersion: Property<String>

    /**
     * The artifact to use for Parchment mappings.
     * By default, this is `org.parchmentmc.data:parchment-$minecraftVersion:$mappingsVersion@zip`.
     */
    val parchmentArtifact: Property<String>

    /**
     * Whether Parchment is enabled.
     * This is true if [parchmentArtifact] is not empty.
     */
    val enabled: Property<Boolean>
}
@Suppress("LeakingThis") // Extension must remain open for Gradle to inject the implementation. This is safe.
open class ParchmentBlockImpl @Inject constructor(objects: ObjectFactory) : ParchmentBlock {
    override val minecraftVersion = objects.property<String>()
    override val mappingsVersion = objects.property<String>()
    override val parchmentArtifact = objects.property<String>().convention(minecraftVersion.zip(mappingsVersion) { mc, mappings -> "org.parchmentmc.data:parchment-$mc:$mappings@zip" })
    override val enabled = objects.property<Boolean>().convention(parchmentArtifact.map { it.isNotEmpty() }.orElse(false))
}