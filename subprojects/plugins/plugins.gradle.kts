import gradlebuild.cleanup.WhenNotEmpty
import org.gradle.gradlebuild.test.integrationtests.integrationTestUsesSampleDir

/*
 * Copyright 2010 the original author or authors.
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
plugins {
    gradlebuild.distribution.`api-java`
}

dependencies {
    implementation(project(":baseServices"))
    implementation(project(":logging"))
    implementation(project(":processServices"))
    implementation(project(":fileCollections"))
    implementation(project(":persistentCache"))
    implementation(project(":coreApi"))
    implementation(project(":modelCore"))
    implementation(project(":core"))
    implementation(project(":workers"))
    implementation(project(":dependencyManagement"))
    implementation(project(":reporting"))
    implementation(project(":platformBase"))
    implementation(project(":platformJvm"))
    implementation(project(":languageJvm"))
    implementation(project(":languageJava"))
    implementation(project(":languageGroovy"))
    implementation(project(":diagnostics"))
    implementation(project(":testingBase"))
    implementation(project(":testingJvm"))
    implementation(project(":snapshots"))

    implementation(library("slf4j_api"))
    implementation(library("groovy"))
    implementation(library("ant"))
    implementation(library("asm"))
    implementation(library("guava"))
    implementation(library("commons_io"))
    implementation(library("commons_lang"))
    implementation(library("inject"))

    testImplementation(project(":messaging"))
    testImplementation(project(":native"))
    testImplementation(project(":resources"))
    testImplementation(library("gson")) {
        because("for unknown reason (bug in the Groovy/Spock compiler?) requires it to be present to use the Gradle Module Metadata test fixtures")
    }
    testImplementation(testLibrary("jsoup"))
    testImplementation(testFixtures(project(":core")))
    testImplementation(testFixtures(project(":dependencyManagement")))
    testImplementation(testFixtures(project(":resourcesHttp")))
    testImplementation(testFixtures(project(":platformNative")))
    testImplementation(testFixtures(project(":languageJvm")))
    testImplementation(testFixtures(project(":languageJava")))
    testImplementation(testFixtures(project(":languageGroovy")))
    testImplementation(testFixtures(project(":diagnostics")))

    testFixturesImplementation(testFixtures(project(":core")))
    testFixturesImplementation(project(":baseServicesGroovy"))
    testFixturesImplementation(project(":fileCollections"))
    testFixturesImplementation(project(":languageJvm"))
    testFixturesImplementation(project(":internalIntegTesting"))
    testFixturesImplementation(project(":processServices"))
    testFixturesImplementation(project(":resources"))
    testFixturesImplementation(library("guava"))

    testRuntimeOnly(project(":distributionsCore")) {
        because("ProjectBuilder tests load services from a Gradle distribution.")
    }
    integTestDistributionRuntimeOnly(project(":distributionsJvm"))
}

strictCompile {
    ignoreRawTypes() // raw types used in public API
    ignoreDeprecations() // uses deprecated software model types
}

classycle {
    excludePatterns.set(listOf("org/gradle/**"))
}

testFilesCleanup {
    policy.set(WhenNotEmpty.REPORT)
}

integrationTestUsesSampleDir("subprojects/plugins/src/main")
