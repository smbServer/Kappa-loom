package top.sunmoonbay.gradle.utils

import org.gradle.api.Project

class OutUtils(private val project : Project, private val name : String) {
    fun info(msg : String) {
        project.logger.lifecycle("[$name] [Info] - $msg")
    }

    @Deprecated(message = "This method is useless and has been deprecated.")
    fun error(msg : String) {
        project.logger.error("[$name] [Error] - $msg")
    }

    @Deprecated(message = "This method is useless and has been deprecated.")
    fun warn(msg : String) {
        project.logger.warn("[$name] [Warn] - $msg")
    }
}
