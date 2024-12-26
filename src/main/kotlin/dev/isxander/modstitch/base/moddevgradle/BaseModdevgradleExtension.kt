package dev.isxander.modstitch.base.moddevgradle

import dev.isxander.modstitch.base.extensions.modstitch
import dev.isxander.modstitch.util.ExtensionGetter
import dev.isxander.modstitch.util.NotExistsDelegate
import org.gradle.api.Action
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import net.neoforged.moddevgradle.dsl.RunModel
import net.neoforged.moddevgradle.legacyforge.dsl.MixinExtension
import net.neoforged.moddevgradle.legacyforge.dsl.Obfuscation
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject
import org.gradle.kotlin.dsl.*

interface BaseModDevGradleExtension {
    val forgeVersion: Property<String>
    val neoformVersion: Property<String>

    val neoforgeExtension: NeoForgeExtension
    fun configureNeoforge(action: Action<NeoForgeExtension>) = action.execute(neoforgeExtension)

    val obfuscationExtension: Obfuscation
    fun configureObfuscation(action: Action<Obfuscation>) = action.execute(obfuscationExtension)

    val mixinExtension: MixinExtension
    fun configureMixin(action: Action<MixinExtension>) = action.execute(mixinExtension)

    /**
     * Creates two run configurations: one for the client and one for the server.
     * [namingConvention] is a function that takes a string (the side, Client or Server) and returns the IDE name of the run config.
     */
    fun defaultRuns(namingConvention: (String) -> String = { "NeoForge $it" }) {
        configureNeoforge {
            runs {
                fun registerOrConfigure(name: String, action: Action<RunModel>) = action(maybeCreate(name))

                registerOrConfigure("client") {
                    client()
                    ideName = namingConvention("Client")
                }
                registerOrConfigure("server") {
                    server()
                    ideName = namingConvention("Server")
                }
            }
        }
    }
}

open class BaseModDevGradleExtensionImpl @Inject constructor(
    objects: ObjectFactory,
    private val project: Project,
    private val type: MDGType,
) : BaseModDevGradleExtension {
    override val forgeVersion: Property<String> = objects.property<String>()
    override val neoformVersion: Property<String> = objects.property<String>()

    override val neoforgeExtension: NeoForgeExtension by ExtensionGetter(project)
    override val mixinExtension: MixinExtension by ExtensionGetter(project)
    override fun configureMixin(action: Action<MixinExtension>) =
        if (type == MDGType.Legacy) super.configureMixin(action) else {}
    override val obfuscationExtension: Obfuscation by ExtensionGetter(project)
    override fun configureObfuscation(action: Action<Obfuscation>) =
        if (type != MDGType.Legacy) super.configureObfuscation(action) else {}
}

open class BaseModDevGradleExtensionDummy : BaseModDevGradleExtension {
    override val forgeVersion: Property<String> by NotExistsDelegate()
    override val neoformVersion: Property<String> by NotExistsDelegate()
    override val neoforgeExtension: NeoForgeExtension by NotExistsDelegate()
    override val mixinExtension: MixinExtension by NotExistsDelegate()
    override val obfuscationExtension: Obfuscation by NotExistsDelegate()
}

val Project.msModdevgradle: BaseModDevGradleExtension
    get() = extensions.getByType<BaseModDevGradleExtension>()
fun Project.msModdevgradle(block: BaseModDevGradleExtension.() -> Unit) {
    if (project.modstitch.isModDevGradle) {
        msModdevgradle.block()
    }
}