package dev.isxander.modstitch.base.neoforge

import dev.isxander.modstitch.base.BaseCommonImpl
import dev.isxander.modstitch.base.modstitch
import dev.isxander.modstitch.util.Platform
import dev.isxander.modstitch.util.PlatformExtensionInfo
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources
import org.slf4j.event.Level

object BaseNeoforgeImpl : BaseCommonImpl<BaseNeoForgeExtension>(Platform.NeoForge) {
    override val platformExtensionInfo = PlatformExtensionInfo(
        "neoforge",
        BaseNeoForgeExtension::class,
        BaseNeoForgeExtensionImpl::class,
        BaseNeoForgeExtensionDummy::class
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
        target.neoforge.modDevGradle {
            ideSyncTask(generateModMetadata)
        }

        return generateModMetadata
    }
}