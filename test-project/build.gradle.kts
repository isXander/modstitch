plugins {
    id("dev.isxander.modstitch.base")
}

modstitch {
    minecraftVersion = "1.21.11"

    unitTesting()

    loom {
        fabricLoaderVersion = "0.18.4"
    }

    moddevgradle {
        neoForgeVersion = "26.1.0.0-alpha.1+snapshot-1"
    }

    runs {
        register("funny") {
            client()
        }
    }

    println(modLoaderManifest.getOrElse("'modLoaderManifest' is not set."))
    println(javaVersion.map { "Java version: $it" }.getOrElse("'javaVersion' is not set."))
    println(isUnobfuscated.orNull?.toString()?.let { "Is unobfuscated: $it" } ?: "'isUnobfuscated' is not set.")

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
        //modstitchModImplementation("net.fabricmc.fabric-api:fabric-api:0.140.3+26.1")
    }

    "org.commonmark:commonmark:0.21.0".let {
        modstitchImplementation(it)
//        msShadow.dependency(it, mapOf(
//            "org.commonmark" to "commonmark"
//        ))
        modstitchJiJ(it)
    }
}

//msShadow {
//    relocatePackage = "dev.isxander.test.libs"
//}
//
//java {
//    withSourcesJar()
//}
//
//msPublishing {
//    maven {
//        repositories {
//            mavenLocal()
//        }
//    }
//
//    mpp {
//        type = STABLE
//
//        modrinth {
//            accessToken = findProperty("pub.modrinth.token") as String?
//            projectId = "12345678"
//        }
//
//        dryRun = true
//    }
//}
