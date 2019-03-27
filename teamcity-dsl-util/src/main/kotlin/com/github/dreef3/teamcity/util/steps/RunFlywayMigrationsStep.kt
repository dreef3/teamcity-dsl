package com.github.dreef3.teamcity.util.steps

import com.github.dreef3.teamcity.util.snippets.SKIP_STEP_CHECK
import jetbrains.buildServer.configs.kotlin.v2017_2.BuildSteps
import jetbrains.buildServer.configs.kotlin.v2017_2.buildSteps.ScriptBuildStep
import com.github.dreef3.teamcity.util.snippets.SHEBANG

open class RunFlywayMigrationsStep(init: RunFlywayMigrationsStep.() -> Unit = {}) : ScriptBuildStep() {

    var dockerImageName = ""
    var dockerImageTag = ""
    var userName = ""
    var userPassword = ""
    var databaseHost = ""
    var databasePort = ""
    var databaseName = ""
    var databaseSchema = ""
    var skip = false

    init {
        init()
        name = "Run flyway migrations"
        scriptContent = """
        $SHEBANG

        ${SKIP_STEP_CHECK(skip)}

        DOCKER_IMAGE_TAG=${'$'}{DOCKER_IMAGE_TAG:-$dockerImageTag}
        DOCKER_IMAGE_NAME=${'$'}{DOCKER_IMAGE_NAME:-$dockerImageName}
        TAG="${'$'}DOCKER_REGISTRY/${'$'}DOCKER_IMAGE_NAME:${'$'}DOCKER_IMAGE_TAG"
        echo "Docker image tag: ${'$'}TAG"

        flyway_schemas=""
        SCHEMA_PROVIDED=$databaseSchema
        db_schema=${'$'}{SCHEMA_PROVIDED:-${'$'}DB_SCHEMA}
        if [[ -n ${'$'}db_schema ]]; then
            flyway_schemas="-e FLYWAY_SCHEMAS=$databaseSchema"
        fi

        docker pull ${'$'}TAG

        docker run --network host -e FLYWAY_USER=$userName -e FLYWAY_PASSWORD=$userPassword -e FLYWAY_URL="jdbc:postgresql://$databaseHost:$databasePort/$databaseName" ${'$'}flyway_schemas -t ${'$'}TAG migrate

        """.trimIndent()
    }
}

fun BuildSteps.runFlywayMigrations(init: RunFlywayMigrationsStep.() -> Unit = {}) {
    step(RunFlywayMigrationsStep(init))
}
