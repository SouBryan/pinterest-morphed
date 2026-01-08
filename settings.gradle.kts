pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/MorpheApp/registry")
            credentials {
                username = providers.gradleProperty("gpr.user").getOrNull() ?: System.getenv("GPR_USER")
                password = providers.gradleProperty("gpr.key").getOrNull() ?: System.getenv("GPR_KEY")
            }
        }
        maven { url = uri("https://jitpack.io") }
    }
}
