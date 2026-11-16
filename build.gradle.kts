import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    `java-library`
    signing
    // Maven Central (Central Portal) publishing: sources + javadoc jars, POM, signing
    id("com.vanniktech.maven.publish") version "0.30.0"
}

group = "io.transynq"
version = "1.0.2"
description = "Transynq - Intelligent Transport Operations. Official Java SDK for the Transynq TMS API: a typed client for all 705 API operations with authentication, retries, typed errors, paging, ETag concurrency, file transfer and long-running operations."

repositories {
    mavenCentral()
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
    mainClass.set("io.transynq.sdk.examples.Flows")
}

tasks.named("check") {
    dependsOn("compileExamplesJava")
}

tasks.jar {
    manifest {
        attributes("Implementation-Title" to "transynq-sdk", "Implementation-Version" to project.version)
    }
}

// Signing: an ASCII-armoured private key in memory, only when one is supplied (release workflow).
// Local and CI builds without SIGNING_KEY skip signing, so `./gradlew build` and `publishToMavenLocal` need no secrets.
val signingKey: String? = providers.environmentVariable("SIGNING_KEY").orNull
    ?: providers.gradleProperty("signingInMemoryKey").orNull
val signingPassword: String? = providers.environmentVariable("SIGNING_PASSWORD").orNull
    ?: providers.gradleProperty("signingInMemoryKeyPassword").orNull

mavenPublishing {
    // credentials: gradle properties mavenCentralUsername / mavenCentralPassword
    // (ORG_GRADLE_PROJECT_mavenCentralUsername / ..Password in the release workflow, from the Central Portal user token)
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    if (!signingKey.isNullOrBlank()) {
        signAllPublications()
    }
    coordinates("io.transynq", "transynq-sdk", version.toString())
    configure(JavaLibrary(javadocJar = JavadocJar.Javadoc(), sourcesJar = true))

    pom {
        name.set("Transynq Java SDK")
        description.set(project.description)
        inceptionYear.set("2026")
        url.set("https://github.com/transynq-io/transynq-sdk-java")
        organization {
            name.set("Transynq")
            url.set("https://transynq.io")
        }
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("transynq")
                name.set("Transynq")
                url.set("https://transynq.io")
                organization.set("Transynq")
                organizationUrl.set("https://transynq.io")
            }
        }
        scm {
            url.set("https://github.com/transynq-io/transynq-sdk-java")
            connection.set("scm:git:https://github.com/transynq-io/transynq-sdk-java.git")
            developerConnection.set("scm:git:ssh://git@github.com/transynq-io/transynq-sdk-java.git")
        }
        issueManagement {
            system.set("GitHub")
            url.set("https://github.com/transynq-io/transynq-sdk-java/issues")
        }
        ciManagement {
            system.set("GitHub Actions")
            url.set("https://github.com/transynq-io/transynq-sdk-java/actions")
        }
    }
}

if (!signingKey.isNullOrBlank()) {
    signing {
        useInMemoryPgpKeys(signingKey, signingPassword ?: "")
    }
}
