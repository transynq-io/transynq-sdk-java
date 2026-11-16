plugins {
    `java-library`
    `maven-publish`
}

group = "com.transynq"
version = "1.0.0"
description = "Official Java client for the Transynq TMS API"

repositories {
    mavenCentral()
}

java {
    withSourcesJar()
    withJavadocJar()
}

sourceSets {
    main {
        java.srcDir("src/generated/java")
    }
    create("examples") {
        java.srcDir("examples/src/main/java")
        compileClasspath += sourceSets.main.get().output + configurations.runtimeClasspath.get()
        runtimeClasspath += sourceSets.main.get().output + configurations.runtimeClasspath.get()
    }
}

dependencies {
    api("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2")

    testImplementation(platform("org.junit:junit-bom:5.10.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all,-serial,-processing", "-Werror"))
}

tasks.named<JavaCompile>("compileTestJava") {
    options.compilerArgs.remove("-Werror")
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).apply {
        encoding = "UTF-8"
        addStringOption("Xdoclint:all,-missing", "-quiet")
        windowTitle = "Transynq Java SDK ${project.version}"
        docTitle = "Transynq Java SDK ${project.version}"
        header = "<img src=\"{@docRoot}/resources/brand/transynq-icon-32.png\" alt=\"Transynq\" width=\"20\" height=\"20\"> Transynq TMS"
        bottom = "&copy; 2026 Transynq &middot; MIT licence &middot; Intelligent Transport Operations"
        addFileOption("-add-stylesheet", file("src/main/javadoc/transynq.css"))
    }
    isFailOnError = true
    doLast {
        // the header logo: the brand file itself, copied next to the generated pages
        copy {
            from("assets/brand/transynq-icon-32.png")
            into(destinationDir!!.resolve("resources/brand"))
        }
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.register<JavaExec>("runExample") {
    description = "Runs an example flow: ./gradlew runExample --args=sign-in"
    group = "application"
    classpath = sourceSets["examples"].runtimeClasspath
    mainClass.set("com.transynq.sdk.examples.Flows")
}

tasks.named("check") {
    dependsOn("compileExamplesJava")
}

tasks.jar {
    manifest {
        attributes("Implementation-Title" to "transynq-sdk", "Implementation-Version" to project.version)
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "transynq-sdk"
            from(components["java"])
            pom {
                name.set("Transynq SDK")
                description.set(project.description)
                url.set("https://github.com/qadir0108/transynq-sdk-java")
                organization {
                    name.set("Transynq")
                    url.set("https://github.com/qadir0108/transynq-sdk-java")
                }
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                scm {
                    url.set("https://github.com/qadir0108/transynq-sdk-java")
                    connection.set("scm:git:https://github.com/qadir0108/transynq-sdk-java.git")
                    developerConnection.set("scm:git:ssh://git@github.com/qadir0108/transynq-sdk-java.git")
                }
                developers {
                    developer {
                        name.set("Transynq")
                    }
                }
            }
        }
    }
}
