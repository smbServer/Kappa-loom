package top.sunmoonbay.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import top.sunmoonbay.gradle.tasks.LoomTasks
import top.sunmoonbay.gradle.tasks.mapping.DeobfuscationServerJarTask
import top.sunmoonbay.gradle.tasks.mapping.ObfuscationServerTask
import top.sunmoonbay.gradle.tasks.server.DownloadServerBundleTask
import top.sunmoonbay.gradle.tasks.server.ExportServerJarTask
import top.sunmoonbay.gradle.utils.OutUtils
import top.sunmoonbay.gradle.utils.game.GameUtils
import java.io.FileNotFoundException
import java.net.URI

// kappa-loom
class LoomPlugins : PluginBase {
    companion object {
        private lateinit var outUtils: OutUtils

        @JvmStatic
        fun getOut(): OutUtils {
            return outUtils;
        }
    }

    override fun apply(project: Project) {
        outUtils = OutUtils(project, "Kappa")

        outUtils.info("*************** Kappa-loom ***************")
        outUtils.info("author: SunMoonbay Team")
        outUtils.info("versions: ${project.version}")
        outUtils.info("website: ${project.rootProject.properties["project_website"]}")
        outUtils.info("home: ${project.rootProject.properties["project_home"]}")
        outUtils.info("******************************************")

        val loomExtensions: LoomExtensions = project.extensions.create("kappa", LoomExtensions::class.java, project)
        project.afterEvaluate { target ->
            if (loomExtensions.minecraft.minecraftVersion?.isEmpty() == true) {
                throw NullPointerException("Extension minecraft version is null")
            }

            if (loomExtensions.minecraft.metaDataUrl.isEmpty()) {
                throw NullPointerException("Extension meta data url is null")
            }

            if (loomExtensions.referenceMap.isEmpty()) {
                throw NullPointerException("Extension referenceMap is null")
            }

            if (loomExtensions.minecraft.librariesUrl.isEmpty()) {
                throw NullPointerException("Extension libraries url is null")
            }

            if (loomExtensions.mapping.mappingFile == null) {
                throw NullPointerException("Extension mapping url is null")
            }

            if (!loomExtensions.mapping.mappingFile!!.exists()) {
                throw FileNotFoundException("Extension mapping file is not found")
            }

            LoomRepositories(project, loomExtensions).loadRepositories()
            LoomTasks(target).registerTasks()

            val bundleFile = GameUtils.getBundleCacheFile(loomExtensions)
            val serverFile = GameUtils.getServerCacheFile(loomExtensions)
            val cleanFile = GameUtils.getServerCleanFile(loomExtensions)

            if (serverFile.exists()) {
                target.dependencies.add("runtimeOnly", target.files(serverFile))
            }

            if (cleanFile.exists()) {
                target.dependencies.add("compileOnly", target.files(cleanFile))
            }

            if (bundleFile.exists()) {
                GameUtils.getLibraries(bundleFile).forEach { library ->
                    target.dependencies.add("implementation", library)
                }
            }
        }
    }
}
