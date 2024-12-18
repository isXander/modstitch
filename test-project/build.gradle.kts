plugins {
    id("dev.isxander.modstitch.base")
    id("dev.isxander.modstitch.publishing")
}

fun prop(name: String, consumer: (prop: String) -> Unit) {
    (findProperty(name) as? String?)
        ?.let(consumer)
}

modstitch {
    minecraftVersion = findProperty("minecraftVersion") as String

    metadata {
        modId = "test_project"
        modGroup = "dev.isxander"
        modVersion = "1.0.0"
        modLicense = "ARR"
        modName = "Test Project"
        modDescription = "A test project for ModStitch"
    }

    loom {
        fabricLoaderVersion = "0.16.9"
    }

    moddevgradle {
        prop("forgeVersion") { forgeVersion = it }
        prop("neoformVersion") { neoformVersion = it }

        defaultRuns()
    }

    mixin {
        configs.create("test") {
            side = BOTH
        }
    }
}

dependencies {
    modstitch.loom {
        modstitchModImplementation("net.fabricmc.fabric-api:fabric-api:0.112.0+1.21.3")
    }

    "org.commonmark:commonmark:0.21.0".let {
        modstitchImplementation(it)
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
