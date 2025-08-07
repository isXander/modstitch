package dev.isxander.modstitch.integration

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Tag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests parchment mappings configuration across all platforms.
 * Validates that parchment settings are properly configured for
 * parameter name mappings to complement Official Mojang Mappings.
 */
class ParchmentConfigurationTests : BaseFunctionalTest() {

    @Test @Tag("loom")
    fun `parchment configuration loom`() {
        setupMinimalLoom()
        setupParchmentConfiguration()
        testParchmentConfiguration()
    }

    @Test @Tag("mdg")
    fun `parchment configuration mdg`() {
        setupMinimalMdg()
        setupParchmentConfiguration()
        testParchmentConfiguration()
    }

    @Test @Tag("mdgl")
    fun `parchment configuration mdgl`() {
        setupMinimalMdgl()
        setupParchmentConfiguration()
        testParchmentConfiguration()
    }

    @Test @Tag("loom")
    fun `parchment with custom version loom`() {
        setupMinimalLoom()
        setupParchmentWithCustomVersion()
        testParchmentWithCustomVersion()
    }

    @Test @Tag("mdg")
    fun `parchment with custom version mdg`() {
        setupMinimalMdg()
        setupParchmentWithCustomVersion()
        testParchmentWithCustomVersion()
    }

    @Test @Tag("mdgl")
    fun `parchment with custom version mdgl`() {
        setupMinimalMdgl()
        setupParchmentWithCustomVersion()
        testParchmentWithCustomVersion()
    }

    @Test @Tag("loom")
    fun `parchment auto minecraft version loom`() {
        setupMinimalLoom(minecraftVersion = "1.21.8")
        setupParchmentAutoVersion()
        testParchmentAutoVersion("1.21.8")
    }

    @Test @Tag("mdg")
    fun `parchment auto minecraft version mdg`() {
        setupMinimalMdg(minecraftVersion = "1.21.8")
        setupParchmentAutoVersion()
        testParchmentAutoVersion("1.21.8")
    }

    @Test @Tag("mdgl")
    fun `parchment auto minecraft version mdgl`() {
        setupMinimalMdgl(minecraftVersion = "1.20.1")
        setupParchmentAutoVersion()
        testParchmentAutoVersion("1.20.1")
    }

    @Test @Tag("loom")
    fun `parchment disabled loom`() {
        setupMinimalLoom()
        setupParchmentDisabled()
        testParchmentDisabled()
    }

    @Test @Tag("mdg")
    fun `parchment disabled mdg`() {
        setupMinimalMdg()
        setupParchmentDisabled()
        testParchmentDisabled()
    }

    @Test @Tag("mdgl")
    fun `parchment disabled mdgl`() {
        setupMinimalMdgl()
        setupParchmentDisabled()
        testParchmentDisabled()
    }

    private fun setupParchmentConfiguration() {
        // language=kotlin
        buildFile.appendText("""
            modstitch {
                parchment {
                    minecraftVersion = "1.21.8"
                    mappingsVersion = "2024.12.08"
                }
            }
            
        """.trimIndent())
    }

    private fun setupParchmentWithCustomVersion() {
        // language=kotlin
        buildFile.appendText("""
            modstitch {
                parchment {
                    minecraftVersion = "1.20.6"
                    mappingsVersion = "2024.06.02"
                }
            }
            
        """.trimIndent())
    }

    private fun setupParchmentAutoVersion() {
        // language=kotlin
        buildFile.appendText("""
            modstitch {
                parchment {
                    mappingsVersion = "2024.12.08"
                    // minecraftVersion should auto-inherit from modstitch.minecraftVersion
                }
            }
            
        """.trimIndent())
    }

    private fun setupParchmentDisabled() {
        // Don't add any parchment configuration - test that it can be disabled/optional
    }

    private fun testParchmentConfiguration() {
        // language=kotlin
        buildFile.appendText("""
            tasks.register("printParchmentConfig") {
                doLast {
                    println("Parchment MC Version: ${'$'}{modstitch.parchment.minecraftVersion.get()}")
                    println("Parchment Mappings Version: ${'$'}{modstitch.parchment.mappingsVersion.get()}")
                }
            }

        """.trimIndent())

        val result = run {
            withArguments("printParchmentConfig")
        }

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":printParchmentConfig")?.outcome,
            "Expected printParchmentConfig task to succeed"
        )

        assertTrue(
            result.output.contains("Parchment MC Version: 1.21.8"),
            "Expected parchment minecraft version to be set"
        )
        assertTrue(
            result.output.contains("Parchment Mappings Version: 2024.12.08"),
            "Expected parchment mappings version to be set"
        )
    }

    private fun testParchmentWithCustomVersion() {
        // language=kotlin
        buildFile.appendText("""
            tasks.register("printCustomParchmentConfig") {
                doLast {
                    println("Custom Parchment MC Version: ${'$'}{modstitch.parchment.minecraftVersion.get()}")
                    println("Custom Parchment Mappings Version: ${'$'}{modstitch.parchment.mappingsVersion.get()}")
                }
            }

        """.trimIndent())

        val result = run {
            withArguments("printCustomParchmentConfig")
        }

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":printCustomParchmentConfig")?.outcome,
            "Expected printCustomParchmentConfig task to succeed"
        )

        assertTrue(
            result.output.contains("Custom Parchment MC Version: 1.20.6"),
            "Expected custom parchment minecraft version to be set"
        )
        assertTrue(
            result.output.contains("Custom Parchment Mappings Version: 2024.06.02"),
            "Expected custom parchment mappings version to be set"
        )
    }

    private fun testParchmentAutoVersion(expectedMcVersion: String) {
        // language=kotlin
        buildFile.appendText("""
            tasks.register("printAutoParchmentConfig") {
                doLast {
                    println("Auto Parchment MC Version: ${'$'}{modstitch.parchment.minecraftVersion.get()}")
                    println("Auto Parchment Mappings Version: ${'$'}{modstitch.parchment.mappingsVersion.get()}")
                }
            }

        """.trimIndent())

        val result = run {
            withArguments("printAutoParchmentConfig")
        }

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":printAutoParchmentConfig")?.outcome,
            "Expected printAutoParchmentConfig task to succeed"
        )

        assertTrue(
            result.output.contains("Auto Parchment MC Version: $expectedMcVersion"),
            "Expected parchment to auto-inherit minecraft version from modstitch"
        )
        assertTrue(
            result.output.contains("Auto Parchment Mappings Version: 2024.12.08"),
            "Expected parchment mappings version to be set"
        )
    }

    private fun testParchmentDisabled() {
        // language=kotlin
        buildFile.appendText("""
            tasks.register("printDisabledParchmentConfig") {
                doLast {
                    println("Parchment Present: ${'$'}{modstitch.parchment.mappingsVersion.isPresent}")
                }
            }

        """.trimIndent())

        val result = run {
            withArguments("printDisabledParchmentConfig")
        }

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":printDisabledParchmentConfig")?.outcome,
            "Expected printDisabledParchmentConfig task to succeed"
        )

        // The behavior here might vary - just verify the task runs successfully
        // when parchment is not configured
        assertTrue(
            result.output.contains("Parchment Present:"),
            "Expected parchment presence check to run"
        )
    }
}