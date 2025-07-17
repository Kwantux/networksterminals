import net.minecrell.pluginyml.bukkit.BukkitPluginDescription
import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    `java-library`
    `maven-publish`
    signing
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("net.minecrell.plugin-yml.paper") version "0.6.0"
}

runPaper.folia.registerTask()

group = "de.kwantux.networks"
version = "0.1.0"
description = "Networks Addon for Terminals"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://api.modrinth.com/maven")
}

dependencies {
    compileOnly("dev.folia", "folia-api", "1.20.1-R0.1-SNAPSHOT")
//    compileOnly("maven.modrinth", "Networks", "3.0.11")
    compileOnly(files("run/plugins/Networks-3.1.2.jar"))
    paperLibrary("net.kyori", "adventure-text-minimessage", "4.13.1")
    paperLibrary("org.spongepowered", "configurate-hocon", "4.1.2")
    paperLibrary("org.spongepowered", "configurate-yaml", "4.1.2")
    paperLibrary("org.incendo", "cloud-paper", "2.0.0-beta.10")
    paperLibrary("com.google.code.gson", "gson", "2.10.1")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

paper {
    main = "de.kwantux.networks.terminals.TerminalsPlugin"
    loader = "de.kwantux.networks.terminals.Loader"
    apiVersion = "1.20"
    load = BukkitPluginDescription.PluginLoadOrder.STARTUP
    website = "https://networks.kwantux.de"
    authors = listOf("Kwantux")
    prefix = "Networks] [Terminals"

    permissions {
        register("networks.terminals.use") {
            description = "Allows you to use networks terminals"
            default = BukkitPluginDescription.Permission.Default.TRUE // TRUE, FALSE, OP or NOT_OP
        }
    }

    generateLibrariesJson = true

    foliaSupported = true
    
    serverDependencies {
        register("Networks") {
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            required = true
            joinClasspath = true
        }
    }
}

tasks {
    runServer {
        minecraftVersion("1.21.5")
    }
}