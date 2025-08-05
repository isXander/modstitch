package dev.isxander.modstitch.util

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.FileSystemLocationProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import java.io.File

/**
 * Gets the [SourceSetContainer] from the project's extensions, if available.
 */
internal val Project.sourceSets: SourceSetContainer?
    get() = extensions.findByName("sourceSets") as SourceSetContainer?

/**
 * Gets the `main` [SourceSet] from the project's source sets, if it exists.
 */
internal val Project.mainSourceSet: SourceSet?
    get() = sourceSets?.getByName(SourceSet.MAIN_SOURCE_SET_NAME)

/**
 * Gets the full project chain, starting with the current project.
 */
internal val Project.projectChain: Sequence<Project>
    get() = generateSequence(this) { it.parent }

/**
 * Executes the given [action] after the project has been successfully evaluated.
 *
 * @param action The action to be executed after successful project evaluation.
 */
internal fun Project.afterSuccessfulEvaluate(action: Action<Project>) = project.afterEvaluate {
    if (state.failure == null) {
        action.execute(this)
    }
}

internal fun Project.gradleProperty(key: String): String? =
    project.findProperty(key) as String?

/**
 * Searches for a gradle property deeply through the project tree
 */
internal fun <T> Project.deepGradleProperty(key: String, map: (String, Project) -> T): T? {
    var project: Project? = project
    while (project != null) {
        project.gradleProperty(key)?.let { return map(it, project!!) }
        project = project.parent
    }
    return null
}
internal fun Project.deepGradleProperty(key: String): String? =
    deepGradleProperty(key) { str, p -> str }

internal infix fun <T> Property<T>.assignIfNotNull(value: T?) =
    value?.let { set(it) }
internal infix fun <T : FileSystemLocation> FileSystemLocationProperty<T>.assignIfNotNull(value: File?) =
    value?.let { set(it) }
