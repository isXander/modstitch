package dev.isxander.modstitch.shadow.loom

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import dev.isxander.modstitch.base.extensions.modstitch
import dev.isxander.modstitch.shadow.ShadowCommonImpl
import net.fabricmc.loom.task.RemapJarTask
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.TaskProvider
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

            inputFile = shadowTask.flatMap { it.archiveFile }

            archiveClassifier = ""
        }

        shadowTask {
            archiveClassifier = "dev-fat"
        }
    }
}