val grpcVersion = "1.61.0"
val protobufVersion = "3.25.2"
val slf4jVersion = "2.0.11"

plugins {
    id("regatta.library-conventions")
}

group = "com.jamf.regatta"
version = "1.0.0-SNAPSHOT"

dependencies {
    // This dependency is exported to consumers, that is to say found on their compile classpath.
    api(project(":regatta-java-grpc"))
    api("com.google.protobuf:protobuf-java:${protobufVersion}")
    api("io.grpc:grpc-core:${grpcVersion}")
    api("io.grpc:grpc-stub:${grpcVersion}")
    api("io.grpc:grpc-netty-shaded:${grpcVersion}")


    // This dependency is used internally, and not exposed to consumers on their own compile classpath.
    implementation("org.slf4j:slf4j-api:${slf4jVersion}")
    implementation("org.slf4j:slf4j-jdk14:${slf4jVersion}")
}
