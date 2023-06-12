plugins {
    kotlin("jvm") version "1.8.21"
    application
}

group = "lobaevni.graduate"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val multikVersion = "0.2.1"

dependencies {
    implementation("org.slf4j:slf4j-api:1.7.5")
    implementation("org.slf4j:slf4j-simple:1.7.5")
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
