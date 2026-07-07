group = "app.soubryan"

patches {
    about {
        name = "Pinterest Morphed"
        description = "Morphe patches for Pinterest: removes ads, ad SDKs, trackers and analytics for a cleaner experience."
        source = "git@github.com:SouBryan/pinterest-morphed.git"
        author = "SouBryan"
        contact = "https://github.com/SouBryan"
        website = "https://github.com/SouBryan/pinterest-morphed"
        license = "GPLv3"
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

// Separate configuration so gson is available at runtime for the
// generatePatchesList task but never bundled into the APK.
val patchListGeneratorClasspath: Configuration by configurations.creating

dependencies {
    compileOnly(libs.gson)
    patchListGeneratorClasspath(libs.gson)
}

tasks {
    register<JavaExec>("generatePatchesList") {
        description = "Build patch with patch list"

        dependsOn(build)

        classpath = sourceSets["main"].runtimeClasspath + patchListGeneratorClasspath
        mainClass.set("util.PatchListGeneratorKt")
    }

    // Used by gradle-semantic-release-plugin.
    publish {
        dependsOn("generatePatchesList")
    }
}