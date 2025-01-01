package dev.isxander.modstitch.base.moddevgradle

import dev.isxander.modstitch.base.extensions.modstitch
import dev.isxander.modstitch.util.ExtensionGetter
import dev.isxander.modstitch.util.NotExistsDelegate
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
    fun enable(action: Action<MDGEnableConfiguration>)

    val neoforgeExtension: ModDevExtension
    fun configureNeoforge(action: Action<ModDevExtension>) = action.execute(neoforgeExtension)

    val obfuscationExtension: ObfuscationExtension
    fun configureObfuscation(action: Action<ObfuscationExtension>) = action.execute(obfuscationExtension)

    val mixinExtension: MixinExtension
    fun configureMixin(action: Action<MixinExtension>) = action.execute(mixinExtension)

    /**
     * Creates two run configurations: one for the client and one for the server.
     * [namingConvention] is a function that takes a string (the side, Client or Server) and returns the IDE name of the run config.
     */
    fun defaultRuns(namingConvention: (String) -> String = { "NeoForge $it" })
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

    override fun defaultRuns(namingConvention: (String) -> String) {
        configureNeoforge {
            runs {
                fun registerOrConfigure(name: String, action: Action<RunModel>) = action(maybeCreate(name))

                registerOrConfigure("client-${project.name}") {
                    client()
                    ideName = namingConvention("Client")
                }
                registerOrConfigure("server-${project.name}") {
                    server()
                    ideName = namingConvention("Server")
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

    override fun defaultRuns(namingConvention: (String) -> String) {}
}

sealed interface MDGEnableConfiguration {
    var neoForgeVersion: String?
    var forgeVersion: String?
    var neoFormVersion: String?
    var mcpVersion: String?
}
sealed class MDGEnableConfigurationInternal(protected val impl: BaseModdevgradleImpl) : MDGEnableConfiguration {
    internal abstract fun enable(target: Project)
}
class RegularEnableConfiguration(impl: BaseModdevgradleImpl, private val extension: NeoForgeExtension) : MDGEnableConfigurationInternal(impl) {
    override var neoForgeVersion: String? = null
    override var neoFormVersion: String? = null
    override var forgeVersion: String? by NotExistsDelegate()
    override var mcpVersion: String? by NotExistsDelegate()

    override fun enable(target: Project) {
        extension.enable {
            version = this@RegularEnableConfiguration.neoForgeVersion
            neoFormVersion = this@RegularEnableConfiguration.neoFormVersion
        }
        impl.enable(target)
    }
}
class LegacyEnableConfiguration(impl: BaseModdevgradleImpl, private val extension: LegacyForgeExtension) : MDGEnableConfigurationInternal(impl) {
    override var neoForgeVersion: String? by NotExistsDelegate()
    override var neoFormVersion: String? by NotExistsDelegate()
    override var forgeVersion: String? = null
    override var mcpVersion: String? = null

    override fun enable(target: Project) {
        extension.enable {
            forgeVersion = this@LegacyEnableConfiguration.forgeVersion
            mcpVersion = this@LegacyEnableConfiguration.mcpVersion
        }
    }
}

val Project.msModdevgradle: BaseModDevGradleExtension
    get() = extensions.getByType<BaseModDevGradleExtension>()
fun Project.msModdevgradle(block: BaseModDevGradleExtension.() -> Unit) {
    if (project.modstitch.isModDevGradle) {
        msModdevgradle.block()
    }
}