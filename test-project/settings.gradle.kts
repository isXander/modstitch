rootProject.name = "test-project"

pluginManagement {
    includeBuild("..")
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net")
    }
}
