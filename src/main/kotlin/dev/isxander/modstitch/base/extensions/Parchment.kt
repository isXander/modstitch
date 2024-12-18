package dev.isxander.modstitch.base.extensions

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

interface ParchmentBlock {
    val minecraftVersion: Property<String>
    val mappingsVersion: Property<String>
    val parchmentArtifact: Property<String>
    val enabled: Property<Boolean>
}
@Suppress("LeakingThis") // Extension must remain open for Gradle to inject the implementation. This is safe.
open class ParchmentBlockImpl @Inject constructor(objects: ObjectFactory) : ParchmentBlock {
    override val minecraftVersion = objects.property<String>()
    override val mappingsVersion = objects.property<String>()
    override val parchmentArtifact = objects.property<String>().convention(minecraftVersion.zip(mappingsVersion) { mc, mappings -> "org.parchmentmc.data:parchment-$mc:$mappings@zip" })
    override val enabled = objects.property<Boolean>().convention(parchmentArtifact.map { it.isNotEmpty() }.orElse(false))
}