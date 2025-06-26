package dev.isxander.modstitch.util

import java.io.LineNumberReader
import java.io.Reader
import java.util.function.Function

/**
 * A configurable transformation pipeline that applies class, field, and method mappings to an input value of type [T].
 *
 * Typical usage:
 * ```
 * val foo = ...
 * val remappedFoo = MappingOperation<Foo>()
 *     .remapClass("com/example/MyClass") { foo, newName -> ... }
 *     .remapField("com/example/MyClass", "myField") { foo, newOwner, newName -> ... }
 *     .remapMember("com/example/MyClass", "myMethod", "(Ljava/lang/String;)V") { foo, newOwner, newName, newDesc -> ... }
 *     .loadMappings(reader)
 *     .apply(foo)
 * ```
 *
 * @param T The type of input this operation is applied to.
 */
internal class MappingOperation<T> : Function<T, T> {
    /** The loaded mappings, if any. */
    internal var mappings: Map<CharSequence, ClassMappingInfo>? = null

    /** The set of class names for which mappings are requested. */
    private val requestedMappings: MutableSet<CharSequence> = mutableSetOf()

    /** The list of individual mapping operations to apply to an input. */
    private val atomicOperations: MutableList<AtomicMappingOperation<T>> = mutableListOf()

    /**
     * Registers a remapping operation for a class.
     *
     * @param className The name of the class to be remapped.
     * @param result A function receiving the input and the mapped class name.
     * @return This instance, for fluent chaining.
     */
    fun remapClass(className: String, result: (T, String) -> T) : MappingOperation<T> {
        requestedMappings.add(className)
        atomicOperations.add(ClassMappingOperation(className, result))
        return this
    }

    /**
     * Registers a remapping operation for a field.
     *
     * @param className The class owning the field.
     * @param name The name of the field.
     * @param result A function receiving the input, the mapped class name, and the mapped field name.
     * @return This instance, for fluent chaining.
     */
    fun remapField(className: String, name: String, result: (T, String, String) -> T) : MappingOperation<T> {
        requestedMappings.add(className)
        atomicOperations.add(FieldMappingOperation(className, name, result))
        return this
    }

    /**
     * Registers a remapping operation for a method or a field with descriptor information.
     *
     * @param className The class owning the member.
     * @param name The name of the member.
     * @param desc The JVM descriptor of the member.
     * @param result A function receiving the input, the mapped class name, the mapped member name, and the mapped descriptor.
     * @return This instance, for fluent chaining.
     */
    fun remapMember(className: String, name: String, desc: String, result: (T, String, String, String) -> T) : MappingOperation<T> {
        requestedMappings.add(className)
        for (referencedClassName in splitDescriptor(desc).filter { it.startsWith('L') && it.endsWith(';') }) {
            requestedMappings.add(referencedClassName.subSequence(1, referencedClassName.length - 1))
        }
        atomicOperations.add(MemberMappingOperation(className, name, desc, this, result))
        return this
    }

    /**
     * Loads relevant mappings from the given [Reader].
     *
     * This method must be called **before** [apply].
     *
     * @param reader The source of mapping data.
     * @return This instance, for fluent chaining.
     */
    fun loadMappings(reader: Reader): MappingOperation<T> {
        mappings = loadMappings(reader, requestedMappings)
        return this
    }

    /**
     * Applies all registered mapping operations to the input.
     *
     * If no mapping data has been loaded, the input is returned unchanged.
     *
     * @param input The value to transform.
     * @return The result after applying all registered mapping operations.
     */
    override fun apply(input: T): T {
        val currentMappings = mappings
        if (currentMappings == null || currentMappings.isEmpty()) {
            return input
        }

        var result = input
        for (atomicOperation in atomicOperations) {
            result = atomicOperation.apply(result, currentMappings[atomicOperation.className])
        }
        return result
    }
}

/**
 * Represents the mapping information for a single class.
 *
 * @property className The original class name (e.g., `com/example/MyClass`).
 * @property mappedClassName The mapped class name.
 * @property fields The list of field mappings belonging to this class.
 * @property methods The list of method mappings belonging to this class.
 */
internal data class ClassMappingInfo(
    val className: String,
    val mappedClassName: String,
    val fields: List<MemberMappingInfo>,
    val methods: List<MemberMappingInfo>,
)

/**
 * Represents the mapping of a single class member (either a field or a method).
 *
 * @property name The original member name.
 * @property mappedName The mapped member name.
 * @property descriptor The original descriptor, if any.
 * @property mappedDescriptor The mapped descriptor, if any.
 */
internal data class MemberMappingInfo(
    val name: String,
    val mappedName: String,
    val descriptor: String? = null,
    val mappedDescriptor: String? = null,
)

/**
 * A single remapping operation targeting a specific class.
 */
private interface AtomicMappingOperation<T> {
    /** The class this operation targets. */
    val className: String

    /**
     * Applies the remapping operation using the available class mapping info.
     *
     * @param input The value to transform.
     * @param mappings The mapping info for the target class, or `null` if unavailable.
     * @return The transformed result.
     */
    fun apply(input: T, mappings: ClassMappingInfo?) : T
}

/**
 * A class remapping operation that updates the class name.
 */
private class ClassMappingOperation<T>(
    override val className: String,
    private val selector: (T, String) -> T
) : AtomicMappingOperation<T> {
    override fun apply(input: T, mappings: ClassMappingInfo?): T =
        selector(input, mappings?.mappedClassName ?: className)
}

/**
 * A field remapping operation that updates the field name and the owner class.
 */
private class FieldMappingOperation<T>(
    override val className: String,
    val name: String,
    private val selector: (T, String, String) -> T
) : AtomicMappingOperation<T> {
    override fun apply(input: T, mappings: ClassMappingInfo?): T {
        val field = mappings?.fields?.firstOrNull { it.name == name }
        return selector(input, mappings?.mappedClassName ?: className, field?.mappedName ?: name)
    }
}

/**
 * A member remapping operation that updates its name, its descriptor, and the owner class.
 */
private class MemberMappingOperation<T>(
    override val className: String,
    val name: String,
    val descriptor: String,
    private val root: MappingOperation<T>,
    private val selector: (T, String, String, String) -> T
) : AtomicMappingOperation<T> {
    override fun apply(input: T, mappings: ClassMappingInfo?): T {
        val member = when {
            descriptor.startsWith('(') -> mappings?.methods?.firstOrNull { it.name == name && it.descriptor == descriptor }
            else -> mappings?.fields?.firstOrNull { it.name == name }
        }
        val mappedDesc = member?.mappedDescriptor ?: splitDescriptor(descriptor).map { when {
            it.startsWith('L') && it.endsWith(';') -> {
                val className = it.subSequence(1, it.length - 1)
                "L${root.mappings?.get(className)?.mappedClassName ?: className};"
            }
            else -> it
        }}.joinToString("")
        return selector(input, mappings?.mappedClassName ?: className, member?.mappedName ?: name, mappedDesc)
    }
}

/**
 * Splits a JVM descriptor into a sequence of components.
 *
 * @param desc The character sequence representing the descriptor.
 * @return A sequence of parts from the provided descriptor.
 */
private fun splitDescriptor(desc: CharSequence): Sequence<CharSequence> = sequence {
    var start = 0
    var i = 0
    while (i < desc.length) {
        val j = if (desc[i] == 'L') desc.indexOf(';', i) + 1 else 0
        if (j == 0) {
            i++
            continue
        }

        yield(desc.subSequence(start, i))
        yield(desc.subSequence(i, j))
        start = j
        i = j
    }

    yield(desc.subSequence(start, desc.length))
}

/**
 * Attempts to load class mappings from a text-based mapping file.
 *
 * @param reader A character stream from which mappings will be read.
 * @param requestedMappings A set of class names to load mappings for. If empty, all mappings are returned.
 * @return A map of class names to their corresponding [ClassMappingInfo] representations.
 */
private fun loadMappings(reader: Reader, requestedMappings: Set<CharSequence> = setOf()): Map<CharSequence, ClassMappingInfo> {
    val lineReader = LineNumberReader(reader)
    val header = lineReader.readUncommentedLine()
    return when {
        header == null -> mapOf()
        header.contains(" -> ") -> error("ProGuard mappings are not supported")
        header.startsWith("v1\t") -> error("TinyV1 mappings are not supported")
        header.startsWith("tiny\t") -> error("Tiny mappings are not supported")
        header.startsWith("tsrg2 ") -> error("TSRG2 mappings are not supported")
        header.startsWith("PK:") || header.startsWith("CL:") || header.startsWith("FD:") || header.startsWith("MD:") -> error("SRG mappings are not supported")
        else -> loadTSRG(lineReader, header, requestedMappings)
    }
}

/**
 * Loads mappings written in the TSRG format:
 *
 * ```
 * OldPackage/ NewPackage/
 * OldClass NewClass
 *     OldField NewField
 *     OldMethod OldDescriptor NewMethod
 * OldClass2 OldField2 NewField2
 * OldClass2 OldMethod2 OldDescriptor2 NewMethod2
 * ```
 *
 * @param reader The reader to read mappings from.
 * @param firstLine The first line of the file, already read.
 * @param requestedMappings A set of class names to load mappings for. If empty, all mappings are returned.
 * @return A map of class names to their corresponding [ClassMappingInfo] representations.
 */
private fun loadTSRG(reader: LineNumberReader, firstLine: CharSequence?, requestedMappings: Set<CharSequence>): Map<CharSequence, ClassMappingInfo> {
    fun LineNumberReader.tsrgError(message: String): Nothing =
        error("Failed to parse TSRG mappings: $message")

    val loadAll = requestedMappings.isEmpty()
    val limit = if (loadAll) Int.MAX_VALUE else (requestedMappings.size + 1)
    val mappings = mutableMapOf<CharSequence, ClassMappingInfo>()

    var currentClass: ClassMappingInfo? = null
    var line = firstLine
    while (line != null && mappings.size < limit) {
        val words = line.words(limit = 5)
        when {
            // We should get between 2 and 4 columns, depending on the entry type.
            words.size < 2 || words.size > 4 -> reader.tsrgError("Unexpected amount of columns: ${words.size}")

            // The current line depends on the previous class mapping entry.
            line[0].isWhitespace() -> when {
                // There is nothing we can do if the class has not been specified.
                currentClass == null -> reader.tsrgError("Missing class")

                // OldField NewField
                words.size == 2 -> (currentClass.fields as MutableList<MemberMappingInfo>).add(MemberMappingInfo(words[0], words[1]))

                // OldMethod OldDesc NewMethod
                words.size == 3 -> (currentClass.methods as MutableList<MemberMappingInfo>).add(MemberMappingInfo(words[0], words[2], words[1]))

                else -> reader.tsrgError("Unexpected amount of columns: ${words.size}")
            }

            // Since we don't remap packages, just skip 'em.
            words.size == 2 && words[0].endsWith('/') -> {}

            // Skip classes that we don't need.
            !loadAll && !requestedMappings.contains(words[0]) -> {
                line = reader.readUncommentedLine()
                while (!line.isNullOrEmpty() && line[0].isWhitespace()) {
                    line = reader.readUncommentedLine()
                }
                continue
            }

            else -> when (words.size) {
                // OldClass NewClass
                2 -> currentClass = ClassMappingInfo(words[0], words[1], mutableListOf(), mutableListOf()).also {
                    mappings[it.className] = it
                }

                // OldClass OldField NewField
                3 -> (mappings.computeIfAbsent(words[0]) {
                    ClassMappingInfo(words[0], words[0], mutableListOf(), mutableListOf())
                }.fields as MutableList<MemberMappingInfo>).add(MemberMappingInfo(words[1], words[2]))

                // OldClass OldMethod OldDesc NewMethod
                4 -> (mappings.computeIfAbsent(words[0]) {
                    ClassMappingInfo(words[0], words[0], mutableListOf(), mutableListOf())
                }.methods as MutableList<MemberMappingInfo>).add(MemberMappingInfo(words[1], words[3], words[2]))
            }
        }
        line = reader.readUncommentedLine()
    }
    return mappings
}
