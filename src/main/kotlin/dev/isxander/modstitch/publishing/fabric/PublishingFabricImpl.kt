package dev.isxander.modstitch.publishing.fabric

import dev.isxander.modstitch.publishing.PublishingCommonImpl
import dev.isxander.modstitch.publishing.msPublishing
import net.fabricmc.loom.task.RemapJarTask
import org.gradle.api.Project
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.named

object PublishingFabricImpl : PublishingCommonImpl<Nothing>() {
    override fun apply(target: Project) {
        super.apply(target)

        val remapJar = target.tasks.named<RemapJarTask>("remapJar")

        target.msPublishing.mpp {
            file.assign(remapJar.flatMap { it.archiveFile })
            modLoaders.add("fabric")
        }
    }
}