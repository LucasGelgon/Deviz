plugins {
    kotlin("jvm") version "1.9.24"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":core"))
}

application {
    mainClass.set("deviz.desktop.MainKt")
}

kotlin {
    jvmToolchain(11)
}
