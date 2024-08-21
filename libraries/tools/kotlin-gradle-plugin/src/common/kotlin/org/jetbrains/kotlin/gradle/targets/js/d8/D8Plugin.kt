/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.d8

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.MultiplePluginDeclarationDetector
import org.jetbrains.kotlin.gradle.targets.js.d8.D8Extension.Companion.EXTENSION_NAME
import org.jetbrains.kotlin.gradle.tasks.CleanDataTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.castIsolatedKotlinPluginClassLoaderAware


open class D8Plugin : Plugin<Project> {
    override fun apply(project: Project) {
        MultiplePluginDeclarationDetector.detect(project)

        project.plugins.apply(BasePlugin::class.java)

        val settings = project.extensions.create(EXTENSION_NAME, D8Extension::class.java, project)

        project.registerTask<D8SetupTask>(D8SetupTask.NAME, listOf(settings)) {
            it.group = TASKS_GROUP_NAME
            it.description = "Download and install a D8"
            it.configuration = it.ivyDependencyProvider.map { ivyDependency ->
                project.configurations.detachedConfiguration(project.dependencies.create(ivyDependency))
                    .also { conf -> conf.isTransitive = false }
            }
        }

        project.registerTask<CleanDataTask>("d8" + CleanDataTask.NAME_SUFFIX) {
            it.cleanableStoreProvider = settings.produceEnv().map { it.cleanableStore }
            it.group = TASKS_GROUP_NAME
            it.description = "Clean unused local d8 version"
        }
    }

    companion object {
        const val TASKS_GROUP_NAME: String = "d8"

        // uncomment after bootstrap and changes inside setupV8
//        @InternalKotlinGradlePluginApi
        fun apply(project: Project): D8Extension {
            project.plugins.apply(D8Plugin::class.java)
            return project.extensions.getByName(EXTENSION_NAME) as D8Extension
        }

        @InternalKotlinGradlePluginApi
        val Project.kotlinD8Extension: D8Extension
            get() = extensions.getByName(EXTENSION_NAME).castIsolatedKotlinPluginClassLoaderAware()
    }
}
