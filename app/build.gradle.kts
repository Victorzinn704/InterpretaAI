import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    jacoco
}

val targetAbi = providers.gradleProperty("targetAbi").orNull

android {
    namespace = "br.gov.interpretaai"
    compileSdk = 35

    val productionApiUrl = "https://interpretaai.deskimperial.online"
    val configuredApiUrl = providers.gradleProperty("voiceApiUrl").orElse(productionApiUrl)

    defaultConfig {
        applicationId = "br.gov.interpretaai"
        minSdk = 26
        targetSdk = 35
        versionCode = 23
        versionName = "0.23.0"

        if (targetAbi != null) {
            require(targetAbi == "arm64-v8a") { "A variante compacta aceita somente arm64-v8a." }
            ndk { abiFilters.add(targetAbi) }
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        buildConfigField("String", "VOICE_API_URL", "\"${configuredApiUrl.get()}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
}

jacoco { toolVersion = "0.8.13" }

tasks.register<JacocoReport>("jacocoDebugUnitTestReport") {
    group = "verification"
    description = "Generates JaCoCo coverage for the Android debug unit tests."
    dependsOn("testDebugUnitTest")

    reports {
        xml.required = true
        html.required = true
    }

    val generatedClasses = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "**/*_Factory.*",
        "**/*_Impl.*",
        "**/*JsonAdapter.*",
        "**/Hilt_*.*",
        "**/Dagger*.*"
    )
    classDirectories.setFrom(
        files(
            fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
                exclude(generatedClasses)
            },
            fileTree(
                layout.buildDirectory.dir(
                    "intermediates/javac/debug/compileDebugJavaWithJavac/classes"
                )
            ) {
                exclude(generatedClasses)
            }
        )
    )
    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include(
                "jacoco/testDebugUnitTest.exec"
            )
        }
    )
}

sonar {
    properties {
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            layout.buildDirectory.file(
                "reports/jacoco/jacocoDebugUnitTestReport/jacocoDebugUnitTestReport.xml"
            ).get().asFile.absolutePath
        )
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.google.mlkit:image-labeling:17.0.9")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.work:work-runtime-ktx:2.11.2")
    ksp("androidx.room:room-compiler:2.6.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.json:json:20240303")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
