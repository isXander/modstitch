package dev.isxander.modstitch.base.loom

/**
 * The extension for configuring Fabric-specific settings.
 */

import dev.isxander.modstitch.PlatformExtension
import dev.isxander.modstitch.util.NotExistsDelegate
import dev.isxander.modstitch.util.isLoom
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.*
import javax.inject.Inject

interface BaseLoomExtension : PlatformExtension<BaseLoomExtension> {
    val fabricLoaderVersion: Property<String>

    val loom: LoomGradleExtensionAPI
    fun loom(action: Action<LoomGradleExtensionAPI>) = action.execute(loom)
}

open class BaseLoomExtensionImpl @Inject constructor(objects: ObjectFactory, private val project: Project) :
    BaseLoomExtension {
    override val fabricLoaderVersion: Property<String> = objects.property<String>()

    override val loom: LoomGradleExtensionAPI
        get() = project.extensions.getByType<LoomGradleExtensionAPI>()

    override fun current(configure: Action<BaseLoomExtension>) =
        configure.execute(this)
}

open class BaseLoomExtensionDummy : BaseLoomExtension {
    override val fabricLoaderVersion: Property<String> by NotExistsDelegate()
    override val loom: LoomGradleExtensionAPI by NotExistsDelegate()

    override fun current(configure: Action<BaseLoomExtension>) {}
}

val Project.msLoom: BaseLoomExtension
    get() = extensions.getByType<BaseLoomExtension>()
fun Project.msLoom(block: BaseLoomExtension.() -> Unit) {
    if (isLoom) {
        msLoom.block()
    }
}