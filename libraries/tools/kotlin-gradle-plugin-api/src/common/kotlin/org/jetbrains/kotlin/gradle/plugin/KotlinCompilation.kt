/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("TYPEALIAS_EXPANSION_DEPRECATION")

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.HasAttributes
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptionsDeprecated
import org.jetbrains.kotlin.gradle.dsl.KotlinCompileDeprecated
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.tooling.core.HasMutableExtras

/**
 * # Kotlin Compilation
 * Represents configuration of a Kotlin Compiler invocation.
 * It is recommended to use [KotlinCompilation] API instead of getting
 * tasks, configurations and other related domain objects via Gradle API directly.
 * For Native targets, [KotlinCompilation] also provides API to configure CInterop
 *
 * [KotlinTarget] contains multiple [KotlinCompilation].
 * By default [KotlinTarget] will contain two [KotlinCompilation] with names "main" and "test".
 *
 * Sample: Use [KotlinCompilation] to configure compilation tasks of JVM target.
 * ```kotlin
 * // build.gradle.kts
 * kotlin {
 *   jvm {
 *     compilations.all {
 *       compileTaskProvider {
 *         // configure compile task here
 *       }
 *     }
 *   }
 * }
 * ```
 *
 * ## Main Compilation
 * [KotlinCompilation] with name "main" represents Kotlin Compiler invocation for main sources of [KotlinTarget].
 * Results of Main Compilation are publishable and exposed via [KotlinTarget.apiElementsConfigurationName] consumable configuration.
 *
 * Sample: Consume outputs of Main JVM Compilation for custom processing
 *
 * ```kotlin
 * // build.gradle.kts
 * val jvmMainClasses = kotlin.jvm().compilations.getByName("main").output.classesDirs
 * tasks.register<Jar>("customJar") {
 *     from(jvmMainClasses)
 * }
 * ```
 *
 * ## Test Compilation
 * [KotlinCompilation] with name "test" represents Kotlin Compiler invocation for test source sets.
 * Test Compilation implicitly [KotlinCompilation.associateWith] Main Compilation (see [KotlinCompilation.associatedCompilations])
 * i.e., it sees all dependencies, internal and public declarations of the Main Compilation.
 *
 * ## Custom Compilation
 * It is possible to create additional compilation under [KotlinTarget].
 *
 * ```kotlin
 * kotlin {
 *   jvm {
 *     compilations.create("customCompilation")
 *   }
 * }
 * ```
 *
 * **NB: ** It is recommended to use a separate Gradle Project instead of creating custom compilation.
 *
 * ## Metadata Target Compilation
 *
 * Kotlin Metadata Target is a special [KotlinTarget] that manages compiler invocations to compile code of shared source sets.
 * There are no "main" or "test" Kotlin Compilations for the Metadata Target, but a dedicated compilation for each shared source set.
 *
 */
@KotlinGradlePluginDsl
interface KotlinCompilation<out T : KotlinCommonOptionsDeprecated> : Named,
    HasProject,
    HasMutableExtras,
    HasAttributes,
    HasKotlinDependencies {

    /**
     * Represents a [KotlinTarget] to which this compilation belongs to.
     */
    val target: KotlinTarget

    /**
     * The name of the compilation.
     */
    val compilationName: String

    /**
     * All [KotlinSourceSet]s used by this compilation.
     *
     * Additional Kotlin source sets could be included either via [source] or [associateWith] methods.
     */
    val kotlinSourceSets: Set<KotlinSourceSet>

    /**
     * All [KotlinSourceSet]s used by this compilation.
     *
     * Additional Kotlin source sets could be included either via [source] or [associateWith] methods.
     */
    val allKotlinSourceSets: Set<KotlinSourceSet>

    /**
     * @suppress
     */
    @Deprecated("Use defaultSourceSet.name instead", ReplaceWith("defaultSourceSet.name"))
    val defaultSourceSetName: String get() = defaultSourceSet.name

    /**
     * [KotlinSourceSet] by default associated with this compilation.
     */
    val defaultSourceSet: KotlinSourceSet

    /**
     * Configures the [defaultSourceSet] provided configuration.
     */
    fun defaultSourceSet(configure: KotlinSourceSet.() -> Unit)

    /**
     * Configures the [defaultSourceSet] provided configuration.
     */
    fun defaultSourceSet(configure: Action<KotlinSourceSet>) = defaultSourceSet { configure.execute(this) }

    /**
     * The name of the Gradle configuration containing all resolved dependencies required for compilation.
     *
     * One of the possible names of such configuration is 'compileClasspath' for Kotlin JVM target.
     */
    val compileDependencyConfigurationName: String

    /**
     * A collection of compilation dependencies artifacts file system locations.
     */
    var compileDependencyFiles: FileCollection

    /**
     * The name of the Gradle configuration containing all resolved dependencies required to run compilation output.
     *
     * One of the possible names of such configuration is 'runtimeClasspath' for Kotlin JVM target.
     */
    val runtimeDependencyConfigurationName: String?

    /**
     * A collection of runtime dependencies artifacts file system locations.
     */
    val runtimeDependencyFiles: FileCollection?

    /**
     * Represents the output of a Kotlin compilation.
     */
    val output: KotlinCompilationOutput

    /**
     * Represents [KotlinPlatformType] to which this compilation belongs.
     *
     * Always the same as in the [target].
     */
    val platformType get() = target.platformType

    /**
     * Kotlin Gradle plugin task name which is used to run the compilation process for this Kotlin compilation.
     */
    val compileKotlinTaskName: String

    /**
     * Provides access to the compilation task for this compilation.
     */
    val compileTaskProvider: TaskProvider<out KotlinCompilationTask<*>>

    /**
     * @suppress
     */
    @Deprecated(
        "To configure compilation compiler options use 'compileTaskProvider':\ncompilation.compileTaskProvider.configure{\n" +
                "    compilerOptions {}\n}"
    )
    @Suppress("DEPRECATION")
    val compilerOptions: HasCompilerOptions<*>

    /**
     * @suppress
     */
    @Deprecated(
        message = "Accessing task instance directly is deprecated",
        replaceWith = ReplaceWith("compileTaskProvider")
    )
    val compileKotlinTask: KotlinCompileDeprecated<T>

    /**
     * @suppress
     */
    @Deprecated(
        message = "Replaced with compileTaskProvider",
        replaceWith = ReplaceWith("compileTaskProvider")
    )
    val compileKotlinTaskProvider: TaskProvider<out KotlinCompileDeprecated<T>>

    /**
     * @suppress
     */
    @OptIn(InternalKotlinGradlePluginApi::class)
    @Deprecated(
        message = KOTLIN_OPTIONS_DEPRECATION_MESSAGE
    )
    val kotlinOptions: T

    /**
     * @suppress
     */
    @OptIn(InternalKotlinGradlePluginApi::class)
    @Deprecated(
        message = KOTLIN_OPTIONS_DEPRECATION_MESSAGE
    )
    fun kotlinOptions(configure: T.() -> Unit) {
        @Suppress("DEPRECATION")
        configure(kotlinOptions)
    }

    /**
     * @suppress
     */
    @OptIn(InternalKotlinGradlePluginApi::class)
    @Deprecated(
        message = KOTLIN_OPTIONS_DEPRECATION_MESSAGE
    )
    fun kotlinOptions(configure: Action<@UnsafeVariance T>) {
        @Suppress("DEPRECATION")
        configure.execute(kotlinOptions)
    }

    /**
     * Configures the compilation [AttributeContainer] with provided configuration.
     */
    fun attributes(configure: AttributeContainer.() -> Unit) = attributes.configure()

    /**
     * Configures the compilation [AttributeContainer] with provided configuration.
     */
    fun attributes(configure: Action<AttributeContainer>) = attributes { configure.execute(this) }

    /**
     * Gradle task name which is used to be a meta task triggering all required for this [KotlinCompilation] compilation tasks.
     *
     * For example, in only JVM project it would be "classes" task which depends on "compileKotlin" and "compileJava" tasks.
     */
    val compileAllTaskName: String

    /**
     * Constants for [KotlinCompilation].
     */
    companion object {
        /**
         * Default main compilation name.
         */
        const val MAIN_COMPILATION_NAME = "main"

        /**
         * Default test compilation name.
         */
        const val TEST_COMPILATION_NAME = "test"
    }

    /**
     * Will add a [KotlinSourceSet] directly into this compilation.
     * This method is deprecated and soon to be removed.
     *
     * After this method removal, there will be exactly one SourceSet associated with a given Kotlin Compilation.
     *
     * To include other sources in the compilation, please build a hierarchy of Source Sets instead.
     * See: [KotlinSourceSet.dependsOn] or [KotlinTargetHierarchyDsl].
     * This approach is most applicable if
     * - The sources can be shared for multiple compilations
     * - The sources shall be analyzed in a different context than [defaultSourceSet]
     * - The project uses multiplatform and sources shall provide expects
     *
     * Alternatively, when just including source files from another directory,
     * the [SourceDirectorySet] from the [defaultSourceSet] can be used.
     * This approach is most applicable if
     *  - sources are not intended to be shared across multiple compilations
     *  - sources shall be analyzed in the same context as other sources in the [defaultSourceSet]
     *
     * Example 1: Create a new 'utils' source set and make it available to the 'main' compilation:
     * ```kotlin
     * kotlin {
     *     val compilation = target.compilations.getByName("main")
     *     val utilsSourceSet = sourceSets.create("utils")
     *     compilation.defaultSourceSet.dependsOn(utilsSourceSet)
     * }
     * ```
     *
     * Example 2: Add 'src/utils/kotlin' to the main SourceSet
     * ```kotlin
     * kotlin {
     *     val compilation = target.compilations.getByName("main")
     *     compilation.defaultSourceSet.kotlin.srcDir("src/utils/kotlin")
     * }
     * ```
     * Further details:
     * https://kotl.in/compilation-source-deprecation
     */
    @Deprecated("scheduled for removal with Kotlin 2.1")
    fun source(sourceSet: KotlinSourceSet)

    /**
     * Associates the current KotlinCompilation with another KotlinCompilation.
     *
     * After this compilation will:
     * - use the output of the [other] compilation as compile & runtime dependency
     * - add all 'declared dependencies' present on [other] compilation
     * - see all internal declarations of [other] compilation
     */
    fun associateWith(other: KotlinCompilation<*>)

    /**
     * @suppress
     */
    @Deprecated("Use 'associatedCompilations' instead", ReplaceWith("associatedCompilations.toList()"))
    val associateWith: List<KotlinCompilation<*>> get() = associatedCompilations.toList()

    /**
     * All compilations previously associated using [associateWith].
     *
     * E.g. 'test' compilations will return 'setOf(main)' by default.
     *
     * @since 1.9.20
     */
    val associatedCompilations: Set<KotlinCompilation<*>>

    /**
     * Full transitive closure of [associatedCompilations].
     *
     * @since 1.9.20
     */
    @ExperimentalKotlinGradlePluginApi
    val allAssociatedCompilations: Set<KotlinCompilation<*>>

    /**
     * The object's name.
     *
     * Must be constant for the life of the object.
     */
    override fun getName(): String = compilationName

    /**
     * A unique name for this compilation in the whole project.
     *
     * [KotlinTarget]s may have [KotlinCompilation]s which have the same [compilationName] in different targets (e.g [MAIN_COMPILATION_NAME]).
     * This property provides a unique name for compilation based on the target name, which allows distinguishing them in the whole project.
     */
    val disambiguatedName
        get() = target.disambiguationClassifier + name
}

/**
 * @suppress
 */
@Deprecated("Scheduled for removal with Kotlin 2.1")
interface KotlinCompilationToRunnableFiles<T : KotlinCommonOptionsDeprecated> : KotlinCompilation<T> {
    override val runtimeDependencyConfigurationName: String

    override var runtimeDependencyFiles: FileCollection
}

/**
 * @suppress
 */
@Suppress("Deprecation")
typealias DeprecatedKotlinCompilationToRunnableFiles<T> = KotlinCompilationToRunnableFiles<T>

/**
 * @suppress
 */
@Deprecated("Scheduled for removal with Kotlin 2.1")
@Suppress("EXTENSION_SHADOWED_BY_MEMBER", "deprecation") // kept for compatibility
val <T : KotlinCommonOptionsDeprecated> KotlinCompilation<T>.runtimeDependencyConfigurationName: String?
    get() = (this as? KotlinCompilationToRunnableFiles<T>)?.runtimeDependencyConfigurationName

/**
 * @suppress
 */
@Deprecated("Scheduled for removal with Kotlin 2.1")
interface KotlinCompilationWithResources<T : KotlinCommonOptionsDeprecated> : KotlinCompilation<T> {
    val processResourcesTaskName: String
}

/**
 * @suppress
 */
@Suppress("Deprecation")
typealias DeprecatedKotlinCompilationWithResources<T> = KotlinCompilationWithResources<T>
