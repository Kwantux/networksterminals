pluginManagement {
  repositories {
    gradlePluginPortal()
  }
}

rootProject.name = "NetworksTerminals"

// Only include Networks build if the directory exists
if (file("../Networks").exists()) {
    includeBuild("../Networks")
}