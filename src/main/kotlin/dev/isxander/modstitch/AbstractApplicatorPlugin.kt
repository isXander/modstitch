package dev.isxander.modstitch

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

abstract class AbstractApplicatorPlugin(private val idResolver: (Loader) -> String?) : Plugin<Project> {
    constructor(extensionId: String) : this({ "dev.isxander.multienv.$extensionId.${it.friendlyName}" })

    override fun apply(target: Project) {
        target.childProjects.forEach { (_, childProject) ->
            childProject.run configure@{
                val desiredLoaderStr = project.findProperty("multienv.loader")?.toString() ?: run {
                    println("Subproject `${project.name}` is missing 'multienv.loader' property. No loader configuration will be applied")
                    return@configure
                }

                val desiredLoader = Loader.fromSerialName(desiredLoaderStr)
                    ?: error("Unknown loader on project `${project.name}`: $desiredLoaderStr")

                idResolver(desiredLoader)?.let { apply(plugin = it) }
            }
        }
    }
}