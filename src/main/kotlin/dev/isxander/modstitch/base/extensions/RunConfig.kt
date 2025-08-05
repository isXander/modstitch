package dev.isxander.modstitch.base.extensions

import dev.isxander.modstitch.util.Side
import dev.isxander.modstitch.util.sourceSets
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.*
import org.jetbrains.annotations.ApiStatus
import javax.inject.Inject

open class RunConfig @Inject constructor(private val namekt: String, project: Project, objects: ObjectFactory) : Named {

    val CLIENT = Side.Client
    val SERVER = Side.Server

    val gameDirectory: DirectoryProperty = objects.directoryProperty()
        .convention(project.layout.projectDirectory.dir("run"))

    val mainClass: Property<String> = objects.property()

    val jvmArgs: ListProperty<String> = objects.listProperty()

    val programArgs: ListProperty<String> = objects.listProperty()

    val environmentVariables: MapProperty<String, String> = objects.mapProperty()

    val ideRunName: Property<String> = objects.property<String>()
        .convention(run {
            val isSubProject = project.rootProject != project
            var ideName = name.replaceFirstChar { it.uppercaseChar() }
            if (isSubProject) {
                ideName = "${project.name} - $ideName"
            }
            ideName
        })

    val ideRun: Property<Boolean> = objects.property<Boolean>()
        .convention(ideRunName.map { it.isNotEmpty() }.orElse(false))

    val sourceSet: Property<SourceSet> = objects.property<SourceSet>()
    val side: Property<Side> = objects.property()

    @ApiStatus.Experimental
    val datagen: Property<Boolean> = objects.property<Boolean>().convention(false)

    init {
        when (name) {
            "client" -> client()
            "server" -> server()
        }
    }

    fun client(datagen: Boolean? = null) {
        side.set(CLIENT)
        datagen?.let { this.datagen = it }
    }

    fun server(datagen: Boolean? = null) {
        side.set(SERVER)
        datagen?.let { this.datagen = it }
    }

    fun inherit(other: RunConfig) {
        gameDirectory.set(other.gameDirectory)
        mainClass.set(other.mainClass)
        jvmArgs.set(other.jvmArgs)
        programArgs.set(other.programArgs)
        environmentVariables.set(other.environmentVariables)
        ideRunName.set(other.ideRunName)
        ideRun.set(other.ideRun)
        sourceSet.set(other.sourceSet)
        side.set(other.side)
        datagen.set(other.datagen)
    }

    override fun getName(): String = namekt

}

