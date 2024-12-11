plugins {
    id("dev.isxander.modstitch.base")
    //id("dev.isxander.modstitch.publishing")
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

    fabric {
        fabricLoaderVersion = "0.16.9"
    }

    neoforge {
        neoForgeVersion = "21.4.10-beta"
    }
}

//msPublishing {
//    maven {
//
//        repositories {
//
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
//    }
//}
