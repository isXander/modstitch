package dev.isxander.modstitch.base

/**
 * The "common" extension for all loaders, as much configuration is done in this extension
 * such as configuring Minecraft version, mappings, mixins etc.
 */

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.mapProperty
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

open class ModStitchExtension @Inject constructor(objects: ObjectFactory) {
    // General setup for the mod environment.
    val minecraftVersion = objects.property<String>()
    val javaTarget = objects.property<Int>()

    val parchment = objects.newInstance<ParchmentBlock>(objects)
    init { parchment.minecraftVersion.convention(minecraftVersion) }
    fun parchment(action: Action<ParchmentBlock>) = action.execute(parchment)


    val manifest = objects.newInstance<ManifestBlock>(objects)
    /**
     * Used for injection into mod manifest files like `fabric.mod.json` and `neoforge.mods.toml`.
     * ...and other miscellaneous uses by other extensions, like publishing.
     */
    fun manifest(action: Action<ManifestBlock>) = action.execute(manifest)


    // Abstracted configurations for mod-specific dependencies. Loom uses a `mod` prefix for configurations.
    val _implementation = objects.property<Configuration>()
    val implementation: Configuration get() = _implementation.get()
    val _compileOnly = objects.property<Configuration>()
    val compileOnly: Configuration get() = _compileOnly.get()
    val _runtimeOnly = objects.property<Configuration>()
    val runtimeOnly: Configuration get() = _runtimeOnly.get()
    val _localRuntime = objects.property<Configuration>()
    val localRuntime: Configuration get() = _localRuntime.get()
}

// ------------------------------------
// Inner classes for block organisation
// ------------------------------------
open class ParchmentBlock @Inject constructor(objects: ObjectFactory) {
    val minecraftVersion = objects.property<String>()
    val mappingsVersion = objects.property<String>()
    val parchmentArtifact = objects.property<String>().convention(minecraftVersion.zip(mappingsVersion) { mc, mappings -> "org.parchmentmc.data:parchment-$mc:$mappings@zip" })
    val enabled = objects.property<Boolean>().convention(parchmentArtifact.map { it.isNotEmpty() }.orElse(false))
}

open class ManifestBlock @Inject constructor(objects: ObjectFactory) {
    /** Mods should use a `lower_snake_case` mod-id to obey the conventions of both mod loaders. */
    val modId = objects.property<String>().convention("unnamed_mod")
    val modName = objects.property<String>().convention("Unnamed Mod")
    val modVersion = objects.property<String>().convention("1.0.0")
    val modDescription = objects.property<String>().convention("")
    val modLicense = objects.property<String>().convention("All Rights Reserved")
    val modGroup = objects.property<String>().convention("com.example")
    val replacementProperties = objects.mapProperty<String, String>().convention(emptyMap())
}

operator fun ModStitchExtension.invoke(block: ModStitchExtension.() -> Unit) = block()
val Project.modStitch: ModStitchExtension
    get() = extensions.getByType(ModStitchExtension::class.java)

