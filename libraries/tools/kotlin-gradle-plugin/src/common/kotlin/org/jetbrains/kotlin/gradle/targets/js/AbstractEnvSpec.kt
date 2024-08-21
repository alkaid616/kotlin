package org.jetbrains.kotlin.gradle.targets.js

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider

interface EnvSpec<T> {

    val download: org.gradle.api.provider.Property<Boolean>

    val downloadBaseUrl: org.gradle.api.provider.Property<String>

    val installationDirectory: DirectoryProperty

    val version: org.gradle.api.provider.Property<String>

    val command: org.gradle.api.provider.Property<String>

    fun produceEnv(): Provider<T>
}
