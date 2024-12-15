package dev.isxander.modstitch.publishing.moddevgradle

import dev.isxander.modstitch.base.moddevgradle.MDGType
import dev.isxander.modstitch.publishing.PublishingCommonImpl
import dev.isxander.modstitch.publishing.msPublishing
import net.neoforged.moddevgradle.legacyforge.tasks.RemapJar
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*

class PublishingModdevgradleImpl(private val type: MDGType) : PublishingCommonImpl<Nothing>() {
    override fun apply(target: Project) {
        super.apply(target)

        val jar = when (type) {
            MDGType.Regular, MDGType.Vanilla -> target.tasks.named<Jar>("jar")
            MDGType.Legacy -> target.tasks.named<RemapJar>("reobfJar")
        }

        target.msPublishing.mpp {
            file.assign(jar.flatMap { it.archiveFile })
            modLoaders.add("neoforge")
        }
    }
}