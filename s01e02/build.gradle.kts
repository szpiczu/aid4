plugins {
    id("buildlogic.java-application-conventions")
}

dependencies {
    implementation(libs.jackson.databind)
    implementation("io.github.cdimascio:dotenv-java:3.2.0")
    implementation(project(":s01e01"))
}

application {
    mainClass = "pl.informatysta.aid4.s01e02.App"
}
