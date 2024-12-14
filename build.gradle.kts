plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "1.3.0"
    idea
}

group = "dev.isxander.modstitch"
version = "0.1.0"

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

    fun registerExtension(extensionId: String, platforms: List<String> = listOf("fabric", "neoforge"), description: String) {
        val extensionCapitalised = extensionId.capitalize()

        plugins.create(extensionId) {
            id = "dev.isxander.modstitch.$extensionId"
            implementationClass = "dev.isxander.modstitch.$extensionId.${extensionCapitalised}Plugin"

            displayName = "ModStitch $extensionCapitalised"
            this.description = description
            tags = pluginTags
        }

        for (platform in platforms) {
            plugins.create("$extensionId.$platform") {
                id = "dev.isxander.modstitch.$extensionId.$platform"
                implementationClass = "dev.isxander.modstitch.$extensionId.$platform.${extensionCapitalised}${platform.capitalize()}Plugin"

                displayName = "ModStitch $extensionCapitalised for $platform platform"
                this.description = "The '$platform' platform implementation of ModStitch $extensionCapitalised"
                tags = pluginTags
            }
        }
    }

    // Creates:
    // - dev.isxander.modstitch.base (dev.isxander.modstitch.base.BasePlugin)
    // - dev.isxander.modstitch.base.loom (dev.isxander.modstitch.base.fabric.BaseLoomPlugin)
    // - dev.isxander.modstitch.base.moddevgradle (dev.isxander.modstitch.base.moddevgradle.BaseModdevgradlePlugin)
    registerExtension("base", platforms = listOf("loom", "moddevgradle"), description = "The base plugin for ModStitch")
    registerExtension("publishing", platforms = listOf("loom", "moddevgradle"), description = "Adds mod publishing functionality to ModStitch")
}

dependencies {
    fun plugin(id: String, version: String) = "$id:$id.gradle.plugin:$version"

    implementation(plugin("fabric-loom", "1.9.+"))
    implementation(plugin("net.neoforged.moddev", "1.0.23"))
    implementation(plugin("me.modmuss50.mod-publish-plugin", "0.8.1"))
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


