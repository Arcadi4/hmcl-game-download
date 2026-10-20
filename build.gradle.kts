plugins {
    java
}

group = "io.github.arcadi4.hmcl"
version = "1.0.0"

repositories {
    mavenCentral()
}

sourceSets {
    // The probe deliberately lives outside the test source set: it must load
    // RegionUtils from the HMCL jar, not the test fixture with the same name.
    create("integrationTest")
}

dependencies {
    // ASM is embedded into the agent jar so the agent is self-contained.
    implementation("org.ow2.asm:asm:9.8")
    implementation("org.ow2.asm:asm-tree:9.8")

    // JetBrains nullability annotations are compile-time only.
    compileOnly("org.jetbrains:annotations:26.1.0")
    testCompileOnly("org.jetbrains:annotations:26.1.0")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    add("integrationTestCompileOnly", "org.jetbrains:annotations:26.1.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    archiveFileName.set("hmcl-download-agent.jar")

    from({ configurations.runtimeClasspath.get().map { zipTree(it) } })
    from("THIRD-PARTY-NOTICES.md")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    exclude("META-INF/MANIFEST.MF", "module-info.class", "META-INF/versions/**/module-info.class")

    manifest {
        attributes(
            "Premain-Class" to "io.github.arcadi4.hmcl.agent.HmclDownloadAgent",
            "Can-Redefine-Classes" to "false",
            "Can-Retransform-Classes" to "false",
            "Implementation-Title" to "HMCL Download Agent",
            "Implementation-Version" to project.version,
        )
    }
}

// The HMCL jar to probe is supplied with -PhmclJar=/path/to/HMCL.jar.
val hmclJar = providers.gradleProperty("hmclJar").map { file(it) }
val agentJar = tasks.jar.flatMap { it.archiveFile }

tasks.register<JavaExec>("probeControl") {
    group = "verification"
    description = "Runs the region probe against HMCL without the agent."

    val hmcl = hmclJar
    doFirst {
        if (!hmcl.isPresent) {
            throw GradleException("Pass -PhmclJar=/path/to/HMCL.jar")
        }
    }

    classpath = sourceSets["integrationTest"].runtimeClasspath + files(hmcl)
    mainClass.set("io.github.arcadi4.hmcl.agent.RegionProbe")
    jvmArgs("-Duser.timezone=Asia/Shanghai")
}

tasks.register<JavaExec>("probePatched") {
    group = "verification"
    description = "Runs the region probe against HMCL with the agent attached."
    dependsOn(tasks.jar)

    val hmcl = hmclJar
    val agent = agentJar
    doFirst {
        if (!hmcl.isPresent) {
            throw GradleException("Pass -PhmclJar=/path/to/HMCL.jar")
        }
        jvmArgs("-javaagent:${agent.get().asFile.absolutePath}")
    }

    classpath = sourceSets["integrationTest"].runtimeClasspath + files(hmcl)
    mainClass.set("io.github.arcadi4.hmcl.agent.RegionProbe")
    jvmArgs("-Duser.timezone=Asia/Shanghai")
}
