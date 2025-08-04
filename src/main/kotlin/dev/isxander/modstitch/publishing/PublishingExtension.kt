package dev.isxander.modstitch.publishing

import dev.isxander.modstitch.base.extensions.modstitch
import me.modmuss50.mpp.ModPublishExtension
import org.gradle.api.Action
import org.gradle.api.DomainObjectSet
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.publish.PublishingExtension as MavenPublishingExtension
import org.gradle.kotlin.dsl.*
import javax.inject.Inject

interface PublishingExtension {
    /**
     * The Maven group ID to use for publishing.
     * By default, this is the same as the mod group ID.
     */
    val mavenGroup: Property<String>

    /**
     * The Maven artifact ID to use for publishing.
     * By default, this is the same as the mod ID, but with underscores replaced with hyphens.
     */
    val mavenArtifact: Property<String>

    /**
     * Additional artifacts to publish.
     */
    val additionalArtifacts: DomainObjectSet<Any>

    /**
     * The `me.modmuss50.mod-publish-plugin` plugin extension.
     */
    val mpp: ModPublishExtension
    /**
     * Configures the `me.modmuss50.mod-publish-plugin` plugin extension.
     */
    fun mpp(action: Action<ModPublishExtension>) = action.execute(mpp)

    /**
     * The `maven-publish` plugin extension.
     */
    val maven: MavenPublishingExtension
    /**
     * Configures the `maven-publish` plugin extension.
     */
    fun maven(action: Action<MavenPublishingExtension>) = action.execute(maven)
}

open class PublishingExtensionImpl @Inject constructor(objects: ObjectFactory, private val project: Project) : PublishingExtension {
    override val mavenGroup = objects.property<String>().convention(project.provider { project.modstitch.metadata.modGroup }.flatMap { it })
    override val mavenArtifact = objects.property<String>().convention(project.provider { project.modstitch.metadata.modId.map { it.replace('_', '-') } }.flatMap { it })
    override val additionalArtifacts = objects.domainObjectSet(Any::class)

    override val mpp: ModPublishExtension
        get() = project.extensions.getByType<ModPublishExtension>()
    override val maven: MavenPublishingExtension
        get() = project.extensions.getByType<MavenPublishingExtension>()
}

val Project.msPublishing: PublishingExtension
    get() = extensions.getByType<PublishingExtension>()
fun Project.msPublishing(action: Action<PublishingExtension>) = action.execute(msPublishing)