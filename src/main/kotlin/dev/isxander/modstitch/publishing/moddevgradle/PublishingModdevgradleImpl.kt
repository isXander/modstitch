package dev.isxander.modstitch.publishing.moddevgradle

import dev.isxander.modstitch.base.extensions.modstitch
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

        target.modstitch.onEnable {
            target.msPublishing.mpp {
                modLoaders.add(
                    when (this@PublishingModdevgradleImpl.type) {
                        MDGType.Regular -> "neoforge"
                        MDGType.Legacy -> "forge"
                    }
                )
            }
        }
    }
}