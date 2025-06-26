package dev.isxander.modstitch.util

/**
 * Adds a given [prefix] to the beginning of the string and capitalizes the first character
 * of the original string to form a camel-case-like identifier.
 *
 * @param prefix The string to prepend to the original string.
 * @return A new string with the prefix added and the first character of the original string capitalized.
 */
internal fun String.addCamelCasePrefix(prefix: String): String =
    replaceFirstChar { prefix + it.uppercaseChar() }

/**
 * Splits the given [CharSequence] into a list of words using whitespace as the delimiter.
 *
 * @param limit An optional limit on the number of words to return.
 * @return A list of words extracted from the string.
 */
internal fun CharSequence.words(limit: Int = 0): List<String> =
    trim().split(WHITESPACE, limit)

private val WHITESPACE = Regex("\\s+")
