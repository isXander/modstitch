package dev.isxander.modstitch.base.loom

import dev.isxander.modstitch.base.BaseCommonImpl
import dev.isxander.modstitch.base.modstitch
import dev.isxander.modstitch.util.Platform
import dev.isxander.modstitch.util.PlatformExtensionInfo
import dev.isxander.modstitch.util.addCamelCasePrefix
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources

class BaseLoomImpl : BaseCommonImpl<BaseLoomExtension>(Platform.Loom) {
    override val platformExtensionInfo = PlatformExtensionInfo(
        "msLoom",
        BaseLoomExtension::class,
        BaseLoomExtensionImpl::class,
        BaseLoomExtensionDummy::class
    )

    override fun apply(target: Project) {
        target.pluginManager.apply("fabric-loom")
        super.apply(target)

        val fabricExt = createRealPlatformExtension(target)!!

        target.dependencies {
            "minecraft"(target.modstitch.minecraftVersion.map { "com.mojang:minecraft:$it" })
            "mappings"(fabricExt.loomExtension.officialMojangMappings())

            "modImplementation"(fabricExt.fabricLoaderVersion.map { "net.fabricmc:fabric-loader:$it" })
        }

        target.afterEvaluate {
            if (target.modstitch.parchment.enabled.get()) {
                error("Parchment is not supported on Loom yet.")
            }
        }
    }

    override fun applyDefaultRepositories(repositories: RepositoryHandler) {
        super.applyDefaultRepositories(repositories)
        repositories.maven("https://maven.fabricmc.net") { name = "FabricMC" }
    }

    override fun applyMetadataStringReplacements(target: Project): TaskProvider<ProcessResources> {
        val generateModMetadata = super.applyMetadataStringReplacements(target)

        target.tasks.named("ideaSyncTask") {
            dependsOn("generateModMetadata")
        }

        return generateModMetadata
    }

    override fun createProxyConfigurations(target: Project, sourceSet: SourceSet) {
        if (sourceSet.name != SourceSet.MAIN_SOURCE_SET_NAME) {
            target.loom.createRemapConfigurations(sourceSet)
        }

        super.createProxyConfigurations(target, sourceSet)
    }

    override fun createProxyConfigurations(target: Project, configuration: Configuration) {
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
            configuration.extendsFrom(this@proxy)
        }
    }

    private val Project.loom: LoomGradleExtensionAPI
        get() = extensions.getByType<LoomGradleExtensionAPI>()
}