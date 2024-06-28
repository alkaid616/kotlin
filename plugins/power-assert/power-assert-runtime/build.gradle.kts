description = "Runtime library for the Power-Assert compiler plugin"

plugins {
    kotlin("multiplatform")
    id("jps-compatible")
}

kotlin {
    jvm()
    js {
        binaries.executable()
    }

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlinStdlib())
            }
        }
        val jvmMain by getting {
        }

        val jsMain by getting {
        }
    }
}
