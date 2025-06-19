package dev.isxander.modstitch.base.extensions

import dev.isxander.modstitch.base.*
import dev.isxander.modstitch.base.loom.BaseLoomExtension
import dev.isxander.modstitch.base.moddevgradle.BaseModDevGradleExtension
import dev.isxander.modstitch.base.moddevgradle.BaseModDevGradleExtensionImpl
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
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import java.util.*
import javax.inject.Inject

interface ModstitchExtension {
    /**
     * The mod loader version to target.
     *
     * - When target platform is `loom`, this property is equivalent to [fabricLoaderVersion].
     * - When target platform is `moddevgradle`, this property is equivalent to [neoForgeVersion].
     * - When target platform is `moddevgradle-legacy`, this property is equivalent to [forgeVersion].
     */
    var modLoaderVersion: String?

    /**
     * The version of Fabric Loader to use.
     *
     * Setting this property is loosely equivalent to:
     * ```
     * loom {
     *     fabricLoaderVersion = value
     * }
     * ```
     */
    var fabricLoaderVersion: String?

    /**
     * The version of NeoForge to use.
     *
     * Setting this property is loosely equivalent to:
     * ```
     * moddevgradle {
     *     enable {
     *         neoForgeVersion = value
     *     }
     * }
     * ```
     */
    var neoForgeVersion: String?

    /**
     * The version of Forge to use.
     *
     * Setting this property is loosely equivalent to:
     * ```
     * moddevgradle {
     *     enable {
     *         forgeVersion = value
     *     }
     * }
     * ```
     */
    var forgeVersion: String?

    /**
     * The version of NeoForm to use.
     *
     * Setting this property is loosely equivalent to:
     * ```
     * moddevgradle {
     *     enable {
     *         neoFormVersion = value
     *     }
     * }
     * ```
     */
    var neoFormVersion: String?

    /**
     * The version of MCP to use.
     *
     * Setting this property is loosely equivalent to:
     * ```
     * moddevgradle {
     *     enable {
     *         mcpVersion = value
     *     }
     * }
     * ```
     */
    var mcpVersion: String?

    /**
     * The version of Minecraft to target by this build.
     */
    val minecraftVersion: Property<String>

    /**
     * The Java version to target.
     */
    val javaTarget: Property<Int>

    /**
     * The Parchment configuration block.
     * Parchment is a parameter name mapping to compliment Official Mojang Mappings.
     */
    val parchment: ParchmentBlock

    /**
     * The Parchment configuration block.
     * Parchment is a parameter name mapping to compliment Official Mojang Mappings.
     */
    fun parchment(action: Action<ParchmentBlock>) = action.execute(parchment)

    /**
     * The metadata block.
     * Configures mod necessary and optional metadata about your mod.
     */
    val metadata: MetadataBlock
    fun metadata(action: Action<MetadataBlock>) = action.execute(metadata)

    /**
     * The mixin configuration block.
     * Configures Mixin settings for your mod, including registration of mixin configs.
     */
    val mixin: MixinBlock
    fun mixin(action: Action<MixinBlock>) = action.execute(mixin)

    /**
     * The mod loader manifest to use.
     * - On Loom, this is `fabric.mod.json`.
     * - On ModDevGradle (>1.20.4), this is `META-INF/neoforge.mods.toml`.
     * - On ModDevGradle (<1.20.5), this is `META-INF/mods.toml`.
     * - On ModDevGradle Legacy, this is `META-INF/mods.toml`.
     * - In environments where there is no mod loader manifest, like vanilla mode of MDG, this is an empty string.
     */
    val modLoaderManifest: String

    /**
     * Creates proxy configurations for the given configuration.
     * This is used to abstract the differences in platforms, where some may require
     * additional logic when the dependency is NOT a mod, and some may require additional logic
     * when the dependency IS a mod.
     *
     * By calling this function, you create two configurations.
     * For example, if you use `createProxyConfigurations(configurations.compile)`,
     * it will create `modstitchCompile` and `modstitchModCompile`.
     */
    fun createProxyConfigurations(configuration: Configuration)

    /**
     * Creates proxy configurations for common configurations in the given source set.
     * This is used to abstract the differences in platforms, where some may require
     * additional logic when the dependency is NOT a mod, and some may require additional logic
     * when the dependency IS a mod.
     *
     * This method is a shorthand for calling `createProxyConfigurations` on the configurations.
     * It creates them for the following configurations:
     * - `compileOnly` (`modstitchCompileOnly` and `modstitchModCompileOnly`)
     * - `implementation` (`modstitchImplementation` and `modstitchModImplementation`)
     * - `runtimeOnly` (`modstitchRuntimeOnly` and `modstitchModRuntimeOnly`)
     * - `compileOnlyApi` (`modstitchCompileOnlyApi` and `modstitchModCompileOnlyApi`)
     * - `api` (`modstitchApi` and `modstitchModApi`)
     *
     * @see createProxyConfigurations
     */
    fun createProxyConfigurations(sourceSet: SourceSet)

    /** The active platform for this project. */
    val platform: Platform

    /** Whether the active platform is Loom. */
    val isLoom: Boolean
    /** Whether the active platform is ModDevGradle or ModDevGradle Legacy. */
    val isModDevGradle: Boolean
    /** Whether the active platform is ModDevGradle. */
    val isModDevGradleRegular: Boolean
    /** Whether the active platform is ModDevGradle Legacy. */
    val isModDevGradleLegacy: Boolean

    /**
     * The jar task that will produce the final, production jar file for the platform.
     * This may be modified by extensions, like `shadow`.
     */
    val finalJarTask: TaskProvider<out Jar>

    val namedJarTask: TaskProvider<out Jar>

    /**
     * Configures the Loom extension.
     * The action is only executed if the active platform is Loom.
     */
    fun loom(action: Action<BaseLoomExtension>) {}

    /**
     * Configures the ModDevGradle extension.
     * The action is only executed if the active platform is ModDevGradle or ModDevGradle Legacy.
     */
    fun moddevgradle(action: Action<BaseModDevGradleExtension>) {}

    val templatesSourceDirectorySet: SourceDirectorySet

    /**
     * Called when the underlying platform plugin is fully applied.
     */
    fun onEnable(action: Action<Project>)
}

@Suppress("LeakingThis") // Extension must remain open for Gradle to inject the implementation. This is safe.
open class ModstitchExtensionImpl @Inject constructor(
    objects: ObjectFactory,
    @Transient private val project: Project,
    @Transient private val plugin: BaseCommonImpl<*>,
) : ModstitchExtension {
    // General setup for the mod environment.
    override var modLoaderVersion: String?
        get() = when (plugin.platform) {
            Platform.Loom -> fabricLoaderVersion
            Platform.MDG -> neoForgeVersion
            Platform.MDGLegacy -> forgeVersion
        }
        set(value) = when (plugin.platform) {
            Platform.Loom -> fabricLoaderVersion = value
            Platform.MDG -> neoForgeVersion = value
            Platform.MDGLegacy -> forgeVersion = value
        }

    override var fabricLoaderVersion: String?
        get() = platformExtension<BaseLoomExtension, String> { it.fabricLoaderVersion.orNull }
        set(value) = if (value != null) platformExtension<BaseLoomExtension> { fabricLoaderVersion = value } else {}

    override var neoForgeVersion: String?
        get() = platformExtension<BaseModDevGradleExtensionImpl, String> { it.enableConfiguration.neoForgeVersion }
        set(value) = if (value != null) platformExtension<BaseModDevGradleExtension> { enable { neoForgeVersion = value } } else {}

    override var forgeVersion: String?
        get() = platformExtension<BaseModDevGradleExtensionImpl, String> { it.enableConfiguration.forgeVersion }
        set(value) = if (value != null) platformExtension<BaseModDevGradleExtension> { enable { forgeVersion = value } } else {}

    override var neoFormVersion: String?
        get() = platformExtension<BaseModDevGradleExtensionImpl, String> { it.enableConfiguration.neoFormVersion }
        set(value) = if (value != null) platformExtension<BaseModDevGradleExtension> { enable { neoFormVersion = value } } else {}

    override var mcpVersion: String?
        get() = platformExtension<BaseModDevGradleExtensionImpl, String> { it.enableConfiguration.mcpVersion }
        set(value) = if (value != null) platformExtension<BaseModDevGradleExtension> { enable { mcpVersion = value } } else {}

    override val minecraftVersion = objects.property<String>()
    override val javaTarget = objects.property<Int>()

    override val parchment = objects.newInstance<ParchmentBlockImpl>(objects)
    init { parchment.minecraftVersion.convention(minecraftVersion) }

    override val metadata = objects.newInstance<MetadataBlockImpl>(objects)

    override val mixin = objects.newInstance<MixinBlockImpl>(objects)

    var _modLoaderManifest: String? = null
    override val modLoaderManifest
        get() = _modLoaderManifest ?: error("Mod loader manifest not set")

    override fun createProxyConfigurations(configuration: Configuration) =
        plugin.createProxyConfigurations(project, FutureNamedDomainObjectProvider.from(configuration), defer = false)
    override fun createProxyConfigurations(sourceSet: SourceSet) =
        plugin.createProxyConfigurations(project, sourceSet)

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

    private inline fun <reified T : Any, U> platformExtension(func: (T) -> U?) : U? {
        val platformExtension = plugin.platformExtension
        return if (platformExtension is T) func(platformExtension) else null
    }

    internal var _finalJarTaskName: String? = null
        set(value) {
            field = value
            println("Final jar task set to $value")
        }
    override val finalJarTask: TaskProvider<out Jar>
        get() = _finalJarTaskName?.let { project.tasks.named<Jar>(it) } ?: error("Final jar task not set")
    internal var _namedJarTaskName: String? = null
        set(value) {
            field = value
            println("Named jar task set to $value")
        }
    override val namedJarTask: TaskProvider<out Jar>
        get() = _namedJarTaskName?.let { project.tasks.named<Jar>(it) } ?: error("Named jar task not set")

    override val templatesSourceDirectorySet: SourceDirectorySet
        get() = project.extensions.getByType<SourceSetContainer>()["main"].extensions.getByName<SourceDirectorySet>("templates")

    override fun onEnable(action: Action<Project>) {
        plugin.onEnable(project, action)
    }
}

operator fun ModstitchExtension.invoke(block: ModstitchExtension.() -> Unit) = block()
internal val Project.modstitch: ModstitchExtensionImpl
    get() = extensions.getByType<ModstitchExtension>() as ModstitchExtensionImpl
