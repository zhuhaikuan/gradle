import gradlebuild.performance.tasks.PerformanceTest
import gradlebuild.performance.generator.tasks.JvmProjectGeneratorTask

/*
 * Copyright 2016 the original author or authors.
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
    gradlebuild.internal.java
}

dependencies {
    testFixturesApi(project(":internalPerformanceTesting"))
    testFixturesApi(library("commons_io"))
    testFixturesApi(project(":baseServices"))
    testFixturesImplementation(project(":internalTesting"))
    testFixturesImplementation(project(":internalIntegTesting"))

    performanceTestDistributionRuntimeOnly(project(":distributionsFull")) {
        because("so that all Gradle features are available")
    }
}

val generateTemplate = tasks.register<JvmProjectGeneratorTask>("javaProject") {
    dependencyGraph.run {
        size = 200
        depth = 5
        useSnapshotVersions = false // snapshots should not have a build scan specific performance impact
    }

    buildSrcTemplate = "buildsrc-plugins"
    setProjects(50)
    sourceFiles = 200
    testSourceFiles = 50 // verbose tests are time consuming
    filesPerPackage = 5
    linesOfCodePerSourceFile = 150
    numberOfScriptPlugins = 30
    rootProjectTemplates = listOf("root")
    subProjectTemplates = listOf("project-with-source")
    templateArgs = mapOf(
        "fullTestLogging" to true,
        "failedTests" to true,
        "projectDependencies" to true,
        "manyPlugins" to true,
        "manyScripts" to true
    )
}

tasks.withType<PerformanceTest>().configureEach {
    dependsOn(generateTemplate)
    systemProperties["incomingArtifactDir"] = "$rootDir/incoming/"

    environment("ARTIFACTORY_USERNAME", System.getenv("ARTIFACTORY_USERNAME"))
    environment("ARTIFACTORY_PASSWORD", System.getenv("ARTIFACTORY_PASSWORD"))
}
