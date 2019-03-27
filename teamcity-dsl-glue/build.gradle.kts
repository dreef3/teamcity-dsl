repositories {
    maven {
        url = uri("http://download.jetbrains.com/teamcity-repository")
    }
}

version = rootProject.property("version")!!
group = "com.github.dreef3.teamcity"

val dslVersion = "2017.2"
val junitVersion = "5.3.1"

dependencies {
    implementation(project(":teamcity-dsl-core"))
    implementation(project(":teamcity-dsl-util"))

    implementation("org.jetbrains.kotlin:kotlin-script-util:1.2.61")
    implementation("org.jetbrains.kotlin:kotlin-script-runtime:1.2.61")
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.2.61")

    implementation("org.jetbrains.teamcity:configs-dsl-kotlin:$dslVersion")
    implementation("org.jetbrains.teamcity:configs-dsl-kotlin-bundled:1.0-SNAPSHOT")
    implementation("org.jetbrains.teamcity:configs-dsl-kotlin-commandLineRunner:1.0-SNAPSHOT")
    implementation("org.jetbrains.teamcity:configs-dsl-kotlin-commit-status-publisher:1.0-SNAPSHOT")
    implementation("org.jetbrains.teamcity:configs-dsl-kotlin-jetbrains.git:1.0-SNAPSHOT")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.assertj:assertj-core:3.11.1")
    testImplementation("org.testcontainers:testcontainers:1.10.2")
    testRuntime("ch.qos.logback:logback-classic:1.2.3")
    testImplementation("com.github.kittinunf.fuel:fuel:1.15.1")
    testImplementation("com.github.kittinunf.fuel:fuel-coroutines:1.15.1")
    testImplementation("com.github.kittinunf.fuel:fuel-jackson:1.15.1")
}
