plugins {
    application
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(libs.junit.jupiter)

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation(libs.guava)
    implementation("com.fasterxml.jackson.core:jackson-databind:2.0.1")
    implementation("org.apache.logging.log4j:log4j-api:2.23.1")
    implementation("org.apache.logging.log4j:log4j-core:2.23.1")
    implementation("com.discord4j:discord4j-core:3.2.6")
    implementation("org.xerial:sqlite-jdbc:3.46.0.0")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(22)
    }
}

application {
    mainClass = "arbor.astralis.qotd.Main"
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
