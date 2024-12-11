package dev.isxander.modstitch.base

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Provider

typealias ConfigurationResolver = NamedDomainObjectContainer<Configuration>.() -> Provider<Configuration>

class RemapConfigurations(
    val implementation: ConfigurationResolver,
    val compileOnly: ConfigurationResolver,
    val runtimeOnly: ConfigurationResolver,
    val localRuntime: ConfigurationResolver,
)