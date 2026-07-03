import org.gradle.accessors.dm.LibrariesForLibs

/**
 * Convention plugin for Conduit library modules: Kotlin/JVM with explicit API mode, detekt
 * (with formatting rules), Kover with the project-wide minimum coverage rule, Dokka, and the
 * JUnit 5 platform with kotlin-test.
 */
plugins {
    kotlin("jvm")
    id("io.gitlab.arturbosch.detekt")
    id("org.jetbrains.kotlinx.kover")
    id("org.jetbrains.dokka")
}

val libs = the<LibrariesForLibs>()

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

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    detektPlugins(libs.detekt.formatting)
    testImplementation(libs.kotlin.test.junit5)
}
