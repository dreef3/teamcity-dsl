repositories {
    maven {
        url = uri("http://download.jetbrains.com/teamcity-repository")
    }
}

version = rootProject.property("version")!!
group = "com.github.dreef3.teamcity"

val dslVersion = "2017.2"

dependencies {
    implementation("org.jetbrains.teamcity:configs-dsl-kotlin:$dslVersion")
    implementation("org.jetbrains.teamcity:configs-dsl-kotlin-bundled:1.0-SNAPSHOT")
    implementation("org.jetbrains.teamcity:configs-dsl-kotlin-commandLineRunner:1.0-SNAPSHOT")
    implementation("org.jetbrains.teamcity:configs-dsl-kotlin-commit-status-publisher:1.0-SNAPSHOT")
    implementation("org.jetbrains.teamcity:configs-dsl-kotlin-jetbrains.git:1.0-SNAPSHOT")
}
