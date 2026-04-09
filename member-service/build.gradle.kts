plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    id("org.springframework.boot")
    id("org.asciidoctor.jvm.convert")
}

description = "member-service"

val asciidoctorExt: Configuration by configurations.creating

dependencies {
    implementation(project(":common"))
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.postgresql:postgresql")

    testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:testcontainers-postgresql:2.0.4")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    asciidoctorExt("org.springframework.restdocs:spring-restdocs-asciidoctor:3.0.3")
}


val snippetsDir = layout.buildDirectory.dir("generated-snippets").get().asFile

tasks {
    test {
        outputs.dir(snippetsDir)
    }

    asciidoctor {
        inputs.dir(snippetsDir)
        configurations("asciidoctorExt")
        dependsOn(test)
    }
}

tasks.register("prepareKotlinBuildScriptModel") {}