package dev.isxander.modstitch.base

import dev.isxander.modstitch.base.extensions.FinalMixinConfigurationSettings
import dev.isxander.modstitch.base.extensions.modstitch
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction

abstract class AppendMixinDataTask : SourceTask() {
    @get:Input
    abstract val mixinConfigs: ListProperty<FinalMixinConfigurationSettings>

    @TaskAction
    fun run() {
        source.visit {
            val contents = file.readText()
            val newContents = applyModificationsToFile(file.extension, contents)
            file.writeText(newContents)
        }
    }

    abstract fun applyModificationsToFile(fileExtension: String, contents: String): String

    companion object {
        fun configureTask(task: AppendMixinDataTask, project: Project, sourceSet: SourceSet, modMetadataFilePath: String) = with(task) {
            source(sourceSet.output.resourcesDir!!.resolve(modMetadataFilePath))
            mixinConfigs.value(project.provider {
                project.modstitch.mixin.configs.map { it.resolved() }
            })
            dependsOn("processResources")

            return@with
        }
    }
}