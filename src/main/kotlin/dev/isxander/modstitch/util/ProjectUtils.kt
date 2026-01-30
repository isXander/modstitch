package dev.isxander.modstitch.util

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskContainer
import org.gradle.kotlin.dsl.withType

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

/**
 *  Registers or configures the task with the given name and type
 *
 *  @param name The name of the task to configure or register
 *  @param constructorArgs The constructor arguments of the task
 *  @param configure The configuration block
 */
internal inline fun <reified T : Task> TaskContainer.maybeRegister(
    name: String,
    vararg constructorArgs: Any,
    configure: Action<T>,
) {

    if (name in names) {
        this.withType<T>().named(name, configure)
    } else {
        this.register(name, T::class.java, configure, constructorArgs)
    }
}

/**
 * Zips three [Provider]s together, rather than the usual two.
 */
internal fun <A : Any, B : Any, C : Any, T : Any> zip(a: Provider<A>, b: Provider<B>, c: Provider<C>, combiner: (A, B, C) -> T): Provider<T> {
    return a.zip(b) { av, bv -> av to bv }.zip(c) { (av, bv), cv -> combiner(av, bv, cv) }
}
