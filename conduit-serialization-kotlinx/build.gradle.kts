plugins {
    id("conduit.library")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    // api: consumers declaring only this artifact get conduit-core's types transitively,
    // and Json is part of this module's public API (constructor parameter).
    api(project(":conduit-core"))
    api(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlinx.coroutines.test)
}
