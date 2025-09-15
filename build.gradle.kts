plugins {
    kotlin("jvm") version "2.2.0"
    application
}

group = "waer.dev"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(23)
}

application {
    // Main class for the demo application
    mainClass.set("waer.dev.MainKt")
}

// Ensure the 'run' task has the main class explicitly set (compatibility with older Gradle wrappers)
tasks.named("run") {
    doFirst {
        // No-op: this ensures the task is realized and picks up mainClass from the application plugin
    }
}

// Provide an explicit JavaExec task as a reliable run entrypoint
tasks.register<JavaExec>("runApp") {
    group = "application"
    description = "Run the waer.dev.MainKt main class using the project's runtime classpath"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("waer.dev.MainKt")
}
