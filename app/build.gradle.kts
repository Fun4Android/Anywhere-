import java.nio.file.Paths

plugins {
  id("com.android.application")
  kotlin("android")
  id("com.google.devtools.ksp")
  id("kotlin-parcelize")
  id("dev.rikka.tools.materialthemebuilder")
}

val verName = "2.5.5"
val verCode = 2050500

android {
  compileSdk = 34
  buildToolsVersion = "35.0.0"
  ndkVersion = "27.0.12077973"

  defaultConfig {
    applicationId = "com.absinthe.anywhere_"
    namespace = "com.absinthe.anywhere_"
    minSdk = 23
    targetSdk = 33
    versionCode = verCode
    versionName = verName
    manifestPlaceholders["appName"] = "Anywhere-"
    ndk {
      //noinspection ChromeOsAbiSupport
      abiFilters += arrayOf("armeabi-v7a", "arm64-v8a")
    }
    resourceConfigurations += arrayOf("en", "zh-rCN", "zh-rTW", "zh-rHK")

    setProperty("archivesBaseName", "Anywhere-$versionName-$versionCode")
  }

  ksp {
    arg("room.incremental", "true")
    arg("room.schemaLocation", "$projectDir/schemas")
  }

  buildFeatures {
    aidl = true
    buildConfig = true
    viewBinding = true
  }

  buildTypes {
    debug {
      applicationIdSuffix = ".debug"
      manifestPlaceholders["appName"] = "Anywhere-β"
      buildConfigField("boolean", "BETA", "true")
    }
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
      buildConfigField("boolean", "BETA", "false")
    }
    all {
      buildConfigField(
        "String",
        "APP_CENTER_SECRET",
        "\"" + System.getenv("APP_CENTER_SECRET").orEmpty() + "\""
      )
    }
  }

  java {
    toolchain {
      languageVersion = JavaLanguageVersion.of(17)
    }
  }

  kotlin {
    jvmToolchain(17)
    compilerOptions {
      freeCompilerArgs = listOf(
        "-Xno-param-assertions",
        "-Xno-call-assertions",
        "-Xno-receiver-assertions"
      )
    }
  }

  dependenciesInfo {
    includeInBundle = false
    includeInApk = false
  }

  externalNativeBuild {
    cmake {
      path = file("CMakeLists.txt")
    }
  }

  packaging {
    resources {
      excludes += "META-INF/**"
      excludes += "okhttp3/**"
      excludes += "kotlin/**"
      excludes += "org/**"
      excludes += "**.properties"
      // https://github.com/Kotlin/kotlinx.coroutines?tab=readme-ov-file#avoiding-including-the-debug-infrastructure-in-the-resulting-apk
      excludes += "DebugProbesKt.bin"
      // https://issueantenna.com/repo/kotlin/kotlinx.coroutines/issues/3158
      excludes += "kotlin-tooling-metadata.json"

      excludes += "XPP3_1.1.3.2_VERSION"
      excludes += "XPP3_1.1.3.3_VERSION"
    }
    jniLibs {
      useLegacyPackaging = false
    }
    dex {
      useLegacyPackaging = false
    }
  }
}

materialThemeBuilder {
  themes {
    create("anywhere") {
      primaryColor = "#8BC34A"
      lightThemeFormat = "Theme.Material3.Light.%s"
      lightThemeParent = "Theme.Material3.Light.Rikka"
      darkThemeFormat = "Theme.Material3.Dark.%s"
      darkThemeParent = "Theme.Material3.Dark.Rikka"
    }
  }
  generatePalette = true
}

repositories {
  mavenCentral()
}

tasks.register("optimizeReleaseRes") {
  doLast {
    val aapt2 = File(
      androidComponents.sdkComponents.sdkDirectory.get().asFile,
      "build-tools/${project.android.buildToolsVersion}/aapt2"
    )
    val zip = Paths.get(
      buildDir.path,
      "intermediates",
      "optimized_processed_res",
      "release",
      "optimizeReleaseResources",
      "resources-release-optimize.ap_"
    )
    val optimized = File("${zip}.opt")
    val cmd = exec {
      commandLine(
        aapt2, "optimize",
        "--collapse-resource-names",
        "--resources-config-path",
        "aapt2-resources.cfg",
        "-o", optimized,
        zip
      )
      isIgnoreExitValue = false
    }
    if (cmd.exitValue == 0) {
      delete(zip)
      optimized.renameTo(zip.toFile())
    }
  }
  dependsOn("optimizeReleaseResources")
}

configurations.all {
  exclude("androidx.appcompat", "appcompat")
  exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk7")
  exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk8")
}

dependencies {
  implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

  implementation(project(":color-picker"))
  implementation(files("libs/IceBox-SDK-1.0.6.aar"))

  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

  implementation("com.github.zhaobozhen.libraries:me:1.1.4")
  implementation("com.github.zhaobozhen.libraries:utils:1.1.4")

  val appCenterSdkVersion = "5.0.4"
  implementation("com.microsoft.appcenter:appcenter-analytics:${appCenterSdkVersion}")
  implementation("com.microsoft.appcenter:appcenter-crashes:${appCenterSdkVersion}")

  // Android X
  val roomVersion = "2.6.1"
  implementation("androidx.room:room-runtime:${roomVersion}")
  implementation("androidx.room:room-ktx:${roomVersion}")
  ksp("androidx.room:room-compiler:${roomVersion}")

  val lifecycleVersion = "2.8.4"
  implementation("androidx.lifecycle:lifecycle-livedata-ktx:${lifecycleVersion}")
  implementation("androidx.lifecycle:lifecycle-common-java8:${lifecycleVersion}")
  implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:${lifecycleVersion}")

  implementation("androidx.browser:browser:1.8.0")
  implementation("androidx.constraintlayout:constraintlayout:2.1.4")
  implementation("androidx.coordinatorlayout:coordinatorlayout:1.3.0-alpha02")
  implementation("androidx.viewpager2:viewpager2:1.1.0")
  implementation("androidx.recyclerview:recyclerview:1.3.2")
  implementation("androidx.drawerlayout:drawerlayout:1.2.0")

  // KTX
  implementation("androidx.collection:collection-ktx:1.4.0")
  implementation("androidx.activity:activity-ktx:1.9.1")
  implementation("androidx.fragment:fragment-ktx:1.8.2")
  implementation("androidx.palette:palette-ktx:1.0.0")
  implementation("androidx.core:core-ktx:1.14.0-alpha01")
  implementation("androidx.preference:preference-ktx:1.2.1")

  // Google
  implementation("com.google.android.material:material:1.13.0-alpha05")

  // Function
  implementation("com.github.bumptech.glide:glide:4.16.0")
  ksp("com.github.bumptech.glide:compiler:4.16.0")

  implementation("com.google.code.gson:gson:2.11.0")
  implementation("com.google.zxing:core:3.5.3")
  implementation("com.blankj:utilcodex:1.31.1")
  implementation("com.tencent:mmkv-static:1.3.9")
  implementation("com.github.CymChad:BaseRecyclerViewAdapterHelper:3.0.11")
  implementation("com.github.heruoxin.Delegated-Scopes-Manager:client:master-SNAPSHOT")
  implementation("com.github.topjohnwu.libsu:core:6.0.0")
  implementation("com.github.thegrizzlylabs:sardine-android:0.8")
  implementation("com.jonathanfinerty.once:once:1.3.1")
  implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")
  implementation("com.jakewharton.timber:timber:5.0.1")

  // UX
  implementation("com.drakeet.about:about:2.5.2")
  implementation("com.drakeet.multitype:multitype:4.3.0")
  implementation("com.drakeet.drawer:drawer:1.0.3")
  implementation("com.github.sephiroth74:android-target-tooltip:2.0.4")
  implementation("com.leinardi.android:speed-dial:3.3.0")
  implementation("me.zhanghai.android.fastscroll:library:1.3.0")

  val shizukuVersion = "12.2.0"
  // required by Shizuku and Sui
  implementation("dev.rikka.shizuku:api:$shizukuVersion")
  // required by Shizuku
  implementation("dev.rikka.shizuku:provider:$shizukuVersion")

  implementation("dev.rikka.rikkax.appcompat:appcompat:1.6.1")
  implementation("dev.rikka.rikkax.core:core:1.4.1")
  implementation("dev.rikka.rikkax.material:material:2.7.0")
  implementation("dev.rikka.rikkax.recyclerview:recyclerview-ktx:1.3.2")
  implementation("dev.rikka.rikkax.widget:borderview:1.1.0")
  implementation("dev.rikka.rikkax.preference:simplemenu-preference:1.0.3")
  implementation("dev.rikka.rikkax.insets:insets:1.3.0")
  implementation("dev.rikka.rikkax.layoutinflater:layoutinflater:1.3.0")
  implementation("dev.rikka.rikkax.material:material-preference:2.0.0")

  // Network
  implementation("com.squareup.okhttp3:okhttp:4.12.0")
  implementation("com.squareup.retrofit2:retrofit:2.11.0")
  implementation("com.squareup.retrofit2:converter-gson:2.11.0")
  implementation("com.squareup.okio:okio:3.9.0")

  // Rx
  implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
  implementation("io.reactivex.rxjava2:rxjava:2.2.21")
  implementation("org.reactivestreams:reactive-streams:1.0.4")

  // Debug
  debugImplementation("com.squareup.leakcanary:leakcanary-android:3.0-alpha-8")
}
