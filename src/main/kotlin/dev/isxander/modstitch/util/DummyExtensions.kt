package dev.isxander.modstitch.util

import kotlin.reflect.KClass

data class PlatformExtensionInfo<T : Any>(
    val name: String,
    val api: Class<T>,
    val realImpl: Class<out T>,
    val dummyImpl: Class<out T>
) {
    constructor(name: String, api: KClass<T>, realImpl: KClass<out T>, dummyImpl: KClass<out T>)
            : this(name, api.java, realImpl.java, dummyImpl.java)

    constructor(name: String, api: KClass<T>, realImpl: KClass<out T>)
            : this(name, api.java, realImpl.java, realImpl.java)
}