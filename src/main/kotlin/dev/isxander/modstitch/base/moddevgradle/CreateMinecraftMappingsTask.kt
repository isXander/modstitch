package dev.isxander.modstitch.base.moddevgradle

import net.neoforged.nfrtgradle.NeoFormRuntimeTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault

/**
 * A lightweight variant of the `createMinecraftArtifacts` task that focuses solely on generating mappings.
 */
@DisableCachingByDefault(because = "Implements its own caching")
abstract class CreateMinecraftMappingsTask : NeoFormRuntimeTask() {
    init {
        outputs.upToDateWhen { (it as CreateMinecraftMappingsTask).enableCache.get() }
        enableCache.convention(true)
        analyzeCacheMisses.convention(false)
    }

    /**
     * The path to the Java installation to use when running external tools.
     */
    @get:Input
    @get:Optional
    abstract val toolsJavaExecutable: Property<String>

    /**
     * The Gradle dependency notation for the NeoForge userdev artifact.
     *
     * Either this or [neoFormArtifact] must be specified.
     */
    @get:Input
    @get:Optional
    abstract val neoForgeArtifact: Property<String>

    /**
     * The Gradle dependency notation for the NeoForm data artifact.
     *
     * Either this or [neoForgeArtifact] must be specified.
     */
    @get:Input
    @get:Optional
    abstract val neoFormArtifact: Property<String>

    /**
     * Enables the use of the NeoForm Runtime cache.
     *
     * Defaults to `true`.
     */
    @get:Internal
    abstract val enableCache: Property<Boolean>

    /**
     * When the cache is enabled, and this is set to `true`, additional information
     * will be printed to the console when a cache miss occurs.
     *
     * Defaults to `false`.
     */
    @get:Internal
    abstract val analyzeCacheMisses: Property<Boolean>

    /**
     * The output file for the generated Named-to-Intermediary mappings.
     */
    @get:OutputFile
    @get:Optional
    abstract val namedToIntermediaryMappings: RegularFileProperty

    /**
     * The output file for the generated Intermediary-to-Named mappings.
     */
    @get:OutputFile
    @get:Optional
    abstract val intermediaryToNamedMappings: RegularFileProperty

    /**
     * Generates the requested mapping files for the provided artifacts and settings.
     */
    @TaskAction
    fun createMappings() {
        if (!namedToIntermediaryMappings.isPresent && !intermediaryToNamedMappings.isPresent) {
            return
        }

        val args = mutableListOf<String>()
        args.add("run")

        if (toolsJavaExecutable.isPresent) {
            args.add("--java-executable")
            args.add(toolsJavaExecutable.get())
        }

        if (!enableCache.get()) {
            args.add("--disable-cache")
        }

        if (analyzeCacheMisses.get()) {
            args.add("--analyze-cache-misses")
        }

        if (neoForgeArtifact.isPresent) {
            args.add("--neoforge")
            args.add(neoForgeArtifact.get())
        }
        if (neoFormArtifact.isPresent) {
            args.add("--neoform")
            args.add(neoFormArtifact.get())
        }

        if (namedToIntermediaryMappings.isPresent) {
            args.add("--write-result")
            args.add("namedToIntermediaryMapping:${namedToIntermediaryMappings.get().asFile.absolutePath}")
        }
        if (intermediaryToNamedMappings.isPresent) {
            args.add("--write-result")
            args.add("intermediaryToNamedMapping:${intermediaryToNamedMappings.get().asFile.absolutePath}")
        }

        args.add("--dist")
        args.add("joined")

        run(args)
    }
}
