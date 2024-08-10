include(":app")
include(":color-picker")

rootProject.apply {
    name = "Anywhere-"
    buildFileName = "build.gradle.kts"
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
