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

import com.gradle.scan.plugin.BuildScanExtension
import gradlebuild.basics.BuildEnvironment
import gradlebuild.basics.kotlindsl.execAndGetStdout
import gradlebuild.jvm.tasks.ClasspathManifest
import org.gradle.api.internal.StartParameterInternal
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.net.URLEncoder
import java.util.concurrent.atomic.AtomicBoolean

val serverUrl = "https://e.grdev.net"
val gitCommitName = "Git Commit ID"
val ciBuildTypeName = "CI Build Type"
val watchFileSystemName = "watchFileSystem"

val cacheMissTagged = AtomicBoolean(false)

val buildScan = the<BuildScanExtension>() // we can not use plugin {} because this is registered by a settings plugin
inline fun buildScan(configure: BuildScanExtension.() -> Unit) {
    buildScan.apply(configure)
}

extractCiOrLocalData()
extractVcsData()

if (BuildEnvironment.isCiServer) {
    doNotUploadInBackground()
    if (!BuildEnvironment.isTravis && !BuildEnvironment.isJenkins) {
        extractAllReportsFromCI()
        monitorUnexpectedCacheMisses()
    }
}

extractCheckstyleAndCodenarcData()
extractBuildCacheData()
extractWatchFsData()

// TODO LD - adapt after changes merged and master updated to build with them
//        if ((project.gradle as GradleInternal).buildType != GradleInternal.BuildType.TASKS) {
//            buildScan.tag("SYNC")
//        }

fun monitorUnexpectedCacheMisses() {
    gradle.taskGraph.afterTask {
        if (buildCacheEnabled() && isCacheMiss() && isNotTaggedYet()) {
            buildScan.tag("CACHE_MISS")
        }
    }
}

fun buildCacheEnabled() = gradle.startParameter.isBuildCacheEnabled

fun isNotTaggedYet() = cacheMissTagged.compareAndSet(false, true)

fun Task.isCacheMiss() = !state.skipped && (isCompileCacheMiss() || isAsciidoctorCacheMiss())

fun Task.isCompileCacheMiss() = isMonitoredCompileTask() && !isExpectedCompileCacheMiss()

fun Task.isAsciidoctorCacheMiss() = isMonitoredAsciidoctorTask() && !isExpectedAsciidoctorCacheMiss()

fun Task.isMonitoredCompileTask() = this is AbstractCompile || this is ClasspathManifest

fun Task.isMonitoredAsciidoctorTask() = false // No asciidoctor tasks are cacheable for now

fun Task.isExpectedAsciidoctorCacheMiss() =
// Expected cache-miss for asciidoctor task:
// 1. CompileAll is the seed build for docs:distDocs
// 2. Gradle_Check_BuildDistributions is the seed build for other asciidoctor tasks
// 3. buildScanPerformance test, which doesn't depend on compileAll
    // 4. buildScanPerformance test, which doesn't depend on compileAll
    isInBuild(
        "Gradle_Check_CompileAll",
        "Gradle_Check_BuildDistributions",
        "Enterprise_Master_Components_GradleBuildScansPlugin_Performance_PerformanceLinux",
        "Enterprise_Release_Components_BuildScansPlugin_Performance_PerformanceLinux"
    )

fun Task.isExpectedCompileCacheMiss() =
// Expected cache-miss:
// 1. CompileAll is the seed build
// 2. Gradleception which re-builds Gradle with a new Gradle version
// 3. buildScanPerformance test, which doesn't depend on compileAll
    // 4. buildScanPerformance test, which doesn't depend on compileAll
    isInBuild(
        "Gradle_Check_CompileAll",
        "Enterprise_Master_Components_GradleBuildScansPlugin_Performance_PerformanceLinux",
        "Enterprise_Release_Components_BuildScansPlugin_Performance_PerformanceLinux",
        "Gradle_Check_Gradleception"
    )

fun Task.isInBuild(vararg buildTypeIds: String) = System.getenv("BUILD_TYPE_ID") in buildTypeIds

fun extractCheckstyleAndCodenarcData() {
    gradle.taskGraph.afterTask {
        if (state.failure != null) {
            if (this is Checkstyle && reports.xml.destination.exists()) {
                val checkstyle = Jsoup.parse(reports.xml.destination.readText(), "", Parser.xmlParser())
                val errors = checkstyle.getElementsByTag("file").flatMap { file ->
                    file.getElementsByTag("error").map { error ->
                        val filePath = rootProject.relativePath(file.attr("name"))
                        "$filePath:${error.attr("line")}:${error.attr("column")} \u2192 ${error.attr("message")}"
                    }
                }

                errors.forEach { buildScan.value("Checkstyle Issue", it) }
            }

            if (this is CodeNarc && reports.xml.destination.exists()) {
                val codenarc = Jsoup.parse(reports.xml.destination.readText(), "", Parser.xmlParser())
                val errors = codenarc.getElementsByTag("Package").flatMap { codenarcPackage ->
                    codenarcPackage.getElementsByTag("File").flatMap { file ->
                        file.getElementsByTag("Violation").map { violation ->
                            val filePath = rootProject.relativePath(file.attr("name"))
                            val message = violation.run {
                                getElementsByTag("Message").first()
                                    ?: getElementsByTag("SourceLine").first()
                            }
                            "$filePath:${violation.attr("lineNumber")} \u2192 ${message.text()}"
                        }
                    }
                }

                errors.forEach { buildScan.value("CodeNarc Issue", it) }
            }
        }
    }
}

fun isEc2Agent() = java.net.InetAddress.getLocalHost().hostName.startsWith("ip-")

fun Project.extractCiOrLocalData() {
    if (BuildEnvironment.isCiServer) {
        buildScan {
            tag("CI")
            if (isEc2Agent()) {
                tag("EC2")
            }
            when {
                BuildEnvironment.isTravis -> {
                    link("Travis Build", System.getenv("TRAVIS_BUILD_WEB_URL"))
                    value("Build ID", System.getenv("TRAVIS_BUILD_ID"))
                    setCommitId(System.getenv("TRAVIS_COMMIT"))
                }
                BuildEnvironment.isJenkins -> {
                    link("Jenkins Build", System.getenv("BUILD_URL"))
                    value("Build ID", System.getenv("BUILD_ID"))
                    setCommitId(System.getenv("GIT_COMMIT"))
                }
                BuildEnvironment.isGhActions -> {
                    link("GitHub Actions Build", "https://github.com/gradle/gradle/runs/${System.getenv("GITHUB_RUN_ID")}")
                    value("Build ID", "${System.getenv("GITHUB_RUN_ID")} ${System.getenv("GITHUB_RUN_NUMBER")}")
                    setCommitId(System.getenv("GITHUB_SHA"))
                }
                else -> {
                    link("TeamCity Build", System.getenv("BUILD_URL"))
                    value("Build ID", System.getenv("BUILD_ID"))
                    setCommitId(System.getenv("BUILD_VCS_NUMBER"))
                }
            }
            whenEnvIsSet("BUILD_TYPE_ID") { buildType ->
                value(ciBuildTypeName, buildType)
                link("Build Type Scans", customValueSearchUrl(mapOf(ciBuildTypeName to buildType)))
            }
        }
    } else {
        buildScan.tag("LOCAL")
        if (listOf("idea.registered", "idea.active", "idea.paths.selector").map(System::getProperty).filterNotNull().isNotEmpty()) {
            buildScan.tag("IDEA")
            System.getProperty("idea.paths.selector")?.let { ideaVersion ->
                buildScan.value("IDEA version", ideaVersion)
            }
        }
    }
}

fun BuildScanExtension.whenEnvIsSet(envName: String, action: BuildScanExtension.(envValue: String) -> Unit) {
    val envValue: String? = System.getenv(envName)
    if (!envValue.isNullOrEmpty()) {
        action(envValue)
    }
}

fun extractVcsData() {
    buildScan {
        if (!BuildEnvironment.isCiServer) {
            background {
                setCommitId(execAndGetStdout("git", "rev-parse", "--verify", "HEAD"))
            }
        }

        background {
            execAndGetStdout("git", "status", "--porcelain").takeIf { it.isNotEmpty() }?.let { status ->
                tag("dirty")
                value("Git Status", status)
            }
        }

        background {
            execAndGetStdout("git", "rev-parse", "--abbrev-ref", "HEAD").takeIf { it.isNotEmpty() && it != "HEAD" }?.let { branchName ->
                tag(branchName)
                value("Git Branch Name", branchName)
            }
        }
    }
}

fun extractBuildCacheData() {
    if (gradle.startParameter.isBuildCacheEnabled) {
        buildScan.tag("CACHED")
    }
}

fun extractWatchFsData() {
    val watchFileSystem = (project.gradle.startParameter as StartParameterInternal).isWatchFileSystem
    buildScan.value(watchFileSystemName, watchFileSystem.toString())
}

fun extractAllReportsFromCI() {
    val capturedReportingTypes = listOf("html") // can add xml, text, junitXml if wanted
    val basePath = "${System.getenv("BUILD_SERVER_URL")}/repository/download/${System.getenv("BUILD_TYPE_ID")}/${System.getenv("BUILD_ID")}:id"

    gradle.taskGraph.afterTask {
        if (state.failure != null && this is Reporting<*>) {
            this.reports.filter { it.name in capturedReportingTypes && it.isEnabled && it.destination.exists() }
                .forEach { report ->
                    val linkName = "${this::class.java.simpleName.split("_")[0]} Report ($path)" // Strip off '_Decorated' addition to class names
                    // see: ciReporting.gradle
                    val reportPath =
                        if (report.destination.isDirectory) "report-${project.name}-${report.destination.name}.zip"
                        else "report-${project.name}-${report.destination.parentFile.name}-${report.destination.name}"
                    val reportLink = "$basePath/$reportPath"
                    buildScan.link(linkName, reportLink)
                }
        }
    }
}

fun BuildScanExtension.setCommitId(commitId: String) {
    value(gitCommitName, commitId)
    link("Source", "https://github.com/gradle/gradle/commit/$commitId")
    if (!BuildEnvironment.isTravis) {
        link("Git Commit Scans", customValueSearchUrl(mapOf(gitCommitName to commitId)))
        link("CI CompileAll Scan", customValueSearchUrl(mapOf(gitCommitName to commitId)) + "&search.tags=CompileAll")
    }
}

fun Project.doNotUploadInBackground() {
    buildScan.isUploadInBackground = false
}

fun customValueSearchUrl(search: Map<String, String>): String {
    val query = search.map { (name, value) ->
        "search.names=${name.urlEncode()}&search.values=${value.urlEncode()}"
    }.joinToString("&")

    return "$serverUrl/scans?$query"
}

fun String.urlEncode() = URLEncoder.encode(this, Charsets.UTF_8.name())
