package dev.isxander.modstitch.base.moddevgradle

import dev.isxander.modstitch.base.extensions.modstitch
import dev.isxander.modstitch.util.ExtensionGetter
import dev.isxander.modstitch.util.NotExistsDelegate
import net.neoforged.moddevgradle.dsl.ModDevExtension
import org.gradle.api.Action
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import net.neoforged.moddevgradle.dsl.RunModel
import net.neoforged.moddevgradle.legacyforge.dsl.MixinExtension
import net.neoforged.moddevgradle.legacyforge.dsl.ObfuscationExtension
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject
import org.gradle.kotlin.dsl.*

interface BaseModDevGradleExtension {
    /**
     * The version of NeoForge to use.
     */
    val neoForgeVersion: Property<String>

    /**
     * The version of Forge to use.
     */
    val forgeVersion: Property<String>

    /**
     * The version of NeoForm to use.
     */
    val neoFormVersion: Property<String>

    /**
     * The version of MCP to use.
     */
    val mcpVersion: Property<String>

    /**
     * The underlying platform-specific extension: `neoForge`
     */
    val neoforgeExtension: ModDevExtension
    /**
     * Configures the NeoForge extension.
     * This action will only be executed if the current platform is ModDevGradle.
     */
    fun configureNeoforge(action: Action<ModDevExtension>)

    /**
     * The underlying platform-specific extension: `obfuscation`
     * Accessing this property will throw an exception if the current platform is not ModDevGradle Legacy.
     */
    val obfuscationExtension: ObfuscationExtension
    /**
     * Configures the Obfuscation extension.
     * This action will only be executed if the current platform is ModDevGradle Legacy.
     */
    fun configureObfuscation(action: Action<ObfuscationExtension>)

    /**
     * The underlying platform-specific extension: `mixin`
     * Accessing this property will throw an exception if the current platform is not ModDevGradle Legacy.
     */
    val mixinExtension: MixinExtension
    /**
     * Configures the Mixin extension.
     * This action will only be executed if the current platform is ModDevGradle Legacy.
     */
    fun configureMixin(action: Action<MixinExtension>)

    /**
     * Creates two run configurations: one for the client and one for the server.
     *
     * [namingConvention] is a function that takes two strings:
     * the platform name ("Forge", "NeoForge", or "Minecraft") and the side ("Client" or "Server"),
     * and returns the IDE name of the run configuration (e.g., "NeoForge Client", "Forge Server").
     */
    fun defaultRuns(client: Boolean = true, server: Boolean = true, namingConvention: (String, String) -> String = { name, side -> "$name $side" })
}

open class BaseModDevGradleExtensionImpl @Inject constructor(
    objects: ObjectFactory,
    @Transient private val project: Project,
    val type: MDGType,
) : BaseModDevGradleExtension {
    override val neoForgeVersion: Property<String> = objects.property()
    override val forgeVersion: Property<String> = objects.property()
    override val neoFormVersion: Property<String> = objects.property()
    override val mcpVersion: Property<String> = objects.property()

    override val neoforgeExtension: ModDevExtension by ExtensionGetter(project)
    override fun configureNeoforge(action: Action<ModDevExtension>) =
        action(neoforgeExtension)

    override val mixinExtension: MixinExtension by ExtensionGetter(project)
    override fun configureMixin(action: Action<MixinExtension>) =
        if (type == MDGType.Legacy) action(mixinExtension) else {}

    override val obfuscationExtension: ObfuscationExtension by ExtensionGetter(project)
    override fun configureObfuscation(action: Action<ObfuscationExtension>) =
        if (type == MDGType.Legacy) action(obfuscationExtension) else {}

    override fun defaultRuns(client: Boolean, server: Boolean, namingConvention: (String, String) -> String) {
        val project = project
        val name = when {
            neoForgeVersion.isPresent -> "NeoForge"
            forgeVersion.isPresent -> "Forge"
            else -> "Minecraft"
        }
        configureNeoforge {
            runs {
                fun registerOrConfigure(name: String, action: Action<RunModel>) = action(maybeCreate(name))

                if (client) {
                    registerOrConfigure("client") {
                        client()
                        ideName = "${namingConvention(name, "Client")} (${project.path})"
                    }
                }
                if (server) {
                    registerOrConfigure("server") {
                        server()
                        ideName = "${namingConvention(name, "Server")} (${project.path})"
                    }
                }
            }
        }
    }
}

open class BaseModDevGradleExtensionDummy : BaseModDevGradleExtension {
    override val neoForgeVersion: Property<String> by NotExistsDelegate()
    override val forgeVersion: Property<String> by NotExistsDelegate()
    override val neoFormVersion: Property<String> by NotExistsDelegate()
    override val mcpVersion: Property<String> by NotExistsDelegate()
    override val neoforgeExtension: NeoForgeExtension by NotExistsDelegate()
    override val mixinExtension: MixinExtension by NotExistsDelegate()
    override val obfuscationExtension: ObfuscationExtension by NotExistsDelegate()

    override fun configureNeoforge(action: Action<ModDevExtension>) {}
    override fun configureMixin(action: Action<MixinExtension>) {}
    override fun configureObfuscation(action: Action<ObfuscationExtension>) {}
    override fun defaultRuns(client: Boolean, server: Boolean, namingConvention: (String, String) -> String) {}
}

val Project.msModdevgradle: BaseModDevGradleExtension
    get() = extensions.getByType<BaseModDevGradleExtension>()
fun Project.msModdevgradle(block: BaseModDevGradleExtension.() -> Unit) {
    if (project.modstitch.isModDevGradle) {
        msModdevgradle.block()
    }
}
