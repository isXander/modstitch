package dev.isxander.modstitch.base.loom

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import dev.isxander.modstitch.base.AppendMixinDataTask
import dev.isxander.modstitch.util.Side

abstract class FMJAppendMixinDataTask : AppendMixinDataTask() {
    override fun applyModificationsToFile(fileExtension: String, contents: String): String {
        if (fileExtension != "json") error("Invalid file extension: $fileExtension")

        val gson = GsonBuilder().setPrettyPrinting().create()

        val json = gson.fromJson(contents, JsonObject::class.java)
        val mixins = json.getAsJsonArray("mixins") ?: JsonArray().also { json.add("mixins", it) }
        val existingConfigs = mixins.map { when {
            it.isJsonObject -> it.asJsonObject.get("config").asString
            it.isJsonPrimitive && it.asJsonPrimitive.isString -> it.asString
            else -> ""
        }}

        mixinConfigs.get().forEach {
            val obj = JsonObject()
            // ensure idempotentness
            if (existingConfigs.contains(it.config)) {
                return@forEach
            }
            obj.addProperty("config", it.config)
            obj.addProperty("environment", when (it.side) {
                Side.Both -> "*"
                Side.Client -> "client"
                Side.Server -> "server"
            })
            mixins.add(obj)
        }

        return gson.toJson(json)
    }
}