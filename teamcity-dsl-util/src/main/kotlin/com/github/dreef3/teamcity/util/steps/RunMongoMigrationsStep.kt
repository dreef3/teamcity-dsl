package com.github.dreef3.teamcity.util.steps

import com.github.dreef3.teamcity.util.snippets.SKIP_STEP_CHECK
import jetbrains.buildServer.configs.kotlin.v2017_2.BuildSteps
import jetbrains.buildServer.configs.kotlin.v2017_2.buildSteps.ScriptBuildStep
import com.github.dreef3.teamcity.util.snippets.SHEBANG

open class RunMongoMigrationsStep(init: RunMongoMigrationsStep.() -> Unit = {}) : ScriptBuildStep() {

    var dockerImageName = ""
    var dockerImageTag = ""
    var databaseHost = ""
    var databasePort = ""
    var databaseName = ""
    var dbUserName = ""
    var dbPassword = ""
    var replicaset = ""
    var skip = false

    init {
        init()
        name = "Run mongo migrations"
        scriptContent = """
        $SHEBANG

        ${SKIP_STEP_CHECK(skip)}

        DOCKER_IMAGE_TAG=${'$'}{DOCKER_IMAGE_TAG:-$dockerImageTag}
        DOCKER_IMAGE_NAME=${'$'}{DOCKER_IMAGE_NAME:-$dockerImageName}
        TAG="${'$'}DOCKER_REGISTRY/${'$'}DOCKER_IMAGE_NAME:${'$'}DOCKER_IMAGE_TAG"
        echo "Docker image tag: ${'$'}TAG"

        replica_set_name=$replicaset
        mongo_replicaset=""
        if [[ -n ${'$'}replica_set_name ]]; then
            mongo_replicaset="-e REPLICASET=$replicaset"
        fi

        db_user=$dbUserName
        db_auth_creds=""
        if [[ -n ${'$'}db_user ]]; then
            db_auth_creds="-e DB__USER=$dbUserName -e DB__PASSWORD=$dbPassword"
        fi

        DB_PROVIDED=$databaseName
        DB_NAME=${'$'}{DB_PROVIDED:-${'$'}{DOCKER_IMAGE_TAG}}

        docker pull ${'$'}TAG

        docker run --network host -e DB__HOST=$databaseHost -e DB__PORT=$databasePort -e DB__DATABASE=${'$'}{DB_NAME} ${'$'}{db_auth_creds} ${'$'}{mongo_replicaset} -t ${'$'}TAG

        """.trimIndent()
    }
}

fun BuildSteps.runMongoMigrations(init: RunMongoMigrationsStep.() -> Unit = {}) {
    step(RunMongoMigrationsStep(init))
}
