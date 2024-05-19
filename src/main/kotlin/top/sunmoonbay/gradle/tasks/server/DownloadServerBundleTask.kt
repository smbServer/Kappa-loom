package top.sunmoonbay.gradle.tasks.server

import org.apache.commons.io.FileUtils
import org.gradle.api.tasks.TaskAction
import top.sunmoonbay.gradle.utils.game.GameUtils
import top.sunmoonbay.gradle.utils.web.WebUtils

open class DownloadServerBundleTask : ServerTaskBase() {
    @TaskAction
    fun downloadServerBundle() {
        out.info("Downloading server bundle...")

        val mcVersion = extension.minecraft.minecraftVersion
        out.info("Minecraft version: $mcVersion")

        val bundleUrl = GameUtils.getBundleDownloadUrl(extension)
        val bundleFileByte = WebUtils.readWebFile(bundleUrl)
        val bundleFile = GameUtils.getBundleCacheFile(extension)

        if (bundleFile.exists()) {
            val inputStream = bundleFile.inputStream()
            if (inputStream.readBytes().isEmpty()) {
                FileUtils.writeByteArrayToFile(bundleFile, bundleFileByte)
            }
        } else {
            bundleFile.createNewFile()

            FileUtils.writeByteArrayToFile(bundleFile, bundleFileByte)
        }

        out.info("Minecraft $mcVersion bundle is downloaded!")
        out.info("Minecraft $mcVersion bundle link: ${bundleFile.absolutePath}")
    }
}
