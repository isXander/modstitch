package dev.isxander.modstitch.util

import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.kotlin.dsl.getByType
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class NotExistsDelegate<T> : ReadWriteProperty<Any, T> {
    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        error("Property ${property.name} does not exist")
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        error("Property ${property.name} does not exist")
    }
}

class NotExistsNullableDelegate<T> : ReadWriteProperty<Any, T?> {
    override fun getValue(thisRef: Any, property: KProperty<*>): T? {
        return null
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T?) {
        error("Property ${property.name} does not exist")
    }
}

class ExtensionGetter<T : Any>(
    @Transient private val container: ExtensionAware,
    private val extensionType: Class<T>
) : ReadOnlyProperty<Any, T> {
    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        return container.extensions.getByType(extensionType)
    }
}
inline fun <reified T : Any> ExtensionGetter(container: ExtensionAware) =
    ExtensionGetter(container, T::class.java)