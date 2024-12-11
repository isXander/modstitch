package dev.isxander.modstitch.util

import kotlin.reflect.KClass

data class PlatformExtensionInfo<T : Any>(
    val name: String,
    val api: KClass<T>,
    val realImpl: KClass<out T>,
    val dummyImpl: KClass<out T>
)