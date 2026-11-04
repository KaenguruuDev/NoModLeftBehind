pluginManagement {
    repositories {
        maven {
            url = uri("../KaenguruuGradlePlugins/build/local-repository")
        }
        maven {
            name = "KaenguruuPlugins"
            url = uri(
                providers.gradleProperty("kaenguruuPluginRepository")
                    .orElse(providers.environmentVariable("KAENGURUU_PLUGIN_REPOSITORY"))
                    .orElse("https://maven.pkg.github.com/KaenguruuDev/KaenguruuGradlePlugins")
                    .get()
            )
            val pluginUser = providers.gradleProperty("kaenguruuPluginUser")
                .orElse(providers.environmentVariable("KAENGURUU_PLUGIN_USER"))
            val pluginToken = providers.gradleProperty("kaenguruuPluginToken")
                .orElse(providers.environmentVariable("KAENGURUU_PLUGIN_TOKEN"))
            if (pluginUser.isPresent && pluginToken.isPresent) {
                credentials {
                    username = pluginUser.get()
                    password = pluginToken.get()
                }
            }
        }
        gradlePluginPortal()
        maven("https://maven.neoforged.net/releases")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "NoModLeftBehind"
