package com.github.dreef3.teamcity.dsl.impl

import com.sun.net.httpserver.HttpServer
import jetbrains.buildServer.configs.kotlin.v2017_2.Project
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngineFactory
import org.junit.jupiter.api.Test
import com.github.dreef3.dsl.Manifest
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.Paths
import javax.script.ScriptEngineManager

class ManifestTest {
    @Test
    internal fun `can read manifest file`() {
        val manager = ScriptEngineManager()

        manager.registerEngineExtension("kts", KotlinJsr223JvmLocalScriptEngineFactory())

        with(manager.getEngineByExtension("kts")) {
            val path = Paths.get("src/main/kotlin/com/github/dreef3/teamcity/dsl/impl/manifest.kts")
            val result = eval(Files.newBufferedReader(path))

            assertThat(result).isInstanceOf(Manifest::class.java)

            val manifest = result as Manifest
            val service = manifest.toService()

            assertThat(service.name).isEqualTo("sla-bot")

            val project = Project()

            project.apply {
                subProject(manifest.toProject(service, this))
            }

            assertThat(project.subProjects).hasSize(1)
        }
    }

    @Test
    internal fun `can read manifest from URL`() {
        val server = HttpServer.create(InetSocketAddress(8081), 0)
        server.createContext("/") {
            val path = Paths.get("src/main/kotlin/com/github/dreef3/teamcity/dsl/impl/manifest.kts")

            it.sendResponseHeaders(200, 0)
            it.responseBody.write(Files.readAllBytes(path))
            it.responseBody.close()
        }
        server.start()

        val manifest = Manifest.fromUrl("http://localhost:8081/manifest.kts")

        assertThat(manifest).isNotNull
    }
}
