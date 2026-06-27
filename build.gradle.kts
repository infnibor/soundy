plugins {
    java
    `maven-publish`
    id("dev.arbjerg.lavalink.gradle-plugin") version "1.0.16" apply false
}

allprojects {
    group = "com.waris4ly.soundy"
    version = System.getenv("GITHUB_SHA")?.take(7) ?: "local"

    repositories {
        mavenCentral()
        maven("https://www.maven.pcreators.pl/snapshots")
        maven("https://www.maven.pcreators.pl/releases")
        maven("https://maven.lavalink.dev/releases")
        maven("https://maven.lavalink.dev/snapshots")
    }
}

subprojects {
    apply(plugin = "java")

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    dependencies {
        compileOnly("dev.arbjerg:lavaplayer:2.2.6")
    }
}

project(":plugin") {
    apply(plugin = "maven-publish")
    apply(plugin = "java")

    extensions.configure<PublishingExtension> {
        repositories {
            maven {
                name = "MyMaven"
                url = uri("https://www.maven.pcreators.pl/snapshots")
                credentials {
                    username = System.getenv("MAVEN_USERNAME")
                    password = System.getenv("MAVEN_PASSWORD")
                }
            }
        }

        publications {
            create<MavenPublication>("soundy") {
                from(components["java"])

                groupId = "com.github.infnibor.soundy"
                artifactId = "soundy-plugin"
                version = project.version.toString()
            }
        }
    }
}