package dev.isxander.modstitch.publishing

import dev.isxander.modstitch.PlatformPlugin
import dev.isxander.modstitch.base.extensions.modstitch
import dev.isxander.modstitch.util.printVersion
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.kotlin.dsl.*

abstract class PublishingCommonImpl<T : Any> : PlatformPlugin<T>() {
    override fun apply(target: Project) {
        printVersion("Publishing", target)

        val msPublishing = target.extensions.create(
            PublishingExtension::class.java,
            "msPublishing",
            PublishingExtensionImpl::class.java,
        )

        val publishMod by target.tasks.registering {
            group = "modstitch/publishing"
        }

        target.pluginManager.apply("maven-publish")
        target.pluginManager.withPlugin("maven-publish") {
            msPublishing.maven {
                publications {
                    register<MavenPublication>("mod") {
                        from(target.components["java"])

                        msPublishing.additionalArtifacts.whenObjectAdded obj@{
                            artifact(this@obj)
                        }

                        target.afterEvaluate {
                            groupId = target.modstitch.metadata.modGroup.get()
                            artifactId = target.modstitch.metadata.modId.get()
                        }
                    }
                }
            }

            publishMod { dependsOn(target.tasks.named("publish")) }
        }

        target.pluginManager.apply("me.modmuss50.mod-publish-plugin")
        target.pluginManager.withPlugin("me.modmuss50.mod-publish-plugin") {
            msPublishing.mpp {
                displayName = target.modstitch.metadata.modName
                version = target.modstitch.metadata.modVersion

                file.set(target.provider { target.modstitch.finalJarTask.flatMap { it.archiveFile } }.flatMap { it })

                msPublishing.additionalArtifacts.whenObjectAdded obj@{
                    additionalFiles.from(if (this@obj is AbstractArchiveTask) this@obj.archiveFile else this@obj)
                }
            }

            publishMod { dependsOn(target.tasks.named("publishMods")) }
        }
    }


}