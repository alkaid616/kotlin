/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.HasConfigurableKotlinCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptionsDefault
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetComponent
import org.jetbrains.kotlin.gradle.targets.metadata.KotlinMetadataTargetConfigurator
import org.jetbrains.kotlin.gradle.utils.newInstance
import javax.inject.Inject

abstract class KotlinMetadataTarget @Inject constructor(
    project: Project,
) : KotlinOnlyTarget<KotlinCompilation<*>>(project, KotlinPlatformType.common),
    HasConfigurableKotlinCompilerOptions<KotlinCommonCompilerOptions> {

    override val artifactsTaskName: String
        get() = KotlinMetadataTargetConfigurator.ALL_METADATA_JAR_NAME

    override val kotlinComponents: Set<KotlinTargetComponent> by lazy {
        /*
        Metadata Target does not have a KotlinTargetComponent on it's own.
        Responsibility is shifted to the root KotlinSoftwareComponent
        */
        emptySet()
    }

    @ExperimentalKotlinGradlePluginApi
    override val compilerOptions: KotlinCommonCompilerOptions = project.objects
        .newInstance<KotlinCommonCompilerOptionsDefault>()

    companion object {
        const val METADATA_TARGET_NAME = "metadata"
    }
}

