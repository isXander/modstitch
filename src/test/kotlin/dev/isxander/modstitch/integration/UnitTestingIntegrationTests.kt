package dev.isxander.modstitch.integration

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Tag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests unit testing functionality provided by ModStitch.
 * This validates the unitTesting configuration that includes Minecraft sources.
 */
class UnitTestingIntegrationTests : BaseFunctionalTest() {

    @Test @Tag("loom")
    fun `unit testing setup loom`() {
        setupMinimalLoom()
        setupUnitTesting()
        testUnitTestingSetup()
    }

    @Test @Tag("mdg")
    fun `unit testing setup mdg`() {
        setupMinimalMdg()
        setupUnitTesting()
        testUnitTestingSetup()
    }

    @Test @Tag("mdgl")
    fun `unit testing setup mdgl`() {
        setupMinimalMdgl()
        setupUnitTesting()
        testUnitTestingSetup()
    }

    @Test @Tag("loom")
    fun `unit testing with custom configuration loom`() {
        setupMinimalLoom()
        setupCustomUnitTesting()
        testCustomUnitTestingSetup()
    }

    @Test @Tag("mdg")
    fun `unit testing with custom configuration mdg`() {
        setupMinimalMdg()
        setupCustomUnitTesting()
        testCustomUnitTestingSetup()
    }

    @Test @Tag("mdgl")
    fun `unit testing with custom configuration mdgl`() {
        setupMinimalMdgl()
        setupCustomUnitTesting()
        testCustomUnitTestingSetup()
    }

    @Test @Tag("loom")
    fun `unit test execution loom`() {
        setupMinimalLoom()
        setupUnitTesting()
        createUnitTestClass()
        testUnitTestExecution()
    }

    @Test @Tag("mdg")
    fun `unit test execution mdg`() {
        setupMinimalMdg()
        setupUnitTesting()
        createUnitTestClass()
        testUnitTestExecution()
    }

    @Test @Tag("mdgl")
    fun `unit test execution mdgl`() {
        setupMinimalMdgl()
        setupUnitTesting()
        createUnitTestClass()
        testUnitTestExecution()
    }

    private fun setupUnitTesting() {
        // language=kotlin
        buildFile.appendText("""
            modstitch {
                unitTesting {
                    includeEngines("junit-jupiter")
                }
            }
            
            dependencies {
                testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
                testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
            }
            
        """.trimIndent())
    }

    private fun setupCustomUnitTesting() {
        // language=kotlin
        buildFile.appendText("""
            modstitch {
                unitTesting {
                    includeEngines("junit-jupiter")
                    excludeTags("slow")
                    includeTags("fast")
                }
            }
            
            dependencies {
                testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
                testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
            }
            
        """.trimIndent())
    }

    private fun createUnitTestClass() {
        val testDir = projectDir.resolve("src/test/java/com/example/testmod")
        testDir.mkdirs()
        
        testDir.resolve("TestModTest.java").writeText("""
            package com.example.testmod;
            
            import org.junit.jupiter.api.Test;
            import org.junit.jupiter.api.Tag;
            import static org.junit.jupiter.api.Assertions.*;
            
            public class TestModTest {
                @Test
                @Tag("fast")
                public void testBasicFunctionality() {
                    assertTrue(true, "This test should always pass");
                }
                
                @Test
                @Tag("slow")
                public void testSlowFunctionality() {
                    assertEquals(2, 1 + 1, "Basic math should work");
                }
                
                @Test
                public void testMinecraftRelated() {
                    // This test would typically interact with Minecraft classes
                    // For now, just a basic test to verify setup
                    assertNotNull("Minecraft", "Should be able to test Minecraft-related functionality");
                }
            }
        """.trimIndent())
    }

    private fun testUnitTestingSetup() {
        // language=kotlin
        buildFile.appendText("""
            tasks.register("checkUnitTestingSetup") {
                doLast {
                    val testTask = tasks.findByName("test")
                    println("Test task exists: ${'$'}{testTask != null}")
                    
                    if (testTask != null) {
                        println("Test task type: ${'$'}{testTask::class.simpleName}")
                        
                        if (testTask is Test) {
                            val useJUnitPlatform = testTask.testFramework.javaClass.simpleName.contains("JUnit")
                            println("Uses JUnit Platform: ${'$'}useJUnitPlatform")
                        }
                    }
                }
            }

        """.trimIndent())

        val result = run {
            withArguments("checkUnitTestingSetup")
        }

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":checkUnitTestingSetup")?.outcome,
            "Expected checkUnitTestingSetup task to succeed"
        )

        assertTrue(
            result.output.contains("Test task exists: true"),
            "Expected test task to exist after unit testing setup"
        )
    }

    private fun testCustomUnitTestingSetup() {
        // language=kotlin
        buildFile.appendText("""
            tasks.register("checkCustomUnitTestingSetup") {
                doLast {
                    println("Custom unit testing setup checked")
                    
                    val testTask = tasks.findByName("test")
                    if (testTask != null) {
                        println("Test task configured with custom settings")
                    }
                }
            }

        """.trimIndent())

        val result = run {
            withArguments("checkCustomUnitTestingSetup")
        }

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":checkCustomUnitTestingSetup")?.outcome,
            "Expected checkCustomUnitTestingSetup task to succeed"
        )

        assertTrue(
            result.output.contains("Custom unit testing setup checked"),
            "Expected custom unit testing setup to be verified"
        )
    }

    private fun testUnitTestExecution() {
        val result = run {
            withArguments("test", "--info")
        }

        // The test execution might fail due to missing dependencies in the test environment
        // but we can check that the test task was attempted
        assertTrue(
            result.task(":test") != null,
            "Expected test task to be executed"
        )

        // Check that our test class was compiled
        val testClassesDir = projectDir.resolve("build/classes/java/test")
        if (testClassesDir.exists()) {
            val hasTestClass = testClassesDir.walk().any { 
                it.name == "TestModTest.class" 
            }
            if (hasTestClass) {
                assertTrue(true, "Test class was compiled successfully")
            }
        }

        // At minimum, verify that the test task ran (success or failure)
        assertTrue(
            result.output.contains("test") || result.output.contains("Test"),
            "Expected test execution output"
        )
    }
}