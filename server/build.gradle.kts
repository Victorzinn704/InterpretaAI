plugins {
    java
    id("org.springframework.boot")
}

group = "br.gov.interpretaai"
version = "0.1.0"

java { sourceCompatibility = JavaVersion.VERSION_17 }

tasks.withType<JavaCompile> { options.release = 17 }

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.5.16"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.flywaydb:flyway-core")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:2.4.0")
    implementation("dev.langchain4j:langchain4j:1.20.0")
    implementation("dev.langchain4j:langchain4j-google-genai:1.20.0-beta30")
    implementation("dev.langchain4j:langchain4j-ollama:1.20.0")
    implementation("dev.langchain4j:langchain4j-open-ai:1.20.0")
    implementation(platform("com.google.cloud:libraries-bom:26.88.1"))
    implementation("com.google.cloud:google-cloud-texttospeech")
    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
}

tasks.withType<Test> { useJUnitPlatform() }
