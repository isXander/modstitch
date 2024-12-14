package dev.isxander.modstitch.base.moddevgradle

import dev.isxander.modstitch.util.NotExistsDelegate
import dev.isxander.modstitch.util.isModDevGradle
import org.gradle.api.Action
import net.neoforged.moddevgradle.dsl.NeoForgeExtension as ModDevGradleExtension
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject
import org.gradle.kotlin.dsl.*

interface BaseModDevGradleExtension {
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

open class BaseModDevGradleExtensionImpl @Inject constructor(objects: ObjectFactory, private val project: Project) :
    BaseModDevGradleExtension {
    override val neoForgeVersion: Property<String> = objects.property(String::class.java)

    override val modDevGradle: ModDevGradleExtension
        get() = project.extensions.getByType<ModDevGradleExtension>()
}

open class BaseModDevGradleExtensionDummy : BaseModDevGradleExtension {
    override val neoForgeVersion: Property<String> by NotExistsDelegate()
    override val modDevGradle: ModDevGradleExtension by NotExistsDelegate()
}

val Project.msModdevgradle: BaseModDevGradleExtension
    get() = extensions.getByType<BaseModDevGradleExtension>()
fun Project.msModdevgradle(block: BaseModDevGradleExtension.() -> Unit) {
    if (isModDevGradle) {
        msModdevgradle.block()
    }
}