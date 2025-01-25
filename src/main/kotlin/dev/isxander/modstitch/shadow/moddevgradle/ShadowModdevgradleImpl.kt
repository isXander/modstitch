package dev.isxander.modstitch.shadow.moddevgradle

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import dev.isxander.modstitch.base.extensions.modstitch
import dev.isxander.modstitch.base.moddevgradle.MDGType
import dev.isxander.modstitch.shadow.ShadowCommonImpl
import net.neoforged.moddevgradle.legacyforge.dsl.ObfuscationExtension
import net.neoforged.moddevgradle.legacyforge.tasks.RemapJar
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
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

        shadowTask {
            target.modstitch.onEnable {
                from(target.tasks.named("jarJar"))
            }
        }

        when (type) {
            MDGType.Regular -> {
                target.modstitch.onEnable {
                    target.tasks.named<Jar>("jar") {
                        archiveClassifier = "slim"
                    }
                }

                target.modstitch._finalJarTaskName = "shadowJar"
            }
            MDGType.Legacy -> {
                target.modstitch.onEnable {
                    target.tasks.named<RemapJar>("reobfJar") {
                        archiveClassifier = "slim"
                        destinationDirectory = project.layout.buildDirectory.map { it.dir("devlibs") }
                    }
                }

                target.extensions.configure<ObfuscationExtension> {
                    target.modstitch._finalJarTaskName = reobfuscate(shadowTask, target.extensions.getByType<SourceSetContainer>()["main"])
                        .name
                }
            }
        }

        target.tasks.named<Jar>("jar") {
            // shadowJar does not use jar as an input
            // bundling jar is a waste of time
            enabled = false
        }
    }
}