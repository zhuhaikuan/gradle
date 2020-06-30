import gradlebuild.basics.BuildEnvironment
import gradlebuild.integrationtests.integrationTestUsesSampleDir
import gradlebuild.integrationtests.tasks.IntegrationTest

plugins {
    gradlebuild.distribution.`api-java`
}

val integTestRuntimeResources: Configuration by configurations.creating {
    isCanBeResolved = false
    isCanBeConsumed = false
}
val integTestRuntimeResourcesClasspath: Configuration by configurations.creating {
    extendsFrom(integTestRuntimeResources)
    isCanBeResolved = true
    isCanBeConsumed = false
    attributes {
        // play test apps MUST be found as exploded directory
        attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, Usage.JAVA_RUNTIME))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.objects.named(LibraryElements::class.java, LibraryElements.RESOURCES))
    }
    isTransitive = false
}

dependencies {
    implementation(project(":baseServices"))
    implementation(project(":files"))
    implementation(project(":messaging"))
    implementation(project(":processServices"))
    implementation(project(":logging"))
    implementation(project(":workerProcesses"))
    implementation(project(":coreApi"))
    implementation(project(":modelCore"))
    implementation(project(":core"))
    implementation(project(":fileCollections"))
    implementation(project(":snapshots"))
    implementation(project(":dependencyManagement"))
    implementation(project(":workers"))
    implementation(project(":plugins"))
    implementation(project(":platformBase"))
    implementation(project(":platformJvm"))
    implementation(project(":languageJvm"))
    implementation(project(":languageJava"))
    implementation(project(":languageScala"))
    implementation(project(":testingBase"))
    implementation(project(":testingJvm"))
    implementation(project(":javascript"))
    implementation(project(":diagnostics"))
    implementation(project(":reporting"))

    implementation(library("groovy"))
    implementation(library("slf4j_api"))
    implementation(library("guava"))
    implementation(library("commons_lang"))
    implementation(library("inject"))

    testImplementation(project(":native"))
    testImplementation(project(":resources"))
    testImplementation(project(":baseServicesGroovy"))

    integTestImplementation(library("ant"))
    integTestRuntimeOnly(project(":compositeBuilds"))
    integTestRuntimeOnly(project(":idePlay"))
    integTestRuntimeOnly(project(":testingJunitPlatform"))

    testFixturesApi(project(":platformBase")) {
        because("Test fixtures export the Platform class")
    }
    testFixturesApi(testFixtures(project(":core")))
    testFixturesApi(testFixtures(project(":platformNative")))
    testFixturesApi(testFixtures(project(":languageJvm")))
    testFixturesApi(project(":internalIntegTesting"))
    testFixturesImplementation(project(":processServices"))
    testFixturesImplementation(library("commons_io"))
    testFixturesImplementation(library("commons_httpclient"))
    testFixturesImplementation(library("slf4j_api"))
    testFixturesApi(testFixtures(project(":languageScala")))
    testFixturesApi(testFixtures(project(":languageJava")))

    testImplementation(testFixtures(project(":dependencyManagement")))
    testImplementation(testFixtures(project(":diagnostics")))
    testImplementation(testFixtures(project(":platformBase")))

    integTestDistributionRuntimeOnly(project(":distributionsFull"))

    testRuntimeOnly(project(":distributionsCore")) {
        because("Tests instantiate DefaultClassLoaderRegistry which requires a 'gradle-plugins.properties' through DefaultPluginModuleRegistry")
    }
    integTestRuntimeResources(testFixtures(project(":platformPlay")))
}

strictCompile {
    ignoreRawTypes() // deprecated raw types
    ignoreDeprecations() // uses deprecated software model types
}
val integTestPrepare by tasks.registering(IntegrationTest::class) {
    systemProperties["org.gradle.integtest.executer"] = "embedded"
    if (BuildEnvironment.isCiServer) {
        systemProperties["org.gradle.integtest.multiversion"] = "all"
    }
    include("org/gradle/play/prepare/**")
    maxParallelForks = 1
}

tasks.withType<IntegrationTest>().configureEach {
    if (name != "integTestPrepare") {
        dependsOn(integTestPrepare)
        exclude("org/gradle/play/prepare/**")
        // this is a workaround for which we need a better fix:
        // it sets the platform play test fixtures resources directory in front
        // of the classpath, so that we can find them when executing tests in
        // an exploded format, rather than finding them in the test fixtures jar
        classpath = integTestRuntimeResourcesClasspath + classpath
    }
}

integrationTestUsesSampleDir("subprojects/platform-play/src/main")
