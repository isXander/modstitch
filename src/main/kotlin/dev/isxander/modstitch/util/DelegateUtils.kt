package dev.isxander.modstitch.util

import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.kotlin.dsl.getByType
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class NotExistsDelegate<T> : ReadOnlyProperty<Any, T> {
    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        error("Property ${property.name} does not exist")
    }
}

class ExtensionGetter<T : Any>(
    private val container: ExtensionAware,
    private val extensionType: KClass<T>
) : ReadOnlyProperty<Any, T> {
    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        return container.extensions.getByType(extensionType)
    }
}
inline fun <reified T : Any> ExtensionGetter(container: ExtensionAware) =
    ExtensionGetter(container, T::class)