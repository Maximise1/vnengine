plugins {
    kotlin("jvm") version "2.3.0"
    //kotlin("plugin.serialization") version "2.3.0"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    // Hash computation
    //implementation("org.lz4:lz4-java:1.8.0")

    // Serialization
    //implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    //implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.9.0")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("MainKt")
}
