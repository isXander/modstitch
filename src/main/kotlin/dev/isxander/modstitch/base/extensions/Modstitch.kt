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

    val generateModMetadataTask: TaskProvider<ProcessResources>
    val templatesSourceDirectorySet: SourceDirectorySet
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

    override val mixin = objects.newInstance<MixinBlockImpl>(objects)

    override val modLoaderManifest = objects.property<String>()

    override fun createProxyConfigurations(configuration: Configuration) = plugin.createProxyConfigurations(project, configuration)
    override fun createProxyConfigurations(sourceSet: SourceSet) = plugin.createProxyConfigurations(project, sourceSet)

    override val platform: Platform
        get() = project.platform
    override val isLoom: Boolean
        get() = project.isLoom
    override val isModDevGradle: Boolean
        get() = project.isModDevGradle
    override val isModDevGradleRegular: Boolean
        get() = project.isModDevGradleRegular
    override val isModDevGradleLegacy: Boolean
        get() = project.isModDevGradleLegacy

    override fun loom(action: Action<BaseLoomExtension>) {
        if (project.isLoom) {
            action.execute(project.extensions.getByType<BaseLoomExtension>())
        }
    }
    override fun moddevgradle(action: Action<BaseModDevGradleExtension>) {
        if (project.isModDevGradle) {
            action.execute(project.extensions.getByType<BaseModDevGradleExtension>())
        }
    }

    override val generateModMetadataTask: TaskProvider<ProcessResources>
        get() = project.tasks.getByName<TaskProvider<ProcessResources>>("generateModMetadata")
    override val templatesSourceDirectorySet: SourceDirectorySet
        get() = project.extensions.getByType<SourceSetContainer>()["main"].extensions.getByName<SourceDirectorySet>("templates")
}

operator fun ModstitchExtension.invoke(block: ModstitchExtension.() -> Unit) = block()
val Project.modstitch: ModstitchExtension
    get() = extensions.getByType<ModstitchExtension>()
