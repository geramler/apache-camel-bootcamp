plugins {
    java
    id("io.quarkus") version "3.27.1"
}

group = "com.bootcamp"
version = "1.0-SNAPSHOT"

java {
    toolchain { languageVersion = JavaLanguageVersion.of(17) }
}

repositories {
    mavenCentral()
}

val quarkusPlatformVersion = "3.27.1"

dependencies {
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:$quarkusPlatformVersion"))
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-camel-bom:$quarkusPlatformVersion"))

    implementation("org.apache.camel.quarkus:camel-quarkus-core")
    implementation("org.apache.camel.quarkus:camel-quarkus-platform-http")
    implementation("org.apache.camel.quarkus:camel-quarkus-rest")
    implementation("org.apache.camel.quarkus:camel-quarkus-jackson")
    implementation("org.apache.camel.quarkus:camel-quarkus-timer")
    implementation("org.apache.camel.quarkus:camel-quarkus-direct")
    implementation("org.apache.camel.quarkus:camel-quarkus-seda")
    implementation("org.apache.camel.quarkus:camel-quarkus-file")
    implementation("org.apache.camel.quarkus:camel-quarkus-log")
    implementation("org.apache.camel.quarkus:camel-quarkus-bean")

    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}

tasks.test {
    useJUnitPlatform()
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
}
