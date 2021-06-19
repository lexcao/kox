plugins {
    application
    kotlin("jvm") version "1.5.10"
}

group = "io.lexcao"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("io.lexcao.kox.MainKt")
    applicationName = "kox"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))

    testImplementation(platform("org.junit:junit-bom:5.7.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}
