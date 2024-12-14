package dev.isxander.modstitch.publishing.moddevgradle

import dev.isxander.modstitch.publishing.PublishingCommonImpl
import dev.isxander.modstitch.publishing.msPublishing
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*

object PublishingModdevgradleImpl : PublishingCommonImpl<Nothing>() {
    override fun apply(target: Project) {
        super.apply(target)

        val jar = target.tasks.named<Jar>("jar")

        target.msPublishing.mpp {
            file.assign(jar.flatMap { it.archiveFile })
            modLoaders.add("neoforge")
        }
    }
}