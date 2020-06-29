/*
 * A set of generic services and utilities.
 *
 * Should have a very small set of dependencies, and should be appropriate to embed in an external
 * application (eg as part of the tooling API).
 */

plugins {
    gradlebuild.distribution.`api-java`
}

gradlebuildJava.usedInWorkers()

dependencies {
    api(project(":baseAnnotations"))
    api(project(":hashing"))

    implementation(library("slf4j_api"))
    implementation(library("guava"))
    implementation(library("commons_lang"))
    implementation(library("commons_io"))
    implementation(library("asm"))

    integTestImplementation(project(":logging"))

    testFixturesImplementation(library("guava"))
    testImplementation(testFixtures(project(":core")))

    integTestDistributionRuntimeOnly(project(":distributionsCore"))

    jmh("org.bouncycastle:bcprov-jdk15on:1.61")
    jmh("com.google.guava:guava:27.1-android")
}

jmh.include = listOf("HashingAlgorithmsBenchmark")

moduleIdentity.createBuildReceipt()
