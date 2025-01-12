package dev.isxander.modstitch.base

import dev.isxander.modstitch.base.extensions.MixinConfigurationSettings
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Abstract super-class, not to be instantiated directly")
abstract class AppendMixinDataTask : DefaultTask() {
    @get:InputFile
    abstract val inputFile: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:Input
    abstract val mixinConfigs: ListProperty<MixinConfigurationSettings>

    @TaskAction
    fun run() {
        val file = inputFile.asFile.get()
        val contents = file.readText()
        val newContents = applyModificationsToFile(file.extension, contents)

        file.writeText(newContents)
    }

    abstract fun applyModificationsToFile(fileExtension: String, contents: String): String
}