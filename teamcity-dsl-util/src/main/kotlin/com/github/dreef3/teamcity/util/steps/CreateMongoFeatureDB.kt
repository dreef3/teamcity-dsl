package com.github.dreef3.teamcity.util.steps

import com.github.dreef3.teamcity.util.snippets.SKIP_STEP_CHECK
import jetbrains.buildServer.configs.kotlin.v2017_2.BuildSteps
import jetbrains.buildServer.configs.kotlin.v2017_2.buildSteps.ScriptBuildStep
import com.github.dreef3.teamcity.util.snippets.SHEBANG

open class CreateMongoFeatureDB(init: CreateMongoFeatureDB.() -> Unit = {}) : ScriptBuildStep() {

    var dbName = ""
    var prodDbName = "raven_metadata"
    var skip = false

    init {
        init()
        name = "Create new mongo database for feature deploy"
        scriptContent = """
        $SHEBANG

        ${SKIP_STEP_CHECK(skip)}

        DB_PROVIDED=$dbName
        DB_NAME=${'$'}{DB_PROVIDED:-${'$'}{DOCKER_IMAGE_TAG}}

        if [[ -z ${'$'}{DB_NAME} ]]; then
            echo "Can't detect the database name. Check the env.DOCKER_IMAGE_TAG variable"
            exit 1
        fi

        container_id=`docker run --entrypoint /bin/sh --network host -td mongo:3.6`
        trap "set +e; docker rm -f ${'$'}{container_id} &> /dev/null" EXIT
        docker exec ${'$'}{container_id} mongodump --host %env.MONGO_DB_HOST% --db $prodDbName --out /tmp/mongo_dump

        docker exec ${'$'}{container_id} mongo %env.MONGO_DB_HOST%/${'$'}{DB_NAME} --eval "db.dropDatabase()"

        for i in ${'$'}(docker exec ${'$'}{container_id} /bin/bash -c "ls /tmp/mongo_dump/$prodDbName/*bson"); do
            docker exec ${'$'}{container_id} mongorestore --drop -h %env.MONGO_DB_HOST% --db ${'$'}{DB_NAME} --collection ${'$'}(echo ${'$'}(basename ${'$'}i) | sed 's/\.bson//') ${'$'}i
        done

        echo "##teamcity[setParameter name='env.MONGO_DB_NAME' value='${'$'}{DB_NAME}']"

        """.trimIndent()
    }
}

fun BuildSteps.createMongoFeatureDB(init: CreateMongoFeatureDB.() -> Unit = {}) {
    step(CreateMongoFeatureDB(init))
}
