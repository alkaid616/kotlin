/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.d8

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.targets.js.EnvSpec
import org.jetbrains.kotlin.gradle.tasks.internal.CleanableStore

open class D8Spec(
    d8: D8Extension,
) : EnvSpec<D8Env> {

    override val download: org.gradle.api.provider.Property<Boolean> = d8.downloadProperty

    // value not convention because this property can be nullable to not add repository
    override val downloadBaseUrl: org.gradle.api.provider.Property<String> = d8.downloadBaseUrlProperty

    override val installationDirectory: DirectoryProperty = d8.installationDirectory

    // Latest version number could be found here https://storage.googleapis.com/chromium-v8/official/canary/v8-linux64-rel-latest.json
    // Bash script/command to check that version specified in `VER` is available for all platforms, just copy-paste and run it in terminal:
    /*
    VER=${"$(curl -s https://storage.googleapis.com/chromium-v8/official/canary/v8-linux64-rel-latest.json)":13:-2}
    echo "VER = $VER"
    echo "=================="
    for p in "mac64" "mac-arm64" "linux32" "linux64" "win32" "win64"; do
        r=$(curl -I -s -o /dev/null -w "%{http_code}" https://storage.googleapis.com/chromium-v8/official/canary/v8-$p-rel-$VER.zip)
        if [ "$r" -eq 200 ]; then
            echo "$p   \t✅";
        else
            echo "$p   \t❌";
        fi;
    done;
    */
    override val version: org.gradle.api.provider.Property<String> = d8.versionProperty

    /**
     * Specify the edition of the D8.
     *
     * Valid options for bundled version are `rel` (release variant) and `dbg` (debug variant).
     */
    val edition: org.gradle.api.provider.Property<String> = d8.edition

    override val command: org.gradle.api.provider.Property<String> = d8.commandProperty

    override fun produceEnv(): Provider<D8Env> {
        return download.flatMap { downloadValue ->
            installationDirectory.flatMap { installationDirectoryValue ->
                version.flatMap { versionValue ->
                    command.flatMap { commandValue ->
                        edition.map { editionValue ->
                            val requiredVersion = "${D8Platform.platform}-${editionValue}-${versionValue}"
                            val requiredVersionName = "v8-$requiredVersion"
                            val cleanableStore = CleanableStore[installationDirectoryValue.asFile.absolutePath]
                            val targetPath = cleanableStore[requiredVersionName].use()
                            val isWindows = D8Platform.name == D8Platform.WIN

                            fun getExecutable(command: String, customCommand: String, windowsExtension: String): String {
                                val finalCommand =
                                    if (isWindows && customCommand == command) "$command.$windowsExtension" else customCommand
                                return if (downloadValue)
                                    targetPath
                                        .resolve(finalCommand)
                                        .absolutePath
                                else
                                    finalCommand
                            }

                            D8Env(
                                download = downloadValue,
                                downloadBaseUrl = downloadBaseUrl.orNull,
                                ivyDependency = "google.d8:v8:$requiredVersion@zip",
                                executable = getExecutable("d8", commandValue, "exe"),
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
        const val EXTENSION_NAME: String = "kotlinD8Spec"
    }
}
