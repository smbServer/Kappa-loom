package top.sunmoonbay.gradle.tasks.server

import top.sunmoonbay.gradle.tasks.TaskBase

open class ServerTaskBase : TaskBase() {
    init {
        group = "kappa-server"
    }
}