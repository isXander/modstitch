package dev.isxander.modstitch.util

interface SyntaxNodeType<T> {
    fun tryParse(string: String): T?
}

class MappedSyntaxNodeType<I, O>(private val nodeType: SyntaxNodeType<I>, private val mapper: (I) -> O?) : SyntaxNodeType<O> {
    override fun tryParse(string: String): O? {
        return nodeType.tryParse(string)?.let(mapper)
    }
}

fun <I, O> SyntaxNodeType<I>.map(mapper: (I) -> O?): SyntaxNodeType<O> =
    MappedSyntaxNodeType(this, mapper)

class KeywordSyntaxNodeType(val keyword: String) : SyntaxNodeType<Unit> {
    override fun tryParse(string: String): Unit? {
        if (string == keyword) return Unit
        return null
    }
}

interface SyntaxTree<R : Any> {
    fun parse(words: List<String>): R?

    /**
     * Throws a syntax error with the given reason.
     */
    fun syntaxError(reason: String): Nothing {
        error("SyntaxTree syntax error: $reason")
    }

    operator fun <U : Any> Branch<U, R>.unaryPlus()

    /**
     * Creates a branch from a node type with an action block.
     * The action receives the parsed value and can add more branches or set a leaf.
     */
    operator fun <T : Any> SyntaxNodeType<T>.invoke(block: SyntaxSubTree<T, R>.(T) -> Unit): Branch<T, R> {
        return Branch(this, block)
    }
}

/**
 * A syntax tree node that can parse a sequence of words.
 * Each node matches one word using its [SyntaxNodeType].
 *
 * @param T The type of value this node produces when parsing
 * @param node The node type used to parse a single word
 */
class SyntaxSubTree<T : Any, R : Any>(val node: SyntaxNodeType<T>) : SyntaxTree<R> {
    private var leafProducer: (() -> R)? = null
    private val branches = mutableListOf<Branch<*, R>>()

    /**
     * Adds a branch to this tree's possible continuations.
     */
    override operator fun <U : Any> Branch<U, R>.unaryPlus() {
        this@SyntaxSubTree.branches.add(this)
    }

    /**
     * Sets this node as a leaf, providing a value to return when parsing ends here.
     * The value can be any type - it will be returned from [parse].
     * If this tree has branches, they are tried first; the leaf is used as a fallback
     * or when no more words remain.
     */
    fun leaf(value: () -> R) {
        leafProducer = value
    }

    fun leaf(value: R) = leaf { value }

    /**
     * Parses the given words starting from the first word.
     * The first word must match this tree's node type.
     *
     * @return The leaf value if parsing succeeds, or null if no match
     */
    override fun parse(words: List<String>): R? {
        if (words.isEmpty()) return leafProducer?.invoke()

        // Match the first word with this tree's node
        node.tryParse(words[0]) ?: return null

        return parseFromIndex(words, 1)
    }

    /**
     * Internal parsing that continues from a given index.
     */
    internal fun parseFromIndex(words: List<String>, index: Int): R? {
        if (index >= words.size) {
            return leafProducer?.invoke()
        }

        // Try each branch to find a match
        for (branch in branches) {
            val result = branch.tryParse(words, index)
            if (result != null) return result
        }

        // No branch matched - return leaf if available
        return leafProducer?.invoke()
    }
}

/**
 * A branch represents a possible continuation in the syntax tree.
 * It holds a node type to match and an action to execute when matched.
 *
 * @param T The type of value produced by parsing
 * @param nodeType The node type used to match a word
 * @param action The action to execute when matched, which populates the subtree
 */
class Branch<T : Any, R : Any>(
    private val nodeType: SyntaxNodeType<T>,
    private val action: SyntaxSubTree<T, R>.(T) -> Unit
) {
    /**
     * Attempts to parse starting at the given index.
     *
     * @return The result if parsing succeeds, or null if the word doesn't match
     */
    fun tryParse(words: List<String>, index: Int): R? {
        if (index >= words.size) return null

        val parsed = nodeType.tryParse(words[index]) ?: return null

        // Create a new tree context and execute the action to populate it
        val subtree = SyntaxSubTree<T, R>(nodeType)
        subtree.action(parsed)

        return subtree.parseFromIndex(words, index + 1)
    }
}

/**
 * Creates a root syntax tree that doesn't match any specific keyword.
 * Parsing starts directly with the branches.
 */
fun <R : Any> syntaxTree(block: SyntaxTree<R>.() -> Unit): SyntaxTree<R> {
    return SyntaxTreeRoot<R>().apply(block)
}

/**
 * A root syntax tree that doesn't consume a word itself.
 * It serves as a container for branches that parse from the first word.
 */
class SyntaxTreeRoot<R : Any> : SyntaxTree<R> {
    private val branches = mutableListOf<Branch<*, R>>()

    /**
     * Adds a branch to this root's possible continuations.
     */
    override operator fun <U : Any> Branch<U, R>.unaryPlus() {
        this@SyntaxTreeRoot.branches.add(this)
    }

    /**
     * Parses the given words, trying each branch from the first word.
     *
     * @return The result if parsing succeeds, or null if no branch matches
     */
    override fun parse(words: List<String>): R? {
        if (words.isEmpty()) return null

        for (branch in branches) {
            val result = branch.tryParse(words, 0)
            if (result != null) return result
        }

        return null
    }
}

