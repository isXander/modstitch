package dev.isxander.modstitch.base

import dev.isxander.modstitch.base.extensions.MixinConfigurationSettings
import dev.isxander.modstitch.util.Side
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.Serializable

@DisableCachingByDefault(because = "Abstract super-class, not to be instantiated directly")
abstract class AppendMixinDataTask : DefaultTask() {
    @get:InputFile
    abstract val inputFile: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:Input
    internal abstract val mixinConfigs: ListProperty<ResolvedMixinConfigSettings>

    @TaskAction
    fun run() {
        val file = inputFile.asFile.get()
        val contents = file.readText()
        val newContents = applyModificationsToFile(file.extension, contents)

        file.writeText(newContents)
    }

    abstract fun applyModificationsToFile(fileExtension: String, contents: String): String
}

data class ResolvedMixinConfigSettings(
    val config: String,
    val side: Side,
) : Serializable
internal fun MixinConfigurationSettings.resolve() =
    ResolvedMixinConfigSettings(
        config.get(),
        side.getOrElse(Side.Both)
    )