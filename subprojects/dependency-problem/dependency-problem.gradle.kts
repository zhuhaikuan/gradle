import org.gradle.gradlebuild.unittestandcompile.ModuleType

plugins {
    java
    `maven-publish`
}


repositories {
    maven {
        url = project.uri("https://repo.gradle.org/gradle/enterprise-libs-training-local/")
    }
}

val defaultVersion = "6.2.1"
// The repo folder contains a full entry for a 6.1 version and a damaged/incomplete entry for 6.2
val requestedVersion = if(project.hasProperty("fail")) {
    "6.2+"
} else {
    "6.2.1"
}

val publishProp: String? = if(project.hasProperty("publicationVersion")) {
    project.property("publicationVersion").toString()
} else {
    null
}

dependencies {
    if(publishProp.isNullOrBlank()) {
        runtimeOnly("org.gradle:example-dependency:$requestedVersion")
    }
}

gradlebuildJava {
    moduleType = ModuleType.INTERNAL
}

val publicationVersion = if(publishProp.isNullOrBlank()) {
    defaultVersion
} else {
    publishProp
}

publishing {
    repositories {
        maven {
            name = "local"
            url = project.uri(file("repo"))
        }
    }
    publications {
        create<MavenPublication>("maven") {
            groupId = "org.gradle"
            artifactId = "example-dependency"
            version = publicationVersion

            from(components["java"])
        }
    }
}

