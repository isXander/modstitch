rootProject.name = "test-project"
rootProject.buildFileName = "root.gradle.kts"

pluginManagement {
    includeBuild("..")

    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net")
    }
}

fun createVersion(name: String) {
    include(name)
    val project = project(":$name")
    project.buildFileName = "../build.gradle.kts"
}

createVersion("fabric")
createVersion("neoforge")
createVersion("forge")
createVersion("vanilla")