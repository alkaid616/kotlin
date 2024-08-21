/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.targets.js.EnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.Platform
import org.jetbrains.kotlin.gradle.tasks.internal.CleanableStore
import org.jetbrains.kotlin.gradle.utils.listProperty
import org.jetbrains.kotlin.gradle.utils.property
import org.jetbrains.kotlin.gradle.utils.providerWithLazyConvention

open class YarnRootEnvSpec(
    val project: Project,
    rootYarn: YarnRootExtension,
) : EnvSpec<YarnEnv> {
    init {
        check(project == project.rootProject)
    }

    override val download: Property<Boolean> = rootYarn.downloadProperty

    override val downloadBaseUrl: Property<String> = rootYarn.downloadBaseUrlProperty

    override val installationDirectory: DirectoryProperty = rootYarn.installationDirectory

    override val version: Property<String> = rootYarn.versionProperty

    override val command: Property<String> = rootYarn.commandProperty

    internal val platform: Property<Platform> = rootYarn.platform

    val ignoreScripts: Property<Boolean> = project.objects.property<Boolean>()
        .convention(project.objects.providerWithLazyConvention { rootYarn.ignoreScripts })

    val yarnLockMismatchReport: Property<YarnLockMismatchReport> = project.objects.property<YarnLockMismatchReport>()
        .convention(project.objects.providerWithLazyConvention { rootYarn.yarnLockMismatchReport })

    val reportNewYarnLock: Property<Boolean> = project.objects.property<Boolean>()
        .convention(project.objects.providerWithLazyConvention { rootYarn.reportNewYarnLock })

    val yarnLockAutoReplace: Property<Boolean> = project.objects.property<Boolean>()
        .convention(project.objects.providerWithLazyConvention { rootYarn.yarnLockAutoReplace })

    val resolutions: ListProperty<YarnResolution> = project.objects.listProperty<YarnResolution>()
        .convention(
            project.objects.listProperty<YarnResolution>().value(
                project.objects.providerWithLazyConvention {
                    rootYarn.resolutions
                }
            )
        )

    override fun produceEnv(): Provider<YarnEnv> {
        return download.flatMap { downloadValue ->
            installationDirectory.flatMap { installationDirectoryValue ->
                version.flatMap { versionValue ->
                    command.flatMap { commandValue ->
                        platform.flatMap { platformValue ->
                            ignoreScripts.flatMap { ignoreScriptsValue ->
                                yarnLockMismatchReport.flatMap { yarnLockMismatchReportValue ->
                                    reportNewYarnLock.flatMap { reportNewYarnLockValue ->
                                        yarnLockAutoReplace.flatMap { yarnLockAutoReplaceValue ->
                                            resolutions.map { resolutionsValue ->
                                                val cleanableStore = CleanableStore[installationDirectoryValue.asFile.path]

                                                val isWindows = platformValue.isWindows()

                                                val home = cleanableStore["yarn-v${versionValue}"].use()

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
                                                    executable = getExecutable("yarn", commandValue, "cmd"),
                                                    ivyDependency = "com.yarnpkg:yarn:${versionValue}@tar.gz",
                                                    ignoreScripts = ignoreScriptsValue,
                                                    yarnLockMismatchReport = yarnLockMismatchReportValue,
                                                    reportNewYarnLock = reportNewYarnLockValue,
                                                    yarnLockAutoReplace = yarnLockAutoReplaceValue,
                                                    yarnResolutions = resolutionsValue
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val YARN: String = "kotlinYarnSpec"
    }

}