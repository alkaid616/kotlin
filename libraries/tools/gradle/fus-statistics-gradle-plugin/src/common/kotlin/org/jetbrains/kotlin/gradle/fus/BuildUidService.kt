/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.fus

import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.util.UUID.randomUUID
import java.util.concurrent.locks.ReentrantLock

abstract class BuildUidService : BuildService<BuildServiceParameters.None>, AutoCloseable {
    val buildId = randomUUID().toString()
    private val log = Logging.getLogger(this.javaClass)

    init {
        log.info("Build $buildId is started")
    }

    companion object {
        private val serviceName = "${BuildUidService::class.java.canonicalName}_${BuildUidService::class.java.classLoader.hashCode()}"

        fun registerIfAbsent(project: Project): Provider<BuildUidService> {
            return project.gradle.sharedServices.registerIfAbsent(serviceName, BuildUidService::class.java) {
            }
        }
    }

    override fun close() {
        log.info("Build $buildId is closed")
    }
}