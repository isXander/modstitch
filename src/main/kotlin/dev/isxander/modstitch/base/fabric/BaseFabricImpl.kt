package dev.isxander.modstitch.base.fabric

import dev.isxander.modstitch.base.BaseCommonImpl
import dev.isxander.modstitch.base.RemapConfigurations
import dev.isxander.modstitch.base.modstitch
import dev.isxander.modstitch.util.Platform
import dev.isxander.modstitch.util.PlatformExtensionInfo
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources

object BaseFabricImpl : BaseCommonImpl<BaseFabricExtension>(Platform.Fabric) {
    override val platformExtensionInfo = PlatformExtensionInfo(
        "fabric",
        BaseFabricExtension::class,
        BaseFabricExtensionImpl::class,
        BaseFabricExtensionDummy::class
    )

    override fun apply(target: Project) {
        target.pluginManager.apply("fabric-loom")
        super.apply(target)

        val fabricExt = createRealPlatformExtension(target)!!

        target.dependencies {
            "minecraft"(target.modstitch.minecraftVersion.map { "com.mojang:minecraft:$it" })
            "mappings"(fabricExt.loom.officialMojangMappings())

            "modImplementation"(fabricExt.fabricLoaderVersion.map { "net.fabricmc:fabric-loader:$it" })
        }

        target.afterEvaluate {
            if (target.modstitch.parchment.enabled.get()) {
                error("Parchment is not supported on Fabric yet.")
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

    override val remapConfigurations = RemapConfigurations(
        implementation = { named("modImplementation") },
        compileOnly = { named("modCompileOnly") },
        runtimeOnly = { named("modRuntimeOnly") },
        localRuntime = { named("modLocalRuntime") },
    )
}