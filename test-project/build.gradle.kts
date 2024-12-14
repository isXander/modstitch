plugins {
    id("dev.isxander.modstitch.base")
    id("dev.isxander.modstitch.publishing")
}

modstitch {
    minecraftVersion = "1.21.4"

    metadata {
        modId = "test_project"
        modGroup = "dev.isxander"
        modVersion = "1.0.0"
        modLicense = "ARR"
        modName = "Test Project"
        modDescription = "A test project for ModStitch"
    }

    msLoom {
        fabricLoaderVersion = "0.16.9"
    }

    msModdevgradle {
        neoForgeVersion = "21.4.10-beta"
    }
}

dependencies {
    modstitch.msLoom {
        "modstitchModImplementation"("net.fabricmc.fabric-api:fabric-api:0.112.0+1.21.3")
    }

    "modstitchImplementation"("org.commonmark:commonmark:0.21.0")
}


msPublishing {
    maven {
        repositories {
            mavenLocal()
        }
    }

    mpp {
        type = STABLE

        modrinth {
            accessToken = findProperty("pub.modrinth.token") as String?
            projectId = "12345678"
        }

        dryRun = true
    }
}
