package top.sunmoonbay.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

interface PluginBase : Plugin<Project> {
    override fun apply(project: Project)
}