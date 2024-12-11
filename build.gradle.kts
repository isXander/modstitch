import java.util.*

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
}

group = "dev.isxander.modstitch"
version = "0.1.0"

repositories {
    mavenCentral()
}

fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

gradlePlugin {
    fun registerExtension(extensionId: String, platforms: List<String> = listOf("fabric", "neoforge")) {
        val extensionCapitalised = extensionId.capitalize()

        plugins.create(extensionId) {
            id = "dev.isxander.modstitch.$extensionId"
            implementationClass = "dev.isxander.modstitch.$extensionId.${extensionCapitalised}ApplicatorPlugin"
        }

        for (platform in platforms) {
            plugins.create("$extensionId.$platform") {
                id = "dev.isxander.modstitch.$extensionId.$platform"
                implementationClass = "dev.isxander.modstitch.$extensionId.${extensionCapitalised}${platform.capitalize()}ImplPlugin"
            }
        }
    }

    // Creates:
    // - dev.isxander.modstitch.base (dev.isxander.modstitch.base.BaseApplicatorPlugin)
    // - dev.isxander.modstitch.base.fabric (dev.isxander.modstitch.base.BaseFabricImplPlugin)
    // - dev.isxander.modstitch.base.neoforge (dev.isxander.modstitch.base.BaseNeoforgeImplPlugin)
    registerExtension("base", platforms = listOf("fabric", "neoforge"))
}

dependencies {

}
