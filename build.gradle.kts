plugins {
    alias(libs.plugins.kotlinJvm)
    id("org.ohmyloader.gradle")
}

version = "0.1.0"

kotlin {
    jvmToolchain(27)
}

val omlVersion = providers.gradleProperty("oml_version").get()
val gameVersion = providers.gradleProperty("oml.gameVersion").orElse("26.3").get()

oml {
    minecraftVersion.set(gameVersion)
    apiVersion.set(omlVersion)
}
