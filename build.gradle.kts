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

plugins {
    base                                     // Build Distribution: 'base.distsDirName' is used for the location of the distribution, might move this to "subprojects/distributions/build/..."
    gradlebuild.security                     // Check: Ensure sure we only use secure repositories

    gradlebuild.lifecycle                    // CI: Add lifecycle tasks to for the CI pipeline (currently needs to be applied early as it might modify global properties)
    gradlebuild.`generate-subprojects-info`  // CI: Generate subprojects information for the CI testing pipeline fan out
    gradlebuild.cleanup                      // CI: Advanced cleanup after the build (like stopping daemons started by tests)
    gradlebuild.`ci-reporting`               // CI: Prepare reports to be uploaded to TeamCity

    gradlebuild.buildscan                    // Reporting: Add more data through custom tags to build scans

    gradlebuild.`build-version`              // Release process: Set the version for this build

    gradlebuild.ide                          // Local development: Tweak IDEA import
    gradlebuild.`update-versions`            // Local development: Convenience tasks to update versions in this build: 'released-versions.json', 'agp-versions.properties', ...
    gradlebuild.wrapper                      // Local development: Convenience tasks to update the wrapper (like 'nightlyWrapper')
    gradlebuild.`quick-check`                // Local development: Convenience task `quickCheck` for running checkstyle/codenarc only on changed files before commit
}

buildscript {
    dependencies {
        constraints {
            classpath("xerces:xercesImpl:2.12.0") {
                // it's unclear why we don't get this version directly from buildSrc constraints
                because("Maven Central and JCenter disagree on version 2.9.1 metadata")
            }
        }
    }
}

// This an example of a task that is only created conditionally
// Conditional creation of tasks is sometimes used to avoid expensive configuration slowing down builds that don't need it
// This technique has been supplanted by the TaskContainer.register() API which accomplishes the time savings without the pitfalls
// See this guide on task configuration avoidance for more information:
//      https://docs.gradle.org/current/userguide/task_configuration_avoidance.html
if(System.getenv("CI") != null) {
    tasks.register("ciDiagnostics") {
        // This is admittedly contrived, so let's make-believe this does something actually useful
        doLast {
            println("Doing some expensive calculations...")
            Thread.sleep(1000L)
            println("Everything is A-Okay")
        }
    }
}
