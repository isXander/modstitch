package dev.isxander.modstitch.base.moddevgradle

import dev.isxander.modstitch.base.BaseCommonImpl
import dev.isxander.modstitch.base.modstitch
import dev.isxander.modstitch.util.Platform
import dev.isxander.modstitch.util.PlatformExtensionInfo
import dev.isxander.modstitch.util.addCamelCasePrefix
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources
import org.slf4j.event.Level

object BaseModdevgradleImpl : BaseCommonImpl<BaseModDevGradleExtension>(Platform.ModDevGradle) {
    override val platformExtensionInfo = PlatformExtensionInfo(
        "msModdevgradle",
        BaseModDevGradleExtension::class,
        BaseModDevGradleExtensionImpl::class,
        BaseModDevGradleExtensionDummy::class
    )

    override fun apply(target: Project) {
        val neoExt = createRealPlatformExtension(target, target)!!

        target.pluginManager.apply("net.neoforged.moddev")

        target.configurations.create("localRuntime") localRuntime@{
            target.configurations.named("runtimeOnly") {
                extendsFrom(this@localRuntime)
            }
        }

        super.apply(target)

        neoExt.modDevGradle {
            version = neoExt.neoForgeVersion

            parchment {
                parchmentArtifact = target.modstitch.parchment.parchmentArtifact
                enabled = target.modstitch.parchment.enabled
            }

            validateAccessTransformers = true

            runs {
                configureEach {
                    // Recommended practice per Neoforge MDK
                    logLevel = Level.DEBUG
                }
            }
        }
    }

    override fun applyMetadataStringReplacements(target: Project): TaskProvider<ProcessResources> {
        val generateModMetadata = super.applyMetadataStringReplacements(target)

        // Generate mod metadata every project reload, instead of manually
        // (see `generateModMetadata` task in `common.gradle.kts`)
        target.msModdevgradle.modDevGradle {
            ideSyncTask(generateModMetadata)
        }

        return generateModMetadata
    }

    override fun createProxyConfigurations(target: Project, configuration: Configuration) {
        val proxyModConfigurationName = configuration.name.addCamelCasePrefix("modstitchMod")
        val proxyRegularConfigurationName = configuration.name.addCamelCasePrefix("modstitch")

        // already created
        if (target.configurations.find { it.name == proxyModConfigurationName } != null) {
            return
        }

        target.configurations.create(proxyModConfigurationName) proxy@{
            configuration.extendsFrom(this@proxy)
        }

        target.configurations.create(proxyRegularConfigurationName) proxy@{
            configuration.extendsFrom(this@proxy)
            target.configurations.named("additionalRuntimeClasspath") {
                extendsFrom(this@proxy)
            }
        }
    }
}