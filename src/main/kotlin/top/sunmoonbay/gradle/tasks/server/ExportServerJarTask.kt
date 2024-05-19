package top.sunmoonbay.gradle.tasks.server

import org.apache.commons.io.FileUtils
import org.gradle.api.tasks.TaskAction
import top.sunmoonbay.gradle.utils.game.GameUtils
import java.io.FileNotFoundException
import java.nio.charset.StandardCharsets
import java.util.jar.JarEntry
import java.util.jar.JarFile

open class ExportServerJarTask : ServerTaskBase() {
    @TaskAction
    fun exportServerJar() {
        out.info("Exporting server...")

        val mcVersion = extension.minecraft.minecraftVersion
        out.info("Minecraft version: $mcVersion")

        val bundleFile = GameUtils.getBundleCacheFile(extension)

        if (bundleFile.exists()) {
            val serverFile = GameUtils.getServerCacheFile(extension)

            if (serverFile.exists()) {
                val inputStream = serverFile.inputStream()
                if (inputStream.readBytes().isEmpty()) {
                    val jarFile = JarFile(bundleFile)
                    val version = String(
                        jarFile.getInputStream(JarEntry("META-INF/versions.list")).readBytes(),
                        StandardCharsets.UTF_8
                    )
                    val split = version.split("\t")
                    val jarBytes = jarFile.getInputStream(JarEntry("META-INF/versions/${split[2]}")).readBytes()

                    FileUtils.writeByteArrayToFile(serverFile, jarBytes)
                }

            } else {
                serverFile.createNewFile()
                val jarFile = JarFile(bundleFile)
                val version = String(
                    jarFile.getInputStream(JarEntry("META-INF/versions.list")).readBytes(),
                    StandardCharsets.UTF_8
                )
                val split = version.split("\t")
                val jarBytes = jarFile.getInputStream(JarEntry("META-INF/versions/${split[2]}")).readBytes()

                FileUtils.writeByteArrayToFile(serverFile, jarBytes)
            }

            out.info("Minecraft $mcVersion server is exported!")
            out.info("Minecraft $mcVersion server link: ${serverFile.absolutePath}")
        } else {
            throw FileNotFoundException("bundle file is not exist!")
        }
    }
}
