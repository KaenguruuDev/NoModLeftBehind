plugins {
    java
    id("dev.kaenguruu.versioning") version "0.1.1"
    id("net.neoforged.moddev") version "2.0.144"
}

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
val gsonDependency = "com.google.code.gson:gson:2.10.1"

dependencies {
    implementation(flatLafDependency)
    implementation(flatLafExtrasDependency)
    implementation(migLayoutDependency)
    implementation(gsonDependency)

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
    testImplementation(files(sourceSets.main.get().compileClasspath.files))

    // NeoForge 1.21.1 needs non-Minecraft libraries on the development run classpath.
    add("additionalRuntimeClasspath", flatLafDependency)
    add("additionalRuntimeClasspath", flatLafExtrasDependency)
    add("additionalRuntimeClasspath", migLayoutDependency)
    add("additionalRuntimeClasspath", gsonDependency)
}

// This artifact is discovered as a NeoForge early service. Its service layer cannot resolve
// ordinary Jar-in-Jar libraries yet, so the libraries used by the startup UI must be available
// directly from the service JAR.
val earlyServiceDependencies = configurations.create("earlyServiceDependencies") {
    isCanBeConsumed = false
    isCanBeResolved = true
    extendsFrom(configurations.implementation.get())
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(earlyServiceDependencies.files.map { dependency -> zipTree(dependency) }) {
        exclude("META-INF/services/**")
        exclude("META-INF/*.SF", "META-INF/*.RSA", "META-INF/*.DSA")
        exclude("module-info.class", "META-INF/**/module-info.class")
        // NeoForge provides Gson as its own named module. Embedding Gson here
        // would make this mod JAR contain the same package as that module.
        exclude("com/google/gson/**")
    }
}

tasks.test {
    useJUnitPlatform()
    // UI tests exercise Swing components without opening top-level windows.
    systemProperty("java.awt.headless", "true")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

// Generate NeoForge metadata from the same Gradle properties used for the artifact.
// The logo is copied separately because logoFile refers to a file at the JAR root.
tasks.processResources {
    val metadataProperties = mapOf(
        "mod_id" to project.property("mod.id"),
        "mod_name" to project.property("mod.display_name"),
        "mod_version" to project.version.toString(),
        "mod_license" to project.property("mod.license"),
        "mod_author" to project.property("mod.author"),
        "mod_homepage" to project.property("mod.homepage"),
        "mod_issue_tracker" to project.property("mod.issue_tracker"),
        "mod_logo" to project.property("mod.logo"),
        "mod_description" to project.property("mod.description"),
        "loader_version_range" to project.property("mod.loader_version_range"),
        "minecraft_version_range" to project.property("mod.minecraft_version_range"),
        "neo_version" to project.property("neo_version")
    )

    inputs.properties(metadataProperties)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(metadataProperties)
    }
    from(project.file("Logo.png"))
}
