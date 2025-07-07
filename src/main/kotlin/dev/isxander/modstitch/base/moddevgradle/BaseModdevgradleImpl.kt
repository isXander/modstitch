package dev.isxander.modstitch.base.moddevgradle

import dev.isxander.modstitch.base.BaseCommonImpl
import dev.isxander.modstitch.base.FutureNamedDomainObjectProvider
import dev.isxander.modstitch.base.extensions.modstitch
import dev.isxander.modstitch.util.Platform
import dev.isxander.modstitch.util.PlatformExtensionInfo
import dev.isxander.modstitch.util.addCamelCasePrefix
import dev.isxander.modstitch.util.afterSuccessfulEvaluate
import dev.isxander.modstitch.util.mainSourceSet
import net.neoforged.moddevgradle.dsl.ModDevExtension
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import net.neoforged.moddevgradle.legacyforge.dsl.LegacyForgeExtension
import net.neoforged.moddevgradle.legacyforge.dsl.MixinExtension
import net.neoforged.moddevgradle.legacyforge.dsl.ObfuscationExtension
import net.neoforged.nfrtgradle.CreateMinecraftArtifacts
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*
import org.gradle.kotlin.dsl.assign
import org.gradle.language.jvm.tasks.ProcessResources
import org.semver4j.Semver
import org.slf4j.event.Level
import kotlin.collections.dropLast
import kotlin.collections.last

class BaseModdevgradleImpl(
    private val type: MDGType
) : BaseCommonImpl<BaseModDevGradleExtension>(
    type.platform,
    AppendNeoForgeMetadataTask::class.java,
) {
    private lateinit var remapConfiguration: Configuration

    override val platformExtensionInfo = PlatformExtensionInfo(
        "msModdevgradle",
        BaseModDevGradleExtension::class,
        BaseModDevGradleExtensionImpl::class,
        BaseModDevGradleExtensionDummy::class,
    )

    override fun apply(target: Project) {
        createRealPlatformExtension(target, type)!!
        super.apply(target)

        val modstitch = target.modstitch
        modstitch._namedJarTaskName = "jar"
        modstitch._finalJarTaskName = if (type == MDGType.Legacy) "reobfJar" else "jar"

        val moddev = target.extensions.getByType<ModDevExtension>()
        moddev.parchment.parchmentArtifact = modstitch.parchment.parchmentArtifact
        moddev.parchment.enabled = modstitch.parchment.enabled
        moddev.runs.configureEach { logLevel = Level.DEBUG }
        moddev.mods.register("mod") { sourceSet(target.sourceSets["main"]) }

        target.configurations.create("localRuntime") localRuntime@{
            target.configurations.named("runtimeOnly") {
                extendsFrom(this@localRuntime)
            }
        }

        val obfuscation = target.extensions.findByType<ObfuscationExtension>()
        if (obfuscation != null) {
            // Proxy configurations will add remap configurations to this.
            remapConfiguration = target.configurations.create("modstitchMdgRemap")
            obfuscation.createRemappingConfiguration(remapConfiguration)
        }

        val mixin = target.extensions.findByType<MixinExtension>()
        if (mixin != null) {
            configureLegacyMixin(target, mixin)
        }
    }

    override fun finalize(target: Project) {
        enable(target)
        super.finalize(target)
    }

    override fun applyAccessWidener(target: Project) {
        val modstitch = target.modstitch
        val moddev = target.extensions.getByType<ModDevExtension>()
        val obf = target.extensions.findByType<ObfuscationExtension>()
        @Suppress("UnstableApiUsage") val namedToSrgMappings = obf?.namedToSrgMappings ?: target.provider { null }
        @Suppress("UnstableApiUsage") val srgToNamedMappings = obf?.srgToNamedMappings ?: target.provider { null }

        val defaultAccessTransformerName = "META-INF/accesstransformer.cfg"
        val generatedAccessTransformer = target.layout.buildDirectory.file("modstitch/$defaultAccessTransformerName").zip(modstitch.accessWidener) { x, _ -> x }
        val generatedAccessTransformers = generatedAccessTransformer.map { listOf(it) }.orElse(listOf())
        val accessWidenerName = modstitch.accessWidenerName.convention(defaultAccessTransformerName).map { when {
            type == MDGType.Regular || it == defaultAccessTransformerName -> it
            else -> error("Forge does not support custom access transformer paths.")
        }}
        val accessWidenerPath = accessWidenerName.map { it.split('\\', '/') }

        // Finalize our properties so that no further changes can be made to them after they've been read.
        modstitch.accessWidener.finalizeValueOnRead()
        modstitch.accessWidenerName.finalizeValueOnRead()
        modstitch.validateAccessWidener.finalizeValueOnRead()

        // Wire MDG to use our properties.
        moddev.validateAccessTransformers = modstitch.validateAccessWidener
        moddev.accessTransformers.from(generatedAccessTransformers)

        val createMinecraftArtifacts = target.tasks.getByName<CreateMinecraftArtifacts>("createMinecraftArtifacts")
        val createMinecraftMappings = target.tasks.register<CreateMinecraftMappingsTask>("createMinecraftMappings") {
            group = "modstitch/internal"
            description = "Creates Minecraft mappings by invoking NFRT."
            project.configurations.filter { it.name.startsWith("neoFormRuntimeDependencies") }.forEach(this::addArtifactsToManifest)

            enableCache.set(createMinecraftArtifacts.enableCache)
            analyzeCacheMisses.set(createMinecraftArtifacts.analyzeCacheMisses)
            toolsJavaExecutable.set(createMinecraftArtifacts.toolsJavaExecutable)
            neoForgeArtifact.set(createMinecraftArtifacts.neoForgeArtifact)
            neoFormArtifact.set(createMinecraftArtifacts.neoFormArtifact)
            namedToIntermediaryMappings.set(namedToSrgMappings)
            intermediaryToNamedMappings.set(srgToNamedMappings)
        }

        val generateAccessTransformer = target.tasks.register<GenerateAccessTransformerTask>("generateAccessTransformer") {
            group = "modstitch/internal"
            description = "Generates an access transformer."
            dependsOn(createMinecraftMappings)

            accessWidener.set(modstitch.accessWidener)
            mappings.set(namedToSrgMappings)
            accessTransformer.set(generatedAccessTransformer)
        }

        createMinecraftArtifacts.dependsOn(generateAccessTransformer)

        target.tasks.named<ProcessResources>("processResources") {
            dependsOn(generateAccessTransformer)
            from(generatedAccessTransformers) {
                rename { accessWidenerPath.get().last() }
                into(accessWidenerPath.map { it.dropLast(1).joinToString("/") })
            }
        }
    }

    /**
     * Enables the underlying [ModDevExtension].
     *
     * @param target The project for which the ModDev extension is to be enabled.
     */
    private fun enable(target: Project) {
        val modstitch = target.modstitch
        val moddev = target.extensions.getByType<BaseModDevGradleExtension>()
        val neoForge = target.extensions.findByType<NeoForgeExtension>()
        val legacyForge = target.extensions.findByType<LegacyForgeExtension>()

        moddev.neoForgeVersion.finalizeValueOnRead()
        moddev.neoFormVersion.finalizeValueOnRead()
        moddev.forgeVersion.finalizeValueOnRead()
        moddev.mcpVersion.finalizeValueOnRead()

        modstitch._modLoaderManifest = when {
            moddev.neoForgeVersion.isPresent -> {
                if (Semver.coerce(moddev.neoForgeVersion.get())?.satisfies("<20.5") == true) {
                    Platform.MDGLegacy.modManifest
                } else {
                    Platform.MDG.modManifest
                }
            }
            moddev.forgeVersion.isPresent -> Platform.MDGLegacy.modManifest
            else -> "" // MCP and NeoForm don't have a manifest.
        }

        neoForge?.enable {
            version = moddev.neoForgeVersion.orNull
            neoFormVersion = moddev.neoFormVersion.orNull
            moddev.forgeVersion.map { error("Property 'forgeVersion' does not exist.") }.orNull
            moddev.mcpVersion.map { error("Property 'mcpVersion' does not exist.") }.orNull
        }
        legacyForge?.enable {
            forgeVersion = moddev.forgeVersion.orNull
            neoForgeVersion = moddev.neoForgeVersion.orNull
            mcpVersion = moddev.mcpVersion.orNull
            moddev.neoForgeVersion.map { error("Property 'neoForgeVersion' does not exist.") }.orNull
        }
    }

    override fun applyPlugins(target: Project) {
        super.applyPlugins(target)
        target.pluginManager.apply(when (type) {
            MDGType.Regular -> "net.neoforged.moddev"
            MDGType.Legacy -> "net.neoforged.moddev.legacyforge"
        })
    }

    override fun applyMetadataStringReplacements(target: Project): TaskProvider<ProcessResources> {
        val generateModMetadata = super.applyMetadataStringReplacements(target)

        // Generate mod metadata every project reload, instead of manually
        // (see `generateModMetadata` task in `common.gradle.kts`)
        target.msModdevgradle.configureNeoforge {
            ideSyncTask(generateModMetadata)
        }

        return generateModMetadata
    }

    override fun createProxyConfigurations(target: Project, configuration: FutureNamedDomainObjectProvider<Configuration>, defer: Boolean) {
        val proxyModConfigurationName = configuration.name.addCamelCasePrefix("modstitchMod")
        val proxyRegularConfigurationName = configuration.name.addCamelCasePrefix("modstitch")

        // already created
        if (target.configurations.find { it.name == proxyModConfigurationName } != null) {
            return
        }

        fun deferred(action: (Configuration) -> Unit) {
            if (!defer) return action(configuration.get())
            return target.afterSuccessfulEvaluate { action(configuration.get()) }
        }

        target.configurations.create(proxyModConfigurationName) proxy@{
            deferred {
                it.extendsFrom(this@proxy)
            }

            target.afterSuccessfulEvaluate {
                if (type == MDGType.Legacy) {
                    remapConfiguration.extendsFrom(this@proxy)
                }
            }
        }

        target.configurations.create(proxyRegularConfigurationName) proxy@{
            deferred {
                it.extendsFrom(this@proxy)
            }

            target.afterSuccessfulEvaluate {
                target.configurations.named("additionalRuntimeClasspath") {
                    extendsFrom(this@proxy)
                }
            }
        }
    }

    override fun configureJiJConfiguration(target: Project, configuration: Configuration) {
        target.afterSuccessfulEvaluate {
            target.configurations.named("jarJar") {
                extendsFrom(configuration)
            }
        }
    }

    override fun createProxyConfigurations(target: Project, sourceSet: SourceSet) {
        super.createProxyConfigurations(target, sourceSet)

        if (sourceSet.name == SourceSet.MAIN_SOURCE_SET_NAME) {
            createProxyConfigurations(target, FutureNamedDomainObjectProvider.from(target.configurations, "localRuntime"), defer = true)
        }
    }

    /**
     * Configures mixins for Legacy Forge.
     *
     * @param target The project for which mixins are to be configured.
     * @param mixin The mixin extension used to generate refmaps.
     */
    private fun configureLegacyMixin(target: Project, mixin: MixinExtension) {
        val modstitch = target.modstitch
        val stitchedMixin = modstitch.mixin

        target.dependencies {
            "annotationProcessor"("org.spongepowered:mixin:0.8.5:processor")
        }

        stitchedMixin.mixinSourceSets.whenObjectAdded obj@{
            mixin.add(target.sourceSets[this@obj.sourceSetName.get()], this@obj.refmapName.get())
        }
        stitchedMixin.configs.whenObjectAdded obj@{ mixin.configs.add(this@obj.config) }
        stitchedMixin.registerSourceSet(target.mainSourceSet!!, modstitch.metadata.modId.map { "$it.refmap.json" })

        target.afterEvaluate {
            modstitch.namedJarTask {
                manifest.attributes["MixinConfigs"] = stitchedMixin.configs.joinToString(",") { it.config.get() }
            }
        }
    }

    override fun onEnable(target: Project, action: Action<Project>) {
        target.afterSuccessfulEvaluate(action)
    }
}
