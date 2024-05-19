package top.sunmoonbay.gradle.tasks.mapping

import org.gradle.api.tasks.TaskAction
import top.sunmoonbay.gradle.utils.game.GameUtils
import top.sunmoonbay.gradle.utils.mapping.MappingUtil
import top.sunmoonbay.gradle.utils.mapping.RemappingUtil
import java.io.FileNotFoundException
import java.io.IOException

open class DeobfuscationServerJarTask : MappingTaskBase() {
    @TaskAction
    fun notch2intermediaryTask() {
        out.info("Obfuscating the jar of the notch name...")

        val mcVersion = extension.minecraft.minecraftVersion
        val cleanFile = GameUtils.getServerCleanFile(extension)
        val mappingFile = extension.mapping.mappingFile
        val notchFile = GameUtils.getServerCacheFile(extension)
        out.info("Minecraft version: $mcVersion")

        if (notchFile.exists()) {
            try {
                val serverMappingUtil =MappingUtil.getInstance(mappingFile)
                val serverRemappingUtil = RemappingUtil.getInstance("deobfuscation", serverMappingUtil.getMap(true))
                serverRemappingUtil.analyzeJar(notchFile)
                serverRemappingUtil.remappingJar(notchFile, cleanFile)

                out.info("Obfuscated the jar of the notch name.")
                out.info("Cleaned server jar line: ${cleanFile.absolutePath}")
            } catch (e: IOException) {
                throw RuntimeException(e.message)
            }
        } else {
            throw FileNotFoundException("server jar file is not found!")
        }
    }
}
