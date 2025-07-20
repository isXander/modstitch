# Modstitch

Modstitch is an abstraction layer upon first-party Minecraft mod loader tooling to provide a unified developer experience.

```kts
plugins {
    id("dev.isxander.modstitch.base") version "0.6.0-unstable"
}

modstitch {
    minecraftVersion = "1.21.8"
    
    loom {
        fabricLoaderVersion = "0.16.19"
    }
    
    moddevgradle {
        neoForgeVersion = "21.8.0-beta"
    }
    
    parchment {
        version = "2025.12.12"
    }
    
    metadata {
        modId = "my_mod"
        modVersion = "1.0.0"
        modName = "My Mod"
        modGroup = "com.example"
    }
    
    mixins.register("my_mod")
}
```

Modstitch provides a DSL to interact with both [Fabric Loom](https://github.com/fabricmc/fabric-loom) and [ModDevGradle](https://github.com/neoforged/ModDevGradle) with the same buildscript. It is commonly used with Preprocessor plugins such as [Stonecutter](https://stonecutter.kikugie.dev/) which call for a single buildscript for all targets.

Modstitch also provides other DX utilities not found in either Fabric Loom or ModDevGradle, such as automatic
mixin registration in mod manifests, transpiling of AWs/ATs to the correct format for the platform, and sensible defaults to make your build-scripts super concise.