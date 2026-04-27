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
version = "0.1.4"
description = "Networks Addon for Terminals"

// Define Networks version at project level for access in tasks
val networksVersion = "3.1.8"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://api.modrinth.com/maven")
}

dependencies {
    compileOnly("dev.folia", "folia-api", "1.21.4-R0.1-SNAPSHOT")
    
    // Try to use local Networks build first, fallback to Modrinth Maven
    try {
        // Check if local Networks project is available
        if (file("../Networks").exists()) {
            // Read version from Networks project
            println("Using local Networks build version: $networksVersion")
            compileOnly("de.kwantux", "Networks", networksVersion)
        } else {
            println("Local Networks not found, using Modrinth Maven")
            compileOnly("maven.modrinth", "Networks", networksVersion) // Uses latest version
        }
    } catch (e: Exception) {
        println("Failed to use local Networks, falling back to Modrinth Maven: ${e.message}")
        compileOnly("maven.modrinth", "Networks", networksVersion) // Uses latest version
    }
    
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
    // Task to clean up old Networks versions
    val cleanNetworks = task<Delete>("cleanNetworksPlugin") {
        delete(fileTree("run/plugins") {
            include("Networks-*.jar")
            exclude("Networks-${networksVersion}.jar")
        })
        description = "Remove old Networks versions from plugins directory"
        group = "run paper"
    }
    
    // Task to download Networks jar to run/plugins directory
    val downloadNetworks = task<Copy>("downloadNetworksPlugin") {
        dependsOn(cleanNetworks)

        println(cleanNetworks)
        
        val networksJar = file("../Networks/build/libs/Networks-${networksVersion}.jar")
        if (networksJar.exists()) {
            from(networksJar)
            into("run/plugins")
            println("Using local Networks build: ${networksJar.name}")
        } else {
            // Use Modrinth dependency
            val networksConfig = configurations.create("networksJar")
            dependencies {
                add("networksJar", "maven.modrinth:Networks:${networksVersion}") // Use known version
            }
            from(networksConfig)
            into("run/plugins")
            rename { "Networks-${networksVersion}.jar" }
            println("Using Modrinth Networks version: $networksVersion")
        }
        
        description = "Download Networks jar to run/plugins directory"
        group = "run paper"
    }
    
    runServer {
        minecraftVersion("1.21.11")
        // Pass development flag to JVM
        jvmArgs("-Dnetworks.development=true")
        // Ensure Networks is downloaded before running server
        dependsOn(downloadNetworks)
    }
}