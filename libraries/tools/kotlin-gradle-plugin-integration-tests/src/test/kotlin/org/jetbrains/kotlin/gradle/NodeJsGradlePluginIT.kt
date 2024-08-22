/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName


@MppGradlePluginTests
class NodeJsGradlePluginIT : KGPBaseTest() {
    @DisplayName("Set different Node.js versions in different subprojects")
    @GradleTest
    fun testDifferentVersionInSubprojects(gradleVersion: GradleVersion) {
        project(
            "subprojects-nodejs-setup",
            gradleVersion
        ) {
            build(":app1:jsNodeDevelopmentRun") {
                assertOutputContains("Hello with version: v22.2.0")
            }

            build(":app2:jsNodeDevelopmentRun") {
                assertOutputContains("Hello with version: v22.1.0")
            }
        }
    }
}
