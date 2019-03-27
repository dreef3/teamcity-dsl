import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "com.github.dreef3.teamcity"

val junitVersion = "5.3.1"

buildscript {
    repositories {
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
        jcenter()
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.2.61")
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        mavenLocal()
        jcenter()
    }

    dependencies {
        "implementation"(kotlin("stdlib"))

        "testImplementation"("org.junit.jupiter:junit-jupiter-api:$junitVersion")
        "testImplementation"("org.junit.jupiter:junit-jupiter-params:$junitVersion")
        "testRuntime"("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
        "testImplementation"("org.assertj:assertj-core:3.11.1")
    }

    val sourcesJar by tasks.registering(Jar::class) {
        classifier = "sources"
        from(sourceSets["main"].allSource)
    }

    publishing {
        repositories {
            maven {
                url = uri(rootProject.property("maven.url") ?: "")
                credentials {
                    username = if (rootProject.hasProperty("maven.username"))
                        rootProject.property("maven.username") as String
                    else ""
                    password = if (rootProject.hasProperty("maven.password"))
                        rootProject.property("maven.password") as String
                    else ""
                }
            }
        }
        publications {
            register("mavenJava", MavenPublication::class) {
                from(components["java"])
                artifact(sourcesJar.get())
            }
        }
    }

    tasks.withType<KotlinCompile>().all {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}

fun Project.publishing(action: PublishingExtension.() -> Unit) =
    configure(action)

val Project.sourceSets
    get() = the<SourceSetContainer>()

val SourceSetContainer.main
    get() = named("main")
