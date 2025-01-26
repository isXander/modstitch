package dev.isxander.modstitch.shadow.loom

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import dev.isxander.modstitch.base.extensions.modstitch
import dev.isxander.modstitch.shadow.ShadowCommonImpl
import dev.isxander.modstitch.shadow.devlib
import net.fabricmc.loom.task.RemapJarTask
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*

class ShadowLoomImpl : ShadowCommonImpl<Nothing>() {
    override fun configureShadowTask(
        target: Project,
        shadowTask: TaskProvider<ShadowJar>,
        shadeConfiguration: NamedDomainObjectProvider<Configuration>,
    ) {
        super.configureShadowTask(target, shadowTask, shadeConfiguration)

        target.tasks.named<RemapJarTask>("remapJar") {
            dependsOn(shadowTask)

            // change the input from jar to shadowJar
            inputFile = shadowTask.flatMap { it.archiveFile }

            // this is the final jar, so this should have no classifier
            archiveClassifier = ""
        }

        shadowTask {
            archiveClassifier = "dev-fat"
            devlib()
        }
    }
}