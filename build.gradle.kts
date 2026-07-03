plugins {
    // Loaded (not applied) at the root so plugin classes live in one classloader shared by
    // build-logic's convention plugins and root-level plugins that react to them (e.g. BCV).
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.binary.compatibility.validator)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
}

dependencies {
    detektPlugins(libs.detekt.formatting)
}
