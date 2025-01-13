package dev.isxander.modstitch.base.loom

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import dev.isxander.modstitch.base.AppendMixinDataTask
import dev.isxander.modstitch.base.extensions.modstitch
import dev.isxander.modstitch.util.Side

abstract class FMJAppendMixinDataTask : AppendMixinDataTask() {
    override fun applyModificationsToFile(fileExtension: String, contents: String): String {
        if (fileExtension != "json") error("Invalid file extension: $fileExtension")

        val gson = GsonBuilder().setPrettyPrinting().create()

        val json = gson.fromJson(contents, JsonObject::class.java)
        val mixins = json.getAsJsonArray("mixins") ?: JsonArray().also { json.add("mixins", it) }

        project.modstitch.mixin.configs.forEach {
            val obj = JsonObject()
            obj.addProperty("config", it.config.get())
            obj.addProperty("environment", when (it.side.get()) {
                Side.Both -> "*"
                Side.Client -> "client"
                Side.Server -> "server"
            })
            mixins.add(obj)
        }

        return gson.toJson(json)
    }
}