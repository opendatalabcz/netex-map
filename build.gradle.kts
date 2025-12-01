plugins {
    kotlin("jvm") version "2.0.20"
}

group = "cz.cvut.fit.gaierda1"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.entur:netex-java-model:2.0.15")
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.4")
    implementation("org.glassfish.jaxb:jaxb-runtime:4.0.6")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
