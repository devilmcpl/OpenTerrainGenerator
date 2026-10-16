import io.papermc.paperweight.userdev.ReobfArtifactConfiguration

plugins {
    id("platform-conventions")
    id("io.papermc.paperweight.userdev")
    id("xyz.jpenilla.run-paper") // Adds runServer task for testing
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.enginehub.org/repo/")
}

val otg: Configuration by configurations.creating
configurations {
    implementation {
        extendsFrom(otg)
    }
}

dependencies {
    paperweight.paperDevBundle(project.property("paper_dev_bundle").toString())

    otg(project(":common:common-core"))

    // Optional, used for selecting regions for /otg export and /otg edit
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.4.2")
}

// Paper runs Mojang mapped since 1.20.5, so we ship the Mojang mapped jar as is
paperweight.reobfArtifactConfiguration = ReobfArtifactConfiguration.MOJANG_PRODUCTION

tasks {
    processResources {
        inputs.property("version", project.property("otg_version"))
        inputs.property("minecraft_version", project.property("minecraft_version"))

        filesMatching("paper-plugin.yml") {
            val map = mapOf(
                "version" to inputs.properties["version"].toString(),
                "minecraft_version" to inputs.properties["minecraft_version"].toString(),
            )
            expand(map)
        }
    }

    shadowJar {
        configurations.set(listOf(otg))
        archiveClassifier.set("all")
    }

    runServer {
        minecraftVersion(project.property("minecraft_version").toString())
    }
}

otgPlatform {
    productionJar.set(tasks.shadowJar.flatMap { it.archiveFile })
}
