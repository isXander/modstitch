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

    protected fun setupMinimalLoom() {
        gradlePropertiesFile.appendText("modstitch.platform=loom\n")

        // language=kotlin
        buildFile.appendText("""
            plugins {
                id("dev.isxander.modstitch.base")
            }
            
            modstitch {
                minecraftVersion = "1.21.8"
                loom {
                    fabricLoaderVersion = "0.16.14"
                }
            }
        """.trimIndent())
    }

    protected fun setupMinimalMdg() {
        gradlePropertiesFile.appendText("modstitch.platform=moddevgradle\n")

        // language=kotlin
        buildFile.appendText("""
            plugins {
                id("dev.isxander.modstitch.base")
            }
            
            modstitch {
                minecraftVersion = "1.21.8"
                moddevgradle {
                    neoForgeVersion = "21.8.26"
                }
            }
        """.trimIndent())
    }

    protected fun setupMinimalMdgl() {
        gradlePropertiesFile.appendText("modstitch.platform=moddevgradle-legacy\n")

        // language=kotlin
        buildFile.appendText("""
            plugins {
                id("dev.isxander.modstitch.base")
            }
            
            modstitch {
                minecraftVersion = "1.20.1"
                moddevgradle {
                    forgeVersion = "1.20.1-47.4.6"
                }
            }
        """.trimIndent())
    }
}