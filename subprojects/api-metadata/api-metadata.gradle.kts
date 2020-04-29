import org.gradle.gradlebuild.PublicApi
plugins {
    id("gradlebuild.distribution.core-implementation-java")
    id("gradlebuild.api-metadata")
}

apiMetadata {
    includes.addAll(PublicApi.includes)
    excludes.addAll(PublicApi.excludes)
}
