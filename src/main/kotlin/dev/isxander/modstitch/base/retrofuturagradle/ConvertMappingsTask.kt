package dev.isxander.modstitch.base.retrofuturagradle

import net.neoforged.srgutils.IMappingFile
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class ConvertMappingsTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceMappingsFile: RegularFileProperty

    @get:Input
    abstract val targetFormat: Property<IMappingFile.Format>

    @get:OutputFile
    abstract val convertedFile: RegularFileProperty

    @TaskAction
    fun convert() {
        val loadedMappings = IMappingFile.load(this.sourceMappingsFile.get().asFile)
        loadedMappings.write(convertedFile.get().asFile.toPath(), targetFormat.get(), false)
    }

}
