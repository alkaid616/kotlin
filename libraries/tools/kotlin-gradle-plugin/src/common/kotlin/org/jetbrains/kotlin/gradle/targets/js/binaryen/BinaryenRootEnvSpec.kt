/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.binaryen

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.targets.js.EnvSpec
import org.jetbrains.kotlin.gradle.tasks.internal.CleanableStore

open class BinaryenRootEnvSpec(
    val project: Project,
    rootBinaryen: BinaryenRootExtension,
) : EnvSpec<BinaryenEnv> {
    init {
        check(project.rootProject == project)
    }

    override val download: Property<Boolean> = rootBinaryen.downloadProperty

    override val downloadBaseUrl: Property<String> = rootBinaryen.downloadBaseUrlProperty

    override val installationDirectory: DirectoryProperty = rootBinaryen.installationDirectory

    override val version: Property<String> = rootBinaryen.versionProperty

    override val command: Property<String> = rootBinaryen.commandProperty

    internal val platform: Property<BinaryenPlatform> = rootBinaryen.platform

    override fun produceEnv(): Provider<BinaryenEnv> {
        return download.flatMap { downloadValue ->
            installationDirectory.flatMap { installationDirectoryValue ->
                version.flatMap { versionValue ->
                    command.flatMap { commandValue ->
                        platform.map { platformValue ->
                            val requiredVersionName = "binaryen-version_$versionValue"
                            val cleanableStore = CleanableStore[installationDirectoryValue.asFile.absolutePath]
                            val targetPath = cleanableStore[requiredVersionName].use()
                            val isWindows = platformValue.isWindows()

                            fun getExecutable(command: String, customCommand: String, windowsExtension: String): String {
                                val finalCommand =
                                    if (isWindows && customCommand == command) "$command.$windowsExtension" else customCommand
                                return if (downloadValue)
                                    targetPath
                                        .resolve("bin")
                                        .resolve(finalCommand)
                                        .absolutePath
                                else
                                    finalCommand
                            }

                            BinaryenEnv(
                                download = downloadValue,
                                downloadBaseUrl = downloadBaseUrl.orNull,
                                ivyDependency = "com.github.webassembly:binaryen:$versionValue:${platformValue.platform}@tar.gz",
                                executable = getExecutable("wasm-opt", commandValue, "exe"),
                                dir = targetPath,
                                cleanableStore = cleanableStore,
                                isWindows = isWindows,
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val EXTENSION_NAME: String = "kotlinBinaryenSpec"
    }
}
