package dev.isxander.modstitch.util

import java.io.LineNumberReader
import java.io.Reader
import java.io.StringWriter
import java.io.Writer
import kotlin.math.min

/**
 * Represents an access widener which allows modification of class/method/field access levels.
 *
 * @property format The format of the access widener.
 * @property entries The list of access modifications.
 * @property namespace The mapping namespace (e.g., "named", "intermediary", "official").
 */
internal data class AccessWidener(
    val format: AccessWidenerFormat,
    val entries: List<AccessWidenerEntry>,
    val namespace: String = "named",
) {
    /**
     * Converts this access widener to the specified format.
     *
     * @param targetFormat The desired format to convert to.
     * @return A new [AccessWidener] in the target format.
     * @throws IllegalStateException If this instance uses a feature unsupported by the target format.
     */
    fun convert(targetFormat: AccessWidenerFormat): AccessWidener {
        // Only AWv2 supports transitive entries.
        // Throw if the current instance contains any of those.
        if (targetFormat != AccessWidenerFormat.AW_V2 && entries.any { it.accessModifier.isTransitive }) {
            error("${targetFormat.name} does not support transitive access modifiers")
        }

        if (targetFormat != AccessWidenerFormat.AT) {
            // Only AT supports making members less accessible than they already are.
            // Why would anyone need that?
            if (entries.map { it.accessModifier }.any {
                it.isFinal == true || it.isFinal == null && it.type != AccessModifierType.PUBLIC && it.type != AccessModifierType.NONE
            }) {
                error("${targetFormat.name} does not support restricting member accessibility")
            }

            // AT doesn't require a field descriptor for field entries.
            // But for some reason, AW does.
            if (entries.any { it.type == AccessWidenerEntryType.FIELD && it.descriptor.isBlank() }) {
                error("${targetFormat.name} requires a field descriptor to be specified")
            }
        }

        // AT allows making a member public and non-final via a single entry.
        // AW - not so much.
        val convertedEntries = if (targetFormat == AccessWidenerFormat.AT) compactEntries(entries) else entries
        return AccessWidener(targetFormat, convertedEntries, namespace)
    }

    /**
     * Remaps this access widener using the provided mappings.
     *
     * @param mappings A reader to load the mapping data from.
     * @param targetNamespace The mapping namespace to remap this instance to.
     * @return A new [AccessWidener] with remapped names.
     */
    fun remap(mappings: Reader, targetNamespace: String = namespace): AccessWidener {
        val op = MappingOperation<MutableList<AccessWidenerEntry>>()
        for (entry in entries) {
            when {
                entry.type == AccessWidenerEntryType.CLASS -> op.remapClass(entry.className) { acc, x ->
                    acc.also { acc.add(AccessWidenerEntry(entry.type, entry.accessModifier, x, x, x)) }
                }

                entry.type == AccessWidenerEntryType.FIELD && entry.descriptor.isBlank() -> op.remapField(
                    entry.className,
                    entry.name
                ) { acc, cls, x ->
                    acc.also { acc.add(AccessWidenerEntry(entry.type, entry.accessModifier, cls, x, "")) }
                }

                else -> op.remapMember(entry.className, entry.name, entry.descriptor) { acc, cls, x, desc ->
                    acc.also { acc.add(AccessWidenerEntry(entry.type, entry.accessModifier, cls, x, desc)) }
                }
            }
        }

        val remappedEntries = op.loadMappings(mappings).apply(mutableListOf())
        return AccessWidener(format, remappedEntries, targetNamespace)
    }

    /**
     * Writes the access widener to the given writer using the current [format].
     *
     * @param writer A [Writer] to output to.
     */
    fun write(writer: Writer) {
        when (format) {
            AccessWidenerFormat.AT -> writeAT(writer)
            else -> writeAW(writer)
        }
    }

    /**
     * Writes entries in the Access Transformer format.
     *
     * @param writer The output writer.
     */
    private fun writeAT(writer: Writer) {
        for (entry in entries) {
            writer.append(when (entry.accessModifier.type) {
                AccessModifierType.PROTECTED -> "protected"
                AccessModifierType.DEFAULT -> "default"
                AccessModifierType.PRIVATE -> "private"
                else -> "public"
            }).append(when (entry.accessModifier.isFinal) {
                true -> "+f"
                false -> "-f"
                null -> ""
            }).append(' ')

            // AT uses dots instead of slashes here (and only here!) for some reason.
            for (c in entry.className) {
                writer.append(if (c == '/') '.' else c)
            }

            when (entry.type) {
                AccessWidenerEntryType.CLASS -> writer.appendLine()
                AccessWidenerEntryType.FIELD -> writer.append(' ').appendLine(entry.name)
                AccessWidenerEntryType.METHOD -> writer.append(' ').append(entry.name).appendLine(entry.descriptor)
            }
        }
    }

    /**
     * Writes entries in the Access Widener format.
     *
     * @param writer The output writer.
     */
    private fun writeAW(writer: Writer) {
        // Write the header, e.g.:
        // accessWidener v2 named
        //
        // Note: we use tabs instead of spaces here because Fabric's AW parser was very brittle
        // at the time of its inception. For example, I vividly remember crashing it by introducing
        // an empty trailing line in one of my AW files. So, since it was built around tabs, it's
        // better to stick with them to maintain compatibility with older versions of Fabric.
        writer.append("accessWidener").append('\t')
            .append(if (format == AccessWidenerFormat.AW_V1) "v1" else "v2").append('\t')
            .appendLine(namespace)

        for (entry in entries) {
            if (entry.accessModifier.isFinal == false) {
                writeAWEntry(writer, entry, if (entry.type == AccessWidenerEntryType.FIELD) "mutable" else "extendable")

                // An "extendable" class becomes public by default,
                // so there's no need to execute the next block.
                if (entry.type == AccessWidenerEntryType.CLASS) {
                    continue
                }
            }

            if (entry.accessModifier.type == AccessModifierType.PUBLIC) {
                writeAWEntry(writer, entry, "accessible")
            }
        }
    }

    /**
     * Writes a single entry in the Access Widener format.
     *
     * @param writer The output writer.
     * @param entry The entry to write.
     * @param modifier The string representing the access modifier (i.e., "mutable", "extendable", or "accessible").
     */
    private fun writeAWEntry(writer: Writer, entry: AccessWidenerEntry, modifier: String) {
        if (entry.accessModifier.isTransitive) {
            writer.write("transitive-")
        }
        writer.append(modifier).append('\t')

        writer.append(when (entry.type) {
            AccessWidenerEntryType.CLASS -> "class"
            AccessWidenerEntryType.METHOD -> "method"
            AccessWidenerEntryType.FIELD -> "field"
        }).append('\t')

        writer.append(entry.className)
        if (entry.type == AccessWidenerEntryType.CLASS) {
            writer.appendLine()
        } else {
            writer.append('\t').append(entry.name).append('\t').appendLine(entry.descriptor)
        }
    }

    /**
     * Returns a string representation of this access widener in its current [format].
     */
    override fun toString() = StringWriter().use { write(it); it.toString() }

    companion object {
        /**
         * Parses an access widener from the given [Reader] and automatically detects its format.
         *
         * @param reader A reader containing access widener contents.
         * @return A parsed [AccessWidener] instance.
         */
        fun parse(reader: Reader): AccessWidener {
            val lineReader = LineNumberReader(reader)
            val header = lineReader.readUncommentedLine()

            // An empty file is a perfectly valid access transformer.
            if (header == null) {
                return AccessWidener(AccessWidenerFormat.AT, listOf())
            }

            // AW headers begin with the word "accessWidener".
            // However, I'm not entirely sure if Loom ignores its case or not.
            if (header.startsWith("accessWidener", ignoreCase = true)) {
                return parseAW(lineReader, header)
            }

            // ATs don't have a header.
            return parseAT(lineReader, header)
        }

        /**
         * Parses a Fabric-style access widener.
         *
         * @param reader A reader containing access widener contents.
         * @param header The header line, already read.
         * @return A parsed [AccessWidener] instance.
         *
         * @see <a href="https://wiki.fabricmc.net/tutorial:accesswideners">FabricMC: Access Wideners</a>
         */
        private fun parseAW(reader: LineNumberReader, header: CharSequence): AccessWidener {
            fun LineNumberReader.awError(message: String): Nothing =
                error("Failed to parse access widener: $message")

            // Usually, an AW header should look like this:
            // accessWidener v2 named
            val headerData = header.words(limit = 3)
            val format = when (if (headerData.size > 1) headerData[1] else null) {
                "v1" -> AccessWidenerFormat.AW_V1
                "v2" -> AccessWidenerFormat.AW_V2
                else -> null
            }
            val namespace = if (headerData.size > 2) headerData[2].trimEnd() else null

            // Assume that the "accessWidener" part has already been checked by the caller.
            if (headerData.size != 3 || format == null || namespace.isNullOrBlank()) {
                reader.awError("Invalid header: '$header'")
            }

            val entries = mutableListOf<AccessWidenerEntry>()
            var line = reader.readUncommentedLine()
            while (line != null) {
                // We should get exactly 3 or 5 columns, depending on the entry type.
                val parts = line.words(limit = 6)
                if (parts.size < 3 || parts.size != if (parts[1] == "class") 3 else 5) {
                    reader.awError("Unexpected amount of columns: ${parts.size}")
                }

                // The entry type must be one of: "class", "field", or "method":
                // <access> class <className>
                // <access> field <className> <fieldName> <fieldDesc>
                // <access> method <className> <methodName> <methodDesc>
                val className = parts[2]
                val (entryType, name, descriptor) = when(parts[1]) {
                    "class" -> Triple(AccessWidenerEntryType.CLASS, className, className)
                    "field" -> Triple(AccessWidenerEntryType.FIELD, parts[3], parts[4])
                    "method" -> Triple(AccessWidenerEntryType.METHOD, parts[3], parts[4])
                    else -> reader.awError("Unknown member type: '${parts[1]}'")
                }

                // There are 3 access modifiers available in AW:
                //  - "accessible" - makes a member public.
                //  - "extendable" - makes a class public and non-final; makes a method protected and non-final
                //  - "mutable" - makes a field non-final.
                //
                // Since v2, all of them can optionally be prefixed with "transitive-".
                val mod = parts[0]
                val modDelimiter = mod.indexOf('-') + 1
                val modTransitive = mod.subSequence(0, modDelimiter)
                val modAccess = mod.subSequence(modDelimiter, mod.length)
                val isTransitive = when {
                    modTransitive == "transitive-" && format == AccessWidenerFormat.AW_V2 -> true
                    modTransitive == "" -> false
                    else -> reader.awError("Unknown access modifier: '$mod'")
                }
                val (accessType, isFinal) = when {
                    modAccess == "accessible" -> Pair(AccessModifierType.PUBLIC, null)
                    modAccess == "extendable" && entryType == AccessWidenerEntryType.CLASS -> Pair(AccessModifierType.PUBLIC, false)
                    modAccess == "extendable" && entryType == AccessWidenerEntryType.METHOD -> Pair(AccessModifierType.PROTECTED, false)
                    modAccess == "mutable" && entryType == AccessWidenerEntryType.FIELD -> Pair(AccessModifierType.NONE, false)
                    else -> reader.awError("Unknown access modifier: '$mod'")
                }

                entries.add(AccessWidenerEntry(entryType, AccessModifier(accessType, isTransitive, isFinal), className, name, descriptor))
                line = reader.readUncommentedLine()
            }

            return AccessWidener(format, entries, namespace)
        }

        /**
         * Parses a (Neo)Forge-style access transformer.
         *
         * @param reader A reader containing access widener contents.
         * @param firstLine The first line of the file, already read.
         * @return A parsed [AccessWidener] instance.
         *
         * @see <a href="https://docs.neoforged.net/docs/advanced/accesstransformers/">NeoForge: Access Transformers</a>
         */
        private fun parseAT(reader: LineNumberReader, firstLine: CharSequence?): AccessWidener {
            val accessModifierDelimiters = charArrayOf('-', '+')
            fun LineNumberReader.atError(message: String): Nothing =
                error("Failed to parse access transformer: $message")

            val entries = mutableListOf<AccessWidenerEntry>()
            var line = firstLine
            while (line != null) {
                // We should get from 2 to 4 columns, depending on the entry type.
                val parts = line.words(limit = 5)
                if (parts.size < 2 || parts.size > 4) {
                    reader.atError("Unexpected amount of columns: ${parts.size}")
                }

                // There are 4 access levels: "public", "protected", "default", and "private".
                // Each may optionally include a "-f" or "+f" modifier, indicating whether
                // the target member should lose or gain the final modifier, respectively.
                val mod = parts[0]
                val modDelimiter = min(mod.indexOfAny(accessModifierDelimiters).toUInt(), mod.length.toUInt()).toInt()
                val accessType = when (mod.subSequence(0, modDelimiter)) {
                    "public" -> AccessModifierType.PUBLIC
                    "protected" -> AccessModifierType.PROTECTED
                    "default" -> AccessModifierType.DEFAULT
                    "private" -> AccessModifierType.PRIVATE
                    else -> reader.atError("Unknown access modifier: '$mod'")
                }
                val isFinal = when (mod.subSequence(modDelimiter, mod.length)) {
                    "+f" -> true
                    "-f" -> false
                    "" -> null
                    else -> reader.atError("Unknown access modifier: '$mod'")
                }

                // There are 4 types of entries:
                // <access> <className>
                // <access> <className> <fieldName>
                // <access> <className> <methodName><methodDesc>
                // <access> <className> <fieldOrMethodName> <fieldOrMethodDesc>
                //
                // We can differentiate between them based on the number of columns and whether
                // the descriptor starts with '(' (indicating a method descriptor) or not
                // (indicating a field descriptor, if any).
                val className = parts[1].replace('.', '/')
                val descStart = if (parts.size == 3) parts[2].indexOf('(') else -1
                val (entryType, name, descriptor) = when {
                    parts.size == 4 -> Triple(if (parts[3].startsWith('(')) AccessWidenerEntryType.METHOD else AccessWidenerEntryType.FIELD, parts[2], parts[3])
                    parts.size == 3 && descStart < 0 -> Triple(AccessWidenerEntryType.FIELD, parts[2], "")
                    parts.size == 3 -> Triple(AccessWidenerEntryType.METHOD, parts[2].substring(0, descStart), parts[2].substring(descStart))
                    else -> Triple(AccessWidenerEntryType.CLASS, className, className)
                }

                entries.add(AccessWidenerEntry(entryType, AccessModifier(accessType, isFinal = isFinal), className, name, descriptor))
                line = reader.readUncommentedLine()
            }

            return AccessWidener(AccessWidenerFormat.AT, entries)
        }

        /**
         * Merges and deduplicates provided entries.
         *
         * @param entries The list of access widener entries to compact.
         * @return A compacted list of deduplicated entries.
         */
        private fun compactEntries(entries: List<AccessWidenerEntry>): List<AccessWidenerEntry> {
            val entryMap = mutableMapOf<AccessWidenerEntryIdentity, AccessModifier>()
            for (entry in entries) {
                val identity = AccessWidenerEntryIdentity(entry.type, entry.className, entry.name, entry.descriptor)
                val curMod = entry.accessModifier
                val oldMod = entryMap[identity] ?: curMod
                val newMod = AccessModifier(
                    if (curMod.type > oldMod.type) curMod.type else oldMod.type,
                    curMod.isTransitive || oldMod.isTransitive,
                    when {
                        curMod.isFinal == false || oldMod.isFinal == false -> false
                        curMod.isFinal == true || oldMod.isFinal == true -> true
                        else -> null
                    },
                )
                entryMap[identity] = newMod
            }
            return entryMap.map { AccessWidenerEntry(it.key.type, it.value, it.key.className, it.key.name, it.key.descriptor) }
        }
    }
}

/**
 * Represents the supported access widener formats.
 */
internal enum class AccessWidenerFormat {
    /** (Neo)Forge's `accesstransformer.cfg`. */
    AT,

    /** Fabric's `accessWidener v1`. */
    AW_V1,

    /** Fabric's `accessWidener v2`. */
    AW_V2,
}

/**
 * Defines the types of access modifiers that can be applied to classes, methods, or fields.
 */
internal enum class AccessModifierType {
    /** The target keeps its original visibility modifier. */
    NONE,

    /** The target is made private. */
    PRIVATE,

    /** The target is made package-private. */
    DEFAULT,

    /** The target is made protected. */
    PROTECTED,

    /** The target is made public. */
    PUBLIC,
}

/**
 * Represents an access modifier with additional attributes.
 *
 * @property type The type of access modifier.
 * @property isTransitive Indicates whether the access change should be applied transitively.
 * @property isFinal Specifies whether the target element should be marked as `final`. `null` indicates no explicit change.
 */
internal data class AccessModifier(
    val type: AccessModifierType,
    val isTransitive: Boolean = false,
    val isFinal: Boolean? = null
)

/**
 * Represents the type of access widener entry.
 */
internal enum class AccessWidenerEntryType {
    /** A class-level access widener entry. */
    CLASS,

    /** A method-level access widener entry. */
    METHOD,

    /** A field-level access widener entry. */
    FIELD,
}

/**
 * Represents a single access widener rule entry that modifies a target's visibility or attributes.
 *
 * @property type The type of the entry (class, method, or field).
 * @property accessModifier The access modifier to apply.
 * @property className The internal name of the class being modified (e.g., `com/example/MyClass`).
 * @property name The name of the method or field being modified.
 * @property descriptor The descriptor of the method or field.
 */
internal data class AccessWidenerEntry(
    val type: AccessWidenerEntryType,
    val accessModifier: AccessModifier,
    val className: String,
    val name: String,
    val descriptor: String
)

/**
 * Represents the identity of an access widener entry.
 *
 * Used for comparing or grouping entries by their structural identity.
 *
 * @property type The type of the entry (class, method, or field).
 * @property className The internal name of the class being modified (e.g., `com/example/MyClass`).
 * @property name The name of the method or field being modified.
 * @property descriptor The descriptor of the method or field.
 */
private data class AccessWidenerEntryIdentity(
    val type: AccessWidenerEntryType,
    val className: String,
    val name: String,
    val descriptor: String
)
