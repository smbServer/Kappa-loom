plugins {
    id("java")
    id("java-gradle-plugin")
    id("maven-publish")
    kotlin("jvm")
}


fun get(key : String) : String {
    return project.properties[key].toString()
}

group = get("project_group")
version = get("project_version")

repositories {
    maven("https://maven.fabricmc.net")

    repositories {
        mavenCentral()
        mavenLocal()
        google()
    }
}

dependencies {
    implementation(gradleApi())

    implementation("commons-io:commons-io:2.10.0")
    implementation("com.google.code.gson:gson:2.8.0")
    implementation("org.ow2.asm:asm:${get("asm_version")}")
    implementation("org.ow2.asm:asm-analysis:${get("asm_version")}")
    implementation("org.ow2.asm:asm-commons:${get("asm_version")}")
    implementation("org.ow2.asm:asm-tree:${get("asm_version")}")
    implementation("org.ow2.asm:asm-util:${get("asm_version")}")
}

gradlePlugin {
    plugins {
        create("kappa-loom") {
            id = "kappa-loom"
            implementationClass = "top.sunmoonbay.gradle.LoomPlugins"
        }
    }
}

publishing {
    repositories {
        maven(File(project.rootDir, "target")) {
            name = "localRepositories"
        }
    }
}

tasks.register("cleanBuild") {
    group = "kappa-loom"

    doFirst {
        delete(File(project.rootDir, "target"))
	delete(File(project.rootDir, ".gradle"))
        delete(project.layout.buildDirectory)
    }
}
