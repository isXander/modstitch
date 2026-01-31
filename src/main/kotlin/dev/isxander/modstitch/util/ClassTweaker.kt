package dev.isxander.modstitch.util

import java.io.LineNumberReader
import java.io.Reader
import java.io.StringWriter
import java.io.Writer
import kotlin.math.min

internal data class ClassTweaker(
    val format: ClassTweakerFormat,
    val entries: List<ClassTweakerEntry>,
    val namespace: ClassTweakerNamespace
) {
    /**
     * Converts this class tweaker to the specified format.
     *
     * @param targetFormat The desired format to convert to.
     * @return A new [ClassTweaker] in the target format.
     * @throws IllegalStateException If this instance uses a feature unsupported by the target format.
     */
    fun convertFormat(targetFormat: ClassTweakerFormat): ClassTweaker {
        targetFormat.validateCapabilities(this)
        return ClassTweaker(targetFormat, entries, namespace)
    }

    fun convertNamespace(targetNamespace: ClassTweakerNamespace): ClassTweaker {
        return ClassTweaker(format, entries, targetNamespace)
    }

    fun remap(mappings: Reader, targetNamespace: ClassTweakerNamespace): ClassTweaker {
        val op = MappingOperation<MutableList<ClassTweakerEntry>>()
        for (entry in entries) {
            when (entry) {
                is ClassTweakerEntry.AccessModifier.Class -> op.remapClass(entry.className) { acc, cls ->
                    acc.also { acc.add(ClassTweakerEntry.AccessModifier.Class(entry.modification, entry.transitive, entry.final, cls) ) }
                }
                is ClassTweakerEntry.AccessModifier.Method -> op.remapMember(entry.className, entry.methodName, entry.methodDescriptor) { acc, cls, name, desc ->
                    acc.also { acc.add(ClassTweakerEntry.AccessModifier.Method(entry.modification, entry.transitive, entry.final, cls, name, desc) ) }
                }
                is ClassTweakerEntry.AccessModifier.Field if entry.fieldDescriptor != null -> op.remapMember(entry.className, entry.fieldName, entry.fieldDescriptor) { acc, cls, name, desc ->
                    acc.also { acc.add(ClassTweakerEntry.AccessModifier.Field(entry.modification, entry.transitive, entry.final, cls, name, desc) ) }
                }
                is ClassTweakerEntry.AccessModifier.Field -> op.remapField(entry.className, entry.fieldName) { acc, cls, name ->
                    acc.also { acc.add(ClassTweakerEntry.AccessModifier.Field(entry.modification, entry.transitive, entry.final, cls, name, null) ) }
                }
                // FIXME: interface injections require two separate classes to be remapped but these operations do not support that.
                is ClassTweakerEntry.InjectInterface -> error("Cannot yet remap interface injections")
            }
        }

        val remappedEntries = op.loadMappings(mappings).apply(mutableListOf())
        return ClassTweaker(format, remappedEntries, targetNamespace)
    }

    fun write(writer: Writer) {
        when (format) {
            ClassTweakerFormat.AT -> writeAT(writer)
            ClassTweakerFormat.CT, ClassTweakerFormat.AW_V1, ClassTweakerFormat.AW_V2 -> writeCTAW(writer)
        }
    }

    private fun writeAT(writer: Writer) {
        for (entry in entries) {
            when (entry) {
                is ClassTweakerEntry.AccessModifier -> {
                    writer.append(when (entry.modification) {
                        ClassTweakerEntry.AccessModifier.Modification.Protected -> "protected"
                        ClassTweakerEntry.AccessModifier.Modification.Default -> "default"
                        ClassTweakerEntry.AccessModifier.Modification.Private -> "private"
                        else -> "public"
                    }).append(when (entry.final) {
                        true -> "+f"
                        false -> "-f"
                        null -> ""
                    }).append(' ')

                    // AT uses dots instead of slashes here (and only here!) for some reason.
                    for (c in entry.className) {
                        writer.append(if (c == '/') '.' else c)
                    }

                    when (entry) {
                        is ClassTweakerEntry.AccessModifier.Class -> writer.appendLine()
                        is ClassTweakerEntry.AccessModifier.Field -> writer
                            .append(' ')
                            .append(entry.fieldName)
                            .append(' ')
                        // Forge's AccessTransformers don't use field descriptors
                        is ClassTweakerEntry.AccessModifier.Method -> writer
                            .append(' ')
                            .append(entry.methodName)
                            .appendLine(entry.methodDescriptor)
                    }
                }

                is ClassTweakerEntry.InjectInterface -> error("Unsupported entry type for AT format")
            }
        }
    }

    private fun writeCTAW(writer: Writer) {
        writer.append(when (format) {
            ClassTweakerFormat.AW_V1 -> "accessWidener v1 "
            ClassTweakerFormat.AW_V2 -> "accessWidener v2 "
            ClassTweakerFormat.CT -> "classTweaker v1 "
            else -> error("Unsupported format for CT/AW writing: $format")
        }).appendLine(when (namespace) {
            ClassTweakerNamespace.Official -> "official"
            ClassTweakerNamespace.Named -> "named"
            ClassTweakerNamespace.Intermediary -> "intermediary"
        })

        for (entry in entries) {
            when (entry) {
                is ClassTweakerEntry.AccessModifier -> {
                    fun writeEntry(modifier: String) {
                        if (entry.transitive) {
                            writer.write("transitive-")
                        }
                        writer.append(modifier).append(' ')

                        writer.appendLine(when (entry) {
                            is ClassTweakerEntry.AccessModifier.Class -> "class ${entry.className}"
                            is ClassTweakerEntry.AccessModifier.Method -> "method ${entry.className} ${entry.methodName} ${entry.methodDescriptor}"
                            is ClassTweakerEntry.AccessModifier.Field -> "field ${entry.className} ${entry.fieldName} ${entry.fieldDescriptor}"
                        })
                    }

                    if (entry.final == false) {
                        writeEntry(if (entry is ClassTweakerEntry.AccessModifier.Field) "mutable" else "extendable")

                        // An "extendable" class is implicitly also "accessible"
                        if (entry is ClassTweakerEntry.AccessModifier.Class) {
                            continue
                        }
                    }
                    if (entry.modification == ClassTweakerEntry.AccessModifier.Modification.Public) {
                        writeEntry("accessible")
                    }
                }
                is ClassTweakerEntry.InjectInterface -> {
                    writer
                        .append(if (entry.transitive) "transitive-" else "")
                        .append("inject-interface ")
                        .append(entry.targetClass)
                        .append(' ')
                        .append(entry.interfaceToInject)
                }
            }
        }
    }

    override fun toString(): String {
        val writer = StringWriter()
        write(writer)
        return writer.toString()
    }

    companion object {
        fun parse(reader: Reader): ClassTweaker {
            val lineReader = LineNumberReader(reader)
            val header = lineReader.readUncommentedLine()

            // An empty file is a perfectly valid access transformer
            if (header == null) {
                return ClassTweaker(ClassTweakerFormat.AT, listOf(), ClassTweakerNamespace.Official)
            }

            // AW headers begin with the word "accessWidener"
            val (format, namespace) = if (header.startsWith("accessWidener", ignoreCase = false) || header.startsWith("classTweaker", ignoreCase = false)) {
                val headerData = header.words(limit = 3)

                if (headerData.size != 3) {
                    error("Unexpected header data: $headerData")
                }

                val type = headerData[0].lowercase()
                val version = headerData[1]
                val namespace = when (headerData[2]) {
                    "official" -> ClassTweakerNamespace.Official
                    "named" -> ClassTweakerNamespace.Named
                    "intermediary" -> ClassTweakerNamespace.Intermediary
                    else -> error("Unexpected namespace: ${headerData[2]}")
                }
                val format = when (type) {
                    "accesswidener" if version == "v1" ->
                        ClassTweakerFormat.AW_V1
                    "accesswidener" if version == "v2" ->
                        ClassTweakerFormat.AW_V2
                    "classtweaker" if version == "v1" ->
                        ClassTweakerFormat.CT
                    "accesswidener" if version == "v3" ->
                        ClassTweakerFormat.CT
                    else -> error("Unexpected header type: `$header`")
                }

                format to namespace
            } else {
                ClassTweakerFormat.AT to ClassTweakerNamespace.Official
            }

            val lines = if (format == ClassTweakerFormat.AT) {
                sequenceOf(header) + lineReader.lineSequence()
            } else {
                lineReader.lineSequence()
            }

            val entries = lines
                .map { line -> line.trimStart() }
                .filter { !it.startsWith('#') }
                .map { line -> line.removeComment() }
                .filter { line -> line.isNotBlank() }
                .map { line -> line.words() }
                .filter { words -> words.isNotEmpty() }
                .map { words -> format.syntaxTree.parse(words) ?: lineReader.error("Failed to parse $words") }
                .toList()

            return ClassTweaker(format, entries, namespace)
        }
    }
}

internal sealed interface ClassTweakerEntry {
    val transitive: Boolean

    sealed interface AccessModifier : ClassTweakerEntry {
        val modification: Modification
        override val transitive: Boolean
        val final: Boolean?
        val className: String

        data class Class(
            override val modification: Modification,
            override val transitive: Boolean,
            override val final: Boolean?,
            override val className: String,
        ) : AccessModifier

        data class Method(
            override val modification: Modification,
            override val transitive: Boolean,
            override val final: Boolean?,
            override val className: String,
            val methodName: String,
            val methodDescriptor: String,
        ) : AccessModifier

        data class Field(
            override val modification: Modification,
            override val transitive: Boolean,
            override val final: Boolean?,
            override val className: String,
            val fieldName: String,
            val fieldDescriptor: String?,
        ) : AccessModifier

        enum class Modification {
            /** The target keeps its original visibility modifier. */
            Unset,

            /** The target is made private. */
            Private,

            /** The target is made package-private. */
            Default,

            /** The target is made protected. */
            Protected,

            /** The target is made public. */
            Public,
        }
    }

    data class InjectInterface(
        val targetClass: String,
        val interfaceToInject: String,
        override val transitive: Boolean,
    ) : ClassTweakerEntry
}

/**
 * Represents the supported class tweaker formats.
 */
internal enum class ClassTweakerFormat {
    /** (Neo)Forge's `accesstransformer.cfg`. */
    AT,

    /** Fabric's `accessWidener v1`. */
    AW_V1,

    /** Fabric's `accessWidener v2`. */
    AW_V2,

    /** Fabric's `classTweaker v1` aka AW_V3 */
    CT;

    val syntaxTree: SyntaxTree<ClassTweakerEntry>
        get() = SYNTAX_BY_FORMAT[this]!!

    /**
     * @throws IllegalStateException if the provided class tweaker contains any entries not supported by this format
     */
    fun validateCapabilities(classTweaker: ClassTweaker) {
        if (this != CT) {
            if (classTweaker.entries.any { it is ClassTweakerEntry.InjectInterface }) {
                error("Provided format ${classTweaker.format} does not support interface injection.")
            }
        }

        if (this != AT) {
            // Only AT supports making members less accessible than they already are.
            // Why would anyone need that?
            if (classTweaker.entries
                    .filterIsInstance<ClassTweakerEntry.AccessModifier>()
                    .any { it.final == true || it.final == null && it.modification != ClassTweakerEntry.AccessModifier.Modification.Public && it.modification != ClassTweakerEntry.AccessModifier.Modification.Unset }) {
                error("Provided format ${classTweaker.format} does not support restricting member accessibility")
            }

            // AT doesn't require a field descriptor for field entries.
            // But for some reason, AW does.
            if (classTweaker.entries
                    .filterIsInstance<ClassTweakerEntry.AccessModifier.Field>()
                    .any { it.fieldName.isBlank() }) {
                error("Provided format ${classTweaker.format} requires a field descriptor to be specified")
            }
        }
    }

    companion object {
        private val SYNTAX_BY_FORMAT = entries.associateWith { createSyntaxTree(it) }

        private fun createSyntaxTree(format: ClassTweakerFormat): SyntaxTree<ClassTweakerEntry> {
            return when (format) {
                AW_V1, AW_V2, CT -> createFabricSyntaxTree(format)
                AT -> createForgeLikeSyntaxTree()
            }
        }

        private fun createFabricSyntaxTree(format: ClassTweakerFormat) = syntaxTree<ClassTweakerEntry> {
            val accessModifierNode = when {
                format == AW_V1 -> AccessModifierNodeType.map {
                    TransitiveTaggedNodeType.TransitiveTaggedNode(
                        it,
                        false
                    )
                }

                else -> AccessModifierNodeType.transitiveTagged()
            }

            +accessModifierNode() { (modifier, transitive) ->
                val isFinal = when (modifier) {
                    AccessModifierNodeType.AccessModifierType.Accessible -> null
                    else -> false
                }

                +"class" {
                    +StringNodeType() { className ->
                        leaf {
                            ClassTweakerEntry.AccessModifier.Class(
                                modification = when (modifier) {
                                    AccessModifierNodeType.AccessModifierType.Accessible -> ClassTweakerEntry.AccessModifier.Modification.Public
                                    AccessModifierNodeType.AccessModifierType.Extendable -> ClassTweakerEntry.AccessModifier.Modification.Public
                                    else -> syntaxError("Unsupported access modifier for class")
                                },
                                transitive = transitive,
                                isFinal,
                                className
                            )
                        }
                    }
                }
                +"method" {
                    +word()() { className ->
                        +word()() { methodName ->
                            +word()() { methodDescriptor ->
                                leaf {
                                    ClassTweakerEntry.AccessModifier.Method(
                                        modification = when (modifier) {
                                            AccessModifierNodeType.AccessModifierType.Accessible -> ClassTweakerEntry.AccessModifier.Modification.Public
                                            AccessModifierNodeType.AccessModifierType.Extendable -> ClassTweakerEntry.AccessModifier.Modification.Protected
                                            else -> syntaxError("Unsupported access modifier for method")
                                        },
                                        transitive,
                                        isFinal,
                                        className, methodName, methodDescriptor
                                    )
                                }
                            }
                        }
                    }
                }
                +"field" {
                    +word()() { className ->
                        +word()() { fieldName ->
                            +word()() { fieldDescriptor ->
                                leaf {
                                    ClassTweakerEntry.AccessModifier.Field(
                                        modification = when (modifier) {
                                            AccessModifierNodeType.AccessModifierType.Accessible -> ClassTweakerEntry.AccessModifier.Modification.Public
                                            AccessModifierNodeType.AccessModifierType.Mutable -> ClassTweakerEntry.AccessModifier.Modification.Unset
                                            else -> syntaxError("Unsupported access modifier for field")
                                        },
                                        transitive,
                                        isFinal,
                                        className, fieldName, fieldDescriptor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (format == CT) {
                +literal("inject-interface").transitiveTagged()() { (_, transitive) ->
                    +word()() { className ->
                        +word()() { interfaceName ->
                            leaf {
                                ClassTweakerEntry.InjectInterface(
                                    className,
                                    interfaceName,
                                    transitive
                                )
                            }
                        }
                    }
                }
            }
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
        private fun createForgeLikeSyntaxTree() = syntaxTree<ClassTweakerEntry> {
            +AccessTransformerNodeType() { (access, final) ->
                +word()() { className ->
                    val className = className.replace('.', '/')

                    leaf {
                        ClassTweakerEntry.AccessModifier.Class(
                            access, transitive = false, final, className
                        )
                    }

                    +word()() { target ->
                        // target is either <methodName><methodDesc> OR <fieldName> if this is the leaf
                        leaf {
                            val descStart = target.indexOf('(')
                            if (descStart < 0) {
                                ClassTweakerEntry.AccessModifier.Field(
                                    access, transitive = false, final,
                                    className, target, fieldDescriptor = null,
                                )
                            } else {
                                ClassTweakerEntry.AccessModifier.Method(
                                    access, transitive = false, final,
                                    className,
                                    methodName = target.substring(0, descStart),
                                    methodDescriptor = target.substring(descStart),
                                )
                            }
                        }

                        +word()() { descriptor ->
                            leaf {
                                if (descriptor.startsWith('(')) {
                                    ClassTweakerEntry.AccessModifier.Method(
                                        access, transitive = false, final, className, target, descriptor
                                    )
                                } else {
                                    ClassTweakerEntry.AccessModifier.Field(
                                        access, transitive = false, final, className, target, descriptor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

internal enum class ClassTweakerNamespace {
    /** Used when no mappings are used, such as MDG or Loom-noremap */
    Official,
    /** Used when the tweaker is defined in custom mappings, like is common with Loom-remap */
    Named,
    /** Used when named mappings have been converted into an intermediary format */
    Intermediary,
}


object AccessModifierNodeType : SyntaxNodeType<AccessModifierNodeType.AccessModifierType> {
    override fun tryParse(string: String): AccessModifierType? {
        return when (string) {
            "accessible" -> AccessModifierType.Accessible
            "extendable" -> AccessModifierType.Extendable
            "mutable" -> AccessModifierType.Mutable
            else -> null
        }
    }

    enum class AccessModifierType {
        Accessible, Extendable, Mutable
    }
}

class TransitiveTaggedNodeType<T>(private val type: SyntaxNodeType<T>) : SyntaxNodeType<TransitiveTaggedNodeType.TransitiveTaggedNode<T>> {
    override fun tryParse(string: String): TransitiveTaggedNode<T>? {
        val transitiveIndex = string.indexOf("transitive-")
        if (transitiveIndex != 0) {
            // Not transitive
            val node = type.tryParse(string)
                ?: return null
            return TransitiveTaggedNode(node, false)
        }

        val subNodeString = string.substring("transitive-".length)
        val node = type.tryParse(subNodeString)
            ?: return null
        return TransitiveTaggedNode(node, true)
    }

    data class TransitiveTaggedNode<T>(val node: T, val isTransitive: Boolean)
}
fun <T> SyntaxNodeType<T>.transitiveTagged(): TransitiveTaggedNodeType<T> {
    return TransitiveTaggedNodeType(this)
}

internal object AccessTransformerNodeType : SyntaxNodeType<AccessTransformerNodeType.AccessTransformerNode> {
    val accessModifierDelimiters = charArrayOf('-', '+')

    override fun tryParse(string: String): AccessTransformerNode? {
        val modDelimiter = min(string.indexOfAny(accessModifierDelimiters).toUInt(), string.length.toUInt()).toInt()
        val accessType = when (string.subSequence(0, modDelimiter)) {
            "public" -> ClassTweakerEntry.AccessModifier.Modification.Public
            "protected" -> ClassTweakerEntry.AccessModifier.Modification.Protected
            "default" -> ClassTweakerEntry.AccessModifier.Modification.Unset
            "private" -> ClassTweakerEntry.AccessModifier.Modification.Private
            else -> return null
        }
        val isFinal = when (string.subSequence(modDelimiter, string.length)) {
            "+f" -> true
            "-f" -> false
            "" -> null
            else -> return null
        }

        return AccessTransformerNode(accessType, isFinal)
    }

    data class AccessTransformerNode(val modifier: ClassTweakerEntry.AccessModifier.Modification, val final: Boolean?)
}




