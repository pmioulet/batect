/*
    Copyright 2017-2021 Charles Korn.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

val okhttpVersion: String by project
val kotlinxSerializationVersion: String by project
val jnrPosixVersion: String by project
val commonsCompressVersion: String by project
val bouncycastleVersion: String by project
val wireVersion: String by project
val jimfsVersion: String by project
val hamkrestJsonVersion: String by project

buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        val wireVersion: String by project

        classpath("com.squareup.wire:wire-gradle-plugin:$wireVersion")
    }
}

plugins {
    id("batect-kotlin")
    id("de.undercouch.download")
}

dependencies {
    implementation(platform("com.squareup.okhttp3:okhttp-bom:$okhttpVersion"))
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
    implementation("com.github.jnr:jnr-posix:$jnrPosixVersion")
    implementation("com.squareup.okhttp3:okhttp")
    implementation("org.apache.commons:commons-compress:$commonsCompressVersion")
    implementation("org.bouncycastle:bcpkix-jdk15on:$bouncycastleVersion")
    implementation("com.squareup.wire:wire-grpc-client:$wireVersion")

    implementation(project(":libs:logging"))
    implementation(project(":libs:os"))
    implementation(project(":libs:sockets"))
    implementation(project(":libs:telemetry"))
    implementation(project(":libs:primitives"))

    testImplementation("com.google.jimfs:jimfs:$jimfsVersion")
    testImplementation("org.araqnid.hamkrest:hamkrest-json:$hamkrestJsonVersion")

    testImplementation(project(":libs:logging-test-utils"))
    testImplementation(project(":libs:test-utils"))
}

checkUnitTestLayout {
    ignoreFileNameCheck.set(
        fileTree("src/unitTest/kotlin") {
            include("batect/docker/api/Assertions.kt")
        }
    )
}

apply(from = "gradle/integrationTest.gradle.kts")
apply(from = "gradle/protobuf.gradle")
