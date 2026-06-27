plugins {
    java
    `maven-publish`
    id("dev.arbjerg.lavalink.gradle-plugin")
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "com.sedmelluq" && requested.name == "lavaplayer") {
            useTarget("dev.arbjerg:lavaplayer:2.2.6")
        }
    }
}

dependencies {
    implementation(project(":source"))
    implementation(project(":common"))

    compileOnly("dev.arbjerg:lavaplayer:2.2.6")
    compileOnly("dev.arbjerg.lavalink:plugin-api:4.2.2")
}

tasks.jar {
    archiveBaseName.set("soundy-plugin")
}
