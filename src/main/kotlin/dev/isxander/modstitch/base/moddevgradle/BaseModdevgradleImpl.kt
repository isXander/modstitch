package dev.isxander.modstitch.base.moddevgradle

import dev.isxander.modstitch.base.BaseCommonImpl
import dev.isxander.modstitch.base.modstitch
import dev.isxander.modstitch.util.PlatformExtensionInfo
import dev.isxander.modstitch.util.addCamelCasePrefix
import net.neoforged.moddevgradle.legacyforge.dsl.Obfuscation
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources
import org.slf4j.event.Level

class BaseModdevgradleImpl(private val type: MDGType) : BaseCommonImpl<BaseModDevGradleExtension>(type.platform) {
    private lateinit var remapConfiguration: Configuration

    override val platformExtensionInfo = PlatformExtensionInfo(
        "msModdevgradle",
        BaseModDevGradleExtension::class,
        BaseModDevGradleExtensionImpl::class,
        BaseModDevGradleExtensionDummy::class,
    )

    override fun apply(target: Project) {
        val neoExt = createRealPlatformExtension(target, type)!!

        when (type) {
            MDGType.Regular -> target.pluginManager.apply("net.neoforged.moddev")
            MDGType.Legacy -> target.pluginManager.apply("net.neoforged.moddev.legacyforge")
        }

        target.configurations.create("localRuntime") localRuntime@{
            target.configurations.named("runtimeOnly") {
                extendsFrom(this@localRuntime)
            }
        }

        if (type == MDGType.Legacy) {
            // proxy configurations will add remap configurations to this
            remapConfiguration = target.configurations.create("modstitchMdgRemap")
            target.obfuscation.createRemappingConfiguration(remapConfiguration)
        }

        super.apply(target)

        neoExt.configureNeoforge {
            version = neoExt.forgeVersion
            neoFormVersion = neoExt.neoformVersion

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

        target.dependencies {
            if (type == MDGType.Legacy) {
                "annotationProcessor"("org.spongepowered:mixin:0.8.5:processor")
            }
        }
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

    override fun createProxyConfigurations(target: Project, configuration: Configuration) {
        val proxyModConfigurationName = configuration.name.addCamelCasePrefix("modstitchMod")
        val proxyRegularConfigurationName = configuration.name.addCamelCasePrefix("modstitch")

        // already created
        if (target.configurations.find { it.name == proxyModConfigurationName } != null) {
            return
        }

        target.configurations.create(proxyModConfigurationName) proxy@{
            configuration.extendsFrom(this@proxy)

            if (type == MDGType.Legacy)
                remapConfiguration.extendsFrom(this@proxy)
        }

        target.configurations.create(proxyRegularConfigurationName) proxy@{
            configuration.extendsFrom(this@proxy)
            target.configurations.named("additionalRuntimeClasspath") {
                extendsFrom(this@proxy)
            }
        }
    }

    override fun configureJiJConfiguration(target: Project, configuration: Configuration) {
        target.configurations.named("jarJar") {
            extendsFrom(configuration)
        }
    }

    private val Project.obfuscation: Obfuscation
        get() = if (type == MDGType.Legacy) extensions.getByType<Obfuscation>() else error("Obfuscation is not available in this context")
}