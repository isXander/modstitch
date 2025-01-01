package dev.isxander.modstitch.util

import org.gradle.api.provider.Provider

fun <I1, I2, R> zip(i1: Provider<I1>, i2: Provider<I2>, f: (I1, I2) -> R): Provider<R> {
    return i1.zip(i2, f)
}

fun <I1, I2, I3, R> zip(i1: Provider<I1>, i2: Provider<I2>, i3: Provider<I3>, f: (I1, I2, I3) -> R): Provider<R> {
    return (i1.zip(i2) { i1v, i2v -> Pair(i1v, i2v) }).zip(i3) { (i1v, i2v), i3v -> f(i1v, i2v, i3v) }
}

fun <I1, I2, I3, I4, R> zip(i1: Provider<I1>, i2: Provider<I2>, i3: Provider<I3>, i4: Provider<I4>, f: (I1, I2, I3, I4) -> R): Provider<R> {
    return ((i1.zip(i2) { i1v, i2v -> Pair(i1v, i2v) }).zip(i3) { (i1v, i2v), i3v -> Triple(i1v, i2v, i3v) }).zip(i4) { (i1v, i2v, i3v), i4v -> f(i1v, i2v, i3v, i4v) }
}

