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
        maven { url = uri("https://jitpack.io") }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // 第三方仓库：IJKPlayer 旧版包托管在 bintray 镜像、Coil SVG 等
        maven { url = uri("https://jitpack.io") }
        // 高德 SDK 也在 JCenter 镜像
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}
rootProject.name = "let_us_go"
include(":app")
