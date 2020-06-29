/*
 * Copyright 2018 the original author or authors.
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

import org.gradle.gradlebuild.testing.integrationtests.cleanup.WhenNotEmpty

plugins {
    gradlebuild.distribution.`implementation-java`
}

dependencies {
    implementation(project(":baseServices"))
    implementation(project(":messaging"))
    implementation(project(":native"))
    implementation(project(":logging"))
    implementation(project(":files"))
    implementation(project(":fileCollections"))
    implementation(project(":persistentCache"))
    implementation(project(":coreApi"))
    implementation(project(":modelCore"))
    implementation(project(":baseServicesGroovy"))
    implementation(project(":buildCache"))
    implementation(project(":core"))
    implementation(project(":resources"))
    implementation(project(":resourcesHttp"))
    implementation(project(":snapshots"))
    implementation(project(":execution"))
    implementation(project(":security"))

    implementation(library("slf4j_api"))
    implementation(library("groovy"))
    implementation(library("asm"))
    implementation(library("asm_commons"))
    implementation(library("asm_util"))
    implementation(library("guava"))
    implementation(library("commons_lang"))
    implementation(library("commons_io"))
    implementation(library("commons_httpclient"))
    implementation(library("inject"))
    implementation(library("gson"))
    implementation(library("ant"))
    implementation(library("ivy"))
    implementation(library("maven3"))

    testImplementation(project(":processServices"))
    testImplementation(project(":diagnostics"))
    testImplementation(project(":buildCachePackaging"))
    testImplementation(library("nekohtml"))
    testImplementation(testFixtures(project(":core")))
    testImplementation(testFixtures(project(":messaging")))
    testImplementation(testFixtures(project(":coreApi")))
    testImplementation(testFixtures(project(":versionControl")))
    testImplementation(testFixtures(project(":resourcesHttp")))
    testImplementation(testFixtures(project(":baseServices")))
    testImplementation(testFixtures(project(":snapshots")))
    testImplementation(testFixtures(project(":execution")))

    integTestImplementation(project(":buildOption"))
    integTestImplementation(library("jansi"))
    integTestImplementation(library("ansi_control_sequence_util"))
    integTestImplementation(testLibrary("jetty")) {
        because("tests use HttpServlet directly")
    }
    integTestImplementation(testFixtures(project(":security")))

    testFixturesApi(project(":baseServices")) {
        because("Test fixtures export the Action class")
    }
    testFixturesApi(project(":persistentCache")) {
        because("Test fixtures export the CacheAccess class")
    }

    testFixturesApi(testLibrary("jetty"))
    testFixturesImplementation(project(":core"))
    testFixturesImplementation(testFixtures(project(":core")))
    testFixturesImplementation(testFixtures(project(":resourcesHttp")))
    testFixturesImplementation(project(":coreApi"))
    testFixturesImplementation(project(":messaging"))
    testFixturesImplementation(project(":internalIntegTesting"))
    testFixturesImplementation(library("slf4j_api"))
    testFixturesImplementation(library("inject"))
    testFixturesImplementation(library("guava")) {
        because("Groovy compiler reflects on private field on TextUtil")
    }
    testFixturesImplementation(library("bouncycastle_pgp"))
    testFixturesApi(testLibrary("testcontainers_spock")) {
        because("API because of Groovy compiler bug leaking internals")
    }
    testFixturesImplementation(project(":jvmServices")) {
        because("Groovy compiler bug leaks internals")
    }

    testRuntimeOnly(project(":distributionsCore")) {
        because("ProjectBuilder tests load services from a Gradle distribution.")
    }
    integTestDistributionRuntimeOnly(project(":distributionsBasics"))
    crossVersionTestDistributionRuntimeOnly(project(":distributionsCore"))
}

classycle {
    excludePatterns.set(listOf("org/gradle/**"))
}

testFilesCleanup {
    policy.set(WhenNotEmpty.REPORT)
}

tasks.clean {
    val testFiles = layout.buildDirectory.dir("tmp/test files")
    doFirst {
        // On daemon crash, read-only cache tests can leave read-only files around.
        // clean now takes care of those files as well
        testFiles.get().asFileTree.matching {
            include("**/read-only-cache/**")
        }.visit { this.file.setWritable(true) }
    }
}

afterEvaluate {
    // This is a workaround for the validate plugins task trying to inspect classes which
    // have changed but are NOT tasks
    tasks.withType<ValidatePlugins>().configureEach {
        val main = sourceSets.main.get()
        classes.setFrom(main.output.classesDirs.asFileTree.filter { !it.isInternal(main) })
    }
}

fun File.isInternal(sourceSet: SourceSet) = isInternal(sourceSet.output.classesDirs.files)

fun File.isInternal(roots: Set<File>): Boolean = name == "internal" ||
    !roots.contains(parentFile) && parentFile.isInternal(roots)
