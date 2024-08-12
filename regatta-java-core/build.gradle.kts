/*
 * Copyright JAMF Software, LLC
 */

val grpcVersion = "1.66.0"
val protobufVersion = "3.25.4"
val slf4jVersion = "2.0.13"
val snappyVersion = "1.1.10.5"
val sslContextVersion = "8.3.6"
var autoServiceVersion = "1.1.1"
val failsafeVersion = "3.3.2"
val jupiterVersion = "5.10.3"
val assertjVersion = "3.26.3"

plugins {
    id("regatta.library-conventions")
}
group = "com.jamf.regatta"

version = "1.3.1-SNAPSHOT"
dependencies {
    // This dependency is exported to consumers, that is to say found on their compile classpath.
    api(project(":regatta-java-grpc"))
    api("com.google.protobuf:protobuf-java:${protobufVersion}")
    api("io.grpc:grpc-core:${grpcVersion}")
    api("io.grpc:grpc-stub:${grpcVersion}")
    api("io.grpc:grpc-netty-shaded:${grpcVersion}")
    api("io.github.hakky54:sslcontext-kickstart:${sslContextVersion}")


    // This dependency is used internally, and not exposed to consumers on their own compile classpath.
    implementation("org.slf4j:slf4j-api:${slf4jVersion}")
    implementation("org.xerial.snappy:snappy-java:${snappyVersion}")
    implementation("io.github.hakky54:sslcontext-kickstart-for-netty:${sslContextVersion}")
    implementation("io.github.hakky54:sslcontext-kickstart-for-pem:${sslContextVersion}")
    implementation("org.xerial.snappy:snappy-java:${snappyVersion}")
    implementation("dev.failsafe:failsafe:${failsafeVersion}")
    implementation("com.google.auto.service:auto-service-annotations:${autoServiceVersion}")

    annotationProcessor("com.google.auto.service:auto-service:${autoServiceVersion}")

    testImplementation("org.assertj:assertj-core:${assertjVersion}")
    testImplementation("io.grpc:grpc-testing:${grpcVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-migrationsupport:${jupiterVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${jupiterVersion}")
}
