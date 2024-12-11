package dev.isxander.modstitch.base

/**
 * The "common" extension for all loaders, as much configuration is done in this extension
 * such as configuring Minecraft version, mappings, mixins etc.
 */

import dev.isxander.modstitch.base.fabric.BaseFabricExtension
import dev.isxander.modstitch.base.neoforge.BaseNeoForgeExtension
import dev.isxander.modstitch.util.Platform
import dev.isxander.modstitch.util.platform
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.*
import javax.inject.Inject

interface ModstitchExtension {
    val minecraftVersion: Property<String>
    val javaTarget: Property<Int>

    val parchment: ParchmentBlock
    fun parchment(action: Action<ParchmentBlock>) = action.execute(parchment)

    val metadata: MetadataBlock
    fun metadata(action: Action<MetadataBlock>) = action.execute(metadata)

    val implementation: Configuration
    val compileOnly: Configuration
    val runtimeOnly: Configuration
    val localRuntime: Configuration

    val platform: Platform
    val isFabric: Boolean
    val isNeoForge: Boolean

    fun fabric(action: Action<BaseFabricExtension>)
    fun neoforge(action: Action<BaseNeoForgeExtension>)
}

open class ModstitchExtensionImpl @Inject constructor(objects: ObjectFactory, private val project: Project, private val remapConfigurations: RemapConfigurations) : ModstitchExtension {
    // General setup for the mod environment.
    override val minecraftVersion = objects.property<String>()
    override val javaTarget = objects.property<Int>()

    override val parchment = objects.newInstance<ParchmentBlockImpl>(objects)
    init { parchment.minecraftVersion.convention(minecraftVersion) }

    override val metadata = objects.newInstance<MetadataBlockImpl>(objects)

    // Abstracted configurations for mod-specific dependencies. Loom uses a `mod` prefix for configurations.
    override val implementation: Configuration
        get() = resolveConfiguration(remapConfigurations.implementation)
    override val compileOnly: Configuration
        get() = resolveConfiguration(remapConfigurations.compileOnly)
    override val runtimeOnly: Configuration
        get() = resolveConfiguration(remapConfigurations.runtimeOnly)
    override val localRuntime: Configuration
        get() = resolveConfiguration(remapConfigurations.localRuntime)

    private fun resolveConfiguration(resolver: ConfigurationResolver) =
        resolver(project.configurations).get()


    override val platform: Platform
        get() = project.platform
    override val isFabric: Boolean
        get() = platform == Platform.Fabric
    override val isNeoForge: Boolean
        get() = platform == Platform.NeoForge

    override fun fabric(action: Action<BaseFabricExtension>) {
        if (isFabric) {
            action.execute(project.extensions.getByType<BaseFabricExtension>())
        }
    }
    override fun neoforge(action: Action<BaseNeoForgeExtension>) {
        if (isNeoForge) {
            action.execute(project.extensions.getByType<BaseNeoForgeExtension>())
        }
    }
}

// ------------------------------------
// Inner classes for block organisation
// ------------------------------------
interface ParchmentBlock {
    val minecraftVersion: Property<String>
    val mappingsVersion: Property<String>
    val parchmentArtifact: Property<String>
    val enabled: Property<Boolean>
}
open class ParchmentBlockImpl @Inject constructor(objects: ObjectFactory) : ParchmentBlock {
    override val minecraftVersion = objects.property<String>()
    override val mappingsVersion = objects.property<String>()
    override val parchmentArtifact = objects.property<String>().convention(minecraftVersion.zip(mappingsVersion) { mc, mappings -> "org.parchmentmc.data:parchment-$mc:$mappings@zip" })
    override val enabled = objects.property<Boolean>().convention(parchmentArtifact.map { it.isNotEmpty() }.orElse(false))
}

interface MetadataBlock {
    val modId: Property<String>
    val modName: Property<String>
    val modVersion: Property<String>
    val modDescription: Property<String>
    val modLicense: Property<String>
    val modGroup: Property<String>
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
    override val replacementProperties = objects.mapProperty<String, String>().convention(emptyMap())
}

operator fun ModstitchExtension.invoke(block: ModstitchExtension.() -> Unit) = block()
val Project.modstitch: ModstitchExtension
    get() = extensions.getByType<ModstitchExtension>()

