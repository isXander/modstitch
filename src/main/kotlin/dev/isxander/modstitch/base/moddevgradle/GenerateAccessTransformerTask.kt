package dev.isxander.modstitch.base.moddevgradle

import dev.isxander.modstitch.util.ClassTweaker
import dev.isxander.modstitch.util.ClassTweakerFormat
import dev.isxander.modstitch.util.ClassTweakerNamespace
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * A Gradle task that converts the provided [classTweaker] to an [accessTransformer],
 * optionally remapping it using the specified [mappings], if available.
 */
abstract class GenerateAccessTransformerTask : DefaultTask()
{
    /** The class tweaker file to convert into an access transformer. */
    @get:InputFile
    @get:Optional
    abstract val classTweaker: RegularFileProperty

    /** The mappings file used to remap the resulting access transformer, if available. */
    @get:InputFile
    @get:Optional
    abstract val mappings: RegularFileProperty

    /** The output file for the generated access transformer. */
    @get:OutputFile
    @get:Optional
    abstract val accessTransformer: RegularFileProperty

    /**
     * Generates an access transformer based on the provided [classTweaker],
     * optionally remapping it using the supplied [mappings].
     */
    @TaskAction
    fun generateAccessTransformer() {
        if (!classTweaker.isPresent) {
            return
        }

        val parsedClassTweaker = classTweaker.get().asFile.reader().use { ClassTweaker.parse(it) }
        val convertedAccessTransformer = parsedClassTweaker.convertFormat(ClassTweakerFormat.AT)
        val remappedAccessTransformer = when {
            mappings.isPresent -> mappings.get().asFile.reader().use { convertedAccessTransformer.remap(it, ClassTweakerNamespace.Intermediary) }
            else -> convertedAccessTransformer
        }

        accessTransformer.get().asFile.writer().use { remappedAccessTransformer.write(it) }
    }
}
