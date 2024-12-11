plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
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
    fun registerExtension(extensionId: String, platforms: List<String> = listOf("fabric", "neoforge")) {
        val extensionCapitalised = extensionId.capitalize()

        plugins.create(extensionId) {
            id = "dev.isxander.modstitch.$extensionId"
            implementationClass = "dev.isxander.modstitch.$extensionId.${extensionCapitalised}Plugin"
        }

        for (platform in platforms) {
            plugins.create("$extensionId.$platform") {
                id = "dev.isxander.modstitch.$extensionId.$platform"
                implementationClass = "dev.isxander.modstitch.$extensionId.$platform.${extensionCapitalised}${platform.capitalize()}Plugin"
            }
        }
    }

    // Creates:
    // - dev.isxander.modstitch.base (dev.isxander.modstitch.base.BaseApplicatorPlugin)
    // - dev.isxander.modstitch.base.fabric (dev.isxander.modstitch.base.BaseFabricImplPlugin)
    // - dev.isxander.modstitch.base.neoforge (dev.isxander.modstitch.base.BaseNeoforgeImplPlugin)
    registerExtension("base", platforms = listOf("fabric", "neoforge"))
    //registerExtension("publishing", platforms = listOf("fabric", "neoforge"))
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


