import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    gradlebuild.distribution.`implementation-kotlin`
}

tasks {
    withType<KotlinCompile>().configureEach {
        kotlinOptions {
            freeCompilerArgs += listOf(
                "-XXLanguage:+NewInference",
                "-XXLanguage:+SamConversionForKotlinFunctions"
            )
        }
    }

    processResources {
        from({ project(":instantExecutionReport").tasks.named("assembleReport") }) {
            into("org/gradle/instantexecution")
        }
    }

    instantIntegTest {
        enabled = false
    }
}

afterEvaluate {
    // This is a workaround for the validate plugins task trying to inspect classes which have changed but are NOT tasks.
    // For the current project, we simply disable it since there are no tasks in there.
    tasks.withType<ValidatePlugins>().configureEach {
        enabled = false
    }
}

dependencies {
    implementation(project(":baseServices"))
    implementation(project(":baseServicesGroovy"))
    implementation(project(":core"))
    implementation(project(":coreApi"))
    implementation(project(":dependencyManagement"))
    implementation(project(":execution"))
    implementation(project(":fileCollections"))
    implementation(project(":kotlinDsl"))
    implementation(project(":logging"))
    implementation(project(":messaging"))
    implementation(project(":modelCore"))
    implementation(project(":persistentCache"))
    implementation(project(":plugins"))
    implementation(project(":publish"))
    implementation(project(":resources"))
    implementation(project(":snapshots"))

    // TODO - move the isolatable serializer to model-core to live with the isolatable infrastructure
    implementation(project(":workers"))

    // TODO - it might be good to allow projects to contribute state to save and restore, rather than have this project know about everything
    implementation(project(":toolingApi"))
    implementation(project(":buildEvents"))
    implementation(project(":native"))
    implementation(project(":buildOption"))

    implementation(library("groovy"))
    implementation(library("slf4j_api"))
    implementation(library("guava"))

    implementation(libs.futureKotlin("stdlib-jdk8"))
    implementation(libs.futureKotlin("reflect"))

    testImplementation(testFixtures(project(":core")))
    testImplementation(testLibrary("mockito_kotlin2"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:1.3.3")

    integTestImplementation(project(":jvmServices"))
    integTestImplementation(project(":toolingApi"))
    integTestImplementation(project(":platformJvm"))
    integTestImplementation(project(":testKit"))
    integTestImplementation(project(":launcher"))

    integTestImplementation(library("guava"))
    integTestImplementation(library("ant"))
    integTestImplementation(library("inject"))
    integTestImplementation(testFixtures(project(":dependencyManagement")))
    integTestImplementation(testFixtures(project(":jacoco")))

    crossVersionTestImplementation(project(":cli"))

    testRuntimeOnly(project(":distributionsCore")) {
        because("Tests instantiate DefaultClassLoaderRegistry which requires a 'gradle-plugins.properties' through DefaultPluginModuleRegistry")
    }
    integTestDistributionRuntimeOnly(project(":distributionsJvm")) {
        because("Includes tests for builds with TestKit involved; InstantExecutionJacocoIntegrationTest requires JVM distribution")
    }
    crossVersionTestDistributionRuntimeOnly(project(":distributionsCore"))
}

classycle {
    excludePatterns.set(listOf("org/gradle/instantexecution/**"))
}
