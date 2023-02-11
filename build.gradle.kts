plugins {
    kotlin("jvm") version "1.8.0"
    application
}

group = "lobaevni.graduate"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("io.github.rchowell:dotlin:1.0.2")
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