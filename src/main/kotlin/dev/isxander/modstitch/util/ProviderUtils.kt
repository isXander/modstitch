package dev.isxander.modstitch.util

import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

/**
 * If the Gradle property “something” is defined, apply the converted value
 * as the convention (default) for this `Property<T>`.
 * If the property is absent, nothing happens – the `Property` stays unset.
 */
fun <T : Any> Property<T>.propConvention(
    propertyProvider: Provider<String>,           // e.g. providers.gradleProperty("something")
    converter: (String) -> T                      // non-null converter
): Property<T> = apply {
    // `map` is lazy: converter runs only when the provider is present.
    convention(propertyProvider.map(converter))
}
/**
 * If the Gradle property “something” is defined, use it
 * as the convention (default) for this `Property<String>`.
 * If the property is absent, nothing happens – the `Property` stays unset.
 */
fun Property<String>.propConvention(propertyProvider: Provider<String>): Property<String> =
    propConvention(propertyProvider) { it }

fun <I1, I2, R> zip(i1: Provider<I1>, i2: Provider<I2>, f: (I1, I2) -> R): Provider<R> {
    return i1.zip(i2, f)
}

fun <I1, I2, I3, R> zip(i1: Provider<I1>, i2: Provider<I2>, i3: Provider<I3>, f: (I1, I2, I3) -> R): Provider<R> {
    return (i1.zip(i2) { i1v, i2v -> Pair(i1v, i2v) }).zip(i3) { (i1v, i2v), i3v -> f(i1v, i2v, i3v) }
}

fun <I1, I2, I3, I4, R> zip(i1: Provider<I1>, i2: Provider<I2>, i3: Provider<I3>, i4: Provider<I4>, f: (I1, I2, I3, I4) -> R): Provider<R> {
    return ((i1.zip(i2) { i1v, i2v -> Pair(i1v, i2v) }).zip(i3) { (i1v, i2v), i3v -> Triple(i1v, i2v, i3v) }).zip(i4) { (i1v, i2v, i3v), i4v -> f(i1v, i2v, i3v, i4v) }
}

