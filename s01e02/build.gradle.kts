plugins {
    id("buildlogic.java-application-conventions")
}

dependencies {
    implementation(libs.jackson.databind)
}

application {
    mainClass = "pl.informatysta.aid4.s01e02.App"
}
