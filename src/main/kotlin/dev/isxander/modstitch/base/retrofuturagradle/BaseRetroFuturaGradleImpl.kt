package dev.isxander.modstitch.base.retrofuturagradle

import com.gtnewhorizons.retrofuturagradle.MinecraftExtension
import com.gtnewhorizons.retrofuturagradle.UserDevPlugin
import com.gtnewhorizons.retrofuturagradle.mcp.JSTTransformerTask
import com.gtnewhorizons.retrofuturagradle.mcp.MCPTasks
import com.gtnewhorizons.retrofuturagradle.mcp.ReobfuscatedJar
import com.gtnewhorizons.retrofuturagradle.minecraft.RunMinecraftTask
import com.gtnewhorizons.retrofuturagradle.modutils.ModUtils
import com.gtnewhorizons.retrofuturagradle.util.Distribution
import dev.isxander.modstitch.base.BaseCommonImpl
import dev.isxander.modstitch.base.FutureNamedDomainObjectProvider
import dev.isxander.modstitch.base.extensions.ModstitchExtension
import dev.isxander.modstitch.base.extensions.modstitch
import dev.isxander.modstitch.base.moddevgradle.GenerateAccessTransformerTask
import dev.isxander.modstitch.util.*
import net.neoforged.srgutils.IMappingFile
import org.gradle.api.*
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.FileCollectionDependency
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.internal.tasks.JvmConstants
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.testing.junitplatform.JUnitPlatformOptions
import org.gradle.jvm.tasks.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.kotlin.dsl.*
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.language.jvm.tasks.ProcessResources
import xyz.wagyourtail.jvmdg.gradle.JVMDowngraderPlugin
import xyz.wagyourtail.jvmdg.gradle.jvmdg

class BaseRetroFuturaGradleImpl : BaseCommonImpl<BaseRetroFuturaGradleExtension>(
    Platform.RFG,
    AppendRFGMetadataTask::class.java
) {

    val nonDowngradedRegularConfigurations = mutableSetOf<String>()

    override val platformExtensionInfo: PlatformExtensionInfo<BaseRetroFuturaGradleExtension> = PlatformExtensionInfo(
        "msRetroFuturaGradle",
        BaseRetroFuturaGradleExtension::class,
        BaseRetroFuturaGradleExtensionImpl::class,
        BaseRetroFuturaGradleExtensionDummy::class
    )

    override fun applyPlugins(target: Project) {
        super.applyPlugins(target)
        target.pluginManager.apply(UserDevPlugin::class.java)
        target.pluginManager.apply(JVMDowngraderPlugin::class.java)
    }

    override fun applyDefaultRepositories(repositories: RepositoryHandler) {
        super.applyDefaultRepositories(repositories)

        repositories.maven("https://jitpack.io") {
            name = "JitPack UniMixins"
            mavenContent {
                includeGroup("com.github.LegacyModdingMC.UniMixins")
            }
        }
        repositories.maven("https://maven.cleanroommc.com") {
            name = "MixinBooter"
            mavenContent {
                includeModule("zone.rong", "mixinbooter")
            }
        }
    }

    override fun applyJavaSettings(target: Project) {
        super.applyJavaSettings(target)
        val ext = target.extensions.getByType<BaseRetroFuturaGradleExtension>()
        val modstitch = target.extensions.getByType<ModstitchExtension>()
        target.extensions.configure<JavaPluginExtension> {
            target.afterSuccessfulEvaluate {
                val javaVer = ext.developmentJavaVersion.orNull
                ext.enableJvmDowngrader.finalizeValueOnRead()
                if (ext.enableJvmDowngrader.get() && javaVer != null) {
                    JavaVersion.toVersion(javaVer).let {
                        sourceCompatibility = it
                        targetCompatibility = it
                    }
                }
            }
            toolchain {
                languageVersion.set(
                    ext.developmentJavaVersion.orElse(modstitch.javaVersion).map { JavaLanguageVersion.of(it) }
                )
                // https://github.com/MinecraftForge/ForgeGradle/issues/597
                // Important for stable decompilation output or else you get failed
                // patches with: cannot find hunk target
                vendor.set(JvmVendorSpec.AZUL)
            }
        }
    }

    override fun apply(target: Project) {
        val ext = createRealPlatformExtension(target)!!
        super.apply(target)
        val modstitch = target.modstitch
        //todo set final jar tasks modstitch api
        target.tasks.named("reobfJar", ReobfuscatedJar::class.java) {
            if (ext.enableJvmDowngrader.get()) {
                val oldJar = inputJar.get()
                inputJar.set(target.jvmdg.defaultTask.flatMap { it.archiveFile }.orElse(oldJar))
            }
        }
        target.tasks.named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME) {
            if (ext.enableJvmDowngrader.get()) {
                dependsOn(target.jvmdg.defaultShadeTask)
            }
        }
        modstitch._namedJarTaskName = JvmConstants.JAR_TASK_NAME
        target.afterSuccessfulEvaluate {
            if(ext.enableJvmDowngrader.get()) {
                modstitch._finalJarTaskName = target.jvmdg.defaultShadeTask.name
            } else {
                modstitch._finalJarTaskName = "reobfJar"
            }
        }

        modstitch.modLoaderManifest.convention(platform.modManifest)
        val minecraft = target.extensions.getByType<MinecraftExtension>()
        minecraft.mcVersion.set(modstitch.minecraftVersion)
        minecraft.mcpMappingChannel.set(ext.mcpChannel)
        minecraft.mcpMappingVersion.set(ext.mcpVersion)
        minecraft.extraRunJvmArguments.addAll(
            mutableListOf(
                "-ea:${target.group}",
                "-Dmixin.hotSwap=true",
                "-Dmixin.check.interfaces=true",
                "-Dmixin.debug.export=true"
            )
        )
        minecraft.extraRunJvmArguments.addAll(
            ext.coreModClassName.map {
                listOf("-Dfml.coreMods.load=$it")
            }.orElse(emptyList())
        )

        configureLegacyMixin(target)

        applyRuns(target)
        fixJvmDowngraderRuns(target)
    }

    override fun applyClassTweaker(target: Project) {
        val modstitch = target.extensions.getByType<ModstitchExtension>()
        val defaultAccessTransformerName = modstitch.metadata.modId.map { "META-INF/${it}_at.cfg" }
        val generatedAccessTransformer = defaultAccessTransformerName.flatMap {
            target.layout.buildDirectory
                .file("modstitch/$it")
        }.zip(modstitch.classTweaker) { x, _ -> x }
        val generatedAccessTransformersList = generatedAccessTransformer.map { listOf(it) }.orElse(listOf())
        val classTweakerName = modstitch.classTweakerName.convention(defaultAccessTransformerName)
            .map {
                if (!it.startsWith("META-INF")) {
                    error("Access transformer name must be placed in META-INF/")
                } else it
            }
        val classTweakerPath = classTweakerName.map { it.split('\\', '/') }
        modstitch.classTweaker.finalizeValueOnRead()
        modstitch.classTweakerName.finalizeValueOnRead()
        modstitch.validateClassTweaker.finalizeValueOnRead()


        val mcpTasks = target.extensions.getByType<MCPTasks>()

        val convertMappingsTask = target.tasks.register<ConvertMappingsTask>("convertMappingsForAccessTransformers") {
            sourceMappingsFile.set(mcpTasks.taskGenerateForgeSrgMappings.flatMap { it.mcpToSrg })
            targetFormat.set(IMappingFile.Format.TSRG)
            convertedFile.set(target.layout.buildDirectory.file("generated/convertedMappings.txt"))
        }

        val generateAccessTransformerTask =
            target.tasks.register<GenerateAccessTransformerTask>("generateAccessTransformer") {
                group = "modstitch/internal"
                description = "Generates an access transformer."
                classTweaker.set(modstitch.classTweaker)
                mappings.set(
                    convertMappingsTask.flatMap { it.convertedFile }
                )
                accessTransformer.set(generatedAccessTransformer)
            }

//        val projectDir = target.projectDir.toPath()
//        mcpTasks.deobfuscationATs.from(
//            generateAccessTransformerTask.map {
//                it.outputs.files.map {
//                    projectDir.relativize(it.toPath())
//                }
//            }
//        )
        target.tasks.named<JSTTransformerTask>("applyJST") {
            this.accessTransformerFiles.setFrom(generateAccessTransformerTask)
        }

        target.tasks.named<ProcessResources>("processResources") {
            dependsOn(generateAccessTransformerTask)
            from(generatedAccessTransformersList) {
                rename { classTweakerPath.get().last() }
                into(classTweakerPath.map { it.dropLast(1).joinToString("/") })
            }
        }
        target.tasks.named<Jar>("jar") {
            manifest {
                attributes["FMLAT"] = classTweakerPath.get().last()
            }
        }

    }


    private fun fixJvmDowngraderRuns(target: Project) {
        val ext = target.extensions.getByType<BaseRetroFuturaGradleExtension>()
        val defaultShadeTask = target.jvmdg.defaultShadeTask
        val nonDowngradedJar = target.tasks.named("jar", Jar::class.java).map { it.outputs.files }
        val downgradedJarOutput = defaultShadeTask.map { it.outputs.files }
        val nonDowngradedDependencies = target.provider {
            target.files(
                nonDowngradedRegularConfigurations
                    .flatMap {
                        val config = target.configurations.getByName(it)
                        if (config.isCanBeResolved) config.resolve() else emptySet()
                    }
            )
        }
        target.tasks.withType<RunMinecraftTask>().configureEach {
            if (ext.enableJvmDowngrader.get()) {
                dependsOn(defaultShadeTask)
                classpath(downgradedJarOutput.get())
                val originalClasspath = classpath
                //downgraded jar in first position

                classpath = downgradedJarOutput.get()
                    .plus(
                        originalClasspath
                            .minus(nonDowngradedJar.get())
                            .minus(nonDowngradedDependencies.get())
                    )
            }
        }
    }


    private fun applyRuns(target: Project) {
        val modstitch = target.extensions.getByType<ModstitchExtension>()
        val ext = target.extensions.getByType<BaseRetroFuturaGradleExtension>()


        modstitch.runs.whenObjectAdded {
            val config = this
            val taskName = "run${config.name.replaceFirstChar(Char::uppercaseChar)}"

            modstitch.onEnable {
                config.side.finalizeValueOnRead()
                target.tasks.maybeRegister<RunMinecraftTask>(
                    taskName,
                    config.side.map {
                        when (it) {
                            Side.Both -> error("Unknown side for RetroFuturaGradle: $it")
                            Side.Client -> Distribution.CLIENT
                            Side.Server -> Distribution.DEDICATED_SERVER
                        }
                    }
                ) {
                    group = "modstitch/runs"
                    config.gameDirectory.finalizeValueOnRead()
                    config.gameDirectory.orNull?.let {
                        this.workingDir = it.asFile
                    }
                    this.mainClass.set(config.mainClass)
                    this.jvmArguments.set(config.jvmArgs)
                    this.extraArgs.set(config.programArgs)
                    config.environmentVariables.finalizeValueOnRead()
                    config.environmentVariables.orNull?.let { this.environment = it }

                }
                val bool = target.gradle.startParameter.taskNames[0] == "build"
                target.tasks.named<Jar>("jar") {
                    manifest {
                        val attributeMap = mutableMapOf<String, String>()
                        val coreModClassName = ext.coreModClassName.orNull
                        if (coreModClassName != null) {
                            attributeMap["FMLCorePlugin"] = coreModClassName
                            if (ext.hasModAndCoreMod.get()) {
                                attributeMap["FMLCorePluginContainsFMLMod"] = true.toString()
                                attributeMap["ForceLoadAsMod"] = bool.toString()
                            }
                            attributes.putAll(attributeMap)
                        }
                    }
                }
            }
        }

    }

    override fun finalize(target: Project) {
        val ext = target.extensions.getByType<BaseRetroFuturaGradleExtension>()
        val minecraft = target.extensions.getByType<MinecraftExtension>()
        check(minecraft.forgeVersion.get() == ext.forgeVersion.get()) {
            "Unsupported forge version ${ext.forgeVersion.get()} for RetroFuturaGradle, expected ${minecraft.forgeVersion.get()} " +
                    "RFG only supports 1 version for 1.7.10 and another one for 1.12.2"
        }
        super.finalize(target)
    }

    override fun applyMetadataStringReplacements(target: Project): TaskProvider<ProcessResources> {
        val generateModMetadata = super.applyMetadataStringReplacements(target)

        return generateModMetadata
    }

    override fun applyUnitTesting(target: Project, testFrameworkConfigure: Action<in JUnitPlatformOptions>) {
        throw UnsupportedOperationException("RetroFuturaGradle does not support unit testing")
    }

    override fun createProxyConfigurations(
        target: Project,
        configuration: FutureNamedDomainObjectProvider<Configuration>,
        defer: Boolean,
    ) {
        val ext = target.extensions.getByType<BaseRetroFuturaGradleExtension>()
        val modstitch = target.extensions.getByType<ModstitchExtension>()
        val proxyModConfigurationName = configuration.name.addCamelCasePrefix("modstitchMod")
        val proxyRegularConfigurationName = configuration.name.addCamelCasePrefix("modstitch")
        val proxyDowngradeConfigurationName = configuration.name.addCamelCasePrefix("modstitchDowngrade")

        // already created
        if (target.configurations.find { it.name == proxyModConfigurationName } != null) {
            return
        }
        fun deferred(action: (Configuration) -> Unit) {
            if (!defer) return action(configuration.get())
            return target.afterSuccessfulEvaluate { action(configuration.get()) }
        }

        val proxyMod = target.configurations.create(proxyModConfigurationName) proxy@{
            deferred {
                it.extendsFrom(this@proxy)
            }
        }

        val rfg = target.dependencies.extensions.getByType<ModUtils.RfgDependencyExtension>()
        proxyMod.dependencies.configureEach {
            if (this is FileCollectionDependency) {
                rfg.deobf(this.files)
            } else {
                rfg.deobf(
                    mapOf(
                        "group" to this.group.orEmpty(),
                        "name" to this.name.orEmpty(),
                        "version" to this.version.orEmpty(),
                        //todo: RFG also checks the classifier but IDK how to get it from here
                    )
                )
            }
        }

        deferred {
            nonDowngradedRegularConfigurations.add(it.name)
        }
        nonDowngradedRegularConfigurations.add(proxyRegularConfigurationName)
        target.configurations.create(proxyRegularConfigurationName) proxy@{
            deferred {
                it.extendsFrom(this@proxy)
            }
        }
        val javaVersion = modstitch.javaVersion.map {
            JavaVersion.toVersion(it)
        }
        val downgrade = target.configurations.create(proxyDowngradeConfigurationName) proxy@{
            deferred {
                it.extendsFrom(this@proxy)
            }
        }
        target.afterSuccessfulEvaluate {
            if (ext.enableJvmDowngrader.get()) {
                target.jvmdg.dg(downgrade, false) {
                    downgradeTo.set(javaVersion)
                }
            }
        }
    }

    override fun configureJiJConfiguration(target: Project, configuration: Configuration) {
        target.afterSuccessfulEvaluate {
            configuration.dependencies.whenObjectAdded {
                error("RetroFuturaGradle does not support JarInJar, please use the 'modstitch' configuration instead'")
            }
        }
    }


    private fun configureLegacyMixin(target: Project) {
        val modstitch = target.extensions.getByType<ModstitchExtension>()
        val ext = target.extensions.getByType<BaseRetroFuturaGradleExtension>()
        val stitchedMixin = modstitch.mixin
        ext.mixinsDependencies.finalizeValueOnRead()

        addMixinDependencies(target, ext.mixinsDependencies)
        val modUtils = target.extensions.getByType<ModUtils>()
        modUtils.enableMixins(
            null,
            modstitch.metadata.modId.map { "$it.refmap.json" }.get()
        )

        stitchedMixin.mixinSourceSets.whenObjectAdded obj@{
            modUtils.mixinSourceSet.set(target.sourceSets[this.sourceSetName.get()])
        }
        target.afterEvaluate {
            if (stitchedMixin.mixinSourceSets.size > 1) {
                //technically it does but we'll see later
                error("RetroFuturaGradle does not support multiple mixin source sets")
            }
            if (stitchedMixin.configs.size > 1) {
                error("RetroFuturaGradle does not support multiple mixin configs")
            }
        }
    }

    override fun onEnable(target: Project, action: Action<Project>) {
        target.afterSuccessfulEvaluate(action)
    }

    private fun addMixinDependencies(
        target: Project,
        dependencies: Provider<List<String>>,
    ) {
        target.afterSuccessfulEvaluate {
            target.dependencies {
                val implementation = target.configurations.named(JvmConstants.IMPLEMENTATION_CONFIGURATION_NAME)
                val annotationProcessor =
                    target.configurations.getByName(JvmConstants.ANNOTATION_PROCESSOR_CONFIGURATION_NAME)
                dependencies.get().forEach {
                    implementation(it)
                    annotationProcessor(it)
                }
            }
        }
    }
}






