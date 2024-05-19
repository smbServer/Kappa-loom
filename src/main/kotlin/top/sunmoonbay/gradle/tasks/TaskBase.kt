package top.sunmoonbay.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import top.sunmoonbay.gradle.LoomExtensions
import top.sunmoonbay.gradle.LoomPlugins
import top.sunmoonbay.gradle.utils.OutUtils

open class TaskBase : DefaultTask() {
    @Input
    @JvmField
    var extension : LoomExtensions
    @Input
    @JvmField
    var out : OutUtils

    init {
        extension = project.extensions.getByType(LoomExtensions::class.java)
        out = LoomPlugins.getOut()
    }
}
