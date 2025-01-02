package dev.isxander.modstitch.base

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class AppendMixinDataTask : DefaultTask() {
    @get:Input
    abstract val source: Property<SourceSet>

    @get:Input
    abstract val modMetadataFile: Property<String>

    @TaskAction
    fun run() {
        val root = source.get().output.resourcesDir ?: error("No output resources dir")
        val file = File(root, modMetadataFile.get())

        val contents = file.readText()
        val newContents = applyModificationsToFile(file.extension, contents)

        file.writeText(newContents)
    }

    abstract fun applyModificationsToFile(fileExtension: String, contents: String): String
}