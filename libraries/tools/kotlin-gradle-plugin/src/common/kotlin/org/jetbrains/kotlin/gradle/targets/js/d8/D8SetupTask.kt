/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.d8

import org.gradle.api.tasks.Internal
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.targets.js.AbstractSetupTask
import org.jetbrains.kotlin.gradle.targets.js.d8.D8Plugin.Companion.kotlinD8Extension
import org.jetbrains.kotlin.gradle.utils.getFile
import java.io.File
import javax.inject.Inject

@DisableCachingByDefault
abstract class D8SetupTask @Inject constructor(
    settings: D8Extension,
) : AbstractSetupTask<D8Env, D8Extension>(settings) {

    @get:Internal
    override val artifactPattern: String
        get() = "[artifact]-[revision].[ext]"

    @get:Internal
    override val artifactModule: String
        get() = "google.d8"

    @get:Internal
    override val artifactName: String
        get() = "v8"

    private val isWindows = env.map { it.isWindows }

    private val executable = env.map { it.executable }

    override fun extract(archive: File) {
        fs.copy {
            it.from(archiveOperations.zipTree(archive))
            it.into(destinationProvider.getFile())
        }

        if (!isWindows.get()) {
            File(executable.get()).setExecutable(true)
        }
    }

    companion object {
        const val NAME: String = "kotlinD8Setup"
    }
}
