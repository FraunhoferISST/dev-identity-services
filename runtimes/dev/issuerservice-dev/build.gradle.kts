/*
 *   Copyright (c) 2025 Cofinity-X
 *   Copyright (c) 2025 LKS NEXT
 *   Copyright (c) 2025 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
 *   Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 *   See the NOTICE file(s) distributed with this work for additional
 *   information regarding copyright ownership.
 *
 *   This program and the accompanying materials are made available under the
 *   terms of the Apache License, Version 2.0 which is available at
 *   https://www.apache.org/licenses/LICENSE-2.0.
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *   WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *   License for the specific language governing permissions and limitations
 *   under the License.
 *
 *   SPDX-License-Identifier: Apache-2.0
 *
 */

plugins {
    `java-library`
    id("application")
    alias(libs.plugins.shadow)
}

dependencies {

    val edcVersion = "0.15.1"

    implementation("org.eclipse.edc:connector-core:$edcVersion")
    implementation("org.eclipse.edc:monitor-jdk-logger:$edcVersion")
    implementation("org.eclipse.edc:issuerservice-bom:$edcVersion")
    implementation("org.eclipse.edc:issuerservice-feature-sql-bom:$edcVersion")
    implementation("org.eclipse.edc:participantcontext-config-store-sql:$edcVersion")
    implementation("org.eclipse.edc:issuerservice-issuance:$edcVersion")
    implementation("org.eclipse.edc:token-lib:$edcVersion")
    implementation("org.eclipse.edc:identity-hub-spi:$edcVersion")

    implementation(project(":extensions:store:fsvault"))

    // This module was removed in upstream EDC version 0.15.0, therefore using legacy version of it
    implementation("org.eclipse.edc:issuerservice-presentation-attestations:0.14.1")

    runtimeOnly(libs.postgres)

    runtimeOnly("org.slf4j:slf4j-simple:2.0.17")

    implementation("com.nimbusds:nimbus-jose-jwt:10.7")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.83")
    implementation("com.google.crypto.tink:tink:1.20.0")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()
    archiveFileName.set("${project.name}.jar")
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}
