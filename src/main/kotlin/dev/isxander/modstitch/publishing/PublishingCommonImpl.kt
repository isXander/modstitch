package dev.isxander.modstitch.publishing

import dev.isxander.modstitch.PlatformPlugin
import dev.isxander.modstitch.base.modstitch
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.*

abstract class PublishingCommonImpl<T : Any> : PlatformPlugin<T>() {
    override fun apply(target: Project) {
        val msPublishing = target.extensions.create(
            PublishingExtension::class.java,
            "msPublishing",
            PublishingExtensionImpl::class.java,
        )

        val publishMod by target.tasks.creating {
            group = "modstitch"
        }

        target.pluginManager.apply("maven-publish")
        target.pluginManager.withPlugin("maven-publish") {
            msPublishing.maven {
                publications {
                    create<MavenPublication>("mod") {
                        from(target.components["java"])

                        target.afterEvaluate {
                            groupId = target.modstitch.metadata.modGroup.get()
                            artifactId = target.modstitch.metadata.modId.get()
                        }
                    }
                }
            }

            publishMod.dependsOn(target.tasks.named("publish"))
        }

        target.pluginManager.apply("me.modmuss50.mod-publish-plugin")
        target.pluginManager.withPlugin("me.modmuss50.mod-publish-plugin") {
            msPublishing.mpp {
                displayName = target.modstitch.metadata.modName
                version = target.modstitch.metadata.modVersion
            }

            publishMod.dependsOn(target.tasks.named("publishMods"))
        }
    }


}