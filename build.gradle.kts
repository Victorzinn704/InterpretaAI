plugins {
    id("com.android.application") version "9.4.0" apply false
    id("org.jetbrains.kotlin.android") version "2.4.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20" apply false
    id("com.google.devtools.ksp") version "2.3.12" apply false
    id("org.springframework.boot") version "4.1.1" apply false
    id("org.sonarqube") version "7.5.0.8588"
}

sonar {
    properties {
        property("sonar.projectKey", "interpretaai")
        property("sonar.projectName", "InterpretaAI")
        property("sonar.projectVersion", "0.22.0")
        property("sonar.sourceEncoding", "UTF-8")
        property(
            "sonar.exclusions",
            "**/build/**,**/generated/**,**/schemas/**,**/output/**,**/dist/**"
        )
        property(
            "sonar.coverage.exclusions",
            listOf(
                "**/ui/**",
                "**/*Activity.kt",
                "**/*Application.kt",
                "**/*Receiver.kt",
                "**/platform/VoiceAssistant.kt"
            ).joinToString(",")
        )
    }
}

tasks.named("sonar") {
    dependsOn(
        ":server:jacocoTestReport",
        ":app:jacocoDebugUnitTestReport",
        ":app:lintDebug"
    )
}
