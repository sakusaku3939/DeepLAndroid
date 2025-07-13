plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.shot) apply false
}

buildscript {
    dependencies {
        classpath(libs.shot.classpath)
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}