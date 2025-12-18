package dev.isxander.modstitch.unit

import dev.isxander.modstitch.util.ClassTweaker
import dev.isxander.modstitch.util.ClassTweakerEntry
import dev.isxander.modstitch.util.ClassTweakerFormat
import dev.isxander.modstitch.util.ClassTweakerNamespace
import java.io.StringReader
import kotlin.test.Test
import kotlin.test.assertEquals

class ClassTweakerTest {
    companion object {
        internal fun sampleCT(format: ClassTweakerFormat, namespace: ClassTweakerNamespace = ClassTweakerNamespace.Official): ClassTweaker {
            return ClassTweaker(
                format = format,
                entries = listOf(
                    ClassTweakerEntry.AccessModifier.Class(
                        ClassTweakerEntry.AccessModifier.Modification.Public,
                        transitive = false, final = null,
                        "class/Name"
                    ),
                    ClassTweakerEntry.AccessModifier.Method(
                        ClassTweakerEntry.AccessModifier.Modification.Protected,
                        transitive = false, final = false,
                        "class/Name",
                        "methodName",
                        "()Lreturn/Type;"
                    ),
                    ClassTweakerEntry.AccessModifier.Field(
                        ClassTweakerEntry.AccessModifier.Modification.Public,
                        transitive = false, final = null,
                        "class/Name",
                        "fieldName",
                        "Lfield/Type;"
                    ),
                    ClassTweakerEntry.InjectInterface(
                        "class/Name",
                        "interface/Name",
                        transitive = false
                    )
                ).let { list -> if (format != ClassTweakerFormat.CT) list.filterNot { it is ClassTweakerEntry.InjectInterface } else list },
                namespace = namespace
            )
        }
    }

    @Test
    fun `AW parsing`() {
        // language=access widener
        val input = """
            accessWidener v1 official
            accessible class class/Name
            # this is a valid AW comment
            extendable method class/Name methodName ()Lreturn/Type; #comment
            accessible field class/Name fieldName Lfield/Type;
        """.trimIndent()

        val parsed = ClassTweaker.parse(StringReader(input))

        assertEquals(
            sampleCT(ClassTweakerFormat.AW_V1),
            parsed,
            "ClassTweaker parsed from AW format should match the expected sample"
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

        val parsed = ClassTweaker.parse(StringReader(input))

        assertEquals(
            sampleCT(ClassTweakerFormat.AT),
            parsed,
            "ClassTweaker parsed from AT format should match the expected sample"
        )
    }

    @Test
    fun `AT reproducibility`() {
        val expected = sampleCT(ClassTweakerFormat.AT)
        val stringified = expected.toString()
        val parsed = ClassTweaker.parse(StringReader(stringified))

        assertEquals(expected, parsed, "ClassTweaker parsed from stringified version should match the original")
    }

    @Test
    fun `AW_V1 reproducibility`() {
        val expected = sampleCT(ClassTweakerFormat.AW_V1)
        val stringified = expected.toString()
        val parsed = ClassTweaker.parse(StringReader(stringified))

        assertEquals(expected, parsed, "ClassTweaker parsed from stringified version should match the original")
    }

    @Test
    fun `AW_V2 reproducibility`() {
        val expected = sampleCT(ClassTweakerFormat.AW_V2)
        val stringified = expected.toString()
        val parsed = ClassTweaker.parse(StringReader(stringified))

        assertEquals(expected, parsed, "ClassTweaker parsed from stringified version should match the original")
    }

    @Test
    fun `CT reproducibility`() {
        val expected = sampleCT(ClassTweakerFormat.CT)
        val stringified = expected.toString()
        println(stringified)
        val parsed = ClassTweaker.parse(StringReader(stringified))

        assertEquals(expected, parsed, "ClassTweaker parsed from stringified version should match the original")
    }
}