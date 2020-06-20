/*
 * Copyright 2019 the original author or authors.
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
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackOutput.Target.GLOBAL
import java.util.Base64

plugins {
    gradlebuild.internal.`kotlin-js`
}

kotlin {
    js {
        browser {
            webpackTask {
                output.libraryTarget = GLOBAL
            }
        }
    }
    sourceSets {
        named("jsMain") {
            dependencies {
                compileOnly(kotlin("stdlib-js"))
            }
        }
    }
}

tasks {

    val jsBrowserProductionWebpack by existing(KotlinWebpack::class)

    val assembleReport by registering(MergeReportAssets::class) {
        htmlFile.set(layout.projectDirectory.file("src/jsMain/assets/configuration-cache-report.html"))
        logoFile.set(layout.projectDirectory.file("src/jsMain/assets/configuration-cache-report-logo.png"))
        cssFile.set(layout.projectDirectory.file("src/jsMain/assets/configuration-cache-report.css"))
        jsFile.set(jsBrowserProductionWebpack.map { layout.projectDirectory.file(it.outputFile.absolutePath) })
        outputFile.set(layout.buildDirectory.file("$name/configuration-cache-report.html"))
    }

    assemble {
        dependsOn(assembleReport)
    }

    val stageDevReport by registering(Sync::class) {
        from(assembleReport)
        from(processTestResources)
        into("$buildDir/$name")
    }

    test {
        inputs.dir(stageDevReport.map { it.destinationDir })
    }
}

@CacheableTask
abstract class MergeReportAssets : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val htmlFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val logoFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val cssFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val jsFile: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun action() {
        outputFile.get().asFile.writeText(
            htmlFile.get().asFile.readText().also {
                require(it.contains(cssTag))
                require(it.contains(jsTag))
            }.replace(
                cssTag,
                """
                <style type="text/css">
                ${cssFile.get().asFile.readText().also {
                    require(it.contains(logoStyle))
                }.replace(
                    logoStyle,
                    """background-image: url("data:image/png;base64,${logoFile.get().asFile.base64Encode()}");"""
                )}
                </style>
                """.trimIndent()
            ).replace(
                jsTag, """
                <script type="text/javascript">
                ${jsFile.get().asFile.readText()}
                </script>
                """.trimIndent()
            )
        )
    }

    private
    val cssTag = """<link rel="stylesheet" href="./configuration-cache-report.css">"""

    private
    val kotlinJsTag = """<script type="text/javascript" src="kotlin.js"></script>"""

    private
    val jsTag = """<script type="text/javascript" src="configuration-cache-report.js"></script>"""

    private
    val logoStyle = """background-image: url("configuration-cache-report-logo.png");"""

    private
    fun File.base64Encode() =
        Base64.getEncoder().encodeToString(readBytes())
}
