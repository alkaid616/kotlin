import gradle.GradlePluginVariant
import org.jetbrains.kotlin.gradle.internal.builtins.StandardNames.FqNames.target

plugins {
    id("gradle-plugin-common-configuration")
    `jvm-test-suite`
    id("gradle-plugin-api-reference")
}

dependencies {
    commonApi(platform(project(":kotlin-gradle-plugins-bom")))
    commonApi(project(":kotlin-gradle-plugin-model"))
    commonApi(project(":kotlin-gradle-plugin"))
}

gradlePlugin {
    plugins {
        create("kotlinComposeCompilerPlugin") {
            id = "org.jetbrains.kotlin.plugin.compose"
            displayName = "Compose Compiler Gradle plugin"
            description = displayName
            implementationClass = "org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradleSubplugin"
        }
    }
}

pluginApiReference {
    enableForGradlePluginVariants(GradlePluginVariant.values().toSet())
    enableKotlinlangDocumentation()

    failOnWarning = true

    additionalDokkaConfiguration {
        reportUndocumented.set(true)
        perPackageOption {
            matchingRegex.set("org\\.jetbrains\\.kotlin\\.compose\\.compiler\\.gradle\\.model(\$|\\.).*")
            suppress.set(true)
        }
    }
}

if (!kotlinBuildProperties.isInJpsBuildIdeaSync) {
    testing {
        suites {
            val test by getting(JvmTestSuite::class) {
                useJUnitJupiter(libs.versions.junit5)
            }

            register<JvmTestSuite>("functionalTest") {
                dependencies {
                    implementation(project())
                    implementation(gradleKotlinDsl())
                    implementation(platform(libs.junit.bom))
                    implementation(libs.junit.jupiter.api)
                    implementation(project(":kotlin-test"))

                    runtimeOnly(libs.junit.jupiter.engine)
                }

                // For internal symbols visibility
                val functionalTestCompilation = kotlin.target.compilations.getByName("functionalTest")
                functionalTestCompilation.associateWith(kotlin.target.compilations.getByName("common"))
                functionalTestCompilation.associateWith(kotlin.target.compilations.getByName(GradlePluginVariant.GRADLE_85.sourceSetName))

                targets {
                    all {
                        testTask.configure {
                            shouldRunAfter(test)
                        }
                    }
                }
            }
        }
    }

    tasks.named("check") {
        dependsOn(testing.suites.named("functionalTest"))
    }
}
