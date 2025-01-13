package dev.isxander.modstitch.shadow

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import dev.isxander.modstitch.PlatformPlugin
import dev.isxander.modstitch.shadow.extensions.*
import dev.isxander.modstitch.util.printVersion
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*

abstract class ShadowCommonImpl<T : Any> : PlatformPlugin<T>() {
    override fun apply(target: Project) {
        printVersion("Shadow", target)

        val extension = target.extensions.create(
            ShadowExtension::class.java,
            "msShadow",
            ShadowExtensionImpl::class.java
        )

        target.pluginManager.apply("com.gradleup.shadow")

        val modstitchShadow by target.configurations.registering {
            isCanBeResolved = true
            isCanBeConsumed = false
            isTransitive = false
        }

        target.pluginManager.withPlugin("com.gradleup.shadow") {
            val shadowJar = target.tasks.named<ShadowJar>("shadowJar")
            configureShadowTask(target, shadowJar, modstitchShadow)

            target.artifacts {
                add("archives", shadowJar)
            }
        }
    }

    protected open fun configureShadowTask(
        target: Project,
        shadowTask: TaskProvider<ShadowJar>,
        shadeConfiguration: NamedDomainObjectProvider<Configuration>,
    ) {
        shadowTask {
            configurations = listOf(shadeConfiguration.get())
            archiveClassifier = ""
        }
    }
}