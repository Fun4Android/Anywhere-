plugins {
  id("com.android.library")
}

android {
  namespace = "com.flask.colorpicker"
	compileSdk = 34
  buildToolsVersion = "35.0.0"
  ndkVersion = "27.0.12077973"

	defaultConfig {
		minSdk = 23
  }

	buildTypes {
		release {
			isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
		}
	}

  java {
    toolchain {
      languageVersion = JavaLanguageVersion.of(17)
    }
  }
}

configurations.all {
  exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk7")
  exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk8")
}

dependencies {
	api("androidx.appcompat:appcompat:1.7.0")
}
