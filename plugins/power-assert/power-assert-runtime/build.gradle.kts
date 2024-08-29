description = "Runtime library for the Power-Assert compiler plugin"

plugins {
    kotlin("multiplatform")
    id("jps-compatible")
}

kotlin {
    jvm()

    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlinStdlib())
            }
        }
    }
}
