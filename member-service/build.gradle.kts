plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("org.asciidoctor.jvm.convert")
}

description = "member-service"

val asciidoctorExt: Configuration by configurations.creating

dependencies {
    implementation(project(":common"))
    implementation("org.springframework.boot:spring-boot-starter-webmvc")

    testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
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