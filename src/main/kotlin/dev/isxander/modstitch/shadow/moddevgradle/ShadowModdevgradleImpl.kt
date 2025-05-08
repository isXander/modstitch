package dev.isxander.modstitch.shadow.moddevgradle

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import dev.isxander.modstitch.base.extensions.modstitch
import dev.isxander.modstitch.base.moddevgradle.MDGType
import dev.isxander.modstitch.shadow.ShadowCommonImpl
import dev.isxander.modstitch.shadow.devlib
import net.neoforged.moddevgradle.legacyforge.dsl.ObfuscationExtension
import net.neoforged.moddevgradle.legacyforge.tasks.RemapJar
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*

class ShadowModdevgradleImpl(private val type: MDGType) : ShadowCommonImpl<Nothing>() {
    override fun configureShadowTask(
        target: Project,
        shadowTask: TaskProvider<ShadowJar>,
        shadeConfiguration: NamedDomainObjectProvider<Configuration>
    ) {
        super.configureShadowTask(target, shadowTask, shadeConfiguration)

        /**
         * Creates a new Jar task that adds the jarJar output to the shadowJar output.
         * You cannot directly source jarJar into shadow as it would flatten all jars into the main one,
         * breaking jarJar.
         */
        val jijJar = target.tasks.register<Jar>("jijJar") {
            val shadowJar = target.tasks.named<ShadowJar>("shadowJar")
            from(target.zipTree(shadowJar.map { it.archiveFile }))
            dependsOn(shadowJar)

            val jarJar = target.tasks.named("jarJar")
            from(jarJar)
            dependsOn(jarJar)
        }.also {
           addToArtifacts(target, it)
        }
        target.tasks["assemble"].dependsOn(jijJar)

        shadowTask {
            archiveClassifier = "dev-fat"
            devlib()
        }

        target.modstitch._namedJarTaskName = "jijJar"
        when (type) {
            MDGType.Regular -> {
                target.modstitch._finalJarTaskName = "jijJar"
            }
            MDGType.Legacy -> {
                target.modstitch.onEnable {
                    target.tasks.named<RemapJar>("reobfJar") {
                        // reobfJar takes jar, which is disabled
                        enabled = false
                    }
                }

                target.extensions.configure<ObfuscationExtension> {
                    val reobfJar = reobfuscate(
                        jijJar,
                        target.extensions.getByType<SourceSetContainer>()["main"]
                    ).also {
                        addToArtifacts(target, it)
                    }

                    reobfJar {
                        archiveClassifier = ""
                    }

                    target.modstitch._finalJarTaskName = reobfJar.name
                }
            }
        }

        target.afterEvaluate {
            // Removes the original jar from the configurations so it doesn't get published.
            for (configurationName in arrayOf<String>(
                JavaPlugin.API_ELEMENTS_CONFIGURATION_NAME,
                JavaPlugin.RUNTIME_ELEMENTS_CONFIGURATION_NAME
            )) {
                val configuration: Configuration = configurations.getByName(configurationName)
                val jarTask = tasks.getByName(JavaPlugin.JAR_TASK_NAME) as Jar
                configuration.artifacts.removeIf { artifact: PublishArtifact? ->
                    (artifact!!.file.absolutePath == jarTask.archiveFile.get().asFile.absolutePath
                            && artifact.buildDependencies.getDependencies(null).contains(jarTask))
                        .also { if (it) println("Removed ${artifact.name} from $configurationName") }
                }
            }
        }
    }

    private fun addToArtifacts(target: Project, task: TaskProvider<out Jar>) {
        target.artifacts.add(JavaPlugin.API_ELEMENTS_CONFIGURATION_NAME, task)
        target.artifacts.add(JavaPlugin.RUNTIME_ELEMENTS_CONFIGURATION_NAME, task)

        target.artifacts {
            add("archives", task)
        }
    }
}