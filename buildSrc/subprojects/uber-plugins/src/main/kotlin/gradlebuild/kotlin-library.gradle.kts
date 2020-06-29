/*
 * Copyright 2020 the original author or authors.
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
package gradlebuild

import org.gradle.api.internal.initialization.DefaultClassLoaderScope

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

import org.jlleitschuh.gradle.ktlint.KtlintCheckTask
import org.jlleitschuh.gradle.ktlint.KtlintFormatTask


plugins {
    kotlin("jvm")
    id("gradlebuild.java-library")
    id("org.gradle.kotlin-dsl.ktlint-convention")
}

tasks {
    withType<KotlinCompile>().configureEach {
        configureKotlinCompilerForGradleBuild()
        kotlinOptions.allWarningsAsErrors = true
    }

    withType<KtlintFormatTask>().configureEach {
        enabled = false
    }

    val ktlintCheckTasks = withType<KtlintCheckTask>()

    named("codeQuality") {
        dependsOn(ktlintCheckTasks)
    }

    withType<Test>().configureEach {

        shouldRunAfter(ktlintCheckTasks)

        // enables stricter ClassLoaderScope behaviour
        systemProperty(
            DefaultClassLoaderScope.STRICT_MODE_PROPERTY,
            true)
    }

    compileTestKotlin.configure {
        // Make sure the classes dir is used for test compilation (required by tests accessing internal methods) - https://github.com/gradle/gradle/issues/11501
        classpath = sourceSets.main.get().output.classesDirs + classpath - files(tasks.jar)
    }
}

fun KotlinCompile.configureKotlinCompilerForGradleBuild() {
    kotlinOptions {
        apiVersion = "1.3"
        languageVersion = "1.3"
        freeCompilerArgs += listOf(
            "-Xjsr305=strict",
            "-java-parameters",
            "-Xskip-runtime-version-check",
            "-progressive"
        )
        jvmTarget = "1.8"
    }
}
