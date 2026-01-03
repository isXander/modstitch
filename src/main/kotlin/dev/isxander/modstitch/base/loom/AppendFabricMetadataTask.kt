package dev.isxander.modstitch.base.loom

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import dev.isxander.modstitch.base.AppendModMetadataTask
import dev.isxander.modstitch.util.Side
import java.io.File

/**
 * A Gradle task that appends metadata entries to Fabric's `fabric.mod.json` files.
 */
abstract class AppendFabricMetadataTask : AppendModMetadataTask() {
    override fun appendModMetadata(file: File) {
        val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
        val json = file.reader().use { gson.fromJson(it, JsonObject::class.java) }

        val schemaVersion = json["schemaVersion"]?.asInt ?: error("Did not detect a schemaVersion in fabric.mod.json.")
        if (schemaVersion != 1) {
            // TODO: support schema version 2
            error("Unsupported fabric.mod.json schema version: $schemaVersion.")
        }

        val classTweaker = classTweakers.get().let { if (it.isEmpty()) null else it.single() }
        val existingClassTweaker = json["classTweaker"]?.asString
        if (existingClassTweaker != null && existingClassTweaker != classTweaker) {
            error("A class tweaker has already been specified: '$existingClassTweaker'.")
        }
        if (classTweaker != null) {
            json.addProperty("accessWidener", classTweaker)
        }

        val mixinConfigs = json.getAsJsonArray("mixins") ?: JsonArray().also { json.add("mixins", it) }
        val existingMixinConfigs = mixinConfigs.map { when {
            it.isJsonObject -> it.asJsonObject.get("config")?.asString ?: ""
            it.isJsonPrimitive && it.asJsonPrimitive.isString -> it.asString
            else -> it.toString()
        }}
        for (mixin in mixins.get()) {
            if (existingMixinConfigs.contains(mixin.config)) {
                continue
            }

            val mixinConfig = JsonObject()
            mixinConfig.addProperty("config", mixin.config)
            mixinConfig.addProperty("environment", when (mixin.side) {
                Side.Both -> "*"
                Side.Client -> "client"
                Side.Server -> "server"
            })
            mixinConfigs.add(mixinConfig)
        }

        return file.writer().use { gson.toJson(json, it) }
    }
}
