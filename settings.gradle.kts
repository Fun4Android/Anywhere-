pluginManagement {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}

dependencyResolutionManagement {
  repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
  repositories {
    google()
    mavenCentral()
    maven("https://jitpack.io")
  }
}

rootProject.name = "Anywhere-"
include(":app", ":color-picker")

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
