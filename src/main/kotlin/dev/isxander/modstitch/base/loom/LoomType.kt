package dev.isxander.modstitch.base.loom

import dev.isxander.modstitch.util.Platform

enum class LoomType(val platform: Platform) {
    NoRemap(Platform.Loom),
    Remap(Platform.LoomRemap)
}