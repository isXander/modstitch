package dev.isxander.modstitch.base

import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer

sealed interface FutureNamedDomainObjectProvider<T : Any> {
    val name: String

    fun get(): T

    companion object {
        fun <T : Any> from(container: NamedDomainObjectContainer<T>, name: String): FutureNamedDomainObjectProvider<T> {
            return FutureNamedDomainObjectProviderImpl(container, name)
        }

        fun <T : Named> from(value: T): FutureNamedDomainObjectProvider<T> {
            return ResolvedNamedDomainObjectProviderImpl(value)
        }
    }
}

class FutureNamedDomainObjectProviderImpl<T : Any>(
    private val container: NamedDomainObjectContainer<T>,
    override val name: String
) : FutureNamedDomainObjectProvider<T> {
    override fun get(): T {
        return container.getByName(name)
    }
}

class ResolvedNamedDomainObjectProviderImpl<T : Named>(
    private val value: T
) : FutureNamedDomainObjectProvider<T> {
    override val name = value.name

    override fun get(): T = value
}