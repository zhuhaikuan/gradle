val login = tasks.register<Exec>("login") {
    val providers = project.getProviders()
    val username = providers.gradleProperty("username")
    val password = providers.gradleProperty("password")

    inputs.property("username", username)
    inputs.property("password", password)
    
    doFirst {
        standardInput = java.io.ByteArrayInputStream("${username.get()}\n${password.get()}".toByteArray())
    }

    commandLine = listOf("sh", "login.sh")
}

tasks.register("doAuthenticated") {
    dependsOn(login)
    doLast {
        logger.lifecycle("Doing authenticated task")
    }
}
