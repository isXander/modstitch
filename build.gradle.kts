plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "1.3.0"
    idea
}

group = "dev.isxander.modstitch"
version = "0.2.1"

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
}

dependencies {
    fun plugin(id: String, version: String? = null, prop: String? = null) = "$id:$id.gradle.plugin:${version ?: property(prop!!) as String}"

    implementation(plugin("fabric-loom", prop = "deps.loom"))
    implementation(plugin("net.neoforged.moddev", prop = "deps.moddevgradle"))
    implementation(plugin("net.neoforged.moddev.legacyforge", prop = "deps.moddevgradle"))
    implementation(plugin("me.modmuss50.mod-publish-plugin", prop = "deps.mpp"))
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


