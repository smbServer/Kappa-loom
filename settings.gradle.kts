pluginManagement {
    repositories {
        mavenCentral()
        google()
    }

    val kotlin_version : String by settings

    plugins {
        kotlin("jvm") version kotlin_version
    }
}

rootProject.name = "Kappa-loom"
