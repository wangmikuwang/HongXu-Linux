import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
}

// Linux desktop edition. Sources are a port of the Android app; tools/sync_linux.py keeps the unchanged files current.
val appName = "虹叙"
val buildFields = mapOf(
    "APP_NAME" to appName,
    "APPLICATION_ID" to "io.wenyou.textquest.alpha",
    "FLAVOR" to "alpha",
    "SHARE_ORIGIN" to "hx",
    "SHARE_SCHEME" to "hongxu",
    "APP_FILE_PREFIX" to "HongXu",
    "UPDATE_REPOSITORY" to "wangmikuwang/HongXu-Linux",
)

// The Linux edition has its own version line, starting at 1.0.0.
val versionProps = Properties().apply { rootProject.file("version.properties").inputStream().use { load(it) } }
val appVersionName = listOf("versionMajor", "versionMinor", "versionPatch").joinToString(".") { versionProps.getProperty(it) }
val appVersionCode = versionProps.getProperty("versionCode").toInt()

// Build output outside OneDrive, like the Android projects (Flatpak builds keep the default build/).
if (System.getenv("FLATPAK_ID") == null) layout.buildDirectory.set(File(System.getProperty("user.home"), ".gradle/caches/wnq-build/HongXu-Linux"))

val generateBuildConfig by tasks.registering {
    val out = layout.buildDirectory.dir("generated/buildconfig")
    val fields = buildFields + mapOf("VERSION_NAME" to appVersionName)
    inputs.property("fields", fields)
    inputs.property("code", appVersionCode)
    outputs.dir(out)
    doLast {
        val body = fields.entries.joinToString("\n") { (k, v) -> "    const val $k = \"$v\"" }
        out.get().file("io/wenyou/textquest/BuildConfig.kt").asFile.apply { parentFile.mkdirs() }.writeText(
            "package io.wenyou.textquest\n\nobject BuildConfig {\n$body\n    const val VERSION_CODE = $appVersionCode\n    const val DEBUG = false\n}\n"
        )
    }
}

// Java 17 bytecode, built by whichever JDK (17 or newer) runs Gradle: Debian 13 only ships JDK 21.
java { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

tasks.test {
    systemProperty("renderDir", layout.buildDirectory.dir("render").get().asFile.path)
    providers.gradleProperty("storeScreenshots").orNull?.let { systemProperty("storeScreenshots", file(it).path) }
    filter {
        // Synced from the Android app but about its packaging (backup XML, changelog asset, preset files).
        excludeTestsMatching("*BackupSafetyTest.systemBackupsNeverCarryTheKeys")
        excludeTestsMatching("*WhatsNewTest.bundledNotesMatchTheChangelogAndLeadWithThisVersion")
        excludeTestsMatching("*BaselineTest.bundledPresetsLeaveSafetyRulesToTheBaseline")
    }
}

sourceSets.main { kotlin.srcDir(generateBuildConfig) }

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(libs.jb.material3)
    implementation(libs.jb.lifecycle.viewmodel.compose)
    implementation(libs.jb.lifecycle.runtime.compose)
    implementation(libs.jb.navigation.compose)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.zxing.core)
    compileOnly(libs.errorprone.annotations) // used by third_party material-color-utilities
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

compose.desktop {
    application {
        mainClass = "io.wenyou.textquest.MainKt"
        jvmArgs += listOf("-Dfile.encoding=UTF-8")
        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm)
            packageName = appName // menu entry and launcher show the Chinese name
            packageVersion = appVersionName
            description = appName
            vendor = "wangmikuwang"
            licenseFile.set(project.file("LICENSE"))
            modules("java.naming", "jdk.unsupported")
            linux {
                packageName = "hongxu" // the .deb package itself keeps an ASCII name
                iconFile.set(project.file("src/main/resources/icon.png"))
                debMaintainer = "wangmikuwang@users.noreply.github.com"
                menuGroup = "Game"
                appCategory = "Game"
                shortcut = true
            }
        }
    }
}
