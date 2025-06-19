package dev.isxander.modstitch.base.moddevgradle

import dev.isxander.modstitch.base.extensions.modstitch
import dev.isxander.modstitch.util.ExtensionGetter
import dev.isxander.modstitch.util.NotExistsDelegate
import dev.isxander.modstitch.util.NotExistsNullableDelegate
import net.neoforged.moddevgradle.dsl.ModDevExtension
import org.gradle.api.Action
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import net.neoforged.moddevgradle.dsl.RunModel
import net.neoforged.moddevgradle.legacyforge.dsl.LegacyForgeExtension
import net.neoforged.moddevgradle.legacyforge.dsl.MixinExtension
import net.neoforged.moddevgradle.legacyforge.dsl.ObfuscationExtension
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject
import org.gradle.kotlin.dsl.*

interface BaseModDevGradleExtension {
    /**
     * Configures and enables ModDevGradle for this project.
     * Within [MDGEnableConfiguration] you can set the versions of NeoForge, Forge, NeoForm, and MCP.
     * You must call [enable] exactly once in your build script.
     */
    fun enable(action: Action<MDGEnableConfiguration>)

    /**
     * The underlying platform-specific extension: `neoForge`
     */
    val neoforgeExtension: ModDevExtension
    /**
     * Configures the NeoForge extension.
     * This action will only be executed if the current platform is ModDevGradle.
     */
    fun configureNeoforge(action: Action<ModDevExtension>) = action.execute(neoforgeExtension)

    /**
     * The underlying platform-specific extension: `obfuscation`
     * Accessing this property will throw an exception if the current platform is not ModDevGradle Legacy.
     */
    val obfuscationExtension: ObfuscationExtension
    /**
     * Configures the Obfuscation extension.
     * This action will only be executed if the current platform is ModDevGradle Legacy.
     */
    fun configureObfuscation(action: Action<ObfuscationExtension>) = action.execute(obfuscationExtension)

    /**
     * The underlying platform-specific extension: `mixin`
     * Accessing this property will throw an exception if the current platform is ModDevGradle Legacy.
     */
    val mixinExtension: MixinExtension
    /**
     * Configures the Mixin extension.
     * This action will only be executed if the current platform is ModDevGradle Legacy.
     */
    fun configureMixin(action: Action<MixinExtension>) = action.execute(mixinExtension)

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
    private val enableConfiguration: MDGEnableConfigurationInternal,
    private val type: MDGType,
) : BaseModDevGradleExtension {
    override fun enable(action: Action<MDGEnableConfiguration>) {
        action.execute(enableConfiguration)
        enableConfiguration.enable(project)
    }

    override val neoforgeExtension: ModDevExtension by ExtensionGetter(project)
    override val mixinExtension: MixinExtension by ExtensionGetter(project)
    override fun configureMixin(action: Action<MixinExtension>) =
        if (type == MDGType.Legacy) super.configureMixin(action) else {}
    override val obfuscationExtension: ObfuscationExtension by ExtensionGetter(project)
    override fun configureObfuscation(action: Action<ObfuscationExtension>) =
        if (type != MDGType.Legacy) super.configureObfuscation(action) else {}

    override fun defaultRuns(client: Boolean, server: Boolean, namingConvention: (String, String) -> String) {
        val project = project
        val name = when {
            !enableConfiguration.neoForgeVersion.isNullOrEmpty() -> "NeoForge"
            !enableConfiguration.forgeVersion.isNullOrEmpty() -> "Forge"
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
    override fun enable(action: Action<MDGEnableConfiguration>) {}

    override val neoforgeExtension: NeoForgeExtension by NotExistsDelegate()
    override val mixinExtension: MixinExtension by NotExistsDelegate()
    override val obfuscationExtension: ObfuscationExtension by NotExistsDelegate()

    override fun defaultRuns(client: Boolean, server: Boolean, namingConvention: (String, String) -> String) {}
}

sealed interface MDGEnableConfiguration {
    var neoForgeVersion: String?
    var forgeVersion: String?
    var neoFormVersion: String?
    var mcpVersion: String?
}
sealed class MDGEnableConfigurationInternal(protected val impl: BaseModdevgradleImpl) : MDGEnableConfiguration {
    internal open fun enable(target: Project) {
        impl.enable(target, this)
    }
}
class RegularEnableConfiguration(impl: BaseModdevgradleImpl, private val extension: NeoForgeExtension) : MDGEnableConfigurationInternal(impl) {
    override var neoForgeVersion: String? = null
    override var neoFormVersion: String? = null
    override var forgeVersion: String? by NotExistsNullableDelegate()
    override var mcpVersion: String? by NotExistsNullableDelegate()

    override fun enable(target: Project) {
        extension.enable {
            version = this@RegularEnableConfiguration.neoForgeVersion
            neoFormVersion = this@RegularEnableConfiguration.neoFormVersion
        }
        super.enable(target)
    }
}
class LegacyEnableConfiguration(impl: BaseModdevgradleImpl, private val extension: LegacyForgeExtension) : MDGEnableConfigurationInternal(impl) {
    override var neoForgeVersion: String? by NotExistsNullableDelegate()
    override var neoFormVersion: String? by NotExistsNullableDelegate()
    override var forgeVersion: String? = null
    override var mcpVersion: String? = null

    override fun enable(target: Project) {
        extension.enable {
            forgeVersion = this@LegacyEnableConfiguration.forgeVersion
            mcpVersion = this@LegacyEnableConfiguration.mcpVersion
        }
        super.enable(target)
    }
}

val Project.msModdevgradle: BaseModDevGradleExtension
    get() = extensions.getByType<BaseModDevGradleExtension>()
fun Project.msModdevgradle(block: BaseModDevGradleExtension.() -> Unit) {
    if (project.modstitch.isModDevGradle) {
        msModdevgradle.block()
    }
}