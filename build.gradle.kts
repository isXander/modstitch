plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "2.0.0"
    idea
}

group = "dev.isxander.modstitch"
version = "0.7.1-unstable"

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://maven.fabricmc.net/")
    maven("https://maven.neoforged.net/releases/")
}

fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

gradlePlugin {
    website = "https://github.com/isXander/modstitch"
    vcsUrl = "https://github.com/isXander/modstitch.git"
    val pluginTags = listOf("modstitch", "minecraft", "mod")

    fun registerExtension(extensionId: String, description: String) {
        val extensionCapitalised = extensionId.capitalize()

        plugins.create(extensionId) {
            id = "dev.isxander.modstitch.$extensionId"
            implementationClass = "dev.isxander.modstitch.$extensionId.${extensionCapitalised}Plugin"

            displayName = "ModStitch $extensionCapitalised"
            this.description = description
            tags = pluginTags
        }
    }

    registerExtension("base", description = "The base plugin for ModStitch")
    registerExtension("publishing", description = "Adds mod publishing functionality to ModStitch")
    registerExtension("shadow", description = "Adds shadow plugin functionality, automatically configured for mod platforms.")
}

dependencies {
    fun plugin(id: String?, version: String? = null, prop: String? = null): String {
        return listOfNotNull(
            id?.let { "$it:$it.gradle.plugin" },
            version ?: prop?.let { findProperty(it)?.toString() },
        ).joinToString(separator = ":")
    }

    // Gradle Plugins
    implementation(plugin("fabric-loom", prop = "deps.loom"))
    implementation(plugin("net.neoforged.moddev", prop = "deps.moddevgradle"))
    implementation(plugin("net.neoforged.moddev.legacyforge", prop = "deps.moddevgradle"))
    implementation(plugin("me.modmuss50.mod-publish-plugin", prop = "deps.mpp"))
    implementation(plugin("com.gradleup.shadow", prop = "deps.shadow"))

    // Libraries used within the plugin
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.electronwill.night-config:toml:3.8.1")
    implementation("org.semver4j:semver4j:5.5.0")


    // Libraries used for testing
    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.jar {
    manifest.attributes["Implementation-Version"] = project.version
}

tasks.test {
    useJUnitPlatform {
        excludeTags("mdgl") // mdgl does not work without at least one mixin
    }

    testLogging {
        // Log all standard lifecycle events: PASSED, FAILED, SKIPPED
        events("passed", "failed", "skipped")

        // For failed tests, show the full stack trace
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

        // Show any output streams (println, etc.) from the tests
        showStandardStreams = true
    }
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

val releasePlugin by tasks.registering {
    group = "modstitch"

    dependsOn("publishPlugins")
    dependsOn("publishAllPublicationsToXanderReleasesRepository")
}


