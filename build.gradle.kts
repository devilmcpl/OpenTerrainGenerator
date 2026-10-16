plugins {
    id("parent-logic")
    id ("architectury-plugin") version "3.5-SNAPSHOT" apply false
    id ("dev.architectury.loom") version "1.17-SNAPSHOT" apply false
    id ("io.papermc.paperweight.userdev") version "2.0.0-beta.23" apply false
    id ("xyz.jpenilla.run-paper") version "3.0.2" apply false
    id ("maven-publish")
}

defaultTasks = arrayListOf("build", "publishToMavenLocal")

val ignored = listOf("common", "platforms")

subprojects {
    if (!ignored.contains(project.name)) {
        apply(plugin = "base-conventions")
    }
}

version = project.property("otg_version").toString()
group = project.property("otg_group").toString()

val universalJar = tasks.register<Jar>("universalJar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    val otgBuild = project.property("otg_build").toString()
    archiveFileName.set("otg-$version-$otgBuild.jar")
    // Paper loads plugin jars marked as Mojang mapped without remapping them
    manifest.attributes("paperweight-mappings-namespace" to "mojang")
}

tasks.build {
    dependsOn(universalJar)
}

listOf(
    project(":platforms:paper"),
    //project(":platforms:forge"),
    project(":platforms:fabric"),
).forEach { proj ->
    proj.afterEvaluate {
        // Show more errors in intellij
        proj.tasks.withType<JavaCompile>() {
            options.compilerArgs.add("-Xmaxerrs")
            options.compilerArgs.add("5000")
        }
        universalJar {
            val tree = zipTree(proj.the<OTGPlatformExtension>().productionJar)
            from(tree) {
                exclude("META-INF/MANIFEST.MF")
            }
//            val manifestFile = tree.elements.map { files ->
//                files.find { it.asFile.path.endsWith("META-INF/MANIFEST.MF") }!!
//            }
//            manifest.from(manifestFile)
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifact(universalJar)
        }
    }
}
