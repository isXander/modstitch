package dev.isxander.modstitch.integration

import com.google.gson.JsonObject
import dev.isxander.modstitch.unit.ClassTweakerTest
import dev.isxander.modstitch.util.ClassTweaker
import dev.isxander.modstitch.util.ClassTweakerFormat
import dev.isxander.modstitch.util.ClassTweakerNamespace
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Tag
import java.io.File
import java.util.jar.JarFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AccessWidenerIntegration : BaseFunctionalTest() {
    // all valid searched paths for access widener files
    val `modstitch dot ct` by lazy { projectDir.resolve("modstitch.ct") }
    val `dot classTweaker` by lazy { projectDir.resolve(".classTweaker") }
    val `accesstransformer dot cfg` by lazy { projectDir.resolve("accesstransformer.cfg") }

    @Test @Tag("mdg")
    fun `AT appear in JAR`() {
        setupMinimalMdg()

        // create AW file in project root for modstitch to find
        createCTFile(`dot classTweaker`, ClassTweakerFormat.AW_V1, ClassTweakerNamespace.Official)

        // run gradle build to produce the JAR
        val result = run {
            withArguments("build", "--stacktrace")
        }

        // assert that the build was successful
        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":build")?.outcome,
            "Expected build task to succeed, but it failed with outcome: ${result.task(":build")?.outcome}"
        )

        confirmCTInJar(ClassTweakerFormat.AT, remap = false)
    }

    @Test @Tag("loom")
    fun `AW appear in JAR (remap)`() {
        setupMinimalLoomAW(remap = true)

        // run gradle build to produce the JAR
        val result = run {
            withArguments("build", "--stacktrace")
        }

        println(result.output)

        // assert that the build was successful
        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":build")?.outcome,
            "Expected build task to succeed, but it failed with outcome: ${result.task(":build")?.outcome}"
        )

        confirmCTInJar(ClassTweakerFormat.CT, remap = true)
    }

    @Test @Tag("loom-noremap")
    fun `AW appear in JAR (no remap)`() {
        setupMinimalLoomAW(remap = false)

        // run gradle build to produce the JAR
        val result = run {
            withArguments("build", "--stacktrace")
        }

        // assert that the build was successful
        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":build")?.outcome,
            "Expected build task to succeed, but it failed with outcome: ${result.task(":build")?.outcome}"
        )

        confirmCTInJar(ClassTweakerFormat.CT, remap = false)
    }

    @Test @Tag("loom-noremap")
    fun `AW appear in JAR with configuration cache`() {
        setupMinimalLoomAW(remap = false)

        repeat(2) {
            val result = run {
                withArguments("clean", "build", "--stacktrace", "--configuration-cache")
            }
            assertEquals(
                TaskOutcome.SUCCESS,
                result.task(":build")?.outcome,
                "Expected build task to succeed, but it failed with outcome: ${result.task(":build")?.outcome}"
            )
        }

        confirmCTInJar(ClassTweakerFormat.CT, remap = false)
    }

    private fun setupMinimalLoomAW(remap: Boolean) {
        if (remap) {
            setupMinimalLoomRemap()
        } else {
            setupMinimalLoom()
        }

        // create AW file in project root for modstitch to find
        createCTFile(`dot classTweaker`, ClassTweakerFormat.CT, if (remap) ClassTweakerNamespace.Named else ClassTweakerNamespace.Official)

        // create FMJ so we can check that we're putting the AW in the FMJ
        val fmj = JsonObject().apply {
            // required for loom to not die
            addProperty("schemaVersion", 1)
            addProperty("id", "unnamed_mod")
        }
        fabricModJson.writeText(fmj.toString())
    }

    private fun createCTFile(file: File, format: ClassTweakerFormat, namespace: ClassTweakerNamespace) {
        file.writeText(ClassTweakerTest.sampleCT(format, namespace).toString())
    }

    private fun confirmCTInJar(format: ClassTweakerFormat, remap: Boolean) {
        val awLocation = when (format) {
            ClassTweakerFormat.AT -> "META-INF/accesstransformer.cfg"
            ClassTweakerFormat.AW_V1, ClassTweakerFormat.AW_V2, ClassTweakerFormat.CT -> "unnamed_mod.ct"
        }

        // Get the JAR file and check if it exists
        val jarFile = projectDir.resolve("build/libs/unnamed_mod-1.0.0.jar")
        assert(jarFile.exists()) { "Expected JAR file to exist at ${jarFile.absolutePath}. Libs are: ${projectDir.resolve("build/libs").listFiles()?.joinToString { it.name }}" }

        // Check if the AT file is present in the JAR
        JarFile(jarFile).use { jar ->
            val awFile = jar.getJarEntry(awLocation)
            assertNotNull(awFile) { "Expected AW/AT file to be present within JAR but is not." }

            jar.getInputStream(awFile).use { ins ->
                val parsedAW = ClassTweaker.parse(ins.bufferedReader())

                assertEquals(
                    ClassTweakerTest.sampleCT(format, namespace = when (format) {
                        ClassTweakerFormat.AT -> ClassTweakerNamespace.Official
                        ClassTweakerFormat.AW_V1, ClassTweakerFormat.AW_V2, ClassTweakerFormat.CT ->
                            if (remap) ClassTweakerNamespace.Intermediary else ClassTweakerNamespace.Official
                    }),
                    parsedAW,
                    "Parsed AW/AT/CT does not match expected sample AW/AT/CT"
                )
            }
        }
    }
}