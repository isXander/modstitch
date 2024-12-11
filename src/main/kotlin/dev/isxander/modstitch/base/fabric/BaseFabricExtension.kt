package dev.isxander.modstitch.base.fabric

/**
 * The extension for configuring Fabric-specific settings.
 */

import dev.isxander.modstitch.PlatformExtension
import dev.isxander.modstitch.util.NotExistsDelegate
import dev.isxander.modstitch.util.isFabric
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.*
import javax.inject.Inject

interface BaseFabricExtension : PlatformExtension<BaseFabricExtension> {
    val fabricLoaderVersion: Property<String>

    val loom: LoomGradleExtensionAPI
    fun loom(action: Action<LoomGradleExtensionAPI>) = action.execute(loom)
}

open class BaseFabricExtensionImpl @Inject constructor(objects: ObjectFactory, private val project: Project) : BaseFabricExtension {
    override val fabricLoaderVersion: Property<String> = objects.property<String>()

    override val loom: LoomGradleExtensionAPI
        get() = project.extensions.getByType<LoomGradleExtensionAPI>()

    override fun current(configure: Action<BaseFabricExtension>) =
        configure.execute(this)
}

open class BaseFabricExtensionDummy : BaseFabricExtension {
    override val fabricLoaderVersion: Property<String> by NotExistsDelegate()
    override val loom: LoomGradleExtensionAPI by NotExistsDelegate()

    override fun current(configure: Action<BaseFabricExtension>) {}
}

val Project.fabric: BaseFabricExtension
    get() = extensions.getByType<BaseFabricExtension>()
fun Project.fabric(block: BaseFabricExtension.() -> Unit) {
    if (isFabric) {
        fabric.block()
    }
}