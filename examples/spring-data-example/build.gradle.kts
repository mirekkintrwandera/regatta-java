/*
 * Copyright JAMF Software, LLC
 */

plugins {
    application
    id("org.springframework.boot") version "3.2.2"
    id("io.spring.dependency-management") version "1.1.4"
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("org.springframework.data:spring-data-keyvalue")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("com.jamf.regatta:regatta-java-spring-data")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.jamf.regatta:regatta-java-test")
}

testing {
    suites {
        // Configure the built-in test suite
        val test by getting(JvmTestSuite::class) {
            // Use JUnit Jupiter test framework
            useJUnitJupiter("5.9.3")
        }
    }
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
