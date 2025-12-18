package dev.isxander.modstitch.util

import java.io.LineNumberReader
import java.io.Reader
import kotlin.math.min

internal data class ClassTweaker(
    val format: ClassTweakerFormat,
    val entries: List<ClassTweakerEntry>,
    val namespace: ClassTweakerNamespace
) {
    /**
     * Converts this access widener to the specified format.
     *
     * @param targetFormat The desired format to convert to.
     * @return A new [AccessWidener] in the target format.
     * @throws IllegalStateException If this instance uses a feature unsupported by the target format.
     */
    fun convert(targetFormat: ClassTweakerFormat): ClassTweaker {
        targetFormat.validateCapabilities(this)
        return ClassTweaker(targetFormat, entries, namespace)
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

            val entries = lineReader.lineSequence()
                .map { line -> line.trimStart() }
                .filter { !it.startsWith('#') }
                .map { line -> line.words() }
                .map { words -> format.syntaxTree.parse(words) ?: lineReader.error("Failed to parse $words") }
                .toList()

            return ClassTweaker(format, entries, namespace)
        }
    }
}

internal sealed interface ClassTweakerEntry {
    sealed class AccessModifier(
        val access: AccessModifierType,
        val isTransitive: Boolean,
        val isFinal: Boolean? = null,
    ) : ClassTweakerEntry {
        class Class(
            access: AccessModifierType,
            isTransitive: Boolean,
            isFinal: Boolean?,
            val className: String,
        ) : AccessModifier(access, isTransitive)

        class Method(
            access: AccessModifierType,
            isTransitive: Boolean,
            isFinal: Boolean?,
            val className: String,
            val methodName: String,
            val methodDescriptor: String,
        ) : AccessModifier(access, isTransitive, isFinal)

        class Field(
            access: AccessModifierType,
            isTransitive: Boolean,
            isFinal: Boolean?,
            val className: String,
            val fieldName: String,
            val fieldDescriptor: String?,
        ) : AccessModifier(access, isTransitive, isFinal)

        enum class AccessModifierType {
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
        val isTransitive: Boolean,
    ) : ClassTweakerEntry
}

/**
 * Represents the supported access widener formats.
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

    val syntaxTree by lazy { createSyntaxTree(this) }

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
                    .any { it.isFinal == true || it.isFinal == null && it.access != ClassTweakerEntry.AccessModifier.AccessModifierType.Public && it.access != ClassTweakerEntry.AccessModifier.AccessModifierType.Unset }) {
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

                +KeywordSyntaxNodeType("class")() { _ ->
                    +StringNodeType() { className ->
                        leaf {
                            ClassTweakerEntry.AccessModifier.Class(
                                access = when (modifier) {
                                    AccessModifierNodeType.AccessModifierType.Accessible -> ClassTweakerEntry.AccessModifier.AccessModifierType.Public
                                    AccessModifierNodeType.AccessModifierType.Extendable -> ClassTweakerEntry.AccessModifier.AccessModifierType.Public
                                    else -> syntaxError("Unsupported access modifier for class")
                                },
                                isTransitive = transitive,
                                isFinal,
                                className
                            )
                        }
                    }
                }
                +KeywordSyntaxNodeType("method")() { _ ->
                    +StringNodeType() { className ->
                        +StringNodeType() { methodName ->
                            +StringNodeType() { methodDescriptor ->
                                leaf {
                                    ClassTweakerEntry.AccessModifier.Method(
                                        access = when (modifier) {
                                            AccessModifierNodeType.AccessModifierType.Accessible -> ClassTweakerEntry.AccessModifier.AccessModifierType.Public
                                            AccessModifierNodeType.AccessModifierType.Extendable -> ClassTweakerEntry.AccessModifier.AccessModifierType.Protected
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
                +KeywordSyntaxNodeType("field")() { _ ->
                    +StringNodeType() { className ->
                        +StringNodeType() { fieldName ->
                            +StringNodeType() { fieldDescriptor ->
                                leaf {
                                    ClassTweakerEntry.AccessModifier.Field(
                                        access = when (modifier) {
                                            AccessModifierNodeType.AccessModifierType.Accessible -> ClassTweakerEntry.AccessModifier.AccessModifierType.Public
                                            AccessModifierNodeType.AccessModifierType.Mutable -> ClassTweakerEntry.AccessModifier.AccessModifierType.Unset
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
                +KeywordSyntaxNodeType("inject-interface").transitiveTagged()() { (_, transitive) ->
                    +StringNodeType() { className ->
                        +StringNodeType() { interfaceName ->
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
                +StringNodeType() { className ->
                    val className = className.replace('.', '/')

                    leaf {
                        ClassTweakerEntry.AccessModifier.Class(
                            access, isTransitive = false, final, className
                        )
                    }

                    +StringNodeType() { target ->
                        // target is either <methodName><methodDesc> OR <fieldName> if this is the leaf
                        leaf {
                            val descStart = target.indexOf('(')
                            if (descStart < 0) {
                                ClassTweakerEntry.AccessModifier.Field(
                                    access, isTransitive = false, final,
                                    className, target, fieldDescriptor = null,
                                )
                            } else {
                                ClassTweakerEntry.AccessModifier.Method(
                                    access, isTransitive = false, final,
                                    className,
                                    methodName = target.substring(0, descStart),
                                    methodDescriptor = target.substring(descStart),
                                )
                            }
                        }

                        +StringNodeType() { descriptor ->
                            leaf {
                                if (descriptor.startsWith('(')) {
                                    ClassTweakerEntry.AccessModifier.Method(
                                        access, isTransitive = false, final, className, target, descriptor
                                    )
                                } else {
                                    ClassTweakerEntry.AccessModifier.Field(
                                        access, isTransitive = false, final, className, target, descriptor
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
        val cmdDelim = string.indexOf('-') + 1
        val cmdTransitive = string.subSequence(0, cmdDelim)
        val cmdName = string.subSequence(cmdDelim, string.length).toString()
        val isTransitive = when (cmdTransitive) {
            "transitive-" -> true
            "" -> false
            else -> return null
        }

        val node = type.tryParse(cmdName)
            ?: return null

        return TransitiveTaggedNode(node, isTransitive)
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
            "public" -> ClassTweakerEntry.AccessModifier.AccessModifierType.Public
            "protected" -> ClassTweakerEntry.AccessModifier.AccessModifierType.Protected
            "default" -> ClassTweakerEntry.AccessModifier.AccessModifierType.Unset
            "private" -> ClassTweakerEntry.AccessModifier.AccessModifierType.Private
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

    data class AccessTransformerNode(val modifier: ClassTweakerEntry.AccessModifier.AccessModifierType, val final: Boolean?)
}

object StringNodeType : SyntaxNodeType<String> {
    override fun tryParse(string: String): String? {
        return string
    }
}

