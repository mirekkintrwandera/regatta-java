plugins {
    id("regatta.library-conventions")
}

val grpcVersion = "1.65.1"
val testContainersVersion = "1.20.1"
val jupiterVersion = "5.10.3"
val autoServiceVersion = "1.1.1"

group = "com.jamf.regatta"

version = "1.3.1-SNAPSHOT"

dependencies {
    api("io.grpc:grpc-core:${grpcVersion}")
    api("io.grpc:grpc-stub:${grpcVersion}")
    api("io.grpc:grpc-inprocess:${grpcVersion}")
    api("io.grpc:grpc-util:${grpcVersion}")
    api("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
    api("org.testcontainers:testcontainers:$testContainersVersion")

    implementation("com.google.auto.service:auto-service-annotations:${autoServiceVersion}")

    annotationProcessor("com.google.auto.service:auto-service:${autoServiceVersion}")
}
