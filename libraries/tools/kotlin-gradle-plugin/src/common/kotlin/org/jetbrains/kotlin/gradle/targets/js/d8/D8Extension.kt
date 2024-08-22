/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.d8

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.targets.js.AbstractSettings
import org.jetbrains.kotlin.gradle.utils.property

@ExperimentalWasmDsl
open class D8Extension(
    @Transient val project: Project,
) : AbstractSettings<D8Env>() {

    private val gradleHome = project.gradle.gradleUserHomeDir.also {
        project.logger.kotlinInfo("Storing cached files in $it")
    }

    internal lateinit var d8EnvSpec: () -> D8EnvSpec

    override val downloadProperty: org.gradle.api.provider.Property<Boolean> = project.objects.property<Boolean>()
        .convention(true)

    // value not convention because this property can be nullable to not add repository
    override val downloadBaseUrlProperty: org.gradle.api.provider.Property<String> = project.objects.property<String>()
        .value("https://storage.googleapis.com/chromium-v8/official/canary")

    override val installationDirectory: DirectoryProperty = project.objects.directoryProperty()
        .fileValue(gradleHome.resolve("d8"))

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
    override val versionProperty: org.gradle.api.provider.Property<String> = project.objects.property<String>()
        .convention("11.9.85")

    /**
     * Specify the edition of the D8.
     *
     * Valid options for bundled version are `rel` (release variant) and `dbg` (debug variant).
     */
    val edition: org.gradle.api.provider.Property<String> = project.objects.property<String>()
        .convention("rel")

    override val commandProperty: org.gradle.api.provider.Property<String> = project.objects.property<String>()
        .convention("d8")

    val setupTaskProvider: TaskProvider<D8SetupTask>
        get() = project.tasks.withType(D8SetupTask::class.java).named(D8SetupTask.NAME)

    override fun finalizeConfiguration(): D8Env {
        return d8EnvSpec().produceEnv(project.providers).get()
    }

    companion object {
        const val EXTENSION_NAME: String = "kotlinD8"
    }
}
