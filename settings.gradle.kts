pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Mako"
include(":app")
include(":bohio")

// Activate For Release
project(":bohio").projectDir = file("bohio")

// Activate For Bohio Development (and locate your local source)
//project(":bohio").projectDir = file("../25_mod__bohio")