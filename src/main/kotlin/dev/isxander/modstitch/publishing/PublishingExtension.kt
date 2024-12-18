package dev.isxander.modstitch.publishing

import dev.isxander.modstitch.base.extensions.modstitch
import dev.isxander.modstitch.util.ExtensionGetter
import me.modmuss50.mpp.ModPublishExtension
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.publish.PublishingExtension as MavenPublishingExtension
import org.gradle.kotlin.dsl.*
import javax.inject.Inject

interface PublishingExtension {
    val mavenGroup: Property<String>
    val mavenArtifact: Property<String>

    val mpp: ModPublishExtension
    fun mpp(action: Action<ModPublishExtension>) = action.execute(mpp)

    val maven: MavenPublishingExtension
    fun maven(action: Action<MavenPublishingExtension>) = action.execute(maven)
}

open class PublishingExtensionImpl @Inject constructor(objects: ObjectFactory, private val project: Project) : PublishingExtension {
    override val mavenGroup = objects.property<String>().convention(project.provider { project.modstitch.metadata.modGroup }.flatMap { it })
    override val mavenArtifact = objects.property<String>().convention(project.provider { project.modstitch.metadata.modId.map { it.replace('_', '-') } }.flatMap { it })

    override val mpp: ModPublishExtension by ExtensionGetter(project)
    override val maven: MavenPublishingExtension by ExtensionGetter(project)
}

val Project.msPublishing: PublishingExtension
    get() = extensions.getByType<PublishingExtension>()
fun Project.msPublishing(action: Action<PublishingExtension>) = action.execute(msPublishing)