fun prop(name: String, consumer: (prop: String) -> Unit) {
    (findProperty(name) as? String?)
        ?.let(consumer)
}

modstitch {
    minecraftVersion = findProperty("minecraftVersion") as String
    javaTarget = 17

    metadata {
        modId = "test_project"
        modGroup = "dev.isxander"
        modVersion = "1.0.0"
        modLicense = "ARR"
        modName = "Test Project"
        modDescription = "A test project for ModStitch"
    }

    loom {
        fabricLoaderVersion = "0.16.10"
    }

    moddevgradle {
        enable {
            prop("neoForgeVersion") { neoForgeVersion = it }
            prop("forgeVersion") { forgeVersion = it }
            prop("mcpVersion") { mcpVersion = it }
            prop("neoFormVersion") { neoFormVersion = it }
        }
        println(modLoaderManifest)

        defaultRuns()
    }

    mixin {
        configs.create("test")

        addMixinsToModManifest = true
    }
}

dependencies {
    modstitch.loom {
        modstitchModImplementation("net.fabricmc.fabric-api:fabric-api:0.112.0+1.21.4")
    }

    "org.commonmark:commonmark:0.21.0".let {
        modstitchImplementation(it)
//        msShadow.dependency(it, mapOf(
//            "org.commonmark" to "commonmark"
//        ))
        modstitchJiJ(it)
    }
}

sourceSets.main {
    java.srcDir("../src/main/java")
    resources.srcDir("../src/main/resources")

    modstitch.templatesSourceDirectorySet.srcDir("../src/main/templates")
}

val clientSourceSet = sourceSets.create("client") {
    java.srcDir("../src/client/java")
    resources.srcDir("../src/client/resources")
}

modstitch.createProxyConfigurations(clientSourceSet)

msShadow {
    relocatePackage = "dev.isxander.test.libs"
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
