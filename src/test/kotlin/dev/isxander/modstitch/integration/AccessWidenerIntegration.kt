package dev.isxander.modstitch.integration

import dev.isxander.modstitch.unit.AccessWidenerTest
import dev.isxander.modstitch.util.AccessWidener
import dev.isxander.modstitch.util.AccessWidenerFormat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
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

    private fun createAWFile(file: File, format: AccessWidenerFormat) {
        file.writeText(AccessWidenerTest.sampleAW(format).toString())
    }

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

        // Get the JAR file and check if it exists
        val jarFile = projectDir.resolve("build/libs/unnamed_mod-1.0.0.jar")
        assert(jarFile.exists()) { "Expected JAR file to exist at ${jarFile.absolutePath}. Libs are: ${projectDir.resolve("build/libs").listFiles()?.joinToString { it.name }}" }

        // Check if the AT file is present in the JAR
        JarFile(jarFile).use { jar ->
            val atFile = jar.getJarEntry("META-INF/accesstransformer.cfg")
            assertNotNull(atFile) { "Expected AT file to be present within JAR but is not." }

            jar.getInputStream(atFile).use { ins ->
                val parsedAT = AccessWidener.parse(ins.bufferedReader())

                assertEquals(
                    AccessWidenerTest.sampleAW(AccessWidenerFormat.AT),
                    parsedAT,
                    "Parsed AT does not match expected sample AT"
                )
            }
        }
    }
}