package dev.isxander.modstitch.shadow.extensions

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import dev.isxander.modstitch.base.extensions.modstitch
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.*

interface ShadowExtension {
    val relocatePackage: Property<String>

    fun dependency(dependencyNotation: Any, relocations: Map<PackageName, RelocateId>)
    fun dependency(dependencyNotation: Any, vararg relocations: Pair<PackageName, RelocateId>)
    fun dependency(dependencyNotation: Any, action: Action<MutableMap<PackageName, RelocateId>>)
}

@Suppress("LeakingThis")
open class ShadowExtensionImpl(
    objects: ObjectFactory,
    @Transient private val target: Project
) : ShadowExtension {
    override val relocatePackage = objects.property<String>()
    init { relocatePackage.convention(target.modstitch.metadata.modGroup.map { "$it.shadow" }) }

    override fun dependency(dependencyNotation: Any, relocations: Map<PackageName, RelocateId>) {
        require(relocations.isNotEmpty()) { "At least one relocation must be provided." }

        target.dependencies.add("modstitchShadow", dependencyNotation)
        target.tasks.named<ShadowJar>("shadowJar") {
            relocations.forEach { (pkg, id) ->
                println("Relocating $pkg to ${relocatePackage.get()}.$id")
                relocate(pkg, "${relocatePackage.get()}.$id")
            }
        }
    }

    override fun dependency(dependencyNotation: Any, action: Action<MutableMap<PackageName, RelocateId>>) {
        dependency(dependencyNotation, mutableMapOf<PackageName, RelocateId>().also(action::execute))
    }

    override fun dependency(dependencyNotation: Any, vararg relocations: Pair<PackageName, RelocateId>) {
        dependency(dependencyNotation, relocations.toMap())
    }
}

typealias PackageName = String
typealias RelocateId = String
