package dev.isxander.modstitch.integration

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Tag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests core configuration features of the ModStitch extension across all platforms.
 * These tests validate the main API features like minecraft version, mod loader version,
 * and java version configuration for Loom, MDG, and MDGLegacy platforms.
 */
class CoreConfigurationTests : BaseFunctionalTest() {

    @Test @Tag("loom")
    fun `minecraft version configuration loom`() {
        setupMinimalLoom(minecraftVersion = "1.21.8")
        testMinecraftVersionConfiguration("1.21.8")
    }

    @Test @Tag("mdg")
    fun `minecraft version configuration mdg`() {
        setupMinimalMdg(minecraftVersion = "1.21.8")
        testMinecraftVersionConfiguration("1.21.8")
    }

    @Test @Tag("mdgl")
    fun `minecraft version configuration mdgl`() {
        setupMinimalMdgl(minecraftVersion = "1.20.1")
        testMinecraftVersionConfiguration("1.20.1")
    }

    @Test @Tag("loom")
    fun `mod loader version configuration loom`() {
        setupMinimalLoom(fabricLoaderVersion = "0.16.14")
        testModLoaderVersionConfiguration("0.16.14")
    }

    @Test @Tag("mdg")
    fun `mod loader version configuration mdg`() {
        setupMinimalMdg(neoForgeVersion = "21.8.26")
        testModLoaderVersionConfiguration("21.8.26")
    }

    @Test @Tag("mdgl")
    fun `mod loader version configuration mdgl`() {
        setupMinimalMdgl(forgeVersion = "1.20.1-47.4.6")
        testModLoaderVersionConfiguration("1.20.1-47.4.6")
    }

    @Test @Tag("loom")
    fun `java version auto detection loom modern`() {
        setupMinimalLoom(minecraftVersion = "1.21.8")
        testJavaVersionAutoDetection(21)
    }

    @Test @Tag("loom")
    fun `java version auto detection loom 1_20_5`() {
        setupMinimalLoom(minecraftVersion = "1.20.5")
        testJavaVersionAutoDetection(21)
    }

    @Test @Tag("loom")
    fun `java version auto detection loom 1_18`() {
        setupMinimalLoom(minecraftVersion = "1.18.2")
        testJavaVersionAutoDetection(17)
    }

    @Test @Tag("loom")
    fun `java version auto detection loom 1_17`() {
        setupMinimalLoom(minecraftVersion = "1.17.1")
        testJavaVersionAutoDetection(16)
    }

    @Test @Tag("loom")
    fun `java version auto detection loom legacy`() {
        setupMinimalLoom(minecraftVersion = "1.16.5")
        testJavaVersionAutoDetection(8)
    }

    @Test @Tag("mdg")
    fun `java version auto detection mdg modern`() {
        setupMinimalMdg(minecraftVersion = "1.21.8")
        testJavaVersionAutoDetection(21)
    }

    @Test @Tag("mdgl")
    fun `java version auto detection mdgl`() {
        setupMinimalMdgl(minecraftVersion = "1.20.1")
        testJavaVersionAutoDetection(21)
    }

    @Test @Tag("loom")
    fun `java version manual override loom`() {
        setupMinimalLoom(minecraftVersion = "1.21.8")
        
        // language=kotlin
        buildFile.appendText("""
            modstitch {
                javaVersion = 17
            }
            
        """.trimIndent())
        
        testJavaVersionConfiguration(17)
    }

    @Test @Tag("mdg")
    fun `java version manual override mdg`() {
        setupMinimalMdg(minecraftVersion = "1.21.8")
        
        // language=kotlin
        buildFile.appendText("""
            modstitch {
                javaVersion = 17
            }
            
        """.trimIndent())
        
        testJavaVersionConfiguration(17)
    }

    @Test @Tag("loom")
    fun `platform detection loom`() {
        setupMinimalLoom()
        testPlatformDetection("Loom", isLoom = true, isModDevGradle = false)
    }

    @Test @Tag("mdg")
    fun `platform detection mdg`() {
        setupMinimalMdg()
        testPlatformDetection("MDG", isLoom = false, isModDevGradle = true, isModDevGradleRegular = true)
    }

    @Test @Tag("mdgl")
    fun `platform detection mdgl`() {
        setupMinimalMdgl()
        testPlatformDetection("MDGLegacy", isLoom = false, isModDevGradle = true, isModDevGradleLegacy = true)
    }

    private fun testMinecraftVersionConfiguration(expectedVersion: String) {
        // language=kotlin
        buildFile.appendText("""
            tasks.register("printMinecraftVersion") {
                doLast {
                    println("Minecraft Version: ${'$'}{modstitch.minecraftVersion.get()}")
                }
            }

        """.trimIndent())

        val result = run {
            withArguments("printMinecraftVersion")
        }

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":printMinecraftVersion")?.outcome,
            "Expected printMinecraftVersion task to succeed"
        )

        assertTrue(
            result.output.contains("Minecraft Version: $expectedVersion"),
            "Expected Minecraft version $expectedVersion, but output was: ${result.output}"
        )
    }

    private fun testModLoaderVersionConfiguration(expectedVersion: String) {
        // language=kotlin
        buildFile.appendText("""
            tasks.register("printModLoaderVersion") {
                doLast {
                    println("Mod Loader Version: ${'$'}{modstitch.modLoaderVersion.get()}")
                }
            }

        """.trimIndent())

        val result = run {
            withArguments("printModLoaderVersion")
        }

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":printModLoaderVersion")?.outcome,
            "Expected printModLoaderVersion task to succeed"
        )

        assertTrue(
            result.output.contains("Mod Loader Version: $expectedVersion"),
            "Expected mod loader version $expectedVersion, but output was: ${result.output}"
        )
    }

    private fun testJavaVersionAutoDetection(expectedVersion: Int) {
        // language=kotlin
        buildFile.appendText("""
            tasks.register("printJavaVersion") {
                doLast {
                    println("Java Version: ${'$'}{modstitch.javaVersion.get()}")
                }
            }

        """.trimIndent())

        val result = run {
            withArguments("printJavaVersion")
        }

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":printJavaVersion")?.outcome,
            "Expected printJavaVersion task to succeed"
        )

        assertTrue(
            result.output.contains("Java Version: $expectedVersion"),
            "Expected Java version $expectedVersion, but output was: ${result.output}"
        )
    }

    private fun testJavaVersionConfiguration(expectedVersion: Int) {
        // language=kotlin
        buildFile.appendText("""
            tasks.register("printJavaVersionOverride") {
                doLast {
                    println("Java Version Override: ${'$'}{modstitch.javaVersion.get()}")
                }
            }

        """.trimIndent())

        val result = run {
            withArguments("printJavaVersionOverride")
        }

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":printJavaVersionOverride")?.outcome,
            "Expected printJavaVersionOverride task to succeed"
        )

        assertTrue(
            result.output.contains("Java Version Override: $expectedVersion"),
            "Expected Java version override $expectedVersion, but output was: ${result.output}"
        )
    }

    private fun testPlatformDetection(
        expectedPlatform: String,
        isLoom: Boolean = false,
        isModDevGradle: Boolean = false,
        isModDevGradleRegular: Boolean = false,
        isModDevGradleLegacy: Boolean = false
    ) {
        // language=kotlin
        buildFile.appendText("""
            tasks.register("printPlatformInfo") {
                doLast {
                    println("Platform: ${'$'}{modstitch.platform}")
                    println("Is Loom: ${'$'}{modstitch.isLoom}")
                    println("Is ModDevGradle: ${'$'}{modstitch.isModDevGradle}")
                    println("Is ModDevGradle Regular: ${'$'}{modstitch.isModDevGradleRegular}")
                    println("Is ModDevGradle Legacy: ${'$'}{modstitch.isModDevGradleLegacy}")
                }
            }

        """.trimIndent())

        val result = run {
            withArguments("printPlatformInfo")
        }

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":printPlatformInfo")?.outcome,
            "Expected printPlatformInfo task to succeed"
        )

        assertTrue(
            result.output.contains("Platform: $expectedPlatform"),
            "Expected platform $expectedPlatform, but output was: ${result.output}"
        )
        assertTrue(
            result.output.contains("Is Loom: $isLoom"),
            "Expected isLoom $isLoom, but output was: ${result.output}"
        )
        assertTrue(
            result.output.contains("Is ModDevGradle: $isModDevGradle"),
            "Expected isModDevGradle $isModDevGradle, but output was: ${result.output}"
        )
        assertTrue(
            result.output.contains("Is ModDevGradle Regular: $isModDevGradleRegular"),
            "Expected isModDevGradleRegular $isModDevGradleRegular, but output was: ${result.output}"
        )
        assertTrue(
            result.output.contains("Is ModDevGradle Legacy: $isModDevGradleLegacy"),
            "Expected isModDevGradleLegacy $isModDevGradleLegacy, but output was: ${result.output}"
        )
    }
}