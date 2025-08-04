package dev.isxander.modstitch.integration

import com.google.gson.JsonObject
import dev.isxander.modstitch.unit.AccessWidenerTest
import dev.isxander.modstitch.util.AccessWidener
import dev.isxander.modstitch.util.AccessWidenerFormat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Tag
import java.io.File
import java.util.jar.JarFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AccessWidenerIntegration : BaseFunctionalTest() {
    // all valid searched paths for access widener files
    val `modstitch dot accessWidener` by lazy { projectDir.resolve("modstitch.accessWidener") }
    val `dot accessWidener` by lazy { projectDir.resolve(".accessWidener") }
    val `accesstransformer dot cfg` by lazy { projectDir.resolve("accesstransformer.cfg") }

    @Test @Tag("mdg")
    fun `AT appear in JAR`() {
        setupMinimalMdg()

        // create AW file in project root for modstitch to find
        createAWFile(`modstitch dot accessWidener`, AccessWidenerFormat.AW_V1)

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

        confirmAWInJar(AccessWidenerFormat.AT)
    }

    @Test @Tag("loom")
    fun `AW appear in JAR`() {
        setupMinimalLoomAW()

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

        confirmAWInJar(AccessWidenerFormat.AW_V2)
    }

    @Test @Tag("loom")
    fun `AW appear in JAR with configuration cache`() {
        setupMinimalLoomAW()

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

        confirmAWInJar(AccessWidenerFormat.AW_V2)
    }

    private fun setupMinimalLoomAW() {
        setupMinimalLoom()

        // create AW file in project root for modstitch to find
        createAWFile(`modstitch dot accessWidener`, AccessWidenerFormat.AW_V1)

        // create FMJ so we can check that we're putting the AW in the FMJ
        val fmj = JsonObject().apply {
            // required for loom to not die
            addProperty("schemaVersion", 1)
            addProperty("id", "unnamed_mod")
        }
        fabricModJson.writeText(fmj.toString())
    }

    private fun createAWFile(file: File, format: AccessWidenerFormat) {
        file.writeText(AccessWidenerTest.sampleAW(format).toString())
    }

    private fun confirmAWInJar(format: AccessWidenerFormat) {
        val awLocation = when (format) {
            AccessWidenerFormat.AT -> "META-INF/accesstransformer.cfg"
            AccessWidenerFormat.AW_V1, AccessWidenerFormat.AW_V2 -> "unnamed_mod.accessWidener"
        }

        // Get the JAR file and check if it exists
        val jarFile = projectDir.resolve("build/libs/unnamed_mod-1.0.0.jar")
        assert(jarFile.exists()) { "Expected JAR file to exist at ${jarFile.absolutePath}. Libs are: ${projectDir.resolve("build/libs").listFiles()?.joinToString { it.name }}" }

        // Check if the AT file is present in the JAR
        JarFile(jarFile).use { jar ->
            val awFile = jar.getJarEntry(awLocation)
            assertNotNull(awFile) { "Expected AW/AT file to be present within JAR but is not." }

            jar.getInputStream(awFile).use { ins ->
                val parsedAW = AccessWidener.parse(ins.bufferedReader())

                assertEquals(
                    AccessWidenerTest.sampleAW(format, namespace = when (format) {
                        AccessWidenerFormat.AT -> "named"
                        AccessWidenerFormat.AW_V1, AccessWidenerFormat.AW_V2 -> "intermediary"
                    }),
                    parsedAW,
                    "Parsed AT does not match expected sample AT"
                )
            }
        }
    }
}