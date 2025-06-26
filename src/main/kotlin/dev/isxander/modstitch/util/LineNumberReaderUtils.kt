package dev.isxander.modstitch.util

import java.io.LineNumberReader

/**
 * Reads the next non-blank, non-commented line from the [LineNumberReader].
 *
 * If a line contains an inline comment, only the part before the `#` is returned.
 *
 * @return The uncommented portion of the next relevant line, or `null` if end of stream is reached.
 */
internal fun LineNumberReader.readUncommentedLine(): CharSequence? {
    while (true) {
        val line = readLine()
        if (line == null) {
            return null
        }

        val i = line.indexOfFirst { !it.isWhitespace() }
        if (i < 0 || line[i] == '#') {
            continue
        }

        val j = line.indexOf('#', i)
        return if (j < 0) line else line.subSequence(0, j)
    }
}

/**
 * Throws a [FormatException] with the given [message].
 *
 * @param message The error message to include in the exception.
 * @throws FormatException Always thrown with the provided message and current line number.
 */
internal fun LineNumberReader.error(message: String): Nothing =
    throw FormatException(message, lineNumber)

/**
 * Exception thrown when a format error occurs while reading a text stream.
 *
 * @param message The error message.
 * @param lineNumber The line number where the error was encountered.
 */
private class FormatException(message: String, val lineNumber: Int): Exception("$message:line $lineNumber")
