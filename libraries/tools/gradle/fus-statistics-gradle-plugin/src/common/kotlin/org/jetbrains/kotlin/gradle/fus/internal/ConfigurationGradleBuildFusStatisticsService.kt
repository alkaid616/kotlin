/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.fus.internal

import org.gradle.api.provider.ListProperty
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.gradle.fus.GradleBuildFusStatisticsService
import org.jetbrains.kotlin.gradle.fus.Metric
import org.jetbrains.kotlin.gradle.fus.UniqueId

abstract class ConfigurationGradleBuildFusStatisticsService :
    GradleBuildFusStatisticsService<BuildServiceParameters.None> {
    val configurationMetrics = ArrayList<Metric>()

    override fun reportMetric(name: String, value: String, uniqueId: UniqueId) {
        configurationMetrics.add(Metric(name, value, uniqueId))
    }

    override fun reportMetric(name: String, value: Number, uniqueId: UniqueId) {
        configurationMetrics.add(Metric(name, value, uniqueId))
    }

    override fun reportMetric(name: String, value: Boolean, uniqueId: UniqueId) {
        configurationMetrics.add(Metric(name, value, uniqueId))
    }

    override fun close() {
    }
}