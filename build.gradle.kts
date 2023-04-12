plugins {
    kotlin("jvm") version "1.8.10"
    application
}

group = "lobaevni.graduate"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val multikVersion = "0.2.1"

dependencies {
    implementation("io.github.rchowell:dotlin:1.0.2")
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")
    implementation("org.jetbrains.kotlinx:multik-core:$multikVersion")
    implementation("org.jetbrains.kotlinx:multik-default:$multikVersion")
    implementation("org.jetbrains.kotlinx:multik-kotlin:$multikVersion")
    implementation("org.jetbrains.kotlinx:multik-openblas:$multikVersion")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}

application {
    mainClass.set("MainKt")
}
