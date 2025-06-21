package dev.isxander.modstitch.base.loom

import dev.isxander.modstitch.base.BaseCommonImpl
import dev.isxander.modstitch.base.FutureNamedDomainObjectProvider
import dev.isxander.modstitch.base.extensions.modstitch
import dev.isxander.modstitch.util.*
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.util.Constants
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources

class BaseLoomImpl : BaseCommonImpl<BaseLoomExtension>(
    Platform.Loom,
    FMJAppendMixinDataTask::class.java,
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
            "mappings"(zip(parchment.enabled, parchment.parchmentArtifact.orElse("")) { enabled, parchmentArtifact ->
                loom.layered {
                    officialMojangMappings()
                    if (enabled && parchmentArtifact.isNotEmpty()) {
                        parchment(parchmentArtifact)
                    }
                }
            })

            "modImplementation"(fabricExt.fabricLoaderVersion.map { "net.fabricmc:fabric-loader:$it" })
        }

        target.modstitch._modLoaderManifest = Platform.Loom.modManifest

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
