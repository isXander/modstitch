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
import org.gradle.language.jvm.tasks.ProcessResources

class BaseLoomImpl : BaseCommonImpl<BaseLoomExtension>(
    Platform.Loom,
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

            "modImplementation"(fabricExt.fabricLoaderVersion.map { "net.fabricmc:fabric-loader:$it" })
        }

        target.modstitch.modLoaderManifest.convention(Platform.Loom.modManifest)

        target.modstitch._finalJarTaskName = "remapJar"
        target.modstitch._namedJarTaskName = "jar"

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
    }

    override fun applyAccessWidener(target: Project) {
        // (Un)fortunately, Loom doesn't fully utilize Gradle's task system and performs much of its logic
        // during the configuration phase - including applying an access widener to the Minecraft sources.
        // Thus, we need to generate it eagerly, right here and right now.
        val modstitch = target.modstitch
        val loom = target.loom

        // Loom doesn't offer a way to configure whether access widener validation should be enabled.
        // Fortunately, it uses a separate task for this purpose, so we can simply disable it when needed.
        if (!modstitch.validateAccessWidener.get()) {
            target.tasks["validateAccessWidener"].enabled = false
        }

        // If no access widener is specified, there's nothing else for us to do.
        val accessWidenerFile = modstitch.accessWidener.orNull?.asFile ?: return

        // Read the access widener from the specified path, convert it to the `accessWidener v2` format,
        // save it to a static location, and point Loom to it. If the specified file does not exist,
        // allow it to throw - we don't want to silently ignore a potential misconfiguration.
        //
        // Also note: we intentionally avoid using the user-provided name here to prevent leaving behind
        // stale cached files when the user changes the name of their access widener.
        val generateAccessWidenerTask = target.tasks.register("generateAccessWidener") {
            group = "modstitch/internal"
            description = "Generates the access widener file for Loom"

            inputs.file(accessWidenerFile)
            val tmpAccessWidenerFile = target.layout.buildDirectory.file("modstitch/modstitch.accessWidener").get().asFile
            outputs.file(tmpAccessWidenerFile)

            doLast {
                // Read the access widener from the specified path, convert it to the `accessWidener v2` format,
                // save it to a static location. If the specified file does not exist,
                // allow it to throw - we don't want to silently ignore a potential misconfiguration.
                val accessWidener = accessWidenerFile.reader().use { AccessWidener.parse(it) }.convert(AccessWidenerFormat.AW_V2)
                tmpAccessWidenerFile.parentFile.mkdirs()
                tmpAccessWidenerFile.writer().use { accessWidener.write(it) }
            }
        }

        // For Loom's configuration phase, we still need to provide the file path immediately
        // Create the file during configuration for Loom, but ensure it gets regenerated properly at execution time
        val tmpAccessWidenerFile = target.layout.buildDirectory.file("modstitch/modstitch.accessWidener").get().asFile

        // Generate the file immediately for Loom's configuration phase
        val accessWidener = accessWidenerFile.reader().use { AccessWidener.parse(it) }.convert(AccessWidenerFormat.AW_V2)
        tmpAccessWidenerFile.parentFile.mkdirs()
        tmpAccessWidenerFile.writer().use { accessWidener.write(it) }
        loom.accessWidenerPath = tmpAccessWidenerFile

        // Finally, include the generated access widener in the final JAR.
        val defaultAccessWidenerName = modstitch.metadata.modId.map { "$it.accessWidener" }
        val accessWidenerName = modstitch.accessWidenerName.convention(defaultAccessWidenerName).get()
        val accessWidenerPath = accessWidenerName.split('\\', '/')
        target.tasks.named<ProcessResources>("processResources") {
            dependsOn(generateAccessWidenerTask)
            from(tmpAccessWidenerFile) {
                rename { accessWidenerPath.last() }
                into(accessWidenerPath.dropLast(1).joinToString("/"))
            }
        }
    }

    override fun applyPlugins(target: Project) {
        super.applyPlugins(target)
        target.plugins.apply("fabric-loom")
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
            target.loom.createRemapConfigurations(sourceSet)
        } else {
            createProxyConfigurations(target, FutureNamedDomainObjectProvider.from(target.configurations, Constants.Configurations.LOCAL_RUNTIME))
        }

        super.createProxyConfigurations(target, sourceSet)
    }

    override fun createProxyConfigurations(target: Project, configuration: FutureNamedDomainObjectProvider<Configuration>, defer: Boolean) {
        if (defer) error("Cannot defer proxy configuration creation in Loom")

        val remapConfiguration = target.loom.remapConfigurations
            .find { it.targetConfigurationName.get() == configuration.name }
            ?: error("Loom has not created a remap configuration for ${configuration.name}, modstitch cannot proxy it.")

        val proxyModConfigurationName = configuration.name.addCamelCasePrefix("modstitchMod")
        val proxyRegularConfigurationName = configuration.name.addCamelCasePrefix("modstitch")

        target.configurations.create(proxyModConfigurationName) proxy@{
            target.configurations.named(remapConfiguration.name) {
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
