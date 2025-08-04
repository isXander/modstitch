package dev.isxander.modstitch.integration

import dev.isxander.modstitch.util.Platform
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Tag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModManifestTests : BaseFunctionalTest() {
    @Test @Tag("loom")
    fun `mod manifest set correctly loom`() {
        setupMinimalLoom()
        testModManifest(Platform.Loom.modManifest)
    }

    @Test @Tag("mdg")
    fun `mod manifest set correctly mdg new`() {
        setupMinimalMdg()
        testModManifest(Platform.MDG.modManifest)
    }

    @Test @Tag("mdg")
    fun `mod manifest set correctly mdg old`() {
        setupMinimalMdg(
            minecraftVersion = "1.20.4",
            neoForgeVersion = "20.4.249",
        )
        testModManifest(Platform.MDGLegacy.modManifest)
    }

    @Test @Tag("mdgl")
    fun `mod manifest set correctly mdgl`() {
        setupMinimalMdgl()
        testModManifest("META-INF/mods.toml")
    }

    @Test @Tag("mdgv")
    fun `mod manifest empty on vanilla mode`() {
        gradlePropertiesFile.appendText("modstitch.platform=moddevgradle\n")

        // language=kotlin
        buildFile.appendText("""
            plugins {
                id("dev.isxander.modstitch.base")
            }
            
            modstitch {
                minecraftVersion = "1.21.8"
                moddevgradle {
                    neoFormVersion = "20250717.133445"
                }
            }

        """.trimIndent())

        testModManifest("null")
    }

    private fun testModManifest(expected: String) {
        // language=kotlin
        buildFile.appendText("""
            tasks.register("printModManifest") {
                doLast {
                    println("Mod Manifest: ${'$'}{modstitch.modLoaderManifest.orNull}")
                }
            }

        """.trimIndent())

        val result = run {
            withArguments("printModManifest")
        }

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":printModManifest")?.outcome,
            "Expected printModManifest task to succeed, but it failed with outcome: ${result.task(":printModManifest")?.outcome}"
        )

        assertTrue(
            result.output.contains("Mod Manifest: $expected"),
            "The output should contain the mod manifest path $expected, but it does not."
        )
    }
}