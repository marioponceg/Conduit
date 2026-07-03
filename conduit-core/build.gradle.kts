plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    alias(libs.plugins.dokka)
}

kotlin {
    explicitApi()
    jvmToolchain(21)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
}

kover {
    reports {
        verify {
            rule {
                minBound(90)
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    detektPlugins(libs.detekt.formatting)
    testImplementation(libs.kotlin.test.junit5)
}
