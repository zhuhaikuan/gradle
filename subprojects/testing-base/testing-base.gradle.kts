/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import gradlebuild.integrationtests.integrationTestUsesSampleDir

plugins {
    gradlebuild.distribution.`api-java`
}

gradlebuildJava.usedInWorkers()

dependencies {
    implementation(project(":baseServices"))
    implementation(project(":messaging"))
    implementation(project(":native"))
    implementation(project(":logging"))
    implementation(project(":processServices"))
    implementation(project(":workerProcesses"))
    implementation(project(":coreApi"))
    implementation(project(":modelCore"))
    implementation(project(":core"))
    implementation(project(":baseServicesGroovy"))
    implementation(project(":reporting"))
    implementation(project(":platformBase"))

    implementation(library("slf4j_api"))
    implementation(library("groovy"))
    implementation(library("guava"))
    implementation(library("commons_lang"))
    implementation(library("commons_io"))
    implementation(library("kryo"))
    implementation(library("inject"))
    implementation(library("ant")) // only used for DateUtils

    testImplementation(project(":fileCollections"))
    testImplementation(testFixtures(project(":core")))
    testImplementation(testFixtures(project(":messaging")))
    testImplementation(testFixtures(project(":platformBase")))
    testImplementation(testFixtures(project(":logging")))
    testImplementation(testFixtures(project(":baseServices")))

    testFixturesImplementation(project(":baseServices"))
    testFixturesImplementation(project(":modelCore"))
    testFixturesImplementation(project(":internalIntegTesting"))
    testFixturesImplementation(library("guava"))
    testFixturesImplementation(testLibrary("jsoup"))

    testRuntimeOnly(project(":distributionsCore")) {
        because("ProjectBuilder tests load services from a Gradle distribution.")
    }
    integTestDistributionRuntimeOnly(project(":distributionsCore"))
}

strictCompile {
    ignoreRawTypes() // raw types used in public API (org.gradle.api.tasks.testing.AbstractTestTask)
}

classycle {
    excludePatterns.set(listOf("org/gradle/api/internal/tasks/testing/**"))
}

integrationTestUsesSampleDir("subprojects/testing-base/src/main")
