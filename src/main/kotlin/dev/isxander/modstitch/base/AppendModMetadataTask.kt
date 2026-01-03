package dev.isxander.modstitch.base

import dev.isxander.modstitch.base.extensions.FinalMixinConfigurationSettings
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * A Gradle task that appends metadata entries to the provided mod manifest file(s).
 */
abstract class AppendModMetadataTask : SourceTask() {
    /** A list of mixin configurations to be appended to the metadata. */
    @get:Input
    abstract val mixins: ListProperty<FinalMixinConfigurationSettings>

    /** A list of class tweaker file paths to be included in the metadata. */
    @get:Input
    abstract val classTweakers: ListProperty<String>

    /**
     * Appends metadata entries to the [source] file(s).
     */
    @TaskAction
    fun appendModMetadata() = source.visit { appendModMetadata(file) }

    /**
     * Appends metadata entries to the specified file.
     *
     * @param file The file to which metadata entries should be appended.
     */
    protected abstract fun appendModMetadata(file: File)
}
