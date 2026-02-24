plugins {
    java
    `java-library`
    id("com.gradleup.shadow") version "9.2.2"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

group = "me.mapacheee.revenge"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    api("com.thewinterframework:paper:1.0.4")
    annotationProcessor("com.thewinterframework:paper:1.0.4")
    api("com.thewinterframework:configuration:1.0.2")
    annotationProcessor("com.thewinterframework:configuration:1.0.2")
    api("com.thewinterframework:command:1.0.1")
    annotationProcessor("com.thewinterframework:command:1.0.1")

    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("me.mapacheee.revenge:RevengeCore:1.0.0-SNAPSHOT")
}

tasks {
    processResources {
        filesMatching("paper-plugin.yml") {
            expand("version" to version)
        }
    }
    shadowJar {
        archiveFileName.set("RevengeEssentials-${project.version}.jar")
    }
}