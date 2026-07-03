plugins {
    id("conduit.library")
}

dependencies {
    // api: consumers declaring only this artifact get conduit-core's types transitively.
    api(project(":conduit-core"))
    implementation(libs.okhttp)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
}
