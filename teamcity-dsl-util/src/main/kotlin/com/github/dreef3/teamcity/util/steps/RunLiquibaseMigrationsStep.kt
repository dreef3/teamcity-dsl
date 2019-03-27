package com.github.dreef3.teamcity.util.steps

import jetbrains.buildServer.configs.kotlin.v2017_2.BuildSteps
import jetbrains.buildServer.configs.kotlin.v2017_2.buildSteps.ScriptBuildStep
import com.github.dreef3.teamcity.util.snippets.SHEBANG
import com.github.dreef3.teamcity.util.snippets.SKIP_STEP_CHECK

open class RunLiquibaseMigrationsStep(init: RunLiquibaseMigrationsStep.() -> Unit = {}) : ScriptBuildStep() {

    var dockerImageName = ""
    var dockerImageTag = ""
    var userName = ""
    var userPassword = ""
    var databaseHost = ""
    var databasePort = ""
    var databaseName = ""
    var skip = false

    init {
        init()
        name = "Run liquibase migrations"
        scriptContent = """
        $SHEBANG

        ${SKIP_STEP_CHECK(skip)}

        DOCKER_IMAGE_TAG=${'$'}{DOCKER_IMAGE_TAG:-$dockerImageTag}
        DOCKER_IMAGE_NAME=${'$'}{DOCKER_IMAGE_NAME:-$dockerImageName}
        TAG="${'$'}DOCKER_REGISTRY/${'$'}DOCKER_IMAGE_NAME:${'$'}DOCKER_IMAGE_TAG"
        echo "Docker image tag: ${'$'}TAG"

        docker pull ${'$'}TAG

        docker run --rm --network host \
        -e LIQUIBASE_HOST=$databaseHost \
        -e LIQUIBASE_PORT=$databasePort \
        -e LIQUIBASE_USERNAME=$userName \
        -e LIQUIBASE_PASSWORD=$userPassword \
        -e LIQUIBASE_DATABASE=$databaseName \
        ${'$'}TAG liquibase updateTestingRollback
        """.trimIndent()
    }
}

fun BuildSteps.runLiquibaseMigrations(init: RunLiquibaseMigrationsStep.() -> Unit = {}) {
    step(RunLiquibaseMigrationsStep(init))
}
