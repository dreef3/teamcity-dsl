package com.github.dreef3.teamcity.util.steps

import com.github.dreef3.teamcity.util.snippets.SKIP_STEP_CHECK
import jetbrains.buildServer.configs.kotlin.v2017_2.BuildSteps
import jetbrains.buildServer.configs.kotlin.v2017_2.buildSteps.ScriptBuildStep
import com.github.dreef3.teamcity.util.snippets.SHEBANG

open class CreatePostgresFeatureDB(init: CreatePostgresFeatureDB.() -> Unit = {}) : ScriptBuildStep() {

    var userName = ""
    var userPass = ""
    var dbName = ""
    var dbHost = ""
    var dbPort = "5432"
    var dbTemplate = ""
    var skip = false
    var postgresVersion = "9.6"

    init {
        init()
        name = "Create new PG database for feature deploy"
        scriptContent = """
        $SHEBANG

        set +e
        docker rm -f postgres_helper 2> /dev/null
        set -e

        DB_PROVIDED=$dbName
        DB_NAME=${'$'}{DB_PROVIDED:-${'$'}{SERVICE_VARIANT:-${'$'}{DOCKER_IMAGE_TAG}}}

        if [[ -z ${'$'}{DB_NAME} ]]; then
            echo "Can't detect the database name. Check the env.SERVICE_VARIANT variable"
            exit 1
        fi

        echo "##teamcity[setParameter name='env.DB_NAME' value='${'$'}{DB_NAME}']"

        ${SKIP_STEP_CHECK(skip)}

        HOST_PROVIDED=$dbHost
        HOST=${'$'}{HOST_PROVIDED:-${'$'}{DB_HOST}}

        PORT_PROVIDED=$dbPort
        PORT=${'$'}{PORT_PROVIDED:-${'$'}{DB_PORT}}

        USER_PROVIDED=$userName
        USER_NAME=${'$'}{USER_PROVIDED:-${'$'}{DB_USER}}

        PASSWORD_PROVIDED=$userPass
        PG_PASSWORD=${'$'}{PASSWORD_PROVIDED:-${'$'}{DB_PASSWORD}}

        DB_TEMPLATE=""
        if [[ -n "$dbTemplate" ]]; then
            DB_TEMPLATE="--template $dbTemplate"
        fi

        ARGS="-h ${'$'}{HOST} -p ${'$'}{PORT} -U ${'$'}{USER_NAME}"

        docker run --network host --entrypoint /bin/sh -e PGPASSWORD=${'$'}{PG_PASSWORD} --name postgres_helper -td postgres:$postgresVersion

        set +e
        docker exec postgres_helper psql -l ${'$'}ARGS ${'$'}{DB_NAME}
        if [[ ${'$'}? -eq 0 ]]; then
            docker exec postgres_helper psql ${'$'}{ARGS} -d ${'$'}{DB_NAME} -c "SELECT pg_terminate_backend(pg_stat_activity.pid) FROM pg_stat_activity WHERE pg_stat_activity.datname = '${'$'}{DB_NAME}' AND pid <> pg_backend_pid();"
        fi
        set -e

        docker exec postgres_helper dropdb --if-exists ${'$'}{ARGS} ${'$'}{DB_NAME}
        docker exec postgres_helper createdb ${'$'}{ARGS} ${'$'}{DB_TEMPLATE} -O ${'$'}{USER_NAME} ${'$'}{DB_NAME}

        docker rm -f postgres_helper
        """.trimIndent()
    }
}

fun BuildSteps.createPostgresFeatureDB(init: CreatePostgresFeatureDB.() -> Unit = {}) {
    step(CreatePostgresFeatureDB(init))
}
