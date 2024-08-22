/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.binaryen

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.EnvSpec
import org.jetbrains.kotlin.gradle.tasks.internal.CleanableStore
import org.jetbrains.kotlin.gradle.utils.getFile

@ExperimentalWasmDsl
abstract class BinaryenRootEnvSpec : EnvSpec<BinaryenEnv>() {

    internal abstract val platform: Property<BinaryenPlatform>

    override fun produceEnv(providerFactory: ProviderFactory): Provider<BinaryenEnv> {
        return providerFactory.provider {
            val versionValue = version.get()
            val requiredVersionName = "binaryen-version_$versionValue"
            val cleanableStore = CleanableStore[installationDirectory.getFile().absolutePath]
            val targetPath = cleanableStore[requiredVersionName].use()
            val platformValue = platform.get()
            val isWindows = platformValue.isWindows()

            val downloadValue = download.get()
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
                executable = getExecutable("wasm-opt", command.get(), "exe"),
                dir = targetPath,
                cleanableStore = cleanableStore,
                isWindows = isWindows,
            )
        }
    }

    companion object {
        const val EXTENSION_NAME: String = "kotlinBinaryenSpec"
    }
}
