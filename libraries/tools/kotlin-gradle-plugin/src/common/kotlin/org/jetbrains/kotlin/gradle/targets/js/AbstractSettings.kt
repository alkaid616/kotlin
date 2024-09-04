package org.jetbrains.kotlin.gradle.targets.js

import org.gradle.api.file.DirectoryProperty
import org.jetbrains.kotlin.gradle.internal.ConfigurationPhaseAware
import org.jetbrains.kotlin.gradle.utils.getFile
import java.io.File

abstract class AbstractSettings<Env : AbstractEnv> : ConfigurationPhaseAware<Env>() {

    @Deprecated("This property will be removed. Use downloadProperty instead. This will be removed in 2.2")
    var download: Boolean
        get() = downloadProperty.get()
        set(value) {
            downloadProperty.set(value)
        }

    abstract val downloadProperty: org.gradle.api.provider.Property<Boolean>

//    @Deprecated("This property will be removed. Use downloadBaseUrlProperty instead. This will be removed in 2.2")
    var downloadBaseUrl: String?
        get() = downloadBaseUrlProperty.getOrNull()
        set(value) {
            downloadBaseUrlProperty.set(value)
        }

    abstract val downloadBaseUrlProperty: org.gradle.api.provider.Property<String>

    @Deprecated("This property will be removed. Use installationDirectory instead. This will be removed in 2.2")
    var installationDir: File
        get() = installationDirectory.getFile()
        set(value) {
            installationDirectory.fileValue(value)
        }

    abstract val installationDirectory: DirectoryProperty

//    @Deprecated("This property will be removed. Use versionProperty instead. This will be removed in 2.2")
    var version: String
        get() = versionProperty.get()
        set(value) {
            versionProperty.set(value)
        }

    abstract val versionProperty: org.gradle.api.provider.Property<String>

    @Deprecated("This property will be removed. Use commandProperty instead. This will be removed in 2.2")
    var command: String
        get() = commandProperty.get()
        set(value) {
            commandProperty.set(value)
        }

    abstract val commandProperty: org.gradle.api.provider.Property<String>
}
