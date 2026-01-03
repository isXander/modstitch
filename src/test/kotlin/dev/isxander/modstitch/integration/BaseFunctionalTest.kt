package dev.isxander.modstitch.integration

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File

abstract class BaseFunctionalTest {
    @TempDir
    lateinit var projectDir: File

    protected lateinit var buildFile: File
    protected lateinit var settingsFile: File
    protected lateinit var gradlePropertiesFile: File

    protected lateinit var templatesDir: File
    protected lateinit var fabricModJson: File
    protected lateinit var neoModsToml: File
    protected lateinit var modsToml: File

    @BeforeEach
    fun setupBase() {
        // Initialise file handles before each test
        buildFile = projectDir.resolve("build.gradle.kts")
        settingsFile = projectDir.resolve("settings.gradle.kts")
        gradlePropertiesFile = projectDir.resolve("gradle.properties")

        templatesDir = projectDir.resolve("src/main/templates")
        templatesDir.mkdirs()
        fabricModJson = templatesDir.resolve("fabric.mod.json")
        neoModsToml = templatesDir.resolve("META-INF/neoforge.mods.toml")
        neoModsToml.parentFile.mkdirs()
        modsToml = templatesDir.resolve("META-INF/mods.toml")

        // You can even create a default settings file automatically
        settingsFile.appendText("""
            rootProject.name = "test-project"
        """.trimIndent())
    }

    protected fun run(block: GradleRunner.() -> Unit = {}): BuildResult {
        return GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .apply(block)
            .build()
    }

    protected fun setupMinimalLoom(
        minecraftVersion: String = "26.1-snapshot-1",
        fabricLoaderVersion: String = "0.18.3"
    ) {
        gradlePropertiesFile.appendText("modstitch.platform=fabric-loom\n")

        // language=kotlin
        buildFile.appendText("""
            plugins {
                id("dev.isxander.modstitch.base")
            }
            
            modstitch {
                minecraftVersion = "$minecraftVersion"
                loom {
                    fabricLoaderVersion = "$fabricLoaderVersion"
                }
            }
            
        """.trimIndent())
    }

    protected fun setupMinimalLoomRemap(
        minecraftVersion: String = "1.21.8",
        fabricLoaderVersion: String = "0.16.14"
    ) {
        gradlePropertiesFile.appendText("modstitch.platform=fabric-loom-remap\n")

        // language=kotlin
        buildFile.appendText("""
            plugins {
                id("dev.isxander.modstitch.base")
            }
            
            modstitch {
                minecraftVersion = "$minecraftVersion"
                loom {
                    fabricLoaderVersion = "$fabricLoaderVersion"
                }
            }
            
        """.trimIndent())
    }

    protected fun setupMinimalMdg(
        minecraftVersion: String = "1.21.8",
        neoForgeVersion: String = "21.8.26"
    ) {
        gradlePropertiesFile.appendText("modstitch.platform=moddevgradle\n")

        // language=kotlin
        buildFile.appendText("""
            plugins {
                id("dev.isxander.modstitch.base")
            }
            
            modstitch {
                minecraftVersion = "$minecraftVersion"
                moddevgradle {
                    neoForgeVersion = "$neoForgeVersion"
                }
            }

        """.trimIndent())
    }

    protected fun setupMinimalMdgl(
        minecraftVersion: String = "1.20.1",
        forgeVersion: String = "1.20.1-47.4.6"
    ) {
        gradlePropertiesFile.appendText("modstitch.platform=moddevgradle-legacy\n")

        // language=kotlin
        buildFile.appendText("""
            plugins {
                id("dev.isxander.modstitch.base")
            }
            
            modstitch {
                minecraftVersion = "$minecraftVersion"
                moddevgradle {
                    forgeVersion = "$forgeVersion"
                }
            }

        """.trimIndent())
    }
}