package dev.isxander.modstitch.base.extensions

import dev.isxander.modstitch.base.*
import dev.isxander.modstitch.base.loom.BaseLoomExtension
import dev.isxander.modstitch.base.moddevgradle.BaseModDevGradleExtension
import dev.isxander.modstitch.util.*
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources
import javax.inject.Inject

interface ModstitchExtension {
    val minecraftVersion: Property<String>
    val javaTarget: Property<Int>

    val parchment: ParchmentBlock
    fun parchment(action: Action<ParchmentBlock>) = action.execute(parchment)

    val metadata: MetadataBlock
    fun metadata(action: Action<MetadataBlock>) = action.execute(metadata)

    val mixin: MixinBlock
    fun mixin(action: Action<MixinBlock>) = action.execute(mixin)

    val modLoaderManifest: Property<String>

    fun createProxyConfigurations(configuration: Configuration)
    fun createProxyConfigurations(sourceSet: SourceSet)

    val platform: Platform
    val isLoom: Boolean
    val isModDevGradle: Boolean
    val isModDevGradleRegular: Boolean
    val isModDevGradleLegacy: Boolean

    fun loom(action: Action<BaseLoomExtension>) {}
    fun moddevgradle(action: Action<BaseModDevGradleExtension>) {}

    val templatesSourceDirectorySet: SourceDirectorySet

    fun onEnable(action: Action<Project>)
}

@Suppress("LeakingThis") // Extension must remain open for Gradle to inject the implementation. This is safe.
open class ModstitchExtensionImpl @Inject constructor(
    objects: ObjectFactory,
    @Transient private val project: Project,
    private val plugin: BaseCommonImpl<*>,
) : ModstitchExtension {
    // General setup for the mod environment.
    override val minecraftVersion = objects.property<String>()
    override val javaTarget = objects.property<Int>()

    override val parchment = objects.newInstance<ParchmentBlockImpl>(objects)
    init { parchment.minecraftVersion.convention(minecraftVersion) }

    override val metadata = objects.newInstance<MetadataBlockImpl>(objects)

    override val mixin = objects.newInstance<MixinBlockImpl>(objects)

    override val modLoaderManifest = objects.property<String>()

    override fun createProxyConfigurations(configuration: Configuration) = plugin.createProxyConfigurations(project, configuration)
    override fun createProxyConfigurations(sourceSet: SourceSet) = plugin.createProxyConfigurations(project, sourceSet)

    override val platform: Platform
        get() = plugin.platform
    override val isLoom: Boolean
        get() = platform.isLoom
    override val isModDevGradle: Boolean
        get() = platform.isModDevGradle
    override val isModDevGradleRegular: Boolean
        get() = platform.isModDevGradleRegular
    override val isModDevGradleLegacy: Boolean
        get() = platform.isModDevGradleLegacy

    override fun loom(action: Action<BaseLoomExtension>) = platformExtension(action)
    override fun moddevgradle(action: Action<BaseModDevGradleExtension>) = platformExtension(action)

    private inline fun <reified T : Any> platformExtension(action: Action<T>) {
        val platformExtension = plugin.platformExtension
        if (platformExtension is T) {
            action.execute(platformExtension)
        }
    }

    override val templatesSourceDirectorySet: SourceDirectorySet
        get() = project.extensions.getByType<SourceSetContainer>()["main"].extensions.getByName<SourceDirectorySet>("templates")

    override fun onEnable(action: Action<Project>) {
        plugin.onEnable(project, action)
    }
}

operator fun ModstitchExtension.invoke(block: ModstitchExtension.() -> Unit) = block()
val Project.modstitch: ModstitchExtension
    get() = extensions.getByType<ModstitchExtension>()
