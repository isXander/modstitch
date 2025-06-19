modstitch {
    fabricLoaderVersion = findProperty("fabricLoaderVersion") as String?
    neoForgeVersion = findProperty("neoForgeVersion") as String?
    forgeVersion = findProperty("forgeVersion") as String?
    mcpVersion = findProperty("mcpVersion") as String?
    neoFormVersion = findProperty("neoFormVersion") as String?
    minecraftVersion = findProperty("minecraftVersion") as String?
    javaTarget = 17
    println(modLoaderManifest)

    metadata {
        modId = "test_project"
        modGroup = "dev.isxander"
        modVersion = "1.0.0"
        modLicense = "ARR"
        modName = "Test Project"
        modDescription = "A test project for ModStitch"
    }

    moddevgradle {
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

java {
    withSourcesJar()
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
