package dev.isxander.modstitch.util

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class NotExistsDelegate<T> : ReadOnlyProperty<Any, T> {
    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        error("Property ${property.name} does not exist")
    }
}