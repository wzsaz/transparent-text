plugins {
    kotlin("jvm") version "1.9.20"
    application
}

group = "waer"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

application {
    mainClass.set("waer.MainKt")
}

kotlin {
    jvmToolchain(11)
}
