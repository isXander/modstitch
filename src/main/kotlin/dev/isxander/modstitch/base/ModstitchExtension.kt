package dev.isxander.modstitch.base

/**
 * The "common" extension for all loaders, as much configuration is done in this extension
 * such as configuring Minecraft version, mappings, mixins etc.
 */

import dev.isxander.modstitch.base.loom.BaseLoomExtension
import dev.isxander.modstitch.base.moddevgradle.BaseModDevGradleExtension
import dev.isxander.modstitch.util.Platform
import dev.isxander.modstitch.util.platform
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.*
import javax.inject.Inject

interface ModstitchExtension {
    val minecraftVersion: Property<String>
    val javaTarget: Property<Int>

    val parchment: ParchmentBlock
    fun parchment(action: Action<ParchmentBlock>) = action.execute(parchment)

    val metadata: MetadataBlock
    fun metadata(action: Action<MetadataBlock>) = action.execute(metadata)

    fun createProxyConfigurations(configuration: Configuration)
    fun createProxyConfigurations(sourceSet: SourceSet)

    val platform: Platform
    val isLoom: Boolean
    val isModDevGradle: Boolean

    fun msLoom(action: Action<BaseLoomExtension>)
    fun msModdevgradle(action: Action<BaseModDevGradleExtension>)
}

@Suppress("LeakingThis") // Extension must remain open for Gradle to inject the implementation. This is safe.
open class ModstitchExtensionImpl @Inject constructor(
    objects: ObjectFactory,
    private val project: Project,
    private val plugin: BaseCommonImpl<*>,
) : ModstitchExtension {
    // General setup for the mod environment.
    override val minecraftVersion = objects.property<String>()
    override val javaTarget = objects.property<Int>()

    override val parchment = objects.newInstance<ParchmentBlockImpl>(objects)
    init { parchment.minecraftVersion.convention(minecraftVersion) }

    override val metadata = objects.newInstance<MetadataBlockImpl>(objects)

    override fun createProxyConfigurations(configuration: Configuration) = plugin.createProxyConfigurations(project, configuration)
    override fun createProxyConfigurations(sourceSet: SourceSet) = plugin.createProxyConfigurations(project, sourceSet)

    override val platform: Platform
        get() = project.platform
    override val isLoom: Boolean
        get() = platform == Platform.Loom
    override val isModDevGradle: Boolean
        get() = platform == Platform.ModDevGradle

    override fun msLoom(action: Action<BaseLoomExtension>) {
        if (isLoom) {
            action.execute(project.extensions.getByType<BaseLoomExtension>())
        }
    }
    override fun msModdevgradle(action: Action<BaseModDevGradleExtension>) {
        if (isModDevGradle) {
            action.execute(project.extensions.getByType<BaseModDevGradleExtension>())
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
@Suppress("LeakingThis") // Extension must remain open for Gradle to inject the implementation. This is safe.
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

operator fun ModstitchExtension.invoke(block: ModstitchExtension.() -> Unit) = block()
val Project.modstitch: ModstitchExtension
    get() = extensions.getByType<ModstitchExtension>()

