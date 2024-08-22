/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.targets.js.EnvSpec
import org.jetbrains.kotlin.gradle.tasks.internal.CleanableStore
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.property
import org.jetbrains.kotlin.gradle.utils.providerWithLazyConvention
import java.io.File

@Suppress("DEPRECATION")
open class NodeJsEnvSpec(
    val project: Project,
    rootNodeJs: () -> NodeJsRootExtension,
) : EnvSpec<NodeJsEnv> {

    override val installationDirectory: DirectoryProperty = project.objects.directoryProperty()
        .convention(
            project.objects.directoryProperty().fileProvider(
                project.objects.providerWithLazyConvention {
                    rootNodeJs().installationDir
                }
            )
        )

    override val download: org.gradle.api.provider.Property<Boolean> = project.objects.property<Boolean>()
        .convention(project.objects.providerWithLazyConvention { rootNodeJs().download })

    // value not convention because this property can be nullable to not add repository
    override val downloadBaseUrl: org.gradle.api.provider.Property<String> = project.objects.property<String>()
        .convention(project.objects.providerWithLazyConvention { rootNodeJs().downloadBaseUrl })

    // Release schedule: https://github.com/nodejs/Release
    // Actual LTS and Current versions: https://nodejs.org/en/download/
    // Older versions and more information, e.g. V8 version inside: https://nodejs.org/en/download/releases/
    override val version: org.gradle.api.provider.Property<String> = project.objects.property<String>()
        .convention(project.objects.providerWithLazyConvention { rootNodeJs().version })

    override val command: org.gradle.api.provider.Property<String> = project.objects.property<String>()
        .convention(project.objects.providerWithLazyConvention { rootNodeJs().command })

    internal val platform: org.gradle.api.provider.Property<Platform> = project.objects.property<Platform>()

    override fun produceEnv(providerFactory: ProviderFactory): Provider<NodeJsEnv> {
        return providerFactory.provider {
            val platformValue = platform.get()
            val name = platformValue.name
            val architecture = platformValue.arch

            val versionValue = version.get()
            val nodeDirName = "node-v$versionValue-$name-$architecture"
            val cleanableStore = CleanableStore[installationDirectory.getFile().absolutePath]
            val nodeDir = cleanableStore[nodeDirName].use()
            val isWindows = platformValue.isWindows()
            val nodeBinDir = if (isWindows) nodeDir else nodeDir.resolve("bin")

            val downloadValue = download.get()
            fun getExecutable(command: String, customCommand: String, windowsExtension: String): String {
                val finalCommand =
                    if (isWindows && customCommand == command) "$command.$windowsExtension" else customCommand
                return if (downloadValue) File(nodeBinDir, finalCommand).absolutePath else finalCommand
            }

            fun getIvyDependency(): String {
                val type = if (isWindows) "zip" else "tar.gz"
                return "org.nodejs:node:$versionValue:$name-$architecture@$type"
            }

            NodeJsEnv(
                download = downloadValue,
                cleanableStore = cleanableStore,
                dir = nodeDir,
                nodeBinDir = nodeBinDir,
                executable = getExecutable("node", command.get(), "exe"),
                platformName = name,
                architectureName = architecture,
                ivyDependency = getIvyDependency(),
                downloadBaseUrl = downloadBaseUrl.orNull,
            )
        }
    }

    val nodeJsSetupTaskProvider: TaskProvider<out NodeJsSetupTask>
        get() = project.tasks.withType(NodeJsSetupTask::class.java).named(NodeJsSetupTask.NAME)

    companion object {
        const val EXTENSION_NAME: String = "kotlinNodeJsSpec"
    }
}
