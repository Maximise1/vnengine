plugins {
    kotlin("jvm") version "2.3.0"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    // Serialization
    //implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    //implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.9.0")

    // Lua
    implementation("org.luaj:luaj-jse:3.0.1")

    // html+css+js
    implementation("me.friwi:jcefmaven:141.0.10")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("MainKt")

    applicationDefaultJvmArgs = listOf(
        "-Xmx2G",
        "-Djava.library.path=${System.getProperty("user.home")}/.jcef"
    )
}
