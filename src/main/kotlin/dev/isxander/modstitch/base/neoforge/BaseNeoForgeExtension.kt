package dev.isxander.modstitch.base.neoforge

import dev.isxander.modstitch.util.NotExistsDelegate
import dev.isxander.modstitch.util.isNeoForge
import org.gradle.api.Action
import net.neoforged.moddevgradle.dsl.NeoForgeExtension as ModDevGradleExtension
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject
import org.gradle.kotlin.dsl.*

interface BaseNeoForgeExtension {
    val neoForgeVersion: Property<String>

    val modDevGradle: ModDevGradleExtension
    fun modDevGradle(action: Action<ModDevGradleExtension>) = action.execute(modDevGradle)

    /**
     * Creates two run configurations: one for the client and one for the server.
     * [namingConvention] is a function that takes a string (the side, Client or Server) and returns the IDE name of the run config.
     */
    fun defaultRuns(namingConvention: (String) -> String = { "NeoForge $it" }) {
        modDevGradle {
            runs {
                create("client") {
                    client()
                    ideName = namingConvention("Client")
                }
                create("server") {
                    server()
                    ideName = namingConvention("Server")
                }
            }
        }
    }
}

open class BaseNeoForgeExtensionImpl @Inject constructor(objects: ObjectFactory, private val project: Project) : BaseNeoForgeExtension {
    override val neoForgeVersion: Property<String> = objects.property(String::class.java)

    override val modDevGradle: ModDevGradleExtension
        get() = project.extensions.getByType<ModDevGradleExtension>()
}

open class BaseNeoForgeExtensionDummy : BaseNeoForgeExtension {
    override val neoForgeVersion: Property<String> by NotExistsDelegate()
    override val modDevGradle: ModDevGradleExtension by NotExistsDelegate()
}

val Project.neoforge: BaseNeoForgeExtension
    get() = extensions.getByType(BaseNeoForgeExtension::class.java)
fun Project.neoforge(block: BaseNeoForgeExtension.() -> Unit) {
    if (isNeoForge) {
        neoforge.block()
    }
}