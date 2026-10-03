plugins {
    java
    id("net.neoforged.moddev") version "2.0.144"
}

version = property("mod.version") as String
group = property("mod.group") as String

base {
    archivesName = property("mod.archive_name") as String
}

tasks.withType<Jar>().configureEach {
    archiveBaseName.set(project.property("mod.archive_name") as String)
    archiveVersion.set(project.version.toString())
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }

    withSourcesJar()
}

repositories {
    mavenCentral()

    maven("https://maven.neoforged.net/releases") {
        name = "NeoForge"
    }
}

neoForge {
    version = property("neo_version") as String

    runs {
        create("client") {
            client()
            systemProperty("java.awt.headless", "false")
            logLevel = org.slf4j.event.Level.DEBUG
        }

        create("server") {
            server()
            logLevel = org.slf4j.event.Level.DEBUG
        }

        create("data") {
            data()
        }
    }

    mods {
        create(property("mod.id") as String) {
            sourceSet(sourceSets.main.get())
        }
    }
}

val flatLafDependency = "com.formdev:flatlaf:3.7.2"
val flatLafExtrasDependency = "com.formdev:flatlaf-extras:3.7.2"
val migLayoutDependency = "com.miglayout:miglayout-swing:11.4.3"

dependencies {
    implementation(flatLafDependency)
    implementation(flatLafExtrasDependency)
    implementation(migLayoutDependency)

    // NeoForge 1.21.1 needs non-Minecraft libraries on the development run classpath.
    add("additionalRuntimeClasspath", flatLafDependency)
    add("additionalRuntimeClasspath", flatLafExtrasDependency)
    add("additionalRuntimeClasspath", migLayoutDependency)

    // Include the libraries in the built mod for production use.
    jarJar(flatLafDependency)
    jarJar(flatLafExtrasDependency)
    jarJar(migLayoutDependency)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.processResources {
    val metadataProperties = mapOf(
        "license" to project.property("mod.license"),
        "github" to project.property("mod.github"),
        "name" to project.property("mod.name"),
        "id" to project.property("mod.id"),
        "modversion" to project.property("mod.version"),
        "display_name" to project.property("mod.display_name"),
        "author" to project.property("mod.author"),
        "description" to project.property("mod.description"),
        "namespace" to project.property("mod.namespace"),
        "neo_version" to project.property("neo_version"),
        "mc" to "[${project.property("minecraft_version")}]"
    )

    inputs.properties(metadataProperties)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(metadataProperties)
    }
}
