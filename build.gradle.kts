plugins {
    java
}

group = "me.uc_hussein"
version = "1.0.0"

// Default build: Paper 26.2 + Java 25.
// Best-effort 1.21.x build (Java 21):  ./gradlew build -Ptarget121
//   optional: -PlegacyApi=1.21.8-R0.1-SNAPSHOT
val legacy = providers.gradleProperty("target121").isPresent
val javaVersion = if (legacy) 21 else 25
val apiVersion = if (legacy) "1.21" else "26.2"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") { name = "papermc" }
}

dependencies {
    if (legacy) {
        compileOnly("io.papermc.paper:paper-api:${providers.gradleProperty("legacyApi").getOrElse("1.21.8-R0.1-SNAPSHOT")}")
    } else {
        compileOnly("io.papermc.paper:paper-api:26.2.build.+")
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(javaVersion)
}

tasks.processResources {
    val props = mapOf("version" to project.version.toString(), "apiVersion" to apiVersion)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") { expand(props) }
}

tasks.jar {
    archiveFileName.set("ULTRAS_Chat_v1.jar")
}
