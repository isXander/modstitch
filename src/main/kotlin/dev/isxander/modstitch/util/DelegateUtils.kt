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
