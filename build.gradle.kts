plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "1.3.0"
    idea
}

group = "dev.isxander.modstitch"
version = "0.5.16-unstable"

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
        return listOfNotNull<String>(
            id?.let { "$it:$it.gradle.plugin" },
            version ?: prop?.let { findProperty(it)?.toString() },
        ).joinToString(separator = ":")
    }
    fun pluginImplStrict(id: String, version: String? = null, prop: String? = null) {
        implementation(plugin(id))
        constraints {
            implementation(plugin(id)) {
                version {
                    strictly(plugin(null, version, prop))
                }
            }
        }
    }

    // Gradle Plugins
    pluginImplStrict("fabric-loom", prop = "deps.loom")

    pluginImplStrict("net.neoforged.moddev", prop = "deps.moddevgradle")

    pluginImplStrict("net.neoforged.moddev.legacyforge", prop = "deps.moddevgradle")

    pluginImplStrict("me.modmuss50.mod-publish-plugin", prop = "deps.mpp")

    pluginImplStrict("com.gradleup.shadow", prop = "deps.shadow")

    // Libraries used within the plugin
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.electronwill.night-config:toml:3.8.1")
    implementation("org.semver4j:semver4j:5.5.0")
}

tasks.jar {
    manifest.attributes["Implementation-Version"] = project.version
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

publishing {
    repositories {
        val username = "XANDER_MAVEN_USER".let { System.getenv(it) ?: findProperty(it) }?.toString()
        val password = "XANDER_MAVEN_PASS".let { System.getenv(it) ?: findProperty(it) }?.toString()
        if (username != null && password != null) {
            maven(url = "https://maven.isxander.dev/releases") {
                name = "XanderReleases"
                credentials {
                    this.username = username
                    this.password = password
                }
            }
        } else {
            println("Xander Maven credentials not satisfied.")
        }
    }
}

val releasePlugin by tasks.registering {
    group = "modstitch"

    dependsOn("publishPlugins")
    dependsOn("publishAllPublicationsToXanderReleasesRepository")
}


