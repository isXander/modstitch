package dev.isxander.modstitch.integration

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.assertNotNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AutoJavaVersionTests : BaseFunctionalTest() {
    @Test @Tag("loom")
    fun `auto java version 12108`() {
        setupMinimalLoom(minecraftVersion = "1.21.8")
        testJavaVersion("21")
    }

    @Test @Tag("loom")
    fun `auto java version loom 12005`() {
        setupMinimalLoom(minecraftVersion = "1.20.5")
        testJavaVersion("21")
    }

    @Test @Tag("loom")
    fun `auto java version loom 12004`() {
        setupMinimalLoom(minecraftVersion = "1.20.4")
        testJavaVersion("17")
    }

    @Test @Tag("loom")
    fun `auto java version loom 11800`() {
        setupMinimalLoom(minecraftVersion = "1.18")
        testJavaVersion("17")
    }

    @Test @Tag("loom")
    fun `auto java version loom 11700`() {
        setupMinimalLoom(minecraftVersion = "1.17")
        testJavaVersion("16")
    }

    @Test @Tag("loom")
    fun `auto java version loom 11600`() {
        setupMinimalLoom(minecraftVersion = "1.16")
        testJavaVersion("8")
    }

    @Test @Tag("loom")
    fun `auto java version loom 25w31a`() {
        setupMinimalLoom(minecraftVersion = "25w31a")
        testJavaVersion("21")
    }

    private fun testJavaVersion(expected: String): BuildResult {
        // language=kotlin
        buildFile.appendText("""
            tasks.register("printJavaVersion") {
                doLast {
                    val javaVersion = modstitch.javaVersion.orNull.toString()
                    println("Java version: ${'$'}javaVersion")
                }
            }
        """.trimIndent())

        val result = run {
            withArguments("printJavaVersion")
        }

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":printJavaVersion")?.outcome,
            "Expected printJavaVersion task to succeed, but it failed with outcome: ${result.task(":printJavaVersion")?.outcome}"
        )

        println("output start")
        println(result.output)
        println("output end")

        val regex = Regex("Java version: (.+)\n")
        val matchResult = regex.find(result.output)
        val actualVersion = matchResult?.groupValues?.get(1)
        assertNotNull(actualVersion, "Expected output to contain 'Java version: <version>' but it was not found.")

        assertEquals(expected, actualVersion, "Expected Java version to be '$expected', but got '$actualVersion'.")

        return result
    }
}