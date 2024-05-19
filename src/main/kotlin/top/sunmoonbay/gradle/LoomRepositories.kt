package top.sunmoonbay.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import java.net.URI

class LoomRepositories(private val project : Project, private val loomExtensions: LoomExtensions) {
    fun loadRepositories() {
        project.repositories.maven { mavenArtifactRepository: MavenArtifactRepository ->
            mavenArtifactRepository.name = "minecraft"
            mavenArtifactRepository.url = URI.create(loomExtensions.minecraft.librariesUrl)
        }

        project.repositories.maven { mavenArtifactRepository: MavenArtifactRepository ->
            mavenArtifactRepository.name = "spongepowered"
            mavenArtifactRepository.url = URI.create("https://repo.spongepowered.org/maven")
        }

        project.repositories.mavenCentral()
        project.repositories.mavenLocal()
        project.repositories.google()
    }
}