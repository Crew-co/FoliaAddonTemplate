import org.gradle.api.tasks.Copy

plugins {
    kotlin("jvm") version "2.1.0"
    id("com.gradleup.shadow") version "8.3.11"
}

group = property("group") as String
version = property("version") as String

val foliaApiVersion = property("foliaApiVersion") as String

repositories {
    mavenCentral()

    maven("https://repo.papermc.io/repository/maven-public/")

    mavenLocal()

    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/Crew-co/FoliaPluginTemplate-Addon-Based-Version")

        credentials {
            username = providers.gradleProperty("gpr.user").orNull
                ?: System.getenv("GITHUB_ACTOR")

            password = providers.gradleProperty("gpr.token").orNull
                ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    // Folia API
    compileOnly("dev.folia:folia-api:$foliaApiVersion")

    // Addon API
    // Replace "com.example" with the actual group if necessary.
    compileOnly("com.example:folia-template-addon-api:1.0.0")

    // Kotlin is provided by the host
    compileOnly(kotlin("stdlib"))

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

tasks {
    shadowJar {
        archiveBaseName.set(providers.gradleProperty("addonName").orElse(project.name))
        archiveClassifier.set("")
    }

    build {
        dependsOn(shadowJar)
    }

    jar {
        enabled = false
    }

    test {
        useJUnitPlatform()
    }
}

tasks.register<Copy>("deployAddon") {
    group = "deployment"
    description = "Builds the addon and copies it into your test server's addons folder."

    dependsOn(tasks.shadowJar)

    val target = providers.gradleProperty("testServerPath").orNull

    onlyIf {
        if (target == null) {
            logger.lifecycle(
                "Set testServerPath in ~/.gradle/gradle.properties to use deployAddon."
            )
        }
        target != null
    }

    from(tasks.shadowJar)

    if (target != null) {
        into("$target/plugins/FoliaTemplate/addons")
    }
}
