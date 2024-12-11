package dev.isxander.modstitch

enum class Loader(val friendlyName: String) {
    Fabric("fabric"),
    NeoForge("neoforge");

    companion object {
        fun fromSerialName(name: String): Loader? {
            return values().firstOrNull { it.friendlyName == name }
        }
    }
}