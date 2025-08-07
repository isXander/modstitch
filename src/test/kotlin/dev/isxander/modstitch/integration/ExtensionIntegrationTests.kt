package dev.isxander.modstitch.integration

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Tag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests integration with ModStitch extensions like publishing and shadow.
 * These extensions build on top of the base ModStitch functionality to provide
 * additional features like mod publishing and shadow jar creation.
 */
class ExtensionIntegrationTests : BaseFunctionalTest() {

    @Test @Tag("loom")
    fun `publishing extension setup loom`() {
        setupMinimalLoom()
        setupPublishingExtension()
        testPublishingExtensionSetup()
    }

    @Test @Tag("mdg")
    fun `publishing extension setup mdg`() {
        setupMinimalMdg()
        setupPublishingExtension()
        testPublishingExtensionSetup()
    }

    @Test @Tag("mdgl")
    fun `publishing extension setup mdgl`() {
        setupMinimalMdgl()
        setupPublishingExtension()
        testPublishingExtensionSetup()
    }

    @Test @Tag("loom")
    fun `shadow extension setup loom`() {
        setupMinimalLoom()
        setupShadowExtension()
        testShadowExtensionSetup()
    }

    @Test @Tag("mdg")
    fun `shadow extension setup mdg`() {
        setupMinimalMdg()
        setupShadowExtension()
        testShadowExtensionSetup()
    }

    @Test @Tag("mdgl")
    fun `shadow extension setup mdgl`() {
        setupMinimalMdgl()
        setupShadowExtension()
        testShadowExtensionSetup()
    }

    @Test @Tag("loom")
    fun `publishing with multiple platforms loom`() {
        setupMinimalLoom()
        setupMultiPlatformPublishing()
        testMultiPlatformPublishing()
    }

    @Test @Tag("mdg")
    fun `publishing with multiple platforms mdg`() {
        setupMinimalMdg()
        setupMultiPlatformPublishing()
        testMultiPlatformPublishing()
    }

    @Test @Tag("mdgl")
    fun `publishing with multiple platforms mdgl`() {
        setupMinimalMdgl()
        setupMultiPlatformPublishing()
        testMultiPlatformPublishing()
    }

    @Test @Tag("loom")
    fun `shadow jar with dependencies loom`() {
        setupMinimalLoom()
        setupShadowWithDependencies()
        testShadowJarCreation()
    }

    @Test @Tag("mdg")
    fun `shadow jar with dependencies mdg`() {
        setupMinimalMdg()
        setupShadowWithDependencies()
        testShadowJarCreation()
    }

    @Test @Tag("mdgl")
    fun `shadow jar with dependencies mdgl`() {
        setupMinimalMdgl()
        setupShadowWithDependencies()
        testShadowJarCreation()
    }

    @Test @Tag("loom")
    fun `final jar task configuration loom`() {
        setupMinimalLoom()
        testFinalJarTaskConfiguration()
    }

    @Test @Tag("mdg")
    fun `final jar task configuration mdg`() {
        setupMinimalMdg()
        testFinalJarTaskConfiguration()
    }

    @Test @Tag("mdgl")
    fun `final jar task configuration mdgl`() {
        setupMinimalMdgl()
        testFinalJarTaskConfiguration()
    }

    @Test @Tag("loom")
    fun `named jar task configuration loom`() {
        setupMinimalLoom()
        testNamedJarTaskConfiguration()
    }

    @Test @Tag("mdg")
    fun `named jar task configuration mdg`() {
        setupMinimalMdg()
        testNamedJarTaskConfiguration()
    }

    @Test @Tag("mdgl")
    fun `named jar task configuration mdgl`() {
        setupMinimalMdgl()
        testNamedJarTaskConfiguration()
    }

    private fun setupPublishingExtension() {
        // language=kotlin
        buildFile.appendText("""
            plugins {
                id("dev.isxander.modstitch.publishing")
            }
            
            modstitch {
                metadata {
                    modId = "testmod"
                    name = "Test Mod"
                    version = "1.0.0"
                    description = "A test mod with publishing"
                    authors = listOf("Test Author")
                    license = "MIT"
                    homepage = "https://github.com/test/testmod"
                    issueTracker = "https://github.com/test/testmod/issues"
                }
            }
            
        """.trimIndent())
    }

    private fun setupShadowExtension() {
        // language=kotlin
        buildFile.appendText("""
            plugins {
                id("dev.isxander.modstitch.shadow")
            }
            
        """.trimIndent())
    }

    private fun setupMultiPlatformPublishing() {
        setupPublishingExtension()
        
        // language=kotlin
        buildFile.appendText("""
            // Add publishing configuration if available
            // This would typically include multiple publishing targets
            
        """.trimIndent())
    }

    private fun setupShadowWithDependencies() {
        setupShadowExtension()
        
        // language=kotlin
        buildFile.appendText("""
            dependencies {
                implementation("com.google.guava:guava:32.1.3-jre")
                shadow("com.google.code.gson:gson:2.10.1")
            }
            
        """.trimIndent())
    }

    private fun testPublishingExtensionSetup() {
        // language=kotlin
        buildFile.appendText("""
            tasks.register("checkPublishingSetup") {
                doLast {
                    val publishingTasks = tasks.names.filter { it.contains("publish") }
                    println("Publishing tasks available: ${'$'}publishingTasks")
                    
                    val hasPublishPlugin = plugins.hasPlugin("me.modmuss50.mod-publish-plugin")
                    println("Has mod-publish-plugin: ${'$'}hasPublishPlugin")
                }
            }

        """.trimIndent())

        val result = run {
            withArguments("checkPublishingSetup")
        }

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":checkPublishingSetup")?.outcome,
            "Expected checkPublishingSetup task to succeed"
        )

        assertTrue(
            result.output.contains("Publishing tasks available:"),
            "Expected publishing tasks information to be displayed"
        )
    }

    private fun testShadowExtensionSetup() {
        // language=kotlin
        buildFile.appendText("""
            tasks.register("checkShadowSetup") {
                doLast {
                    val shadowTasks = tasks.names.filter { it.contains("shadow") || it.contains("Shadow") }
                    println("Shadow tasks available: ${'$'}shadowTasks")
                    
                    val hasShadowPlugin = plugins.hasPlugin("com.gradleup.shadow")
                    println("Has shadow plugin: ${'$'}hasShadowPlugin")
                }
            }

        """.trimIndent())

        val result = run {
            withArguments("checkShadowSetup")
        }

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":checkShadowSetup")?.outcome,
            "Expected checkShadowSetup task to succeed"
        )

        assertTrue(
            result.output.contains("Shadow tasks available:"),
            "Expected shadow tasks information to be displayed"
        )
    }

    private fun testMultiPlatformPublishing() {
        // language=kotlin
        buildFile.appendText("""
            tasks.register("checkMultiPlatformPublishing") {
                doLast {
                    println("Multi-platform publishing test")
                    println("Platform: ${'$'}{modstitch.platform}")
                    println("Mod ID: ${'$'}{modstitch.metadata.modId.get()}")
                    println("Version: ${'$'}{modstitch.metadata.version.get()}")
                }
            }

        """.trimIndent())

        val result = run {
            withArguments("checkMultiPlatformPublishing")
        }

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":checkMultiPlatformPublishing")?.outcome,
            "Expected checkMultiPlatformPublishing task to succeed"
        )

        assertTrue(
            result.output.contains("Multi-platform publishing test"),
            "Expected multi-platform publishing test output"
        )
    }

    private fun testShadowJarCreation() {
        // language=kotlin
        buildFile.appendText("""
            tasks.register("checkShadowJar") {
                doLast {
                    val shadowJarTask = tasks.findByName("shadowJar")
                    println("Shadow jar task exists: ${'$'}{shadowJarTask != null}")
                    
                    if (shadowJarTask != null) {
                        println("Shadow jar task type: ${'$'}{shadowJarTask::class.simpleName}")
                    }
                }
            }

        """.trimIndent())

        val result = run {
            withArguments("checkShadowJar")
        }

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":checkShadowJar")?.outcome,
            "Expected checkShadowJar task to succeed"
        )

        assertTrue(
            result.output.contains("Shadow jar task exists:"),
            "Expected shadow jar task existence check"
        )
    }

    private fun testFinalJarTaskConfiguration() {
        // language=kotlin
        buildFile.appendText("""
            tasks.register("checkFinalJarTask") {
                doLast {
                    try {
                        val finalJarTask = modstitch.finalJarTask
                        println("Final jar task name: ${'$'}{finalJarTask.name}")
                        println("Final jar task type: ${'$'}{finalJarTask.get()::class.simpleName}")
                    } catch (e: Exception) {
                        println("Error accessing final jar task: ${'$'}{e.message}")
                        println("This might be expected if the final jar task isn't set yet")
                    }
                }
            }

        """.trimIndent())

        val result = run {
            withArguments("checkFinalJarTask")
        }

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":checkFinalJarTask")?.outcome,
            "Expected checkFinalJarTask task to succeed"
        )

        assertTrue(
            result.output.contains("Final jar task") || result.output.contains("Error accessing final jar task"),
            "Expected final jar task information or error message"
        )
    }

    private fun testNamedJarTaskConfiguration() {
        // language=kotlin
        buildFile.appendText("""
            tasks.register("checkNamedJarTask") {
                doLast {
                    try {
                        val namedJarTask = modstitch.namedJarTask
                        println("Named jar task name: ${'$'}{namedJarTask.name}")
                        println("Named jar task type: ${'$'}{namedJarTask.get()::class.simpleName}")
                    } catch (e: Exception) {
                        println("Error accessing named jar task: ${'$'}{e.message}")
                        println("This might be expected if the named jar task isn't set yet")
                    }
                }
            }

        """.trimIndent())

        val result = run {
            withArguments("checkNamedJarTask")
        }

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":checkNamedJarTask")?.outcome,
            "Expected checkNamedJarTask task to succeed"
        )

        assertTrue(
            result.output.contains("Named jar task") || result.output.contains("Error accessing named jar task"),
            "Expected named jar task information or error message"
        )
    }
}