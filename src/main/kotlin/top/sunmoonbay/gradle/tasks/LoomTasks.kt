package top.sunmoonbay.gradle.tasks

import org.gradle.api.Project
import top.sunmoonbay.gradle.tasks.mapping.DeobfuscationServerJarTask
import top.sunmoonbay.gradle.tasks.mapping.ObfuscationServerTask
import top.sunmoonbay.gradle.tasks.server.DownloadServerBundleTask
import top.sunmoonbay.gradle.tasks.server.ExportServerJarTask

class LoomTasks(private val project: Project) {
    fun registerTasks() {
        project.tasks.register("downloadServerBundle", DownloadServerBundleTask::class.java)
        project.tasks.register("exportServerJar", ExportServerJarTask::class.java)
        project.tasks.register("deobfuscationServerJar", DeobfuscationServerJarTask::class.java)
        project.tasks.register("obfuscationServerJar", ObfuscationServerTask::class.java)

        project.tasks.getByName("compileJava")
            .finalizedBy(project.tasks.getByName("obfuscationServerJar"))
    }
}