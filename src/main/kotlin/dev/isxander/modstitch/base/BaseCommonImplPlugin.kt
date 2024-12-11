package dev.isxander.modstitch.base

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create

abstract class BaseCommonImplPlugin : Plugin<Project> {
    override fun apply(target: Project) = target.run {
        pluginManager.apply("java-library")
        pluginManager.apply("idea")

        val msExt = extensions.create<ModStitchExtension>("modstitch")

        pluginManager.withPlugin("base") {
            extensions.configure<BasePluginExtension> {
                archivesName.set(msExt.manifest.modId)
            }
        }

        return@run
    }
}