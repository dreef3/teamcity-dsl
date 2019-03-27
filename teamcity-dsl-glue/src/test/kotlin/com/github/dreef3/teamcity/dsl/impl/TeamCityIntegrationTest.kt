package com.github.dreef3.teamcity.dsl.impl

import awaitStringResponse
import com.github.kittinunf.fuel.httpPost
import kotlinx.coroutines.experimental.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.File
import java.time.Duration

class KDockerComposeContainer(files: MutableList<File>)
    : DockerComposeContainer<KDockerComposeContainer>(files)

class TeamCityIntegrationTest {
    val login = "test"
    val password = "test"

    companion object {
        val log = LoggerFactory.getLogger(TeamCityIntegrationTest::class.java)

        val env = KDockerComposeContainer(arrayListOf(File("src/test/resources/docker-compose.yml")))
            .withLocalCompose(true)
            .withPull(false)
            .withExposedService("server", 8111, Wait.forHttp("/").withStartupTimeout(Duration.ofMinutes(5)))
            .withTailChildContainers(true)
    }

    @BeforeEach
    internal fun setUp() {
        env.start()
    }

    @AfterEach
    internal fun tearDown() {
        env.stop()
    }

    @Test
    internal fun `can create project with versioned settings`() {
        val teamcityUrl = "http://${env.getServiceHost("server", 8111)}:${env.getServicePort("server", 8111)}"
        assertThat(teamcityUrl).isNotEmpty()

        runBlocking {
            val (req, res) = "$teamcityUrl/httpAuth/app/rest/projects".httpPost().body("Test")
                .header(mapOf(
                    "Content-Type" to "text/plain",
                    "Accept" to "application/json"
                ))
                .authenticate(login, password)
                .awaitStringResponse()

            log.info(req.toString())
            log.info(res.toString())
        }
    }
}
