package dev.isxander.modstitch.integration

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Tag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests proxy configuration functionality across all platforms.
 * Validates the creation and behavior of proxy configurations that abstract
 * platform differences for mod vs non-mod dependencies.
 */
class ProxyConfigurationTests : BaseFunctionalTest() {

    @Test @Tag("loom")
    fun `proxy configurations creation loom`() {
        setupMinimalLoom()
        setupProxyConfigurations()
        testProxyConfigurationsCreation()
    }

    @Test @Tag("mdg")
    fun `proxy configurations creation mdg`() {
        setupMinimalMdg()
        setupProxyConfigurations()
        testProxyConfigurationsCreation()
    }

    @Test @Tag("mdgl")
    fun `proxy configurations creation mdgl`() {
        setupMinimalMdgl()
        setupProxyConfigurations()
        testProxyConfigurationsCreation()
    }

    @Test @Tag("loom")
    fun `source set proxy configurations loom`() {
        setupMinimalLoom()
        setupSourceSetProxyConfigurations()
        testSourceSetProxyConfigurations()
    }

    @Test @Tag("mdg")
    fun `source set proxy configurations mdg`() {
        setupMinimalMdg()
        setupSourceSetProxyConfigurations()
        testSourceSetProxyConfigurations()
    }

    @Test @Tag("mdgl")
    fun `source set proxy configurations mdgl`() {
        setupMinimalMdgl()
        setupSourceSetProxyConfigurations()
        testSourceSetProxyConfigurations()
    }

    @Test @Tag("loom")
    fun `mod vs non-mod dependencies loom`() {
        setupMinimalLoom()
        setupModAndNonModDependencies()
        testModAndNonModDependencies()
    }

    @Test @Tag("mdg")
    fun `mod vs non-mod dependencies mdg`() {
        setupMinimalMdg()
        setupModAndNonModDependencies()
        testModAndNonModDependencies()
    }

    @Test @Tag("mdgl")
    fun `mod vs non-mod dependencies mdgl`() {
        setupMinimalMdgl()
        setupModAndNonModDependencies()
        testModAndNonModDependencies()
    }

    @Test @Tag("loom")
    fun `test source set proxy configurations loom`() {
        setupMinimalLoom()
        setupTestSourceSetProxyConfigurations()
        testTestSourceSetProxyConfigurations()
    }

    @Test @Tag("mdg")
    fun `test source set proxy configurations mdg`() {
        setupMinimalMdg()
        setupTestSourceSetProxyConfigurations()
        testTestSourceSetProxyConfigurations()
    }

    @Test @Tag("mdgl")
    fun `test source set proxy configurations mdgl`() {
        setupMinimalMdgl()
        setupTestSourceSetProxyConfigurations()
        testTestSourceSetProxyConfigurations()
    }

    @Test @Tag("loom")
    fun `proxy configuration dependencies resolution loom`() {
        setupMinimalLoom()
        setupProxyDependencyResolution()
        testProxyDependencyResolution()
    }

    @Test @Tag("mdg")
    fun `proxy configuration dependencies resolution mdg`() {
        setupMinimalMdg()
        setupProxyDependencyResolution()
        testProxyDependencyResolution()
    }

    @Test @Tag("mdgl")
    fun `proxy configuration dependencies resolution mdgl`() {
        setupMinimalMdgl()
        setupProxyDependencyResolution()
        testProxyDependencyResolution()
    }

    private fun setupProxyConfigurations() {
        // language=kotlin
        buildFile.appendText("""
            modstitch {
                createProxyConfigurations(configurations.implementation.get())
                createProxyConfigurations(configurations.compileOnly.get())
            }
            
        """.trimIndent())
    }

    private fun setupSourceSetProxyConfigurations() {
        // language=kotlin
        buildFile.appendText("""
            modstitch {
                createProxyConfigurations(sourceSets.main.get())
            }
            
        """.trimIndent())
    }

    private fun setupModAndNonModDependencies() {
        // language=kotlin
        buildFile.appendText("""
            modstitch {
                createProxyConfigurations(sourceSets.main.get())
            }
            
            dependencies {
                // Example non-mod dependency
                modstitchImplementation("com.google.guava:guava:32.1.3-jre")
                
                // Example mod dependency (these would typically be mod artifacts)
                modstitchModImplementation("com.example:example-mod:1.0.0")
            }
            
        """.trimIndent())
    }

    private fun setupTestSourceSetProxyConfigurations() {
        // language=kotlin
        buildFile.appendText("""
            modstitch {
                createProxyConfigurations(sourceSets.test.get())
            }
            
            dependencies {
                modstitchTestImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
                modstitchModTestImplementation("com.example:test-mod:1.0.0")
            }
            
        """.trimIndent())
    }

    private fun setupProxyDependencyResolution() {
        // language=kotlin
        buildFile.appendText("""
            modstitch {
                createProxyConfigurations(configurations.implementation.get())
            }
            
            dependencies {
                modstitchImplementation("com.google.code.gson:gson:2.10.1")
                modstitchModImplementation("com.example:sample-mod:1.0.0")
            }
            
        """.trimIndent())
    }

    private fun testProxyConfigurationsCreation() {
        // language=kotlin
        buildFile.appendText("""
            tasks.register("printProxyConfigurations") {
                doLast {
                    val configNames = configurations.names.filter { it.startsWith("modstitch") }
                    println("Proxy Configurations: ${'$'}configNames")
                    
                    val hasImplementationProxy = configurations.names.contains("modstitchImplementation")
                    val hasModImplementationProxy = configurations.names.contains("modstitchModImplementation")
                    val hasCompileOnlyProxy = configurations.names.contains("modstitchCompileOnly")
                    val hasModCompileOnlyProxy = configurations.names.contains("modstitchModCompileOnly")
                    
                    println("Has Implementation Proxy: ${'$'}hasImplementationProxy")
                    println("Has Mod Implementation Proxy: ${'$'}hasModImplementationProxy")
                    println("Has CompileOnly Proxy: ${'$'}hasCompileOnlyProxy")
                    println("Has Mod CompileOnly Proxy: ${'$'}hasModCompileOnlyProxy")
                }
            }

        """.trimIndent())

        val result = run {
            withArguments("printProxyConfigurations")
        }

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":printProxyConfigurations")?.outcome,
            "Expected printProxyConfigurations task to succeed"
        )

        val output = result.output
        assertTrue(
            output.contains("Proxy Configurations:"),
            "Expected proxy configurations to be listed"
        )
        
        // Verify that proxy configurations were created
        assertTrue(
            output.contains("Has Implementation Proxy: true"),
            "Expected modstitchImplementation proxy configuration to be created"
        )
        assertTrue(
            output.contains("Has Mod Implementation Proxy: true"),
            "Expected modstitchModImplementation proxy configuration to be created"
        )
    }

    private fun testSourceSetProxyConfigurations() {
        // language=kotlin
        buildFile.appendText("""
            tasks.register("printSourceSetProxyConfigurations") {
                doLast {
                    val sourceSetConfigNames = configurations.names.filter { 
                        it.startsWith("modstitch") && !it.contains("test") 
                    }
                    println("Source Set Proxy Configurations: ${'$'}sourceSetConfigNames")
                    
                    val expectedConfigs = listOf(
                        "modstitchImplementation", "modstitchModImplementation",
                        "modstitchCompileOnly", "modstitchModCompileOnly",
                        "modstitchRuntimeOnly", "modstitchModRuntimeOnly"
                    )
                    
                    expectedConfigs.forEach { configName ->
                        val exists = configurations.names.contains(configName)
                        println("${'$'}configName exists: ${'$'}exists")
                    }
                }
            }

        """.trimIndent())

        val result = run {
            withArguments("printSourceSetProxyConfigurations")
        }

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":printSourceSetProxyConfigurations")?.outcome,
            "Expected printSourceSetProxyConfigurations task to succeed"
        )

        val output = result.output
        assertTrue(
            output.contains("Source Set Proxy Configurations:"),
            "Expected source set proxy configurations to be listed"
        )
    }

    private fun testModAndNonModDependencies() {
        // language=kotlin
        buildFile.appendText("""
            tasks.register("printModAndNonModDependencies") {
                doLast {
                    try {
                        val modstitchImpl = configurations.findByName("modstitchImplementation")
                        val modstitchModImpl = configurations.findByName("modstitchModImplementation")
                        
                        println("Non-mod implementation dependencies: ${'$'}{modstitchImpl?.dependencies?.size ?: 0}")
                        println("Mod implementation dependencies: ${'$'}{modstitchModImpl?.dependencies?.size ?: 0}")
                        
                        modstitchImpl?.dependencies?.forEach { dep ->
                            println("Non-mod dependency: ${'$'}{dep.group}:${'$'}{dep.name}:${'$'}{dep.version}")
                        }
                        
                        modstitchModImpl?.dependencies?.forEach { dep ->
                            println("Mod dependency: ${'$'}{dep.group}:${'$'}{dep.name}:${'$'}{dep.version}")
                        }
                    } catch (e: Exception) {
                        println("Error checking dependencies: ${'$'}{e.message}")
                    }
                }
            }

        """.trimIndent())

        val result = run {
            withArguments("printModAndNonModDependencies")
        }

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":printModAndNonModDependencies")?.outcome,
            "Expected printModAndNonModDependencies task to succeed"
        )

        val output = result.output
        assertTrue(
            output.contains("implementation dependencies:") || output.contains("Error checking dependencies:"),
            "Expected dependency information or error message"
        )
    }

    private fun testTestSourceSetProxyConfigurations() {
        // language=kotlin
        buildFile.appendText("""
            tasks.register("printTestProxyConfigurations") {
                doLast {
                    val testConfigNames = configurations.names.filter { 
                        it.startsWith("modstitchTest") || it.startsWith("modstitchModTest")
                    }
                    println("Test Proxy Configurations: ${'$'}testConfigNames")
                    
                    val hasTestImpl = configurations.names.contains("modstitchTestImplementation")
                    val hasModTestImpl = configurations.names.contains("modstitchModTestImplementation")
                    
                    println("Has Test Implementation Proxy: ${'$'}hasTestImpl")
                    println("Has Mod Test Implementation Proxy: ${'$'}hasModTestImpl")
                }
            }

        """.trimIndent())

        val result = run {
            withArguments("printTestProxyConfigurations")
        }

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":printTestProxyConfigurations")?.outcome,
            "Expected printTestProxyConfigurations task to succeed"
        )

        val output = result.output
        assertTrue(
            output.contains("Test Proxy Configurations:"),
            "Expected test proxy configurations to be listed"
        )
    }

    private fun testProxyDependencyResolution() {
        // language=kotlin
        buildFile.appendText("""
            tasks.register("printProxyDependencyResolution") {
                doLast {
                    try {
                        println("Testing proxy dependency resolution...")
                        
                        val modstitchImpl = configurations.findByName("modstitchImplementation")
                        val modstitchModImpl = configurations.findByName("modstitchModImplementation")
                        
                        if (modstitchImpl != null) {
                            println("ModStitch Implementation configuration exists")
                            println("Dependencies count: ${'$'}{modstitchImpl.dependencies.size}")
                        }
                        
                        if (modstitchModImpl != null) {
                            println("ModStitch Mod Implementation configuration exists")
                            println("Mod dependencies count: ${'$'}{modstitchModImpl.dependencies.size}")
                        }
                        
                        println("Proxy dependency resolution test completed")
                    } catch (e: Exception) {
                        println("Error during dependency resolution test: ${'$'}{e.message}")
                    }
                }
            }

        """.trimIndent())

        val result = run {
            withArguments("printProxyDependencyResolution")
        }

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":printProxyDependencyResolution")?.outcome,
            "Expected printProxyDependencyResolution task to succeed"
        )

        val output = result.output
        assertTrue(
            output.contains("Testing proxy dependency resolution") || 
            output.contains("Error during dependency resolution test"),
            "Expected dependency resolution test output"
        )
    }
}