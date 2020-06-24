plugins {
    kotlin("jvm") version "1.4-M3-eap-155"
}

version = "1.0.2"
group = "org.gradle.sample"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
}
