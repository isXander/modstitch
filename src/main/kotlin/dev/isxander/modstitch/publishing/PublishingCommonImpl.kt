package dev.isxander.modstitch.publishing

import dev.isxander.modstitch.PlatformPlugin
import dev.isxander.modstitch.base.modstitch
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*

abstract class PublishingCommonImpl<T : Any> : PlatformPlugin<T>() {
    override fun apply(target: Project) {
        val msPublishing = target.extensions.create(
            PublishingExtension::class.java,
            "msPublishing",
            PublishingExtensionImpl::class.java,
        )

        target.pluginManager.apply("maven-publish")
        target.pluginManager.apply("me.modmuss50.mod-publish-plugin")

        target.pluginManager.withPlugin("me.modmuss50.mod-publish-plugin") {
            msPublishing.mpp {
                displayName = target.modstitch.metadata.modName
                version = target.modstitch.metadata.modVersion
            }
        }
    }
}