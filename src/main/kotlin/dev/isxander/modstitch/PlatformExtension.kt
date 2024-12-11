package dev.isxander.modstitch

import org.gradle.api.Action

interface PlatformExtension<T : Any> {
    fun current(configure: Action<T>)
}