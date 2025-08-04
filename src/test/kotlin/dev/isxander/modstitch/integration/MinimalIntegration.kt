package dev.isxander.modstitch.integration

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Tag
import kotlin.test.Test
import kotlin.test.assertEquals

class MinimalIntegration : BaseFunctionalTest() {
    @Test @Tag("loom")
    fun `minimal loom succeeds`() {
        setupMinimalLoom()

        val result = run {
            withArguments("build", "--stacktrace")
        }

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":build")?.outcome,
            "Expected build task to succeed, but it failed with outcome: ${result.task(":build")?.outcome}"
        )
    }

    @Test @Tag("mdg")
    fun `minimal mdg succeeds`() {
        setupMinimalMdg()

        val result = run {
            withArguments("build", "--stacktrace")
        }

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":build")?.outcome,
            "Expected build task to succeed, but it failed with outcome: ${result.task(":build")?.outcome}"
        )
    }

    @Test @Tag("mdgl")
    fun `minimal mdgl succeeds`() {
        setupMinimalMdgl()

        val result = run {
            withArguments("build", "--stacktrace")
        }

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":build")?.outcome,
            "Expected build task to succeed, but it failed with outcome: ${result.task(":build")?.outcome}"
        )
    }
}