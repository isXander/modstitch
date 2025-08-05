modstitch {
    println(modLoaderManifest.getOrElse("'modLoaderManifest' is not set."))
    println(javaVersion.map { "Java version: $it" }.getOrElse("'javaVersion' is not set."))

    moddevgradle {
        defaultRuns()
    }

    mixin {
        configs.create("test")
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
