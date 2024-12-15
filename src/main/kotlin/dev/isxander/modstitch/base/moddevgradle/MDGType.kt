package dev.isxander.modstitch.base.moddevgradle

import dev.isxander.modstitch.util.Platform

enum class MDGType(val platform: Platform) {
    Regular(Platform.MDG),
    Legacy(Platform.MDGLegacy),
    Vanilla(Platform.MDGVanilla),
}