package dev.isxander.modstitch.integration

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Tag
import java.io.File
import java.util.jar.JarFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests that validate built JAR artifacts contain expected files and configurations.
 * This is one of the key testing techniques requested - inspecting the built jar
 * to ensure it is structured correctly for each platform.
 */
class JarInspectionTests : BaseFunctionalTest() {

    @Test @Tag("loom")
    fun `fabric jar structure validation`() {
        setupCompleteLoomProject()
        
        val result = run {
            withArguments("build", "--stacktrace")
        }

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":build")?.outcome,
            "Expected build task to succeed"
        )

        inspectFabricJar()
    }

    @Test @Tag("mdg")
    fun `neoforge jar structure validation`() {
        setupCompleteNeoForgeProject()
        
        val result = run {
            withArguments("build", "--stacktrace")
        }

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":build")?.outcome,
            "Expected build task to succeed"
        )

        inspectNeoForgeJar()
    }

    @Test @Tag("mdgl")
    fun `forge legacy jar structure validation`() {
        setupCompleteForgeLegacyProject()
        
        val result = run {
            withArguments("build", "--stacktrace")
        }

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":build")?.outcome,
            "Expected build task to succeed"
        )

        inspectForgeLegacyJar()
    }

    @Test @Tag("loom")
    fun `fabric jar manifest validation`() {
        setupCompleteLoomProject()
        
        val result = run {
            withArguments("build")
        }

        assertEquals(TaskOutcome.SUCCESS, result.task(":build")?.outcome)
        
        val jarFile = getBuiltJarFile()
        JarFile(jarFile).use { jar ->
            val manifest = jar.manifest
            assertTrue(manifest != null, "JAR should have a manifest")
            
            val attributes = manifest.mainAttributes
            assertTrue(
                attributes.getValue("Implementation-Version") != null,
                "JAR manifest should contain Implementation-Version"
            )
        }
    }

    @Test @Tag("mdg")
    fun `neoforge jar manifest validation`() {
        setupCompleteNeoForgeProject()
        
        val result = run {
            withArguments("build")
        }

        assertEquals(TaskOutcome.SUCCESS, result.task(":build")?.outcome)
        
        val jarFile = getBuiltJarFile()
        JarFile(jarFile).use { jar ->
            val manifest = jar.manifest
            assertTrue(manifest != null, "JAR should have a manifest")
            
            val attributes = manifest.mainAttributes
            assertTrue(
                attributes.getValue("Implementation-Version") != null,
                "JAR manifest should contain Implementation-Version"
            )
        }
    }

    @Test @Tag("loom")
    fun `fabric jar with mixin configs`() {
        setupLoomProjectWithMixins()
        
        val result = run {
            withArguments("build")
        }

        assertEquals(TaskOutcome.SUCCESS, result.task(":build")?.outcome)
        
        val jarFile = getBuiltJarFile()
        JarFile(jarFile).use { jar ->
            // Check for mixin config files
            val mixinConfig = jar.getJarEntry("testmod.mixins.json")
            assertTrue(mixinConfig != null, "Mixin config should be present in JAR")
            
            // Check fabric.mod.json references mixins
            val fabricModJson = jar.getJarEntry("fabric.mod.json")
            assertTrue(fabricModJson != null, "fabric.mod.json should be present")
            
            jar.getInputStream(fabricModJson).use { stream ->
                val content = stream.bufferedReader().readText()
                assertTrue(
                    content.contains("mixins"),
                    "fabric.mod.json should reference mixin configs"
                )
            }
        }
    }

    @Test @Tag("mdg")
    fun `neoforge jar with mixin configs`() {
        setupNeoForgeProjectWithMixins()
        
        val result = run {
            withArguments("build")
        }

        assertEquals(TaskOutcome.SUCCESS, result.task(":build")?.outcome)
        
        val jarFile = getBuiltJarFile()
        JarFile(jarFile).use { jar ->
            // Check for mixin config files
            val mixinConfig = jar.getJarEntry("testmod.mixins.json")
            assertTrue(mixinConfig != null, "Mixin config should be present in JAR")
        }
    }

    @Test @Tag("loom")
    fun `fabric jar with access widener`() {
        setupLoomProjectWithAccessWidener()
        
        val result = run {
            withArguments("build")
        }

        assertEquals(TaskOutcome.SUCCESS, result.task(":build")?.outcome)
        
        val jarFile = getBuiltJarFile()
        JarFile(jarFile).use { jar ->
            // Check for access widener file
            val accessWidener = jar.getJarEntry("testmod.accessWidener")
            assertTrue(accessWidener != null, "Access widener should be present in JAR")
            
            // Check fabric.mod.json references access widener
            val fabricModJson = jar.getJarEntry("fabric.mod.json")
            assertTrue(fabricModJson != null, "fabric.mod.json should be present")
            
            jar.getInputStream(fabricModJson).use { stream ->
                val content = stream.bufferedReader().readText()
                assertTrue(
                    content.contains("accessWidener"),
                    "fabric.mod.json should reference access widener"
                )
            }
        }
    }

    @Test @Tag("mdg")
    fun `neoforge jar with access transformer`() {
        setupNeoForgeProjectWithAccessTransformer()
        
        val result = run {
            withArguments("build")
        }

        assertEquals(TaskOutcome.SUCCESS, result.task(":build")?.outcome)
        
        val jarFile = getBuiltJarFile()
        JarFile(jarFile).use { jar ->
            // Check for access transformer file
            val accessTransformer = jar.getJarEntry("META-INF/accesstransformer.cfg")
            assertTrue(accessTransformer != null, "Access transformer should be present in JAR")
        }
    }

    private fun setupCompleteLoomProject() {
        setupMinimalLoom()
        
        // Add comprehensive metadata
        buildFile.appendText("""
            modstitch {
                metadata {
                    modId = "testmod"
                    name = "Test Mod"
                    version = "1.0.0"
                    description = "A complete test mod"
                    authors = listOf("Test Author")
                    license = "MIT"
                }
            }
        """.trimIndent())
        
        // Create fabric.mod.json template
        fabricModJson.writeText("""
            {
              "schemaVersion": 1,
              "id": "${'$'}{modId}",
              "version": "${'$'}{version}",
              "name": "${'$'}{name}",
              "description": "${'$'}{description}",
              "authors": [
                ${'$'}{authors.joinToString(",") { "\"${'$'}it\"" }}
              ],
              "license": "${'$'}{license}",
              "environment": "*",
              "entrypoints": {
                "main": [
                  "com.example.testmod.TestMod"
                ]
              },
              "depends": {
                "fabricloader": ">=${modLoaderVersion}",
                "minecraft": "${'$'}{minecraftVersion}"
              }
            }
        """.trimIndent())
        
        createBasicModClass()
    }

    private fun setupCompleteNeoForgeProject() {
        setupMinimalMdg()
        
        buildFile.appendText("""
            modstitch {
                metadata {
                    modId = "testmod"
                    name = "Test Mod"
                    version = "1.0.0"
                    description = "A complete test mod"
                    authors = listOf("Test Author")
                    license = "MIT"
                }
            }
        """.trimIndent())
        
        neoModsToml.writeText("""
            modLoader = "neoforge"
            loaderVersion = "${'$'}{modLoaderVersion}"
            license = "${'$'}{license}"

            [[mods]]
            modId = "${'$'}{modId}"
            version = "${'$'}{version}"
            displayName = "${'$'}{name}"
            description = "${'$'}{description}"
            authors = "${'$'}{authors.joinToString(", ")}"

            [[dependencies.${'$'}{modId}]]
            modId = "neoforge"
            type = "required"
            versionRange = "${'$'}{modLoaderVersion}"
            ordering = "NONE"
            side = "BOTH"
        """.trimIndent())
        
        createBasicModClass()
    }

    private fun setupCompleteForgeLegacyProject() {
        setupMinimalMdgl()
        
        buildFile.appendText("""
            modstitch {
                metadata {
                    modId = "testmod"
                    name = "Test Mod"
                    version = "1.0.0"
                    description = "A complete test mod"
                    authors = listOf("Test Author")
                    license = "MIT"
                }
            }
        """.trimIndent())
        
        modsToml.writeText("""
            modLoader = "forge"
            loaderVersion = "${'$'}{modLoaderVersion}"
            license = "${'$'}{license}"

            [[mods]]
            modId = "${'$'}{modId}"
            version = "${'$'}{version}"
            displayName = "${'$'}{name}"
            description = "${'$'}{description}"
            authors = "${'$'}{authors.joinToString(", ")}"
        """.trimIndent())
        
        createBasicModClass()
    }

    private fun setupLoomProjectWithMixins() {
        setupCompleteLoomProject()
        
        buildFile.appendText("""
            modstitch {
                mixin {
                    config("testmod.mixins.json")
                }
            }
        """.trimIndent())
        
        // Update fabric.mod.json to include mixins
        fabricModJson.writeText("""
            {
              "schemaVersion": 1,
              "id": "${'$'}{modId}",
              "version": "${'$'}{version}",
              "name": "${'$'}{name}",
              "description": "${'$'}{description}",
              "authors": [
                ${'$'}{authors.joinToString(",") { "\"${'$'}it\"" }}
              ],
              "license": "${'$'}{license}",
              "environment": "*",
              "entrypoints": {
                "main": [
                  "com.example.testmod.TestMod"
                ]
              },
              "mixins": [
                "testmod.mixins.json"
              ],
              "depends": {
                "fabricloader": ">=${modLoaderVersion}",
                "minecraft": "${'$'}{minecraftVersion}"
              }
            }
        """.trimIndent())
        
        createMixinClass()
    }

    private fun setupNeoForgeProjectWithMixins() {
        setupCompleteNeoForgeProject()
        
        buildFile.appendText("""
            modstitch {
                mixin {
                    config("testmod.mixins.json")
                }
            }
        """.trimIndent())
        
        createMixinClass()
    }

    private fun setupLoomProjectWithAccessWidener() {
        setupCompleteLoomProject()
        
        // Create access widener file
        projectDir.resolve("modstitch.accessWidener").writeText("""
            accessWidener v1 intermediary
            accessible class net/minecraft/client/MinecraftClient
            accessible method net/minecraft/client/MinecraftClient getWindow ()Lnet/minecraft/client/util/Window;
        """.trimIndent())
        
        // Update fabric.mod.json to include access widener
        fabricModJson.writeText("""
            {
              "schemaVersion": 1,
              "id": "${'$'}{modId}",
              "version": "${'$'}{version}",
              "name": "${'$'}{name}",
              "description": "${'$'}{description}",
              "authors": [
                ${'$'}{authors.joinToString(",") { "\"${'$'}it\"" }}
              ],
              "license": "${'$'}{license}",
              "environment": "*",
              "entrypoints": {
                "main": [
                  "com.example.testmod.TestMod"
                ]
              },
              "accessWidener": "testmod.accessWidener",
              "depends": {
                "fabricloader": ">=${modLoaderVersion}",
                "minecraft": "${'$'}{minecraftVersion}"
              }
            }
        """.trimIndent())
    }

    private fun setupNeoForgeProjectWithAccessTransformer() {
        setupCompleteNeoForgeProject()
        
        // Create access transformer file (in AW format, should be converted)
        projectDir.resolve("modstitch.accessWidener").writeText("""
            accessWidener v1 intermediary
            accessible class net/minecraft/client/MinecraftClient
            accessible method net/minecraft/client/MinecraftClient getWindow ()Lnet/minecraft/client/util/Window;
        """.trimIndent())
    }

    private fun createBasicModClass() {
        val modClassDir = projectDir.resolve("src/main/java/com/example/testmod")
        modClassDir.mkdirs()
        
        modClassDir.resolve("TestMod.java").writeText("""
            package com.example.testmod;
            
            public class TestMod {
                public void onInitialize() {
                    System.out.println("Test Mod Initialized");
                }
            }
        """.trimIndent())
    }

    private fun createMixinClass() {
        val mixinDir = projectDir.resolve("src/main/java/com/example/testmod/mixin")
        mixinDir.mkdirs()
        
        mixinDir.resolve("ExampleMixin.java").writeText("""
            package com.example.testmod.mixin;
            
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;
            import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
            
            @Mixin(Object.class)
            public class ExampleMixin {
                @Inject(at = @At("HEAD"), method = "toString")
                private void onToString(CallbackInfo ci) {
                    // Test mixin injection
                }
            }
        """.trimIndent())
    }

    private fun getBuiltJarFile(): File {
        val jarFile = projectDir.resolve("build/libs/test-project-1.0.0.jar")
        assertTrue(
            jarFile.exists(),
            "Expected JAR file to exist at ${jarFile.absolutePath}. Available files: ${projectDir.resolve("build/libs").listFiles()?.joinToString { it.name } ?: "none"}"
        )
        return jarFile
    }

    private fun inspectFabricJar() {
        val jarFile = getBuiltJarFile()
        JarFile(jarFile).use { jar ->
            // Check for fabric.mod.json
            val fabricModJson = jar.getJarEntry("fabric.mod.json")
            assertTrue(fabricModJson != null, "fabric.mod.json should be present in Fabric JAR")
            
            // Check for mod classes
            val hasModClass = jar.entries().asSequence().any { 
                it.name.contains("com/example/testmod/TestMod.class") 
            }
            assertTrue(hasModClass, "Mod class should be compiled and included in JAR")
            
            // Check basic JAR structure
            assertTrue(jar.size() > 0, "JAR should contain files")
        }
    }

    private fun inspectNeoForgeJar() {
        val jarFile = getBuiltJarFile()
        JarFile(jarFile).use { jar ->
            // Check for neoforge.mods.toml
            val neoforgeModsToml = jar.getJarEntry("META-INF/neoforge.mods.toml")
            assertTrue(neoforgeModsToml != null, "neoforge.mods.toml should be present in NeoForge JAR")
            
            // Check for mod classes
            val hasModClass = jar.entries().asSequence().any { 
                it.name.contains("com/example/testmod/TestMod.class") 
            }
            assertTrue(hasModClass, "Mod class should be compiled and included in JAR")
            
            assertTrue(jar.size() > 0, "JAR should contain files")
        }
    }

    private fun inspectForgeLegacyJar() {
        val jarFile = getBuiltJarFile()
        JarFile(jarFile).use { jar ->
            // Check for mods.toml
            val modsToml = jar.getJarEntry("META-INF/mods.toml")
            assertTrue(modsToml != null, "mods.toml should be present in Forge Legacy JAR")
            
            // Check for mod classes
            val hasModClass = jar.entries().asSequence().any { 
                it.name.contains("com/example/testmod/TestMod.class") 
            }
            assertTrue(hasModClass, "Mod class should be compiled and included in JAR")
            
            assertTrue(jar.size() > 0, "JAR should contain files")
        }
    }
}