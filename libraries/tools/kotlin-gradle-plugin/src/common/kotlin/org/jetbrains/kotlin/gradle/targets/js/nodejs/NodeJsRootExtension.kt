/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.findExtension
import org.jetbrains.kotlin.gradle.targets.js.NpmVersions
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinRootNpmResolver
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.PACKAGE_JSON_UMBRELLA_TASK_NAME
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmCachesSetup
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.RootPackageJsonTask
import org.jetbrains.kotlin.gradle.utils.property
import java.io.File
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty

open class NodeJsRootExtension(
    val project: Project,
    private val nodeJs: () -> NodeJsExtension,
) {

    init {
        check(project.rootProject == project)

        val projectProperties = PropertiesProvider(project)

        if (projectProperties.errorJsGenerateExternals != null) {
            project.logger.warn(
                """
                |
                |==========
                |Please note, Dukat integration in Gradle plugin does not work now, it was removed.
                |We rethink how we can integrate properly.
                |==========
                |
                """.trimMargin()
            )
        }
    }

    private val gradleHome = project.gradle.gradleUserHomeDir.also {
        project.logger.kotlinInfo("Storing cached files in $it")
    }

    @Suppress("DEPRECATION")
    @Deprecated(
        "Use installationDir from NodeJsExtension (not NodeJsRootExtension) instead." +
                "You can find this extension after applying NodeJsPlugin"
    )
    var installationDir: File = gradleHome.resolve("nodejs")

    @Suppress("DEPRECATION")
    @Deprecated(
        "Use download from NodeJsExtension (not NodeJsRootExtension) instead" +
                "You can find this extension after applying NodeJsPlugin"
    )
    var download = true

    @Suppress("DEPRECATION")
    @Deprecated(
        "Use downloadBaseUrl from NodeJsExtension (not NodeJsRootExtension) instead" +
                "You can find this extension after applying NodeJsPlugin"
    )
    var nodeDownloadBaseUrl by ::downloadBaseUrl

    @Suppress("DEPRECATION")
//    @Deprecated(
//        "Use downloadBaseUrl from NodeJsExtension (not NodeJsRootExtension) instead" +
//                "You can find this extension after applying NodeJsPlugin"
//    )
    var downloadBaseUrl: String? = "https://nodejs.org/dist"

    @Suppress("DEPRECATION")
    @Deprecated(
        "Use version from NodeJsExtension (not NodeJsRootExtension) instead" +
                "You can find this extension after applying NodeJsPlugin"
    )
    var nodeVersion by ::version

    @Suppress("DEPRECATION")
//    @Deprecated(
//        "Use downloadBaseUrl from NodeJsExtension (not NodeJsRootExtension) instead" +
//                "You can find this extension after applying NodeJsPlugin"
//    )
    var version = "22.0.0"

    @Suppress("DEPRECATION")
    @Deprecated(
        "Use command from NodeJsExtension (not NodeJsRootExtension) instead" +
                "You can find this extension after applying NodeJsPlugin"
    )
    var command = "node"

    @Suppress("DEPRECATION")
    @Deprecated(
        "Use command from NodeJsExtension (not NodeJsRootExtension) instead" +
                "You can find this extension after applying NodeJsPlugin"
    )
    var nodeCommand by ::command

    val rootProjectDir
        get() = project.rootDir

    val packageManagerExtension: org.gradle.api.provider.Property<NpmApiExt> = project.objects.property()

    val taskRequirements: TasksRequirements
        get() = resolver.tasksRequirements

    lateinit var resolver: KotlinRootNpmResolver

    val rootPackageDirectory: Provider<Directory> = project.layout.buildDirectory.dir("js")

    val projectPackagesDirectory: Provider<Directory>
        get() = rootPackageDirectory.map { it.dir("packages") }

    val nodeModulesGradleCacheDirectory: Provider<Directory>
        get() = rootPackageDirectory.map { it.dir("packages_imported") }

    val versions = NpmVersions()

    val npmInstallTaskProvider: TaskProvider<out KotlinNpmInstallTask>
        get() = project.tasks.withType(KotlinNpmInstallTask::class.java).named(KotlinNpmInstallTask.NAME)

    val rootPackageJsonTaskProvider: TaskProvider<RootPackageJsonTask>
        get() = project.tasks.withType(RootPackageJsonTask::class.java).named(RootPackageJsonTask.NAME)

    val packageJsonUmbrellaTaskProvider: TaskProvider<Task>
        get() = project.tasks.named(PACKAGE_JSON_UMBRELLA_TASK_NAME)

    val npmCachesSetupTaskProvider: TaskProvider<out KotlinNpmCachesSetup>
        get() = project.tasks.withType(KotlinNpmCachesSetup::class.java).named(KotlinNpmCachesSetup.NAME)

    @Deprecated(
        "Use nodeJsSetupTaskProvider from NodeJsExtension (not NodeJsRootExtension) instead" +
                "You can find this extension after applying NodeJsPlugin"
    )
    val nodeJsSetupTaskProvider: TaskProvider<out NodeJsSetupTask>
        get() = nodeJs().nodeJsSetupTaskProvider

    @Deprecated("Use NodeJsExtension instead")
    fun requireConfigured(): NodeJsEnv {
        return nodeJs().requireConfigured()
    }

    companion object {
        const val EXTENSION_NAME: String = "kotlinNodeJs"
    }
}
