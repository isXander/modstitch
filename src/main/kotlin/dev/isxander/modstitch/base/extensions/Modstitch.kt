package dev.isxander.modstitch.base.extensions

import dev.isxander.modstitch.base.*
import dev.isxander.modstitch.base.loom.BaseLoomExtension
import dev.isxander.modstitch.base.moddevgradle.BaseModDevGradleExtension
import dev.isxander.modstitch.base.retrofuturagradle.BaseRetroFuturaGradleExtension
import dev.isxander.modstitch.util.*
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.junitplatform.JUnitPlatformOptions
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import javax.inject.Inject

interface ModstitchExtension {
    /**
     * The mod loader version to target.
     *
     * - When target platform is `loom`, this property is equivalent to `loom.fabricLoaderVersion`.
     * - When target platform is `moddevgradle`, this property is equivalent to `moddevgradle.neoForgeVersion`.
     * - When target platform is `moddevgradle-legacy`, this property is equivalent to `moddevgradle.forgeVersion`.
     * - When target platform is `retrofuturagradle`, this property is equivalent to `retrofuturagradle.forgeVersion`.
     *
     *   On retrofuturagradle, the property only serves as a check, because retrofuturagradle forces specific forge
     *   versions
     */
    val modLoaderVersion: Property<String>

    /**
     * The version of Minecraft to target by this build.
     */
    val minecraftVersion: Property<String>

    /**
     * Indicates whether the assigned [minecraftVersion] is an unobfuscated version.
     * This is unable to detect the special unobfuscated aside versions published before `26.1-snapshot-1`
     */
    val isUnobfuscated: Provider<Boolean>

    /**
     * The Java version to target.
     *
     * - Defaults to `21` if [minecraftVersion] >= `1.20.5`.
     * - Defaults to `17` if [minecraftVersion] >= `1.18`.
     * - Defaults to `16` if [minecraftVersion] >= `1.17`.
     * - Defaults to  `8` if [minecraftVersion] <= `1.16.5`.
     * - Has no default value if [minecraftVersion] denotes a snapshot version.
     *
     * Modstitch configures the java plugin's source and target versions. Expands to:
     * ```kt
     * java {
     *     targetCompatibility = modstitch.javaVersion
     *     sourceCompatibility = modstitch.javaVersion
     * }
     * ```
     */
    val javaVersion: Property<Int>

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

    val runs: NamedDomainObjectContainer<RunConfig>
    fun runs(action: Action<NamedDomainObjectContainer<RunConfig>>) = action.execute(runs)

    /**
     * The class tweaker to be applied to the Minecraft source code.
     *
     * Despite the name of this property, Modstitch supports both Fabric's Access Widener / Class Tweaker
     * and (Neo)Forge's Access Transformer. Modstitch will automatically detect which you have
     * specified and convert accordingly.
     *
     * It is recommended that you write your loader-agnostic class tweaker file in
     * [Fabric's AW v1 format](https://wiki.fabricmc.net/tutorial:accesswideners)
     * format, since it's the lowest common denominator: both ATs and AW(v2) and CTs supports all of AW(v1)'s features.
     *
     * By default, Modstitch looks for the following files (case-insensitive) in the specified order:
     * - `modstitch.ct`
     * - `.classTweaker`
     * - `accesstransformer.cfg`
     *
     * Modstitch looks deeply within your Gradle project structure. It will first check
     * within the root directory of this subproject, then the root directory of the parent project, and so on.
     */
    val classTweaker: RegularFileProperty

    /**
     * The path, relative to the root of resulting JAR's resources, where [classTweaker] will be copied.
     *
     * - On Loom, this defaults to `${metadata.modId}.ct`.
     * - On ModDevGradle, this defaults to `META-INF/accesstransformer.cfg`.
     *
     * In most cases you don't need to change this value.
     */
    val classTweakerName: Property<String>

    /**
     * Indicates whether [classTweaker] should be validated.
     *
     * Validation fails with a fatal error if any of the targeted members do not exist.
     * If the [classTweaker] is syntactically invalid, the build will fail regardless of the set value.
     *
     * Defaults to `false`.
     */
    val validateClassTweaker: Property<Boolean>

    /**
     * Configures JUnit testing that includes the Minecraft sources.
     */
    fun unitTesting(testFrameworkConfigure: Action<in JUnitPlatformOptions>)

    /**
     * Configures JUnit testing that includes the Minecraft sources.
     */
    fun unitTesting(testFrameworkConfigure: JUnitPlatformOptions.() -> Unit = {}) =
        unitTesting(Action { testFrameworkConfigure() })

    /**
     * The mod loader manifest to use.
     * - On Loom, this defaults to `fabric.mod.json`.
     * - On ModDevGradle (>=1.20.5), this defaults to `META-INF/neoforge.mods.toml`.
     * - On ModDevGradle (<1.20.5), this defaults to `META-INF/mods.toml`.
     * - On ModDevGradle Legacy, this defaults to `META-INF/mods.toml`.
     * - In environments without a mod loader manifest, this property has no value.
     */
    val modLoaderManifest: Property<String>

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
     * This method is a shorthand for calling `createProxyConfigurations` on the following configurations:
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

    /**
     * Configures the RetroFuturaGradle extension
     * The action is only executed if the active platform is RetroFuturaGradle
     */
    fun retrofuturagradle(action: Action<BaseRetroFuturaGradleExtension>) {}

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
    override val modLoaderVersion: Property<String> get() = when (plugin.platform) {
        Platform.Loom, Platform.LoomRemap -> project.extensions.getByType<BaseLoomExtension>().fabricLoaderVersion
        Platform.MDG -> project.extensions.getByType<BaseModDevGradleExtension>().neoForgeVersion
        Platform.MDGLegacy -> project.extensions.getByType<BaseModDevGradleExtension>().forgeVersion
        Platform.RFG -> project.extensions.getByType<BaseRetroFuturaGradleExtension>().forgeVersion
    }

    override val minecraftVersion = objects.property<String>()

    override val isUnobfuscated = minecraftVersion.map { v ->
        MinecraftVersion.parseOrderableOrNull(v) is MinecraftVersion.Unobfuscated
    }

    override val javaVersion = objects.property<Int>().convention(minecraftVersion.map { v ->
        // https://minecraft.wiki/w/Tutorial:Update_Java
        MinecraftVersion.parseOrderableOrNull(v)?.let { return@map when {
            it >= minecraftVersion("26.1-snapshot-1") -> 25
            it >= minecraftVersion("1.20.5") -> 21
            it >= minecraftVersion("1.18") -> 17
            it >= minecraftVersion("1.17") -> 16
            else -> 8
        }}
        MinecraftVersion.parseLegacySnapshotOrNull(v)?.let { return@map when {
            it >= minecraftLegacySnapshot("24w14a") -> 21
            it >= minecraftLegacySnapshot("21w44a") -> 17
            it >= minecraftLegacySnapshot("21w19a") -> 16
            else -> 8
        }}
    })

    override val parchment = objects.newInstance<ParchmentBlockImpl>(objects)
    init { parchment.minecraftVersion.convention(minecraftVersion) }

    override val metadata = objects.newInstance<MetadataBlockImpl>(objects)

    override val mixin = objects.newInstance<MixinBlockImpl>(objects)

    override val runs = objects.domainObjectContainer(RunConfig::class)

    override val classTweaker = objects.fileProperty().convention(project.layout.file(project.provider {
        val fileNames = listOf("modstitch.ct", ".classTweaker", "accesstransformer.cfg")
        project.projectChain.flatMap { p -> fileNames.map { p.projectDir to it } }.firstNotNullOfOrNull {
            it.first.listFiles().firstOrNull { f -> it.second.equals(f.name, ignoreCase = true) }?.absoluteFile
        }
    }))

    override val classTweakerName = objects.property<String>()

    override val validateClassTweaker = objects.property<Boolean>().convention(false)

    override fun unitTesting(testFrameworkConfigure: Action<in JUnitPlatformOptions>) {
        plugin.applyUnitTesting(project, testFrameworkConfigure)
    }

    override val modLoaderManifest = objects.property<String>()

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
    override fun retrofuturagradle(action: Action<BaseRetroFuturaGradleExtension>) = platformExtension(action)

    private inline fun <reified T : Any> platformExtension(action: Action<T>) {
        val platformExtension = plugin.platformExtension
        if (platformExtension is T) {
            action.execute(platformExtension)
        }
    }

    internal var _finalJarTaskName: String? = null
        set(value) {
            field = value
        }
    override val finalJarTask: TaskProvider<out Jar>
        get() = _finalJarTaskName?.let { project.tasks.named<Jar>(it) } ?: error("Final jar task not set")
    internal var _namedJarTaskName: String? = null
        set(value) {
            field = value
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
