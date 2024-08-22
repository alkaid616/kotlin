package org.jetbrains.kotlin.gradle.targets.js

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory

abstract class EnvSpec<T> {

    abstract val download: org.gradle.api.provider.Property<Boolean>

    abstract val downloadBaseUrl: org.gradle.api.provider.Property<String>

    abstract val installationDirectory: DirectoryProperty

    abstract val version: org.gradle.api.provider.Property<String>

    abstract val command: org.gradle.api.provider.Property<String>

    internal abstract fun produceEnv(providerFactory: ProviderFactory): Provider<T>
}
