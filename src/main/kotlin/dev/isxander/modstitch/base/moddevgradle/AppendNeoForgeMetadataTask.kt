package dev.isxander.modstitch.base.moddevgradle

import com.electronwill.nightconfig.core.Config
import com.electronwill.nightconfig.core.file.FileNotFoundAction
import com.electronwill.nightconfig.core.io.WritingMode
import com.electronwill.nightconfig.toml.TomlFormat
import dev.isxander.modstitch.base.AppendModMetadataTask
import java.io.File

/**
 * A Gradle task that appends metadata entries to (Neo)Forge's `mods.toml` files.
 */
abstract class AppendNeoForgeMetadataTask : AppendModMetadataTask() {
    override fun appendModMetadata(file: File) {
        val config = TomlFormat.instance().createParser().parse(file, FileNotFoundAction.THROW_ERROR)
        if (mixins.isPresent) {
            appendNewEntries(config, "mixins", "config", mixins.get().map { it.config })
        }
        appendNewEntries(config, "accessTransformers", "file", accessWideners.get())

        TomlFormat.instance().createWriter().write(config, file, WritingMode.REPLACE)
    }

    /**
     * Appends provided values to a TOML array under the specified key and field name.
     *
     * The values are added only if they are not already present.
     *
     * @param config The parsed TOML configuration.
     * @param key The TOML table to update.
     * @param name The field within the table to append to.
     * @param values The values to append.
     */
    private fun appendNewEntries(config: Config, key: String, name: String, values: Iterable<String>) {
        val entries = config.getOptional<MutableList<Config>>(key).orElseGet {
            mutableListOf<Config>().also { config.set<MutableList<Config>>(key, it) }
        }
        val existingEntries = entries.map { it.getOptional<String>(name).orElse("") }

        for (value in values) {
            if (existingEntries.contains(value)) {
                continue
            }

            val entry = Config.inMemory()
            entry.set<String>(name, value)
            entries.add(entry)
        }
    }
}
