/*
 * Copyright 2011 the original author or authors.
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
import gradlebuild.basics.util.ReproduciblePropertiesWriter
import java.util.Properties

plugins {
    gradlebuild.internal.java
}

dependencies {
    implementation(project(":baseServices"))
    implementation(project(":messaging"))
    implementation(project(":native"))
    implementation(project(":logging"))
    implementation(project(":cli"))
    implementation(project(":processServices"))
    implementation(project(":coreApi"))
    implementation(project(":modelCore"))
    implementation(project(":baseServicesGroovy"))
    implementation(project(":files"))
    implementation(project(":fileCollections"))
    implementation(project(":resources"))
    implementation(project(":buildCache"))
    implementation(project(":persistentCache"))
    implementation(project(":dependencyManagement"))
    implementation(project(":instantExecution"))
    implementation(project(":jvmServices"))
    implementation(project(":launcher"))
    implementation(project(":internalTesting"))
    implementation(project(":buildEvents"))
    implementation(project(":buildOption"))

    implementation(library("groovy"))
    implementation(library("junit"))
    implementation(testLibrary("spock"))
    implementation(library("nativePlatform"))
    implementation(library("commons_lang"))
    implementation(library("commons_io"))
    implementation(testLibrary("jetty"))
    implementation(testLibrary("littleproxy"))
    implementation(library("gcs"))
    implementation(library("inject"))
    implementation(library("commons_httpclient"))
    implementation(library("joda"))
    implementation(library("jackson_core"))
    implementation(library("jackson_annotations"))
    implementation(library("jackson_databind"))
    implementation(library("ivy"))
    implementation(library("ant"))
    implementation(library("jgit")) {
        because("Some tests require a git reportitory - see AbstractIntegrationSpec.initGitDir(")
    }
    testLibraries("sshd").forEach {
        // we depend on both the platform and the library
        implementation(it)
        implementation(platform(it))
    }
    implementation(library("gson"))
    implementation(library("joda"))
    implementation(library("jsch"))
    implementation(library("jcifs"))
    implementation(library("jansi"))
    implementation(library("ansi_control_sequence_util"))
    implementation("org.apache.mina:mina-core")
    implementation(testLibrary("sampleCheck")) {
        exclude(module = "groovy-all")
        exclude(module = "slf4j-simple")
    }
    implementation(testFixtures(project(":core")))

    testRuntimeOnly(project(":distributionsCore")) {
        because("Tests instantiate DefaultClassLoaderRegistry which requires a 'gradle-plugins.properties' through DefaultPluginModuleRegistry")
    }
    integTestDistributionRuntimeOnly(project(":distributionsCore"))
}

classycle {
    excludePatterns.set(listOf("org/gradle/**"))
}

val prepareVersionsInfo = tasks.register<PrepareVersionsInfo>("prepareVersionsInfo") {
    destFile.set(layout.buildDirectory.file("generated-resources/all-released-versions/all-released-versions.properties"))
    versions.set(moduleIdentity.releasedVersions.map {
        it.allPreviousVersions.joinToString(" ") { it.version }
    })
    mostRecent.set(moduleIdentity.releasedVersions.map { it.mostRecentRelease.version })
    mostRecentSnapshot.set(moduleIdentity.releasedVersions.map { it.mostRecentSnapshot.version })
}

val copyAgpVersionsInfo by tasks.registering(Copy::class) {
    from(rootProject.layout.projectDirectory.file("gradle/dependency-management/agp-versions.properties"))
    into(layout.buildDirectory.dir("generated-resources/agp-versions"))
}

sourceSets.main {
    output.dir(prepareVersionsInfo.map { it.destFile.get().asFile.parentFile })
    output.dir(copyAgpVersionsInfo)
}

@CacheableTask
abstract class PrepareVersionsInfo : DefaultTask() {

    @get:OutputFile
    abstract val destFile: RegularFileProperty

    @get:Input
    abstract val mostRecent: Property<String>

    @get:Input
    abstract val versions: Property<String>

    @get:Input
    abstract val mostRecentSnapshot: Property<String>

    @TaskAction
    fun prepareVersions() {
        val properties = Properties()
        properties["mostRecent"] = mostRecent.get()
        properties["mostRecentSnapshot"] = mostRecentSnapshot.get()
        properties["versions"] = versions.get()
        ReproduciblePropertiesWriter.store(properties, destFile.get().asFile)
    }
}
