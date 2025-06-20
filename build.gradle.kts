plugins {
    kotlin("jvm") version "2.1.0"
}

group = "kioskware.kforms"
version = "v0.2.2-alpha"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}
