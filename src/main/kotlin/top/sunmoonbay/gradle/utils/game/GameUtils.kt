package top.sunmoonbay.gradle.utils.game

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.apache.commons.io.IOUtils
import top.sunmoonbay.gradle.LoomExtensions
import top.sunmoonbay.gradle.utils.web.WebUtils
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.jar.JarEntry
import java.util.jar.JarFile

class GameUtils {
    companion object {
        @JvmStatic
        fun getJson(version: String, extension: LoomExtensions): String {
            var jsonUrl = ""
            for (jsonElement in Gson().fromJson(
                WebUtils.readWebString(extension.minecraft.metaDataUrl),
                JsonObject::class.java
            )["versions"].asJsonArray) {
                if (jsonElement.asJsonObject["id"].asString.equals(version)) {
                    jsonUrl = jsonElement.asJsonObject["url"].asString
                }
            }
            return WebUtils.readWebString(jsonUrl).toString()
        }

        @JvmStatic
        fun getBundleDownloadUrl(extension: LoomExtensions): String {
            return Gson().fromJson(extension.minecraft.minecraftVersion?.let { getJson(it, extension) }, JsonObject::class.java)
                .get("downloads").asJsonObject["server"].asJsonObject["url"].asString
        }

        @JvmStatic
        fun getCacheDir(extension: LoomExtensions) : File {
            val file : File? = extension.minecraft.minecraftVersion?.let { File(extension.getUserCache(), it) }

            if (!file?.exists()!!) {
                file.mkdirs()
            }

            return file
        }

        @JvmStatic
        fun getBundleCacheFile(extension: LoomExtensions) : File {
            val file : File = File(getCacheDir(extension), "minecraft-bundle.jar")

            return file
        }

        @JvmStatic
        fun getServerCacheFile(extension: LoomExtensions) : File {
            val file : File = File(getCacheDir(extension), "minecraft-server.jar")

            return file
        }

        @JvmStatic
        fun getServerCleanFile(extension: LoomExtensions) : File {
            val file : File = File(getCacheDir(extension), "minecraft-clean.jar")

            return file
        }

        @JvmStatic
        fun getLibraries(bundleFile: File) : List<String> {
            val libraries : MutableList<String> = ArrayList()

            val jarFile = JarFile(bundleFile)
            val librariesInputStream = jarFile.getInputStream(JarEntry("META-INF/libraries.list"))
            val enter = IOUtils.readLines(librariesInputStream, StandardCharsets.UTF_8)

            for (line : String in enter) {
                val tab = line.split("\t")
                libraries.add(tab[1].trim())
            }

            return libraries
        }
    }
}
