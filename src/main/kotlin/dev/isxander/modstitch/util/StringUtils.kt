package dev.isxander.modstitch.util

fun String.addCamelCasePrefix(prefix: String): String =
    replaceFirstChar { prefix + it.uppercaseChar() }