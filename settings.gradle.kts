// Flatpak builds run offline against a prepared local Maven folder (see packaging/flatpak).
val offlineMaven: String? = System.getenv("OFFLINE_MAVEN_REPO")

pluginManagement {
    repositories {
        System.getenv("OFFLINE_MAVEN_REPO")?.let { maven(uri(it)); return@repositories }
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        maven("https://maven.aliyun.com/repository/public")
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        offlineMaven?.let { maven(uri(it)); return@repositories }
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/public")
        google()
        mavenCentral()
    }
}

rootProject.name = "HongXu-Linux"