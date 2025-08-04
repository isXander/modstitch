package dev.isxander.modstitch.unit

import dev.isxander.modstitch.util.AccessModifier
import dev.isxander.modstitch.util.AccessModifierType
import dev.isxander.modstitch.util.AccessWidener
import dev.isxander.modstitch.util.AccessWidenerEntry
import dev.isxander.modstitch.util.AccessWidenerEntryType
import dev.isxander.modstitch.util.AccessWidenerFormat
import java.io.StringReader
import kotlin.test.Test
import kotlin.test.assertEquals

class AccessWidenerTest {
    companion object {
        internal fun sampleAW(format: AccessWidenerFormat): AccessWidener {
            val at = format == AccessWidenerFormat.AT
            return AccessWidener(
                format = format,
                entries = listOf(
                    AccessWidenerEntry(
                        AccessWidenerEntryType.CLASS,
                        AccessModifier(AccessModifierType.PUBLIC, isTransitive = false, isFinal = null),
                        "class/Name",
                        "class/Name",
                        "class/Name"
                    ),
                    AccessWidenerEntry(
                        AccessWidenerEntryType.METHOD,
                        AccessModifier(AccessModifierType.PROTECTED, isTransitive = false, isFinal = false),
                        "class/Name",
                        "methodName",
                        "()Lreturn/Type;"
                    ),
                    AccessWidenerEntry(
                        AccessWidenerEntryType.FIELD,
                        AccessModifier(AccessModifierType.PUBLIC, isTransitive = false, isFinal = null),
                        "class/Name",
                        "fieldName",
                        if (at) "" else "Lfield/Type;"
                    )
                )
            )
        }
    }

    @Test
    fun `AW parsing`() {
        // language=access widener
        val input = """
            accessWidener v1 named
            accessible class class/Name
            # this is a valid AW comment
            extendable method class/Name methodName ()Lreturn/Type; #comment
            accessible field class/Name fieldName Lfield/Type;
        """.trimIndent()

        val parsed = AccessWidener.Companion.parse(StringReader(input))

        assertEquals(
            sampleAW(AccessWidenerFormat.AW_V1),
            parsed,
            "AccessWidener parsed from AW format should match the expected sample"
        )
    }

    @Test
    fun `AT parsing`() {
        // language=access transformers
        val input = """
            # test comment
            public class.Name
            protected-f class.Name methodName()Lreturn/Type; #another comment
            public class.Name fieldName 
        """.trimIndent()

        val parsed = AccessWidener.Companion.parse(StringReader(input))

        assertEquals(
            sampleAW(AccessWidenerFormat.AT),
            parsed,
            "AccessWidener parsed from AT format should match the expected sample"
        )
    }

    @Test
    fun `AT reproducibility`() {
        val expected = sampleAW(AccessWidenerFormat.AT)
        val stringified = expected.toString()
        val parsed = AccessWidener.Companion.parse(StringReader(stringified))

        assertEquals(expected, parsed, "AccessWidener parsed from stringified version should match the original")
    }

    @Test
    fun `AW_V1 reproducibility`() {
        val expected = sampleAW(AccessWidenerFormat.AW_V1)
        val stringified = expected.toString()
        val parsed = AccessWidener.Companion.parse(StringReader(stringified))

        assertEquals(expected, parsed, "AccessWidener parsed from stringified version should match the original")
    }

    @Test
    fun `AW_V2 reproducibility`() {
        val expected = sampleAW(AccessWidenerFormat.AW_V2)
        val stringified = expected.toString()
        val parsed = AccessWidener.Companion.parse(StringReader(stringified))

        assertEquals(expected, parsed, "AccessWidener parsed from stringified version should match the original")
    }
}