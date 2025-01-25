package dev.isxander.modstitch.base.moddevgradle

import com.electronwill.nightconfig.core.Config
import com.electronwill.nightconfig.toml.TomlFormat
import dev.isxander.modstitch.base.AppendMixinDataTask
import dev.isxander.modstitch.util.Side

abstract class ModsTomlAppendMixinDataTask : AppendMixinDataTask() {
    override fun applyModificationsToFile(fileExtension: String, contents: String): String {
        if (fileExtension != "toml") error("Invalid file extension: $fileExtension")

        val config = TomlFormat.instance().createParser().parse(contents)

        if (!config.contains("mixins")) {
            config.add("mixins", mutableListOf<Config>())
        }

        val mixins = config.get("mixins") as MutableList<Config>

        mixinConfigs.get().forEach {
            if (it.side != Side.Both) {
                logger.warn("Side-specific mixins are not supported in MDG. Ignoring side for ${it.config}")
            }

            Config.inMemory().apply {
                set("config", it.config)
            }.let { mixins.add(it) }
        }

        return TomlFormat.instance().createWriter().writeToString(config)
    }
}