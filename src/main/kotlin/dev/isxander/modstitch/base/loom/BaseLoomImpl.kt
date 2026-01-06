package dev.isxander.modstitch.base.loom

import dev.isxander.modstitch.base.BaseCommonImpl
import dev.isxander.modstitch.base.FutureNamedDomainObjectProvider
import dev.isxander.modstitch.base.extensions.modstitch
import dev.isxander.modstitch.util.*
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.util.Constants
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.junitplatform.JUnitPlatformOptions
import org.gradle.kotlin.dsl.*
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.getByType
import org.gradle.language.jvm.tasks.ProcessResources

class BaseLoomImpl(
    private val type: LoomType,
) : BaseCommonImpl<BaseLoomExtension>(
    type.platform,
    AppendFabricMetadataTask::class.java,
) {
    override val platformExtensionInfo = PlatformExtensionInfo(
        "msLoom",
        BaseLoomExtension::class,
        BaseLoomExtensionImpl::class,
        BaseLoomExtensionDummy::class
    )

    override fun apply(target: Project) {
        super.apply(target)

        val fabricExt = createRealPlatformExtension(target)!!

        target.dependencies {
            "minecraft"(target.modstitch.minecraftVersion.map { "com.mojang:minecraft:$it" })

            if (type == LoomType.Remap) {
                val parchment = target.modstitch.parchment
                val loom = fabricExt.loomExtension
                "mappings"(parchment.enabled.zip(parchment.parchmentArtifact.orElse("")) { enabled, parchmentArtifact ->
                    loom.layered {
                        officialMojangMappings()
                        if (enabled && parchmentArtifact.isNotEmpty()) {
                            parchment(parchmentArtifact)
                        }
                    }
                })
            }

            "modstitchModImplementation"(fabricExt.fabricLoaderVersion.map { "net.fabricmc:fabric-loader:$it" })
        }

        target.modstitch.modLoaderManifest.convention(Platform.Loom.modManifest)

        target.modstitch._namedJarTaskName = "jar"
        target.modstitch._finalJarTaskName = when (type) {
            LoomType.NoRemap -> "jar"
            LoomType.Remap -> "remapJar"
        }

        target.loom.mixin {
            target.modstitch.mixin.mixinSourceSets.whenObjectAdded {
                val sourceSetName = this@whenObjectAdded.sourceSetName
                val refmapName = this@whenObjectAdded.refmapName

                if (sourceSetName.get() == SourceSet.MAIN_SOURCE_SET_NAME) {
                    defaultRefmapName = refmapName
                } else {
                    add(sourceSetName.get(), refmapName.get())
                }
            }
        }
        target.modstitch.mixin.registerSourceSet(
            target.sourceSets["main"],
            target.modstitch.metadata.modId.map { "$it.refmap.json" },
        )

        applyRuns(target)
    }

    override fun applyClassTweaker(target: Project) {
        // (Un)fortunately, Loom doesn't fully utilize Gradle's task system and performs much of its logic
        // during the configuration phase - including applying a class tweaker to the Minecraft sources.
        // Thus, we need to generate it eagerly, right here and right now.
        val modstitch = target.modstitch
        val loom = target.loom

        // Loom doesn't offer a way to configure whether class tweaker validation should be enabled.
        // Fortunately, it uses a separate task for this purpose, so we can simply disable it when needed.
        if (!modstitch.validateClassTweaker.get()) {
            target.tasks["validateAccessWidener"].enabled = false
        }

        // If no class tweaker is specified, there's nothing else for us to do.
        val classTweakerFile = modstitch.classTweaker.orNull?.asFile ?: return

        // we need to know what namespace (either official or named) the class tweaker needs to be in
        val isUnobfuscated = modstitch.isUnobfuscated.orNull ?: true

        // Read the class tweaker from the specified path, convert it to the `classTweaker v1` format,
        // save it to a static location, and point Loom to it. If the specified file does not exist,
        // allow it to throw - we don't want to silently ignore a potential misconfiguration.
        //
        // Also note: we intentionally avoid using the user-provided name here to prevent leaving behind
        // stale cached files when the user changes the name of their class tweaker.
        val generateClassTweakerTask = target.tasks.register("generateClassTweaker") {
            group = "modstitch/internal"
            description = "Generates the class tweaker file for Loom"

            inputs.file(classTweakerFile)
            val tmpClassTweakerFile = target.layout.buildDirectory.file("modstitch/modstitch.ct").get().asFile
            outputs.file(tmpClassTweakerFile)

            inputs.property("targetNamespace", isUnobfuscated)

            doLast {
                // Read the class tweaker from the specified path, convert it to the `classTweaker v1` format,
                // save it to a static location. If the specified file does not exist,
                // allow it to throw - we don't want to silently ignore a potential misconfiguration.
                val classTweaker = classTweakerFile.reader().use { ClassTweaker.parse(it) }
                    .convertFormat(ClassTweakerFormat.CT)
                    .convertNamespace(if (isUnobfuscated) ClassTweakerNamespace.Official else ClassTweakerNamespace.Named)
                tmpClassTweakerFile.parentFile.mkdirs()
                tmpClassTweakerFile.writer().use { classTweaker.write(it) }
            }
        }

        // For Loom's configuration phase, we still need to provide the file path immediately
        // Create the file during configuration for Loom, but ensure it gets regenerated properly at execution time
        val tmpClassTweakerFile = target.layout.buildDirectory.file("modstitch/modstitch.ct").get().asFile

        // Generate the file immediately for Loom's configuration phase
        val classTweaker = classTweakerFile.reader().use { ClassTweaker.parse(it) }.convertFormat(ClassTweakerFormat.CT)
        tmpClassTweakerFile.parentFile.mkdirs()
        tmpClassTweakerFile.writer().use { classTweaker.write(it) }
        loom.accessWidenerPath = tmpClassTweakerFile

        // Finally, include the generated class tweaker in the final JAR.
        val defaultClassTweakerName = modstitch.metadata.modId.map { "$it.ct" }
        val classTweakerName = modstitch.classTweakerName.convention(defaultClassTweakerName).get()
        val classTweakerPath = classTweakerName.split('\\', '/')
        target.tasks.named<ProcessResources>("processResources") {
            dependsOn(generateClassTweakerTask)
            from(tmpClassTweakerFile) {
                rename { classTweakerPath.last() }
                into(classTweakerPath.dropLast(1).joinToString("/"))
            }
        }
    }

    private fun applyRuns(target: Project) {
        target.modstitch.runs.whenObjectAdded modstitch@{
            val modstitch = this@modstitch

            // loom run configs does not support gradle lazy evaluation
            target.afterSuccessfulEvaluate {
                target.extensions.getByType<LoomGradleExtensionAPI>().runs.register(modstitch.name) loom@{
                    val loom = this@loom

                    modstitch.gameDirectory.orNull?.let { loom.runDir = it.asFile.absolutePath }
                    modstitch.mainClass.orNull?.let { loom.mainClass = it }
                    modstitch.jvmArgs.orNull?.let { loom.vmArgs.addAll(it) }
                    modstitch.programArgs.orNull?.let { loom.programArgs.addAll(it) }
                    modstitch.environmentVariables.orNull?.let { loom.environmentVariables.putAll(it) }
                    modstitch.ideRunName.orNull?.let { loom.configName = it }
                    modstitch.ideRun.orNull?.let { loom.isIdeConfigGenerated = it }
                    modstitch.sourceSet.orNull?.let { loom.source(it) }
                    modstitch.side.orNull?.let {
                        when (it) {
                            Side.Client -> loom.client()
                            Side.Server -> loom.server()
                            else -> error("Unknown side: $side")
                        }
                    }
                    modstitch.datagen.orNull?.let { if (it) throw UnsupportedOperationException("Loom platform does not currently support creating datagen run configs") }
                }

                target.tasks.named("run${modstitch.name.replaceFirstChar { it.uppercaseChar() }}") {
                    group = "modstitch/runs"
                }
            }
        }
    }

    override fun applyPlugins(target: Project) {
        super.applyPlugins(target)
        when (type) {
            LoomType.NoRemap -> target.plugins.apply("net.fabricmc.fabric-loom")
            LoomType.Remap -> target.plugins.apply("net.fabricmc.fabric-loom-remap")
        }
    }

    override fun applyDefaultRepositories(repositories: RepositoryHandler) {
        super.applyDefaultRepositories(repositories)
        repositories.maven("https://maven.fabricmc.net") { name = "FabricMC" }
    }

    override fun applyMetadataStringReplacements(target: Project): TaskProvider<ProcessResources> {
        val generateModMetadata = super.applyMetadataStringReplacements(target)

        target.tasks.named("ideaSyncTask") {
            dependsOn(generateModMetadata)
        }

        return generateModMetadata
    }

    override fun applyUnitTesting(target: Project, testFrameworkConfigure: Action<in JUnitPlatformOptions>) {
        val fabricExt = target.extensions.getByType<BaseLoomExtension>()

        target.tasks.named<Test>("test") {
            useJUnitPlatform(testFrameworkConfigure);
        }

        target.dependencies {
            "testImplementation"(fabricExt.fabricLoaderVersion.map { "net.fabricmc:fabric-loader-junit:$it" })
        }
    }

    override fun createProxyConfigurations(target: Project, sourceSet: SourceSet) {
        if (sourceSet.name != SourceSet.MAIN_SOURCE_SET_NAME) {
            if (type == LoomType.Remap) {
                target.loom.createRemapConfigurations(sourceSet)
            }
        } else {
            createProxyConfigurations(target, FutureNamedDomainObjectProvider.from(target.configurations, Constants.Configurations.LOCAL_RUNTIME))
        }

        super.createProxyConfigurations(target, sourceSet)
    }

    override fun createProxyConfigurations(target: Project, configuration: FutureNamedDomainObjectProvider<Configuration>, defer: Boolean) {
        if (defer) error("Cannot defer proxy configuration creation in Loom")

        // for no-remap, this "remap configuration" is just the regular configuration
        // since there is no such remap configuration to proxy to.
        val remapConfigurationName = when (type) {
            LoomType.Remap -> target.loom.remapConfigurations
                .find { it.targetConfigurationName.get() == configuration.name }
                ?.name
                ?: error("Loom has not created a remap configuration for ${configuration.name}, modstitch cannot proxy it.")
            LoomType.NoRemap -> configuration.name
        }

        val proxyModConfigurationName = configuration.name.addCamelCasePrefix("modstitchMod")
        val proxyRegularConfigurationName = configuration.name.addCamelCasePrefix("modstitch")

        target.configurations.create(proxyModConfigurationName) proxy@{
            target.configurations.named(remapConfigurationName) {
                extendsFrom(this@proxy)
            }
        }
        target.configurations.create(proxyRegularConfigurationName) proxy@{
            configuration.get().extendsFrom(this@proxy)
        }
    }

    override fun configureJiJConfiguration(target: Project, configuration: Configuration) {
        target.configurations.named(Constants.Configurations.INCLUDE) {
            extendsFrom(configuration)
        }
    }

    private val Project.loom: LoomGradleExtensionAPI
        get() = extensions.getByType<LoomGradleExtensionAPI>()
}
