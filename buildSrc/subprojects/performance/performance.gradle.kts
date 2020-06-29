plugins {
    `groovy-gradle-plugin` // Support pre-compiled Groovy script plugins
}

dependencies {
    implementation(project(":basics"))
    implementation(project(":moduleIdentity"))
    implementation(project(":integrationTesting"))
    implementation(project(":cleanup"))

    implementation("org.codehaus.groovy.modules.http-builder:http-builder")
    implementation("org.openmbee.junit:junit-xml-parser") {
        exclude(module = "lombok") // don't need it at runtime
    }
    implementation("com.google.guava:guava")
    implementation("commons-io:commons-io")
    implementation("javax.activation:activation")
    implementation("javax.xml.bind:jaxb-api")

    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("junit:junit")
    testImplementation("io.mockk:mockk")
}

gradlePlugin {
    plugins {
        register("performanceTest") {
            id = "gradlebuild.performance-test"
            implementationClass = "gradlebuild.performance.PerformanceTestPlugin"
        }
    }
}

tasks.compileGroovy.configure {
    classpath = sourceSets.main.get().compileClasspath
}
tasks.compileKotlin.configure {
    classpath += files(tasks.compileGroovy)
}

tasks.withType<Test>().configureEach {
    // This is required for the PerformanceTestIntegrationTest
    environment("BUILD_BRANCH", "myBranch")
    environment("BUILD_COMMIT_ID", "myCommitId")
}
