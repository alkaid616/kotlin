/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.targets.js.EnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.Platform
import org.jetbrains.kotlin.gradle.tasks.internal.CleanableStore
import org.jetbrains.kotlin.gradle.utils.getFile

abstract class YarnRootEnvSpec : EnvSpec<YarnEnv>() {

    internal abstract val platform: Property<Platform>

    abstract val ignoreScripts: Property<Boolean>

    abstract val yarnLockMismatchReport: Property<YarnLockMismatchReport>

    abstract val reportNewYarnLock: Property<Boolean>

    abstract val yarnLockAutoReplace: Property<Boolean>

    abstract val resolutions: ListProperty<YarnResolution>

    override fun produceEnv(providerFactory: ProviderFactory): Provider<YarnEnv> {
        return providerFactory.provider {
            val cleanableStore = CleanableStore[installationDirectory.getFile().path]

            val isWindows = platform.get().isWindows()

            val home = cleanableStore["yarn-v${version.get()}"].use()

            val downloadValue = download.get()
            fun getExecutable(
                command: String,
                customCommand: String,
                windowsExtension: String,
            ): String {
                val finalCommand =
                    if (isWindows && customCommand == command) "$command.$windowsExtension" else customCommand
                return if (downloadValue)
                    home
                        .resolve("bin/yarn.js").absolutePath
                else
                    finalCommand
            }

            YarnEnv(
                download = downloadValue,
                downloadBaseUrl = downloadBaseUrl.orNull,
                cleanableStore = cleanableStore,
                dir = home,
                executable = getExecutable("yarn", command.get(), "cmd"),
                ivyDependency = "com.yarnpkg:yarn:${version.get()}@tar.gz",
                ignoreScripts = ignoreScripts.get(),
                yarnLockMismatchReport = yarnLockMismatchReport.get(),
                reportNewYarnLock = reportNewYarnLock.get(),
                yarnLockAutoReplace = yarnLockAutoReplace.get(),
                yarnResolutions = resolutions.get()
            )
        }
    }

    companion object {
        const val YARN: String = "kotlinYarnSpec"
    }

}