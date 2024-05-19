package top.sunmoonbay.gradle

import org.gradle.api.Action
import org.gradle.api.Project
import java.io.File

open class LoomExtensions(private val project : Project) {
    @JvmField
    val minecraft : MinecraftConfiguration = MinecraftConfiguration()
    @JvmField
    val mapping : MappingConfiguration = MappingConfiguration()
    lateinit var referenceMap : String

    fun minecraft(action : Action<MinecraftConfiguration>) : MinecraftConfiguration {
        action.execute(minecraft)
        return minecraft
    }

    fun mapping(action : Action<MappingConfiguration>) : MappingConfiguration {
        action.execute(mapping)
        return mapping
    }

    class MinecraftConfiguration {
        var metaDataUrl = "https://launchermeta.mojang.com/mc/game/version_manifest.json"
        var librariesUrl = "https://libraries.minecraft.net"
        var minecraftVersion : String? = null
    }

    class MappingConfiguration {
        @JvmField
        var mappingFile : File? = null
    }

    fun getUserCache(): File {
        val userCache = File(project.gradle.gradleUserHomeDir, "caches" + File.separator + "kappa")

        if (!userCache.exists()) {
            userCache.mkdirs()
        }

        return userCache
    }
}
