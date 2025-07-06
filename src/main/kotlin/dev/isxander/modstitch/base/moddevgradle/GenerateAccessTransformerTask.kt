package dev.isxander.modstitch.base.moddevgradle

import dev.isxander.modstitch.util.AccessWidener
import dev.isxander.modstitch.util.AccessWidenerFormat
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * A Gradle task that converts the provided [accessWidener] to an [accessTransformer],
 * optionally remapping it using the specified [mappings], if available.
 */
abstract class GenerateAccessTransformerTask : DefaultTask()
{
    /** The access widener file to convert into an access transformer. */
    @get:InputFile
    @get:Optional
    abstract val accessWidener: RegularFileProperty

    /** The mappings file used to remap the resulting access transformer, if available. */
    @get:InputFile
    @get:Optional
    abstract val mappings: RegularFileProperty

    /** The output file for the generated access transformer. */
    @get:OutputFile
    @get:Optional
    abstract val accessTransformer: RegularFileProperty

    /**
     * Generates an access transformer based on the provided [accessWidener],
     * optionally remapping it using the supplied [mappings].
     */
    @TaskAction
    fun generateAccessTransformer() {
        if (!accessWidener.isPresent) {
            return
        }

        val parsedAccessWidener = accessWidener.get().asFile.reader().use { AccessWidener.parse(it) }
        val convertedAccessTransformer = parsedAccessWidener.convert(AccessWidenerFormat.AT)
        val remappedAccessTransformer = when {
            mappings.isPresent -> mappings.get().asFile.reader().use { convertedAccessTransformer.remap(it) }
            else -> convertedAccessTransformer
        }

        accessTransformer.get().asFile.writer().use { remappedAccessTransformer.write(it) }
    }
}
