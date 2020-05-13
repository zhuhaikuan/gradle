plugins {
    kotlin("jvm") version "1.4-M1"
}

version = "1.0.2"
group = "org.gradle.sample"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
}
