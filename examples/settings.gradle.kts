/*
 * Copyright JAMF Software, LLC
 */

rootProject.name = "regatta-java.examples"
include("spring-data-example")


includeBuild("../") {
    dependencySubstitution {
        substitute(module("com.jamf.regatta:regatta-java-grpc")).using(project(":regatta-java-grpc"))
        substitute(module("com.jamf.regatta:regatta-java-core")).using(project(":regatta-java-core"))
        substitute(module("com.jamf.regatta:regatta-java-spring-data")).using(project(":regatta-java-spring-data"))
        substitute(module("com.jamf.regatta:regatta-java-test")).using(project(":regatta-java-test"))
    }
}
